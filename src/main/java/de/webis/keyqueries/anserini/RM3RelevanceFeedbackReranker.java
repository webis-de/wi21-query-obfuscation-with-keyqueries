package de.webis.keyqueries.anserini;

import io.anserini.index.IndexArgs;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.search.SearchArgs;
import io.anserini.analysis.AnalyzerUtils;
import io.anserini.util.FeatureVector;
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
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;

import static io.anserini.search.SearchCollection.BREAK_SCORE_TIES_BY_DOCID;
import static io.anserini.search.SearchCollection.BREAK_SCORE_TIES_BY_TWEETID;

import static de.webis.keyqueries.util.Util.analyzer;
import static de.webis.keyqueries.util.Util.uniqueInt;
import static de.webis.keyqueries.util.Util.uniqueFloat;

public class RM3RelevanceFeedbackReranker<T> implements Reranker<T>, RelevanceFeedbackAware<T> {
 private static final Logger LOG = LogManager.getLogger(RM3RelevanceFeedbackReranker.class);
 private Set<String> relevantdocs;
 private final Analyzer analyzer;
 private final String field;

 private final int fbTerms;
 private final int fbDocs;
 private final float originalQueryWeight;
 private final boolean outputQuery;

 public RM3RelevanceFeedbackReranker(SearchArgs args) {
   this(
     analyzer(args), IndexArgs.CONTENTS,
     uniqueInt(args.rm3_fbTerms), uniqueInt(args.rm3_fbDocs),
     uniqueFloat(args.rm3_originalQueryWeight), args.rm3_outputQuery
   );
 }
 
 public RM3RelevanceFeedbackReranker(Analyzer analyzer, String field, int fbTerms, int fbDocs, float originalQueryWeight, boolean outputQuery) {
   this.analyzer = analyzer;
   this.field = field;
   this.fbTerms = fbTerms;
   this.fbDocs = fbDocs;
   this.originalQueryWeight = originalQueryWeight;
   this.outputQuery = outputQuery;
 }
 
 public Query feedbackQuery(Query query, RerankerContext<T> context) {
	    try {
	      IndexSearcher searcher = context.getIndexSearcher();
	      TopDocs rs = searcher.search(query, 100);

	      return feedbackQuery(ScoredDocuments.fromTopDocs(rs, searcher), context);	
	    } catch (IOException e) {
	      throw new RuntimeException(e);
	    }
	  }
	  
	  public Query feedbackQuery(ScoredDocuments docs, RerankerContext<T> context) {
	    IndexSearcher searcher = context.getIndexSearcher();
	    IndexReader reader = searcher.getIndexReader();
	    FeatureVector qfv = FeatureVector.fromTerms(AnalyzerUtils.analyze(analyzer, context.getQueryText())).scaleToUnitL1Norm();
	    boolean useRf = (context.getSearchArgs().rf_qrels != null);
	    FeatureVector rm = estimateRelevanceModel(docs, reader, context.getSearchArgs().searchtweets, useRf);
	    rm = FeatureVector.interpolate(qfv, rm, originalQueryWeight);
	    BooleanQuery.Builder feedbackQueryBuilder = new BooleanQuery.Builder();
	    Iterator<String> terms = rm.iterator();
	    while (terms.hasNext()) {
	      String term = terms.next();
	      float prob = rm.getFeatureWeight(term);
	      feedbackQueryBuilder.add(new BoostQuery(new TermQuery(new Term(this.field, term)), prob), BooleanClause.Occur.SHOULD);
	    }
	    return feedbackQueryBuilder.build();
	  }

 @Override
 public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
   assert(docs.documents.length == docs.scores.length);

   IndexSearcher searcher = context.getIndexSearcher();
   IndexReader reader = searcher.getIndexReader();

   FeatureVector qfv = FeatureVector.fromTerms(AnalyzerUtils.analyze(analyzer, context.getQueryText())).scaleToUnitL1Norm();

   boolean useRf = (context.getSearchArgs().rf_qrels != null);
   FeatureVector rm = estimateRelevanceModel(docs, reader, context.getSearchArgs().searchtweets, useRf);

   rm = FeatureVector.interpolate(qfv, rm, originalQueryWeight);

   BooleanQuery.Builder feedbackQueryBuilder = new BooleanQuery.Builder();

   Iterator<String> terms = rm.iterator();
   while (terms.hasNext()) {
     String term = terms.next();
     float prob = rm.getFeatureWeight(term);
     feedbackQueryBuilder.add(new BoostQuery(new TermQuery(new Term(this.field, term)), prob), BooleanClause.Occur.SHOULD);
   }

