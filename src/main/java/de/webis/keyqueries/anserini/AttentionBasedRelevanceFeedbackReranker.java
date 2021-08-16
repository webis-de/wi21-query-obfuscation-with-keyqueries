package de.webis.keyqueries.anserini;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.util.Qrels;

public class AttentionBasedRelevanceFeedbackReranker<T> extends RelevanceFeedbackReranker<T>{

	public AttentionBasedRelevanceFeedbackReranker(Reranker<T> internalReranker, Qrels qrels, int relevantDocuments) {
		super(internalReranker, qrels, relevantDocuments);
	}

	@Override
	public String tag() {
		return "attention-based-relevance-feedback(" + internalReranker.tag() + ";rel=" + relevantDocuments + ")";
	}

	@Override
	protected Map<String, Integer> getRelevanceFeedback(ScoredDocuments docs, T qid, Qrels qrels,
			int relevantDocuments, RerankerContext<T> context) {
		return null;
		// Attention scores laden
		// aus qrels alle Dokumente für das aktuelle topic holen und nach score sortieren und top k nehmen
	}
	
	private Object loadAttentionscore(String topic) {
		InputStream input = AttentionBasedRelevanceFeedbackReranker.class.getResourceAsStream("/attention_score/robust04-attention-score.csv");
		try {
			List<String> lines = IOUtils.readLines(input, StandardCharsets.UTF_8);
			return lines;
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}
}
