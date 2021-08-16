package de.webis.keyqueries.anserini;

import java.util.Map;


import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.util.Qrels;

public abstract class RelevanceFeedbackReranker<T> implements Reranker<T>  {
	protected final int relevantDocuments;

	protected final Reranker<T> internalReranker;

	private final Qrels qrels;
	
	private T qid;

	public RelevanceFeedbackReranker(Reranker<T> internalReranker, Qrels qrels, int relevantDocuments) {
		this.internalReranker = internalReranker;
		this.relevantDocuments = relevantDocuments;
		this.qrels = qrels;
	}

	@Override
	@SuppressWarnings("unchecked")
	public final ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
		qid = context.getQueryId();
		Map<String, Integer> relevanceFeedback = getRelevanceFeedback(docs, qid, qrels, relevantDocuments, context);
		
		if(internalReranker instanceof RelevanceFeedbackAware) {
			((RelevanceFeedbackAware<T>) internalReranker).setRelevanceFeedback(relevanceFeedback, context);
		}
		
		return internalReranker.rerank(docs, context);
	}

	protected abstract Map<String, Integer> getRelevanceFeedback(ScoredDocuments docs, T qid, Qrels qrels, int relevantDocuments, RerankerContext<T> context);
	
}
