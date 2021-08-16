package de.webis.crypsor;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class AnalyzeRunFileTest {
	@Test
	public void bla() {
		String query = "cities city cities";
		List<String> expected = Arrays.asList("citi", "citi", "citi");
		List<String> actual = AnalyzeRunFile.tokensInQuery(query);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void bla2() {
		List<String> queries = EvaluateQuerySavingsByHbcTest.queries(
			"cities city cities",
			"city foo",
			"foo bar foo"
		);
		String expected = "{\"topic\":2,\"vocabularySize\":3,\"meanTokensInQuery\":2.6666666666666665,\"retrievalModel\":\"bm25\",\"queries\":3,\"scramblingApproach\":\"scr\"}";
		String actual = AnalyzeRunFile.queryStatistics(queries, 2, "scr", "bm25").toString();
		
		
		Assert.assertEquals(expected, actual);
	}
}
