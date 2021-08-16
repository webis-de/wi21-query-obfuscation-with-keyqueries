package de.webis.keyqueries.anserini;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.document.Document;

import io.anserini.index.IndexArgs;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;

public class RelevantToTopReranker<T> implements Reranker<T>, RelevanceFeedbackAware<T> {

	private Map<String, Integer> relevanceFeedback;
	
	@Override
	public void setRelevanceFeedback(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		this.relevanceFeedback = relevanceFeedback;
	}

	@Override
	public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
		Map<String,Integer> tmp = new HashMap<String,Integer>();
		for(int i=0; i<docs.documents.length; i++) {
			tmp.put(docs.documents[i].get(IndexArgs.ID), docs.ids[i]);
		}
		docs.documents = rerank(docs.documents);
		for(int i=0; i<docs.documents.length; i++) {
			docs.ids[i]=tmp.get(docs.documents[i].get(IndexArgs.ID));
		}
		docs.scores = new float[docs.documents.length];
		
		for(int i=0; i<docs.documents.length; i++) {
			docs.scores[i] = docs.documents.length-i;
		}
		
		return docs;
	}

	private Document[] rerank(Document[] documents) {
		documents = Arrays.copyOf(documents, documents.length);
		Arrays.sort(documents, (a,b) -> compare(a.get("id"), b.get("id")));
		
		return documents;
	}

	private Integer compare(String idA, String idB) {
		Integer scoreOfA = relevance(idA);
		Integer scoreOfB = relevance(idB);
		
		return scoreOfB.compareTo(scoreOfA);
	}
	
	private Integer relevance(String id) {
		Integer ret = relevanceFeedback.get(id);
		if(ret == null) {
			return 0;
		} else if (ret == 0) {
			return -1;
		}
		
		return ret *10;
	}

	@Override
	public String tag() {
		return "relevat-docs-to-top";
	}

}