   Query feedbackQuery = feedbackQueryBuilder.build();

   if (this.outputQuery) {
     LOG.info("QID: " + context.getQueryId());
     LOG.info("Original Query: " + context.getQuery().toString(this.field));
     LOG.info("Running new query: " + feedbackQuery.toString(this.field));
   }

   TopDocs rs;
   try {
     Query finalQuery = feedbackQuery;
     // If there's a filter condition, we need to add in the constraint.
     // Otherwise, just use the feedback query.
     if (context.getFilter() != null) {
       BooleanQuery.Builder bqBuilder = new BooleanQuery.Builder();
       bqBuilder.add(context.getFilter(), BooleanClause.Occur.FILTER);
       bqBuilder.add(feedbackQuery, BooleanClause.Occur.MUST);
       finalQuery = bqBuilder.build();
     }

     // Figure out how to break the scoring ties.
     if (context.getSearchArgs().arbitraryScoreTieBreak) {
       rs = searcher.search(finalQuery, context.getSearchArgs().hits);
     } else if (context.getSearchArgs().searchtweets) {
       rs = searcher.search(finalQuery, context.getSearchArgs().hits, BREAK_SCORE_TIES_BY_TWEETID, true);
     } else {
       rs = searcher.search(finalQuery, context.getSearchArgs().hits, BREAK_SCORE_TIES_BY_DOCID, true);
     }
   } catch (IOException e) {
     e.printStackTrace();
     return docs;
   }
   return ScoredDocuments.fromTopDocs(rs, searcher);
   
 }
 
 public String reformulatedQuery(ScoredDocuments docs, RerankerContext<T> context) {
	   assert(docs.documents.length == docs.scores.length);

	   IndexSearcher searcher = context.getIndexSearcher();
	   IndexReader reader = searcher.getIndexReader();

	   FeatureVector qfv = FeatureVector.fromTerms(AnalyzerUtils.analyze(analyzer, context.getQueryText())).scaleToUnitL1Norm();

	   boolean useRf = (context.getSearchArgs().rf_qrels != null);
	   FeatureVector rm = estimateRelevanceModel(docs, reader, context.getSearchArgs().searchtweets, useRf);

	   rm = FeatureVector.interpolate(qfv, rm, originalQueryWeight);

	   BooleanQuery.Builder feedbackQueryBuilder = new BooleanQuery.Builder();

	   Iterator<String> terms = rm.iterator();
	   while (terms.hasNext()) {
	     String term = terms.next();
	     float prob = rm.getFeatureWeight(term);
	     feedbackQueryBuilder.add(new BoostQuery(new TermQuery(new Term(this.field, term)), prob), BooleanClause.Occur.SHOULD);
	   }

	   Query feedbackQuery = feedbackQueryBuilder.build();
	   return feedbackQuery.toString();
	   
	 }

 private FeatureVector estimateRelevanceModel(ScoredDocuments docs, IndexReader reader, boolean tweetsearch, boolean useRf) {
   FeatureVector f = new FeatureVector();

   Set<String> vocab = new HashSet<>();
   List<FeatureVector> docvectors = new ArrayList<>();
   List<Float> docScores = new ArrayList<>();
   for (int i=0; i<docs.documents.length; i++) {
     String id = docs.documents[i].get(IndexArgs.ID);
     if(relevantdocs.contains(id)) {
    	 try {
    	       FeatureVector docVector = createdFeatureVector(
    	       reader.getTermVector(docs.ids[i], field), reader, tweetsearch);
    	       docVector.pruneToSize(fbTerms);
    	       vocab.addAll(docVector.getFeatures());
    	       docvectors.add(docVector);
    	       docScores.add(Float.valueOf(docs.scores[i]));
    	     } catch (IOException e) {
    	       throw new RuntimeException(e);
    	     }
     }
   }
   // Precompute the norms once and cache results.
   float[] norms = new float[docvectors.size()];
   for (int i = 0; i < docvectors.size(); i++) {
     norms[i] = (float) docvectors.get(i).computeL1Norm();
   }

   for (String term : vocab) {
     float fbWeight = 0.0f;
     for (int i = 0; i < docvectors.size(); i++) {
       // Avoids zero-length feedback documents, which causes division by zero when computing term weights.
       // Zero-length feedback documents occur (e.g., with CAR17) when a document has only terms 
       // that accents (which are indexed, but not selected for feedback).
       if (norms[i] > 0.001f) {
         fbWeight += (docvectors.get(i).getFeatureWeight(term) / norms[i]) * docScores.get(i);
       }
     }
     f.addFeatureWeight(term, fbWeight);
   }

   f.pruneToSize(fbTerms);
   f.scaleToUnitL1Norm();

   return f;
 }

 private FeatureVector createdFeatureVector(Terms terms, IndexReader reader, boolean tweetsearch) {
   FeatureVector f = new FeatureVector();

   try {
     int numDocs = reader.numDocs();
     TermsEnum termsEnum = terms.iterator();

     BytesRef text;
     while ((text = termsEnum.next()) != null) {
       String term = text.utf8ToString();

       if (term.length() < 2 || term.length() > 20) continue;
       if (!term.matches("[a-z0-9]+")) continue;

       // This seemingly arbitrary logic needs some explanation. See following PR for details:
       //   https://github.com/castorini/Anserini/pull/289
       //
       // We have long known that stopwords have a big impact in RM3. If we include stopwords
       // in feedback, effectiveness is affected negatively. In the previous implementation, we
       // built custom stopwords lists by selecting top k terms from the collection. We only
       // had two stopwords lists, for gov2 and for Twitter. The gov2 list is used on all
       // collections other than Twitter.
       //
       // The logic below instead uses a df threshold: If a term appears in more than n percent
       // of the documents, then it is discarded as a feedback term. This heuristic has the
       // advantage of getting rid of collection-specific stopwords lists, but at the cost of
       // introducing an additional tuning parameter.
       //
       // Cognizant of the dangers of (essentially) tuning on test data, here's what I
       // (@lintool) did:
       //
       // + For newswire collections, I picked a number, 10%, that seemed right. This value
       //   actually increased effectiveness in most conditions across all newswire collections.
       //
       // + This 10% value worked fine on web collections; effectiveness didn't change much.
       //
       // Since this was the first and only heuristic value I selected, we're not really tuning
       // parameters.
       //
       // The 10% threshold, however, doesn't work well on tweets because tweets are much
       // shorter. Based on a list terms in the collection by df: For the Tweets2011 collection,
       // I found a threshold close to a nice round number that approximated the length of the
       // current stopwords list, by eyeballing the df values. This turned out to be 1%. I did
       // this again for the Tweets2013 collection, using the same approach, and obtained a value
       // of 0.7%.
       //
       // With both values, we obtained effectiveness pretty close to the old values with the
       // custom stopwords list.
       int df = reader.docFreq(new Term(IndexArgs.CONTENTS, term));
       float ratio = (float) df / numDocs;
       if (tweetsearch) {
         if (numDocs > 100000000) { // Probably Tweets2013
           if (ratio > 0.007f) continue;
         } else {
           if (ratio > 0.01f) continue;
         }
       } else if (ratio > 0.1f) continue;

       int freq = (int) termsEnum.totalTermFreq();
       f.addFeatureWeight(term, (float) freq);
     }
   } catch (Exception e) {
     e.printStackTrace();
     // Return empty feature vector
     return f;
   }

   return f;
 }
 
 @Override
 public String tag() {
   return "Rm3RelevanceFeedback(fbDocs="+fbDocs+",fbTerms="+fbTerms+",originalQueryWeight:"+originalQueryWeight+")";
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
	IndexSearcher searcher = context.getIndexSearcher();
	IndexReader reader = searcher.getIndexReader();

	FeatureVector qfv = FeatureVector.fromTerms(AnalyzerUtils.analyze(analyzer, context.getQueryText())).scaleToUnitL1Norm();

	boolean useRf = (context.getSearchArgs().rf_qrels != null);
	FeatureVector rm = estimateRelevanceModel(docs, reader, context.getSearchArgs().searchtweets, useRf);

	if(allowedTerms != null && allowedTerms.size() > 0) {
		FeatureVector tmp = new FeatureVector();
		for(String feature: rm.getFeatures()) {
			if(allowedTerms.contains(feature)) {
				tmp.addFeatureWeight(feature, rm.getFeatureWeight(feature));
			}
		}
		rm = tmp;
	}
	
	rm = FeatureVector.interpolate(qfv, rm, originalQueryWeight);

	Iterator<String> features = rm.iterator();
	Map<String, Float> terms = new HashMap<String, Float>();
	while (features.hasNext()) {
	     String term = features.next();
	     float score = rm.getFeatureWeight(term);
	     terms.put(term, score);
	}
	return terms;
}
}

