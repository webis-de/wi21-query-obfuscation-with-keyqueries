package de.webis.keyqueries.anserini;

import io.anserini.index.IndexArgs;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.search.SearchArgs;
import io.anserini.analysis.AnalyzerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.anserini.search.SearchCollection.BREAK_SCORE_TIES_BY_DOCID;
import static io.anserini.search.SearchCollection.BREAK_SCORE_TIES_BY_TWEETID;
import static de.webis.keyqueries.util.Util.analyzer;
import static de.webis.keyqueries.util.Util.uniqueInt;
import static de.webis.keyqueries.util.Util.uniqueFloat;

class BM25PrfSimilarity extends BM25Similarity {

  BM25PrfSimilarity(float k1, float b) {
    super(k1, b);
  }

  @Override
  // idf is not needed in BM25PRF
  protected float idf(long docFreq, long docCount) {
    return 1;
  }
}

public class PrfRelevanceFeedbackReranker<T> implements Reranker<T>, RelevanceFeedbackAware<T> {
  private static final Logger LOG = LogManager.getLogger(PrfRelevanceFeedbackReranker.class);
  private Set<String> relevantdocs;
  private final int fbDocs;
  private final Analyzer analyzer;
  private final String field;
  private final boolean outputQuery;
  private final int fbTerms;
  private final float k1;
  private final float b;
  private final float newTermWeight;

  public PrfRelevanceFeedbackReranker(SearchArgs args) {
    this(
      analyzer(args), IndexArgs.CONTENTS,
      uniqueInt(args.bm25prf_fbTerms), uniqueInt(args.bm25prf_fbDocs),
      uniqueFloat(args.bm25prf_k1), uniqueFloat(args.bm25prf_b), 
      uniqueFloat(args.bm25prf_newTermWeight), args.bm25prf_outputQuery
    );
  }
  
  public PrfRelevanceFeedbackReranker(Analyzer analyzer, String field, int fbTerms, int fbDocs, float k1, float b, float newTermWeight, boolean outputQuery) {
    this.analyzer = analyzer;
    this.outputQuery = outputQuery;
    this.field = field;
    this.fbTerms = fbTerms;
    this.fbDocs = fbDocs;
    this.k1 = k1;
    this.b = b;
    this.newTermWeight = newTermWeight;
  }

  @Override
  public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {

    // set similarity to BM25PRF
    IndexSearcher searcher = context.getIndexSearcher();
    BM25Similarity originalSimilarity = (BM25Similarity) searcher.getSimilarity();
    searcher.setSimilarity(new BM25PrfSimilarity(k1, b));
    IndexReader reader = searcher.getIndexReader();
    List<String> originalQueryTerms = AnalyzerUtils.analyze(analyzer, context.getQueryText());

    boolean useRf = (context.getSearchArgs().rf_qrels != null);
    PrfFeatures fv = expandQuery(originalQueryTerms, docs, reader, useRf);
    Query newQuery = fv.toQuery();

    if (this.outputQuery) {
      LOG.info("QID: " + context.getQueryId());
      LOG.info("Original Query: " + context.getQuery().toString(this.field));
      LOG.info("Running new query: " + newQuery.toString(this.field));
      LOG.info("Features: " + fv.toString());
    }

    TopDocs rs;

    try {
      // Figure out how to break the scoring ties.
      if (context.getSearchArgs().arbitraryScoreTieBreak) {
        rs = searcher.search(newQuery, context.getSearchArgs().hits);
      } else if (context.getSearchArgs().searchtweets) {
        rs = searcher.search(newQuery, context.getSearchArgs().hits, BREAK_SCORE_TIES_BY_TWEETID, true);
      } else {
        rs = searcher.search(newQuery, context.getSearchArgs().hits, BREAK_SCORE_TIES_BY_DOCID, true);
      }
    } catch (IOException e) {
      e.printStackTrace();
      return docs;
    }
    // set similarity back
    searcher.setSimilarity(originalSimilarity);
    return ScoredDocuments.fromTopDocs(rs, searcher);
  }


  private PrfFeatures expandQuery(List<String> originalTerms, ScoredDocuments docs, IndexReader reader, boolean useRf) {
    return expandQuery(originalTerms, docs, reader, useRf, null);
  }
  
