package de.webis.crypsor;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CopyArampatzisQueriesThatAreNotSensitiveTest {
	@Test
	public void bla() {
		List<String> queries = EvaluateQuerySavingsByHbcTest.queries(
			"cities city cities",
			"city foo",
			"foo basr foo"
		);
		List<String> expected = EvaluateQuerySavingsByHbcTest.queries(
			"cities city cities",
			"city foo",
			"foo basr foo"
		);
		
		List<String> actual = CopyArampatzisQueriesThatAreNotSensitive.retainNonSensitiveQueries(queries);
		
		Assert.assertEquals(expected.toString(), actual.toString());
	}
	
	@Test
	public void bla2() {
		List<String> queries = EvaluateQuerySavingsByHbcTest.queries(
			"cities city cities",
			"city foo",
			"foo bar foo"
		);
		List<String> expected = EvaluateQuerySavingsByHbcTest.queries(
			"cities city cities",
			"city foo"
		);
		
		List<String> actual = CopyArampatzisQueriesThatAreNotSensitive.retainNonSensitiveQueries(queries);
		
		Assert.assertEquals(expected.toString(), actual.toString());
	}
}
