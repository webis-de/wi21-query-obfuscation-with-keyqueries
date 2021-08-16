package de.webis.keyqueries.anserini;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import de.webis.keyqueries.util.Util;
import io.anserini.index.IndexArgs;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.search.SearchArgs;

public class QueryExpansionCombiner<T> implements Reranker<T>, RelevanceFeedbackAware<T> {
	private Map<String, Integer> relevanceFeedback;
	@Override
	public void setRelevanceFeedback(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		this.relevanceFeedback = relevanceFeedback;
		
	}

	@Override
	public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
		String output = "Query: " +context.getQueryText() +"\n";
		System.out.print(output);
		SearchArgs args = context.getSearchArgs();
		Map<String, Integer> collect = relevanceFeedback.entrySet().stream()
				.filter(x -> x.getValue() > 0)
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
		List<Map<String, Float>> terms = new ArrayList<Map<String, Float>>();
		final Analyzer analyzer = Util.analyzer(context.getSearchArgs());
		// Keyquery terms
		KeyqueryRelevanceReranker<T> kq = new KeyqueryRelevanceReranker<>();
		kq.setRelevanceFeedback(collect, context);
		terms.add(kq.getTermsAndWeights(collect, context));
		// RM3 terms
		for (String fbTerms : args.rm3_fbTerms) {
	        for (String fbDocs : args.rm3_fbDocs) {
	          for (String originalQueryWeight : args.rm3_originalQueryWeight) {
	    			  RM3RelevanceFeedbackReranker<T> rm3 = new RM3RelevanceFeedbackReranker<>(analyzer, IndexArgs.CONTENTS, Integer.valueOf(fbTerms),
	  		                Integer.valueOf(fbDocs), Float.valueOf(originalQueryWeight), args.rm3_outputQuery);
	    			  rm3.setRelevanceFeedback(collect, context);
	    			  terms.add(rm3.getTermsAndWeights(docs, context));
	        	  }
	          }
	        }
		// BM25PRF terms
		
		// Axiom terms
		
		// intersection
		if(terms.size() == 0) {
			System.out.println("Empty");
			return docs;
		}
		Map<String, Float> intersection = terms.get(0);
		for(int i=1; i<terms.size(); i++) {
			intersection = intersect(intersection, terms.get(i));
		}
		if(intersection.size() == 0 || intersection == null) {
			System.out.println("Empty");
			return docs;
		}
		// reweighting with Rocchio's beta formula
		String tmp = "";
		for(String term: intersection.keySet()) {
			tmp+=term+" ";
		}
		String expanded = tmp.stripTrailing();
		float maxweight = maximumWeight(intersection);
		float alpha = context.getSearchArgs().combiner_alpha;
		float beta = context.getSearchArgs().combiner_beta;
		int maxfreq = maximumTermFrequency(expanded);
		for(String term: intersection.keySet()) {
			int termfreq = termFrequency(term, expanded);
			float newWeight = (float) ((float) alpha*termfreq/maxfreq+beta*intersection.get(term)/maxweight);
			intersection.put(term, newWeight);
		}
		// new query
		BooleanQuery.Builder feedbackQueryBuilder = new BooleanQuery.Builder();
		
		for(String term: intersection.keySet()) {
			feedbackQueryBuilder.add(new BoostQuery(new TermQuery(new Term(IndexArgs.CONTENTS, term)), intersection.get(term)), BooleanClause.Occur.SHOULD);
		}
		Query feedbackQuery = feedbackQueryBuilder.build();
		TopDocs rs = null;
		try {
			rs = context.getIndexSearcher().search(feedbackQuery, context.getSearchArgs().rerankcutoff);
		} catch (IOException e) {
			e.printStackTrace();
		}
		TopDocs newrs = Util.moveToTop(docs, collect.keySet(), rs);
		return ScoredDocuments.fromTopDocs(newrs, context.getIndexSearcher());
	}
	
	public Map<String, Float> intersect(Map<String, Float> input1, Map<String, Float> input2) {
		Map<String, Float> intersection = new HashMap<String, Float>();
		for(String term: input1.keySet()) {
			if(input2.containsKey(term)) {
				intersection.put(term, Math.max(input1.get(term), input2.get(term)));
			}
		}
		return intersection;
	}
	
	public float maximumWeight(Map<String, Float> input) {
		return input
				.entrySet()
				.stream()
				.max(Map.Entry.comparingByValue())
				.get()
				.getValue();
	}
	
	public int termFrequency(String term, String query) {
		int freq = 0;
		String[] splitted = query.split(" ");
		for(String split: splitted) {
			if(term.equals(split)) {
				freq++;
			}
		}
		return freq;
	}
	
	public int maximumTermFrequency(String query) {
		Map<String, Integer> termFreq = new HashMap<String, Integer>();
		String[] splitted = query.split(" ");
		for(String split: splitted) {
			if(termFreq.containsKey(split)) {
				termFreq.put(split, termFreq.get(split)+1);
			} else {
				termFreq.put(split, 1);
			}
		}
		return termFreq
				.entrySet()
				.stream()
				.max(Map.Entry.comparingByValue())
				.get()
				.getValue();
	}

	@Override
	public String tag() {
		return "query-expansion-combiner";
	}

}
