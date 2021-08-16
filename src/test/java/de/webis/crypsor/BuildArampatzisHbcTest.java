package de.webis.crypsor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class BuildArampatzisHbcTest {
	@Test
	public void checkThatEmptyQueriesAreRemoved() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":0,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":4,\"targetDocs\":{}}"
		);
		
		Set<String> expected = new HashSet<>(Arrays.asList("query-01", "query-03"));
		Set<String> actual = BuildArampatzisHbc.docsWithTooFewResults(queries);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void checkThatNonEmptyQueriesAreNotRemoved() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":24,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{}}"
		);
		
		Set<String> expected = new HashSet<>();
		Set<String> actual = BuildArampatzisHbc.docsWithTooFewResults(queries);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void checkThatQueriesWithoutResultsetAreNotConsideredAsKeyQueries() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":24,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{}}"
		);
		
		Set<String> expected = new HashSet<>();
		Set<String> actual = BuildArampatzisHbc.docsWithEnoughMatches(queries);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void checkThatQueriesComingAfterTheThresholdAreNotConsideredAsKeyQueries() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":24,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}"
		);
		
		Set<String> expected = new HashSet<>();
		Set<String> actual = BuildArampatzisHbc.docsWithEnoughMatches(queries);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void checkThatKeyQueryIsRecognized() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":24,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":3,\"b\":2, \"c\":1}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}"
		);
		
		Set<String> expected = new HashSet<>(Arrays.asList("query-02"));
		Set<String> actual = BuildArampatzisHbc.docsWithEnoughMatches(queries);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void checkThatAllQUeriesAreSubmittedInCaseHbcWouldNotSkipAQuery() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":24,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":3,\"b\":2, \"c\":1}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}"
		);
		
		List<String> expected = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":24,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":3,\"b\":2, \"c\":1}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}"
		);
		List<String> actual = BuildArampatzisHbc.queriesThatWouldBeSubmittedByHbc(queries);
		
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void checkThatAllQUeriesAreSubmittedInCaseHbcWouldNotSkipAQuer2y() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":24,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01 query-02 query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02 query-01 query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-02 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-01 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}"
		);
		
		List<String> expected = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":24,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01 query-02 query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02 query-01 query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-02 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-01 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}"
		);
		List<String> actual = BuildArampatzisHbc.queriesThatWouldBeSubmittedByHbc(queries);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void checkThatQueriesWithTooFewMatchesAreRemoved() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":24,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":3,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01 query-02 query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02 query-01 query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-02 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-01 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}"
		);
		
		List<String> expected = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":24,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":3,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}"
		);
		List<String> actual = BuildArampatzisHbc.queriesThatWouldBeSubmittedByHbc(queries);
		
		
		Assert.assertEquals(expected.size(), actual.size());
		Assert.assertEquals(expected.toString(), actual.toString());
	}
	
	@Test
	public void checkThatQueriesWithAboveKeyqueriesAreRemoved() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":24,\"targetDocs\":{\"a\":3,\"b\":2, \"c\":1}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":300,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01 query-02 query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02 query-01 query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-02 query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-01 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}"
		);
		
		List<String> expected = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-01\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":24,\"targetDocs\":{\"a\":3,\"b\":2, \"c\":1}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":40,\"targetDocs\":{\"a\":300,\"b\":200, \"c\":100}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-03 query-02\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":53,\"targetDocs\":{\"a\":200,\"b\":201, \"c\":202}}"
		);
		List<String> actual = BuildArampatzisHbc.queriesThatWouldBeSubmittedByHbc(queries);
		
		
		Assert.assertEquals(expected.size(), actual.size());
		Assert.assertEquals(expected.toString(), actual.toString());
	}
}
