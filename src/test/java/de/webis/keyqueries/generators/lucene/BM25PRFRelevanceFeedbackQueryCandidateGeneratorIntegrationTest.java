package de.webis.keyqueries.generators.lucene;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.search.Query;
import org.junit.Assert;
import org.junit.Test;

import de.webis.keyqueries.anserini.RM3KeyqueryReranker;
import de.webis.keyqueries.anserini.RM3KeyqueryRerankerIntegrationTest;
import de.webis.keyqueries.generators.KeyQueryCandidateGenerator;
import io.anserini.rerank.RerankerContext;
import io.anserini.search.SearchArgs;

public class BM25PRFRelevanceFeedbackQueryCandidateGeneratorIntegrationTest<T> extends RM3KeyqueryRerankerIntegrationTest<T> {

	@Test(expected=RuntimeException.class)
	public void testThatExceptionIsThrownWhenNoRelevanceFeedbackIsAvailable() {
		RM3KeyqueryReranker<T> reranker = new RM3KeyqueryReranker<>(1, 1, 1, 1f);
		reranker.relevanceFeedbackQueryCandidateGenerator(context("test text", new SearchArgs()));
	}
	
	@Test(expected=RuntimeException.class)
	public void testThatExceptionIsThrownWhenTargetDocumentsAreNotNull() {
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		
		KeyQueryCandidateGenerator<Query> generator = generatorForQueriesOfLength1FromTopCandidate(
			relevanceFeedback,
			context("city", new SearchArgs())
		);
		
		generator.generateCandidates(new HashSet<>());
	}
	
	@Test
	public void testThatOriginalQueryIsReturnedForQueryLengthZero() {
		List<String> expected = Arrays.asList("(contents:citi)^4.0430512");
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		
		KeyQueryCandidateGenerator<Query> generator = generatorForQueriesOfLength0(
			relevanceFeedback,
			context("city", new SearchArgs())
		);
		
		List<Query> actual = generator.generateCandidates(null);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testThatOriginalQueryWithoutMatchingTermIsReturnedForQueryLengthZero() {
		List<String> expected = Arrays.asList("(contents:queri)^0.76214004 (contents:match)^0.76214004 (contents:doe)^0.76214004");
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		relevanceFeedback.put("doc2", 1);
		relevanceFeedback.put("doc3", 1);
		
		KeyQueryCandidateGenerator<Query> generator = generatorForQueriesOfLength0(
			relevanceFeedback,
			context("this query does not match", new SearchArgs())
		);
		
		List<Query> actual = generator.generateCandidates(null);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testCombinationOfOriginalQueryInCombinationWithSingleOtherTerms() {
		List<String> expected = Arrays.asList(
			"(contents:more)^0.88853025 (contents:queri)^1.2237754 (contents:match)^1.2237754 (contents:doe)^1.2237754",
			"(contents:queri)^1.2237754 (contents:match)^1.2237754 (contents:doe)^1.2237754",
			"(contents:queri)^1.2237754 (contents:match)^1.2237754 (contents:text)^0.88853025 (contents:doe)^1.2237754"
		);
		
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		relevanceFeedback.put("doc2", 1);
		
		KeyQueryCandidateGenerator<Query> generator = generatorForQueriesOfLength1FromTopCandidate(
			relevanceFeedback,
			context("this query does not match", new SearchArgs())
		);
		
		List<Query> actual = generator.generateCandidates(null);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testCombinationOfOriginalQueryInCombinationWithTwoOtherTerms() {
		List<String> expected = Arrays.asList(
			"(contents:queri)^1.2237754 (contents:match)^1.2237754 (contents:doe)^1.2237754",
			"(contents:queri)^1.2237754 (contents:match)^1.2237754 (contents:text)^0.88853025 (contents:doe)^1.2237754",
			"(contents:more)^0.88853025 (contents:queri)^1.2237754 (contents:match)^1.2237754 (contents:doe)^1.2237754",
			"(contents:more)^0.88853025 (contents:queri)^1.2237754 (contents:match)^1.2237754 (contents:text)^0.88853025 (contents:doe)^1.2237754"
		);
		
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		relevanceFeedback.put("doc2", 1);
		
		KeyQueryCandidateGenerator<Query> generator = generatorForQueriesOfLength2FromTopCandidate(
			relevanceFeedback,
			context("this query does not match", new SearchArgs())
		);
		
		List<Query> actual = generator.generateCandidates(null);
		
		assertEquals(expected, actual);
	}

	@Test
	public void testCombinationOfCityQueryInCombinationWithTwoOtherTerms() {
		List<String> expected = Arrays.asList(
			"(contents:citi)^2.8332133 (contents:more)^0.88853025",
			"(contents:citi)^2.8332133 (contents:more)^0.88853025 (contents:text)^0.88853025",
			"(contents:citi)^2.8332133 (contents:text)^0.88853025"
		);
		
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		relevanceFeedback.put("doc2", 1);
		
		KeyQueryCandidateGenerator<Query> generator = generatorForQueriesOfLength2FromTopCandidate(
			relevanceFeedback,
			context("city", new SearchArgs())
		);
		
		List<Query> actual = generator.generateCandidates(null);
		
		assertEquals(expected, actual);
	}
	
	private void assertEquals(List<String> expected, List<Query> actual) {
		Assert.assertEquals("got :" + actual , expected.size(), actual.size());
		
		for(int i=0; i<expected.size(); i++) {
			Assert.assertEquals("Entry " + i + " is not equal.", expected.get(i), actual.get(i).toString());
		}
	}
	
	public KeyQueryCandidateGenerator<Query> generatorForQueriesOfLength0(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		int queryLength = 0;
		int candidates = 100;
		int unusedInt = -100; //not used, pass garbage
		float unusedFloat = -100; //not used, pass garbage
		context.getSearchArgs().prf_term = true;
		RM3KeyqueryReranker<T> reranker = new RM3KeyqueryReranker<>(queryLength, candidates, unusedInt, unusedFloat);
		reranker.setRelevanceFeedback(relevanceFeedback, context);
		
		return reranker.relevanceFeedbackQueryCandidateGenerator(context);
	}
	
	public KeyQueryCandidateGenerator<Query> generatorForQueriesOfLength1FromTopCandidate(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		int queryLength = 1;
		int candidates = 100;
		int unusedInt = -100; //not used, pass garbage
		float unusedFloat = -100; //not used, pass garbage
		context.getSearchArgs().prf_term = true;
		RM3KeyqueryReranker<T> reranker = new RM3KeyqueryReranker<>(queryLength, candidates, unusedInt, unusedFloat);
		reranker.setRelevanceFeedback(relevanceFeedback, context);
		
		return reranker.relevanceFeedbackQueryCandidateGenerator(context);
	}

	public KeyQueryCandidateGenerator<Query> generatorForQueriesOfLength2FromTopCandidate(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		int queryLength = 2;
		int candidates = 100;
		int unusedInt = -100; //not used, pass garbage
		float unusedFloat = -100; //not used, pass garbage
		context.getSearchArgs().prf_term = true;
		RM3KeyqueryReranker<T> reranker = new RM3KeyqueryReranker<>(queryLength, candidates, unusedInt, unusedFloat);
		reranker.setRelevanceFeedback(relevanceFeedback, context);
		
		return reranker.relevanceFeedbackQueryCandidateGenerator(context);
	}
}
