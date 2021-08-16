package de.webis.keyqueries.anserini;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.util.Qrels;

import static de.webis.keyqueries.anserini.HumanInTheLoopRerankerTest.assertDocsAreRankedAsExpected;
import static de.webis.keyqueries.anserini.HumanInTheLoopRerankerTest.scoreDocs;
import static de.webis.keyqueries.anserini.HumanInTheLoopRerankerTest.qrels;

public class SequentialRelevanceFeedbackRerankerTest<T> {
	@SuppressWarnings("unchecked")
	private final RerankerContext<T> context = Mockito.mock(RerankerContext.class);

	@Test
	public void sequentialRelevanceFeedbackWithOnlyIrrelevantDocumentsSortedAscending() {
		Qrels qrels = qrels(Map.of("a", 0, "b", 0, "c", 0, "d", 0));
		Map<String, Integer> expectedReceivedRelevanceFeedback = Map.of("a", 0, "b", 0, "c", 0, "d", 0);
		RelevanceFeedbackTestReranker<T> internalReranker = new RelevanceFeedbackTestReranker<>();
		internalReranker.comparator = (a, b) -> a.compareTo(b);

		SequentialRelevanceFeedbackReranker<T> humanInTheLoop = new SequentialRelevanceFeedbackReranker<>(
				internalReranker, qrels, 1);
		ScoredDocuments reranked = humanInTheLoop.rerank(scoreDocs("a", "c", "b", "a", "d"), context);

		assertDocsAreRankedAsExpected(reranked, "a", "a", "b", "c", "d");
		Assert.assertEquals(expectedReceivedRelevanceFeedback, internalReranker.getReceivedRelevanceFeedback());
	}
	
	@Test
	public void sequentialRelevanceFeedbackWithOnlyIrrelevantDocumentsSortedDescending() {
		Qrels qrels = qrels(Map.of("a", 0, "b", 0, "c", 0, "d", 0));
		Map<String, Integer> expectedReceivedRelevanceFeedback = Map.of("a", 0, "b", 0, "c", 0, "d", 0);
		RelevanceFeedbackTestReranker<T> internalReranker = new RelevanceFeedbackTestReranker<>();
		internalReranker.comparator = (a, b) -> b.compareTo(a);

		SequentialRelevanceFeedbackReranker<T> humanInTheLoop = new SequentialRelevanceFeedbackReranker<>(
				internalReranker, qrels, 1);
		ScoredDocuments reranked = humanInTheLoop.rerank(scoreDocs("a", "c", "b", "a", "d"), context);

		assertDocsAreRankedAsExpected(reranked,  "d", "c", "b", "a", "a");
		Assert.assertEquals(expectedReceivedRelevanceFeedback, internalReranker.getReceivedRelevanceFeedback());
	}

	@Test
	public void sequentialRelevanceFeedbackWithOnlyRrelevantDocumentsSortedAscending() {
		Qrels qrels = qrels(Map.of("a", 1, "b", 1, "c", 1, "d", 1));
		Map<String, Integer> expectedReceivedRelevanceFeedback = Map.of("a", 1);
		RelevanceFeedbackTestReranker<T> internalReranker = new RelevanceFeedbackTestReranker<>();
		internalReranker.comparator = (a, b) -> a.compareTo(b);

		SequentialRelevanceFeedbackReranker<T> humanInTheLoop = new SequentialRelevanceFeedbackReranker<>(
				internalReranker, qrels, 1);
		ScoredDocuments reranked = humanInTheLoop.rerank(scoreDocs("a", "c", "b", "a", "d"), context);

		assertDocsAreRankedAsExpected(reranked, "a", "a", "b", "c", "d");
		Assert.assertEquals(expectedReceivedRelevanceFeedback, internalReranker.getReceivedRelevanceFeedback());
	}
	
	@Test
	public void sequentialRelevanceFeedbackWithOnlyRrelevantDocumentsSortedAscending2() {
		Qrels qrels = qrels(Map.of("a", 1, "b", 1, "c", 1, "d", 1));
		Map<String, Integer> expectedReceivedRelevanceFeedback = Map.of("a", 1, "c", 1);
		RelevanceFeedbackTestReranker<T> internalReranker = new RelevanceFeedbackTestReranker<>();
		internalReranker.comparator = (a, b) -> a.compareTo(b);

		SequentialRelevanceFeedbackReranker<T> humanInTheLoop = new SequentialRelevanceFeedbackReranker<>(
				internalReranker, qrels, 2);
		ScoredDocuments reranked = humanInTheLoop.rerank(scoreDocs("a", "c", "b", "a", "d"), context);

		assertDocsAreRankedAsExpected(reranked, "a", "a", "b", "c", "d");
		Assert.assertEquals(expectedReceivedRelevanceFeedback, internalReranker.getReceivedRelevanceFeedback());
	}
	
	
	public static class RelevanceFeedbackTestReranker<T> implements Reranker<T>, RelevanceFeedbackAware<T> {
		private Comparator<String> comparator;
		private Map<String, Integer> relevanceFeedback;

		@Override
		public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
			ScoredDocuments ret = new ScoredDocuments();
			ret.ids = null;
			ret.scores = null;
			ret.documents = rerank(docs.documents);

			return ret;
		}

		public Map<String, Integer> getReceivedRelevanceFeedback() {
			return relevanceFeedback;
		}

		private Document[] rerank(Document[] documents) {
			documents = Arrays.copyOf(documents, documents.length);
			Arrays.sort(documents, (a, b) -> comparator.compare(a.get("id"), b.get("id")));
			return documents;
		}

		@Override
		public String tag() {
			return null;
		}

		@Override
		public void setRelevanceFeedback(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
			this.relevanceFeedback = relevanceFeedback;
		}
	}
}
