package de.webis.crypsor;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.webis.keyqueries.selection.ArampatzisPMISelectionStrategy;

public class ArampatzisPmiSelectionTest {

	@Test
	public void pmiExample1() {
		String scrambledQueryResult = "{\"privateQuery\":\"p\",\"scrambledQuery\":\"s\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": 5,\"b\":6}}";
		
		double expected = 15.3296;
		double actual = ArampatzisPMISelectionStrategy.getMI(scrambledQueryResult);
		
		Assert.assertEquals(expected, actual, 1e-3);
	}
	
	@Test
	public void pmiExample2() {
		String scrambledQueryResult = "{\"privateQuery\":\"p\",\"scrambledQuery\":\"s\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": 50,\"b\":6}}";
		
		// fewer target documents found than in pmiExample1.
		// (Due to cutoff at 10)
		// hence the score should be lower than 15.3296
		double expected = 14.6364;
		double actual = ArampatzisPMISelectionStrategy.getMI(scrambledQueryResult);
		
		Assert.assertEquals(expected, actual, 1e-3);
	}
	
	@Test
	public void pmiExample2WithNull() {
		String scrambledQueryResult = "{\"privateQuery\":\"p\",\"scrambledQuery\":\"s\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": null,\"b\":6}}";
		
		// fewer target documents found than in pmiExample1.
		// hence the score should be lower than 15.3296
		double expected = 14.6364;
		double actual = ArampatzisPMISelectionStrategy.getMI(scrambledQueryResult);
		
		Assert.assertEquals(expected, actual, 1e-3);
	}
	
	@Test
	public void pmiExample3() {
		String scrambledQueryResult = "{\"privateQuery\":\"p\",\"scrambledQuery\":\"s\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":7,\"targetDocs\":{\"a\": 5,\"b\":6}}";
		
		// fewer results than in pmiExample1.
		// hence the score should be higher than 15.3296
		double expected = 15.7816;
		double actual = ArampatzisPMISelectionStrategy.getMI(scrambledQueryResult);
		
		Assert.assertEquals(expected, actual, 1e-3);
	}
	
	@Test
	public void pmiExample4() {
		String scrambledQueryResult = "{\"privateQuery\":\"p\",\"scrambledQuery\":\"s\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":100,\"targetDocs\":{\"a\": 5,\"b\":6}}";
		
		// more results than in pmiExample1.
		// hence the score should be lower than 15.3296
		double expected = 13.1223;
		double actual = ArampatzisPMISelectionStrategy.getMI(scrambledQueryResult);
		
		Assert.assertEquals(expected, actual, 1e-3);
	}
	

	@Test
	public void pmiExample5() {
		String scrambledQueryResult = "{\"privateQuery\":\"p\",\"scrambledQuery\":\"s\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": 50,\"b\":23}}";
		
		// fewer target documents found than in pmiExample2.
		// (Due to cutoff at 10)
		// hence the score should be lower than 14.6364
		double expected = Double.NEGATIVE_INFINITY;
		double actual = ArampatzisPMISelectionStrategy.getMI(scrambledQueryResult);
		
		Assert.assertEquals(expected, actual, 1e-3);
	}
	
	@Test
	public void pmiExample5WithNull() {
		String scrambledQueryResult = "{\"privateQuery\":\"p\",\"scrambledQuery\":\"s\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": null,\"b\":null}}";
		
		// fewer target documents found than in pmiExample2.
		// hence the score should be lower than 14.6364
		double expected = Double.NEGATIVE_INFINITY;
		double actual = ArampatzisPMISelectionStrategy.getMI(scrambledQueryResult);
		
		Assert.assertEquals(expected, actual, 1e-3);
	}
	
	@Test
	public void approveSelectedQueriesWithMultipleQueriesPMI() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-not-listed\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":1,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": 5,\"b\":6}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":20,\"targetDocs\":{\"a\": 2,\"b\":1}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 3\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":50,\"targetDocs\":{\"a\": 3,\"b\":1}}"
		);
		
		String expected = "{\"1\":\"query 1\",\"2\":\"query 2\",\"3\":\"query 3\"}";
		String actual = MainQuerySelection.selectTop(queries, 10, "pmi");
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void approveSelectedQueriesWithMultipleQueriesPMI2() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-not-listed\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":1,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": 22,\"b\":6}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": 2,\"b\":1}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 3\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": 24,\"b\":54}}"
		);
		
		String expected = "{\"1\":\"query 2\",\"2\":\"query 1\",\"3\":\"query 3\"}";
		String actual = MainQuerySelection.selectTop(queries, 10, "pmi");
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void approveSelectedQueriesWithMultipleQueriesPMI3() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-not-listed\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":1,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":30,\"targetDocs\":{\"a\": 5,\"b\":6}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":20,\"targetDocs\":{\"a\": 2,\"b\":1}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 3\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":10,\"targetDocs\":{\"a\": 3,\"b\":1}}"
		);
		
		String expected = "{\"1\":\"query 3\",\"2\":\"query 2\",\"3\":\"query 1\"}";
		String actual = MainQuerySelection.selectTop(queries, 10, "pmi");
		
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void approveSelectedQueriesWithMultipleQueriesPMI4() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-not-listed\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":1,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": 22,\"b\":6}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": 22,\"b\":20}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 3\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": 1,\"b\":2}}"
		);
		
		String expected = "{\"1\":\"query 3\",\"2\":\"query 1\",\"3\":\"query 2\"}";
		String actual = MainQuerySelection.selectTop(queries, 10, "pmi");
		
		Assert.assertEquals(expected, actual);
	}
	

	@Test
	public void approveSelectedQueriesWithMultipleQueriesPMI5() {
		List<String> queries = Arrays.asList(
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query-not-listed\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":1,\"targetDocs\":{}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 1\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": null,\"b\":6}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 2\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": null,\"b\":null}}",
			"{\"privateQuery\":\"foo-bar\",\"scrambledQuery\":\"query 3\",\"approach\":\"a\",\"hitsForPrivateQuery\":1000,\"hitsForScrambledQuery\":11,\"targetDocs\":{\"a\": 1,\"b\":2}}"
		);
		
		String expected = "{\"1\":\"query 3\",\"2\":\"query 1\"}";
		String actual = MainQuerySelection.selectTop(queries, 10, "pmi");
		
		Assert.assertEquals(expected, actual);
	}
}
