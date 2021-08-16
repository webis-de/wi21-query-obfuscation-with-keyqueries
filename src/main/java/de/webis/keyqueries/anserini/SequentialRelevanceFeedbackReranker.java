package de.webis.keyqueries.anserini;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.lucene.document.Document;

import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.util.Qrels;

public class SequentialRelevanceFeedbackReranker<T> extends RelevanceFeedbackReranker<T> {

	public SequentialRelevanceFeedbackReranker(Reranker<T> internalReranker, Qrels qrels, int relevantDocuments) {
		super(internalReranker, qrels, relevantDocuments);
	}
	
	@Override
	public String tag() {
		return "sequential-relevance-feedback(" + internalReranker.tag() + ";rel=" + relevantDocuments + ")";
	}

	@Override
	protected Map<String, Integer> getRelevanceFeedback(ScoredDocuments docs, T qid, Qrels qrels,
			int relevantDocuments, RerankerContext<T> context) {
		Map<String, Integer> ret = new LinkedHashMap<>();
		int foundRelevantDocuments = 0;
		
		for(Document doc: docs.documents) {
			String docId = doc.get("id");
			Integer currentRelevanceLabel = qrels.getRelevanceGrade(String.valueOf(qid), docId);
			ret.put(docId, currentRelevanceLabel);
			
			if(currentRelevanceLabel > 0) {
				foundRelevantDocuments++;
			}
			
			if(foundRelevantDocuments >= relevantDocuments) {
				break;
			}
		}
		
		return ret;
	}

}
