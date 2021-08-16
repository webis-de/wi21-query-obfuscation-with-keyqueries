package de.webis.crypsor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;


public class EvaluateQuerySavingsByHbcTest {
	@Test
	public void testThatInitialQueriesAreCorrectlyExtracted() {
		List<String> queries = queries(
			"a",
			"b",
			"c",
			"d"
		);
		
		Set<String> expected = new HashSet<>(Arrays.asList("a", "b", "c", "d"));
		Set<String> actual = EvaluateQuerySavingsByHbc.startupQueries(queries);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testThatHbcCalculation() {
		List<String> queries = queries(
			"a",
			"b",
			"c",
			"d"
		);
		
		String expected = "{submittedQueries=4, exhaustiveSearchNumberOfQueries=15}";
		String actual = EvaluateQuerySavingsByHbc.analyzeTopic(queries).toString();
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testThatInitialQueriesAreCorrectlyExtractedForSomeRedundantQueries() {
		List<String> queries = queries(
			"a",
			"b",
			"c",
			"d a",
			"d",
			"b e"
		);
		
		//TODO FIXME
		Set<String> expected = new HashSet<>(Arrays.asList("a", "b", "c", "d", "b e"));
		Set<String> actual = EvaluateQuerySavingsByHbc.startupQueries(queries);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testThatInitialQueriesAreCorrectlyExtractedForRedundantSubwordQueries() {
		List<String> queries = queries(
			"a b",
			"x d x d",
			"a",
			"b",
			"c",
			"x d x",
			"d",
			"xbx"
		);
		
		//TODO: FIXME
		Set<String> expected = new HashSet<>(Arrays.asList("a", "b", "c", "d", "x d x"));
		Set<String> actual = EvaluateQuerySavingsByHbc.startupQueries(queries);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testHbcCalculationForRedundantSubwordQueries() {
		List<String> queries = queries(
				"a",
				"b",
				"c",
				"x d x",
				"d",
				"xbx"
		);
		
		String expected = "{submittedQueries=6, exhaustiveSearchNumberOfQueries=15}";
		String actual = EvaluateQuerySavingsByHbc.analyzeTopic(queries).toString();
		
		Assert.assertEquals(expected, actual);
	}
	
	static List<String> queries(String...queries) {
		return Stream.of(queries).map(i -> queryJson(i)).collect(Collectors.toList());
	}
	
	private static String queryJson(String query) {
		return "{\"privateQuery\":\"sit and reach test\",\"scrambledQuery\":\"" + query + "\",\"approach\":\"arampatzis\",\"hitsForPrivateQuery\":423953,\"hitsForScrambledQuery\":0,\"targetDocs\":{}}";
	}
}