  private PrfFeatures expandQuery(List<String> originalTerms, ScoredDocuments docs, IndexReader reader, boolean useRf, Set<String> allowedTerms) {
    PrfFeatures newFeatures = new PrfFeatures();

    Set<String> vocab = new HashSet<>();

    Map<Integer, Set<String>> docToTermsMap = new HashMap<>();
    int numDocs = reader.numDocs();
    for (int i = 0; i < docs.documents.length; i++) {
    	String id = docs.documents[i].get(IndexArgs.ID);
    	if(relevantdocs.contains(id)) {
    		try {
    	        Terms terms = reader.getTermVector(docs.ids[i], field);
    	        Set<String> termsStr = getTermsStr(terms);
    	        docToTermsMap.put(docs.ids[i], termsStr);
    	        vocab.addAll(termsStr);
    	      } catch (IOException e) {
    	        e.printStackTrace();
    	      }
    	}
    }

    int numRelDocs = docToTermsMap.size();

    Set<String> originalTermsSet = new HashSet<>(originalTerms);

    // Add New Terms
    for (String term : vocab) {
      if (originalTermsSet.contains(term)) continue;
      if (term.length() < 2 || term.length() > 20) continue;
      if (!term.matches("[a-z0-9]+")) continue;
      if (term.matches("[0-9]+")) continue;
      if (allowedTerms != null && !allowedTerms.isEmpty() && !allowedTerms.contains(term)) continue;
      
      try {
        int df = reader.docFreq(new Term(IndexArgs.CONTENTS, term));
        int dfRel = 0;

        for (Map.Entry<Integer, Set<String>> entry : docToTermsMap.entrySet()) {
          Set<String> terms = entry.getValue();
          if (terms.contains(term)) {
            dfRel++;
          }
        }

        if (dfRel < 2) {
          continue;
        }
        newFeatures.addFeature(term, df, dfRel, numDocs, numRelDocs, newTermWeight);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    newFeatures.pruneToSize(fbTerms);

    for (String term : originalTerms) {
      try {
        int df = reader.docFreq(new Term(IndexArgs.CONTENTS, term));
        int dfRel = 0;

        for (Map.Entry<Integer, Set<String>> entry : docToTermsMap.entrySet()) {
          Set<String> terms = entry.getValue();
          if (terms.contains(term)) {
            dfRel++;
          }
        }
        newFeatures.addFeature(term, df, dfRel, numDocs, numRelDocs);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return newFeatures;
  }

  @Override
  public String tag() {
    return "PRFRelevanceFeedbackReranker(fbDocs=" + fbDocs + ",fbTerms=" + fbTerms + ",k1=" + k1 + ",b=" + b + ",newTermWeight=" + newTermWeight;
  }

  private Set<String> getTermsStr(Terms terms) {
    Set<String> termsStr = new HashSet<>();

    try {
      TermsEnum termsEnum = terms.iterator();

      BytesRef text;
      while ((text = termsEnum.next()) != null) {
        String term = text.utf8ToString();
        termsStr.add(term);

      }
    } catch (Exception e) {
      e.printStackTrace();
      // Return empty feature vector
      return termsStr;
    }

    return termsStr;
  }

  class PrfFeature {
    int df;
    int dfRel;
    int numDocs;
    int numDocsRel;
    float weight;


    PrfFeature(int df, int dfRel, int numDocs, int numDocsRel, float weight) {
      this.df = df;
      this.dfRel = dfRel;
      this.numDocs = numDocs;
      this.numDocsRel = numDocsRel;
      this.weight = weight;
    }

    double getRelWeight() {
      double rw = Math.log((dfRel + 0.5D) * (numDocs - df - numDocsRel + dfRel + 0.5D) /
          ((df - dfRel + 0.5D) * (numDocsRel - dfRel + 0.5D))) * weight;
      if(Double.isNaN(rw)) {
    	  System.out.println("df: " +df);
    	  System.out.println("dfRel: " +dfRel);
    	  System.out.println("numDocs: " +numDocs);
    	  System.out.println("numDocsRel: " +numDocsRel);
    	  System.out.println("weight: " +weight);
      }
      return Math.max(rw, 1e-6);
    }

    double getOfferWeight() {
      // we apply log to dfRel according to
      // Sakai and Robertson (SIGIR 2002)
      return getRelWeight() * Math.log(Math.max(dfRel, 1e-6));
    }


    @Override
    public String toString() {
      return String.format("%d, %d, %d, %d, %f, %f, %f", df, dfRel, numDocs, numDocsRel, weight, getRelWeight(), getOfferWeight());
    }
  }

  class PrfFeatures {
    private HashMap<String, PrfFeature> features;

    PrfFeatures() {
      this.features = new HashMap<>();
    }

    void addFeature(String term, int df, int dfRel, int numDocs, int numDocsRel, float weight) {
      features.put(term, new PrfFeature(df, dfRel, numDocs, numDocsRel, weight));
    }


    void addFeature(String term, int df, int dfRel, int numDocs, int numDocsRel) {
      addFeature(term, df, dfRel, numDocs, numDocsRel, 1.0f);
    }


    public Query toQuery() {
      BooleanQuery.Builder feedbackQueryBuilder = new BooleanQuery.Builder();

      for (Map.Entry<String, PrfFeature> f : features.entrySet()) {
        String term = f.getKey();
        float rw = (float) f.getValue().getRelWeight();
        feedbackQueryBuilder.add(new BoostQuery(new TermQuery(new Term(field, term)), rw), BooleanClause.Occur.SHOULD);
      }
      return feedbackQueryBuilder.build();
    }


    private List<KeyValuePair> getOrderedFeatures() {
      List<KeyValuePair> kvpList = new ArrayList<KeyValuePair>(features.size());
      for (String feature : features.keySet()) {
        PrfFeature value = features.get(feature);
        KeyValuePair keyValuePair = new KeyValuePair(feature, value);
        kvpList.add(keyValuePair);
      }

      Collections.sort(kvpList, new Comparator<KeyValuePair>() {
        public int compare(KeyValuePair x, KeyValuePair y) {
          double xVal = x.getValue();
          double yVal = y.getValue();

          return (Double.compare(yVal, xVal));
        }
      });

      return kvpList;
    }


    PrfFeatures pruneToSize(int k) {
      List<KeyValuePair> pairs = getOrderedFeatures();
      HashMap<String, PrfFeature> pruned = new HashMap<>();

      for (KeyValuePair pair : pairs) {
        if (pruned.size() >= k) {
          break;
        }
        pruned.put(pair.getKey(), pair.getFeature());
      }

      this.features = pruned;
      return this;
    }

    @Override
    public String toString() {
      List<String> strBuilder = new ArrayList<String>();
      List<KeyValuePair> pairs = getOrderedFeatures();

      for (KeyValuePair pair : pairs) {
        strBuilder.add(pair.getKey() + "," + pair.getFeature());
      }

      return String.join("||", strBuilder);
    }

    private class KeyValuePair {
      private String key;
      private PrfFeature value;

      public KeyValuePair(String key, PrfFeature value) {
        this.key = key;
        this.value = value;
      }

      public String getKey() {
        return key;
      }

      @Override
      public String toString() {
        return value + "\t" + key;
      }

      public float getValue() {
        return (float) value.getOfferWeight();
      }


      public PrfFeature getFeature() {
        return value;
      }
    }
  }

@Override
public void setRelevanceFeedback(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
	this.relevantdocs = new HashSet<>();
	if(relevanceFeedback == null) {
		return;
	}
	for(String doc: relevanceFeedback.keySet()) {
		if(relevanceFeedback.get(doc) > 0) {
			relevantdocs.add(doc);
		}
	}
	
}

public Map<String, Float> getTermsAndWeights(ScoredDocuments docs, RerankerContext<T> context) {
	return getTermsAndWeights(docs, context, null);
}
public Map<String, Float> getTermsAndWeights(ScoredDocuments docs, RerankerContext<T> context, Set<String> allowedTerms) {
	Map<String, Float> terms = new HashMap<String, Float>();
	IndexSearcher searcher = context.getIndexSearcher();
	searcher.setSimilarity(new BM25PrfSimilarity(k1, b));
	IndexReader reader = searcher.getIndexReader();
	List<String> originalQueryTerms = AnalyzerUtils.analyze(analyzer, context.getQueryText());

	boolean useRf = (context.getSearchArgs().rf_qrels != null);
	PrfFeatures fv = expandQuery(originalQueryTerms, docs, reader, useRf, allowedTerms);
	for (Map.Entry<String, PrfFeature> f : fv.features.entrySet()) {
	  String term = f.getKey();
	  float rw = (float) f.getValue().getRelWeight();
	  terms.put(term, rw);
	}
    return terms;
}

}
