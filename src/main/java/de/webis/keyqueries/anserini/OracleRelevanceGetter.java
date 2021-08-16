package de.webis.keyqueries.anserini;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.util.Qrels;

public class OracleRelevanceGetter<T> implements Reranker<T> {

	protected final Reranker<T> internalReranker;

	private final Qrels qrels;
	
	private T qid;

	public OracleRelevanceGetter(Reranker<T> internalReranker, Qrels qrels) {
		this.internalReranker = internalReranker;
		this.qrels = qrels;
	}
	@Override
	public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
		qid = context.getQueryId();
		Map<String, Integer> relevanceFeedback = getRelevanceFeedback(docs, qid, qrels);
		
		if(internalReranker instanceof RelevanceFeedbackAware) {
			((RelevanceFeedbackAware) internalReranker).setRelevanceFeedback(relevanceFeedback, context);
		}
		
		return internalReranker.rerank(docs, context);
	}

	private Map<String, Integer> getRelevanceFeedback(ScoredDocuments docs, T qid, Qrels qrels) {
		Map<String, Integer> tmp = new LinkedHashMap<>();
		Map<String, Integer> rel = qrels.getDocMap(String.valueOf(qid));
		if(rel == null) {
			return null;
		}
		for(Map.Entry<String, Integer> entry : rel.entrySet()) {
			String docId = entry.getKey();
			Integer currentRelevanceLabel = qrels.getRelevanceGrade(String.valueOf(qid), docId);
			tmp.put(docId, currentRelevanceLabel);
		}
		return tmp.entrySet().stream()
				.filter(x -> x.getValue() > 0)
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
	}
	@Override
	public String tag() {
		return "oracle-relevance-getter";
	}
}
