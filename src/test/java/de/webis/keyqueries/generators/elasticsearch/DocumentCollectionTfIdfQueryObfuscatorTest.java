package de.webis.keyqueries.generators.elasticsearch;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import de.webis.keyqueries.generators.DocumentCollectionTfIdfKeyQueryCandidateGenerator;
import de.webis.keyqueries.generators.DocumentCollectionTfIdfKeyQueryCandidateGenerator.CombinedTerm;
import de.webis.keyqueries.generators.DocumentTfIdfKeyQueryCandidateGenerator.TermWithScore;

public class DocumentCollectionTfIdfQueryObfuscatorTest {
	@Test
	public void approveTransformationOfTermVectors() {
		List<List<TermWithScore>> terms = Arrays.asList(
				Arrays.asList(t("apfel", 10), t("birne", 9)),
				Arrays.asList(t("apfel", 8), t("birne", 11)),
				Arrays.asList(t("birne", 1), t("pflaume", 3))
		);
		List<CombinedTerm> expected = Arrays.asList(
				new CombinedTerm("apfel", 2, 10f),
				new CombinedTerm("birne", 3, 11f),
				new CombinedTerm("pflaume", 1, 3f)
		);
		List<CombinedTerm> actual = DocumentCollectionTfIdfKeyQueryCandidateGenerator.combine(terms);
		
		Assert.assertEquals(
			expected.stream().map(i -> i.toString()).collect(Collectors.toSet()),
			actual.stream().map(i -> i.toString()).collect(Collectors.toSet())
		);
	}
	
	@Test
	public void selectTopTerms1() {
		List<CombinedTerm> input = Arrays.asList(
				new CombinedTerm("apfel", 2, 10f),
				new CombinedTerm("birne", 3, 11f),
				new CombinedTerm("pflaume", 1, 3f)
		);
		List<String> expected = Arrays.asList("birne");
		
		List<String> actual = DocumentCollectionTfIdfKeyQueryCandidateGenerator.selectTop(input,1);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void selectTopTerms2() {
		List<CombinedTerm> input = Arrays.asList(
				new CombinedTerm("apfel", 3, 12f),
				new CombinedTerm("birne", 3, 11f),
				new CombinedTerm("pflaume", 1, 3f)
		);
		List<String> expected = Arrays.asList("apfel");
		
		List<String> actual = DocumentCollectionTfIdfKeyQueryCandidateGenerator.selectTop(input,1);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void selectTopTerms3() {
		List<CombinedTerm> input = Arrays.asList(
				new CombinedTerm("apfel", 3, 10f),
				new CombinedTerm("birne", 3, 11f),
				new CombinedTerm("pflaume", 1, 3f)
		);
		List<String> expected = Arrays.asList("birne");
		
		List<String> actual = DocumentCollectionTfIdfKeyQueryCandidateGenerator.selectTop(input,1);
		
		Assert.assertEquals(expected, actual);
	}
	
	private static TermWithScore t(String term, float score) {
		return new TermWithScore(term, score);
	}
}