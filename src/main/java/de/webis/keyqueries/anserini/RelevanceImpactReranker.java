package de.webis.keyqueries.anserini;

import java.util.LinkedHashMap;
import java.util.Map;

import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.util.Qrels;

public class RelevanceImpactReranker<T> extends RelevanceFeedbackReranker<T> {

	public RelevanceImpactReranker(Reranker<T> internalReranker, Qrels qrels, int relevantDocuments) {
		super(internalReranker, qrels, relevantDocuments);
	}

	@Override
	public String tag() {
		return "relevance-impact"+ ";rel=" + relevantDocuments;
	}

	@Override
	protected Map<String, Integer> getRelevanceFeedback(ScoredDocuments docs, T qid, Qrels qrels, int relevantDocuments,
			RerankerContext<T> context) {
		int relfound =0;
		Map<String, Integer> ret = new LinkedHashMap<>();
		Map<String,Integer> original = qrels.getDocMap(String.valueOf(qid));
		if(original == null) {
			return ret;
		}
		for(Map.Entry<String,Integer> entry: original.entrySet()) {
			ret.put(entry.getKey(), entry.getValue());
			relfound++;
			if(relfound >= relevantDocuments) {
				break;
			}
		}
		return ret;
	}
	
}
