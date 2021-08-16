package de.webis.keyqueries.anserini;

import static de.webis.keyqueries.anserini.HumanInTheLoopRerankerTest.scoreDocs;

import java.util.Map;

import static de.webis.keyqueries.anserini.HumanInTheLoopRerankerTest.assertDocsAreRankedAsExpected;

import org.junit.Test;
import org.mockito.Mockito;

import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;

public class RelevantToTopRerankerTest<T> {
	@SuppressWarnings("unchecked")
	private final RerankerContext<T> context = Mockito.mock(RerankerContext.class);
	
	@Test
	public void testWithSomeRelevantAndSomeIrrelevantDocuments() {
		RelevantToTopReranker<T> reranker = new RelevantToTopReranker<>();
		reranker.setRelevanceFeedback(Map.of("b", 1, "c", 0), context);
		ScoredDocuments reranked = reranker.rerank(scoreDocs("a", "c", "b", "a", "d"), context);
		
	    assertDocsAreRankedAsExpected(reranked,  "b", "a", "a", "d",  "c");
	}
	
	@Test
	public void testWithSomeRelevantAndSomeIrrelevantDocuments2() {
		RelevantToTopReranker<T> reranker = new RelevantToTopReranker<>();
		reranker.setRelevanceFeedback(Map.of("b", 1, "c", 0), context);
		ScoredDocuments reranked = reranker.rerank(scoreDocs("a", "c", "b", "d", "a"), context);
		
	    assertDocsAreRankedAsExpected(reranked,  "b", "a", "d", "a",  "c");
	}
	
	@Test
	public void testWithSomeRelevantAndSomeIrrelevantDocuments3() {
		RelevantToTopReranker<T> reranker = new RelevantToTopReranker<>();
		reranker.setRelevanceFeedback(Map.of("a", 1, "c", 0), context);
		ScoredDocuments reranked = reranker.rerank(scoreDocs("a", "c", "d", "b", "a"), context);
		
	    assertDocsAreRankedAsExpected(reranked,  "a", "a", "d", "b",  "c");
	}
}
