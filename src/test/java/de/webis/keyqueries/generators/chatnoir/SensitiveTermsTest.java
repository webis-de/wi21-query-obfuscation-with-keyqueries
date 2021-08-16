package de.webis.keyqueries.generators.chatnoir;

import org.junit.Assert;
import org.junit.Test;

public class SensitiveTermsTest {
	
	@Test
	public void testForSynonymSensitiveTerms() {
		SensitiveTerms t = SensitiveTerms.getSensitiveTermsWithSynonyms("gun");
		
		Assert.assertTrue(t.phraseIsDeniedByUser("gun is there"));
		Assert.assertTrue(t.phraseIsDeniedByUser("see the gun"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the GuN"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the weapon"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the gunslinger"));
		Assert.assertTrue(t.phraseIsDeniedByUser("guns is there"));
		Assert.assertTrue(t.phraseIsDeniedByUser("see the guns"));
		
		Assert.assertFalse(t.phraseIsDeniedByUser(" how is the fuN"));
		Assert.assertFalse(t.phraseIsDeniedByUser(" my rifle is here"));
	}
	
	@Test
	public void testForSynonymsHyponymsAndHypernymsSensitiveTerms() {
		SensitiveTerms t = SensitiveTerms.getSensitiveTermsWithSynonymsHyponymsAndHypernyms("gun");
		
		Assert.assertTrue(t.phraseIsDeniedByUser("gun is there"));
		Assert.assertTrue(t.phraseIsDeniedByUser("see the gun"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the GuN"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the weapon"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the gunslinger"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" my rifle is here"));
		Assert.assertTrue(t.phraseIsDeniedByUser("guns is there"));
		Assert.assertTrue(t.phraseIsDeniedByUser("see the guns"));
		
		Assert.assertFalse(t.phraseIsDeniedByUser(" how is the fuN"));
	}
	
	@Test
	public void testForSynonymsSensitiveTermsMultiWordQuery() {
		SensitiveTerms t = SensitiveTerms.getSensitiveTermsWithSynonyms("gun jacket");
		
		Assert.assertTrue(t.phraseIsDeniedByUser("gun is there"));
		Assert.assertTrue(t.phraseIsDeniedByUser("see the gun"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the GuN"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the weapon"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the gunslinger"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" My JackEt is there"));
		Assert.assertTrue(t.phraseIsDeniedByUser("guns is there"));
		Assert.assertTrue(t.phraseIsDeniedByUser("see the guns"));		
		
		Assert.assertFalse(t.phraseIsDeniedByUser(" my rifle is here"));
		Assert.assertFalse(t.phraseIsDeniedByUser(" how is the fuN"));
	}
	
	@Test
	public void testForSynonymsHyponymsAndHypernymsSensitiveTermsMultiWordQuery() {
		SensitiveTerms t = SensitiveTerms.getSensitiveTermsWithSynonymsHyponymsAndHypernyms("gun jacket");
		
		Assert.assertTrue(t.phraseIsDeniedByUser("gun is there"));
		Assert.assertTrue(t.phraseIsDeniedByUser("see the gun"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the GuN"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the weapon"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the gunslinger"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" My JackEt is there"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" my rifle is here"));
		Assert.assertTrue(t.phraseIsDeniedByUser("guns is there"));
		Assert.assertTrue(t.phraseIsDeniedByUser("see the guns"));
		
		Assert.assertFalse(t.phraseIsDeniedByUser(" how is the fuN"));
	}
	
	@Test
	public void testForSynonymSensitiveTermsWithPr() {
		SensitiveTerms t = SensitiveTerms.getSensitiveTermsWithSynonyms("City");
		
		Assert.assertTrue(t.phraseIsDeniedByUser("cities"));
		Assert.assertTrue(t.phraseIsDeniedByUser("City"));
		Assert.assertTrue(t.phraseIsDeniedByUser(" how is the citY"));
	}
}