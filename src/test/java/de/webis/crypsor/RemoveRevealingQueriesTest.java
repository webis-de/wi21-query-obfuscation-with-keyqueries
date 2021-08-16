package de.webis.crypsor;

import org.junit.Assert;
import org.junit.Test;

public class RemoveRevealingQueriesTest {
	@Test
	public void testThatForNonRevealingQueriesNothingIsChanged() {
		String input = "{\"1\":{\"query\":\"not revealing\",\"ranking\":[\"a\",\"b\"]},\"2\":{\"query\":\"not revealing\",\"ranking\":[\"a\",\"b\"]}}";
		String expected = "{\"1\":{\"query\":\"not revealing\",\"ranking\":[\"a\",\"b\"]},\"2\":{\"query\":\"not revealing\",\"ranking\":[\"a\",\"b\"]}}";

		String actual = RemoveRevealingQueries.removeRevealingQueries(input);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testThatForAllRevealingQueriesThatNothingIsChanged() {
		String input = "{\"1\":{\"query\":\"GMATcritical MLICETS\",\"ranking\":[\"a\",\"b\"]},\"2\":{\"query\":\"PrimeGMAT\",\"ranking\":[\"a\",\"b\"]}}";
		String expected = "{}";

		String actual = RemoveRevealingQueries.removeRevealingQueries(input);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testThatForFirstNonRevealingQueries() {
		String input = "{\"1\":{\"query\":\"not-revealing GMATcritical MLICETS\",\"ranking\":[\"a\",\"b\"]},\"2\":{\"query\":\"PrimeGMAT\",\"ranking\":[\"a\",\"b\"]}}";
		String expected = "{\"1\":{\"query\":\"not-revealing GMATcritical MLICETS\",\"ranking\":[\"a\",\"b\"]}}";

		String actual = RemoveRevealingQueries.removeRevealingQueries(input);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testThatForFirstNonRevealingQueries2() {
		String input = "{\"1\":{\"query\":\"GMATcritical MLICETS\",\"ranking\":[\"a\",\"b\"]},\"2\":{\"query\":\"PrimeGMAT n-r\",\"ranking\":[\"c\",\"d\",\"e\"]}}";
		String expected = "{\"1\":{\"query\":\"PrimeGMAT n-r\",\"ranking\":[\"c\",\"d\",\"e\"]}}";

		String actual = RemoveRevealingQueries.removeRevealingQueries(input);
		
		Assert.assertEquals(expected, actual);
	}
}
