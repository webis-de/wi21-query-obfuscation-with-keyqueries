package de.webis.crypsor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.webis.keyqueries.KeyQueryCheckerBase;

public class MainQuerySelectionTest {
	@Test
	public void testSearcherForQueriesWithoutTargetDocuments() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{}}"
		);
		
		KeyQueryCheckerBase<String> searcher = MainQuerySelection.kq(queries);
		
		Assert.assertEquals(new ArrayList<>(), searcher.issueQuery("query 1"));
		Assert.assertEquals(new ArrayList<>(), searcher.issueQuery("query 2"));
	}

	@Test
	public void testSearcherForQueriesWithSomeTargetDocumentsTargetDocuments() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 3,\"b\":10}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 300,\"b\":1000}}"
		);
		
		KeyQueryCheckerBase<String> searcher = MainQuerySelection.kq(queries);
		
		Assert.assertEquals(Arrays.asList("does-not-exist", "does-not-exist", "a", "does-not-exist", "does-not-exist", "does-not-exist", "does-not-exist", "does-not-exist", "does-not-exist", "b"), searcher.issueQuery("query 1").subList(0, 10));
		Assert.assertEquals(new ArrayList<>(), searcher.issueQuery("query 2"));
	}
	
	@Test(expected=RuntimeException.class)
	public void approveSelectedQueriesWithoutTargetDocuments() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{}}"
		);
		
		MainQuerySelection.selectTop(queries, 10, "keyqueryNdcgRelaxed");		
	}
	
	@Test
	public void approveSelectedQueriesWithSingleQuery() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 3,\"b\":10}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{}}"
		);
		
		String expected = "{\"1\":\"query 1\"}";
		String actual = MainQuerySelection.selectTop(queries, 10, "keyqueryNdcgRelaxed");
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void approveSelectedQueriesWithMultipleQueries() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 3,\"b\":10}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 3,\"b\":1}}"
		);
		
		String expected = "{\"1\":\"query 2\",\"2\":\"query 1\"}";
		String actual = MainQuerySelection.selectTop(queries, 10, "keyqueryNdcgRelaxed");
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void approveSelectedQueriesWithMultipleQueriesReverse() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 2,\"b\":1}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 3,\"b\":1}}"
		);
		
		String expected = "{\"1\":\"query 1\",\"2\":\"query 2\"}";
		String actual = MainQuerySelection.selectTop(queries, 10, "keyqueryNdcgRelaxed");
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void approveSelectedQueriesWithMultipleQueriesNdcgOnly() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 22,\"b\":11}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 33,\"b\":11}}"
		);
		
		String expected = "{\"1\":\"query 1\",\"2\":\"query 2\"}";
		String actual = MainQuerySelection.selectTop(queries, 10, "ndcg");
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void approveSelectedQueriesWithMultipleQueriesNdcg() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 3,\"b\":10}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 3,\"b\":1}}"
		);
		
		String expected = "{\"1\":\"query 2\",\"2\":\"query 1\"}";
		String actual = MainQuerySelection.selectTop(queries, 10, "ndcg");
		
		Assert.assertEquals(expected, actual);
	}
	
	
	@Test
	public void approveSelectedQueriesWithMultipleQueriesNdcg2() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 2,\"b\":1}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 3,\"b\":1}}"
		);
		
		String expected = "{\"1\":\"query 1\",\"2\":\"query 2\"}";
		String actual = MainQuerySelection.selectTop(queries, 10, "ndcg");
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void approveSelectedQueriesWithMultipleQueriesNdcg3() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-not-listed\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 0\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 5,\"b\":6}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 2,\"b\":1}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"a\": 3,\"b\":1}}"
		);
		
		String expected = "{\"1\":\"query 1\",\"2\":\"query 2\",\"3\":\"query 0\"}";
		String actual = MainQuerySelection.selectTop(queries, 10, "ndcg");
		
		Assert.assertEquals(expected, actual);
	}
}
