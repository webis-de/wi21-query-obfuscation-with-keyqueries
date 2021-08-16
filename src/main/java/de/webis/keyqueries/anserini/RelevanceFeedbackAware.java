package de.webis.keyqueries.anserini;

import java.util.Map;

import io.anserini.rerank.RerankerContext;

public interface RelevanceFeedbackAware<T> {
	void setRelevanceFeedback(Map<String, Integer> relevanceFeedback, RerankerContext<T> context);
}
