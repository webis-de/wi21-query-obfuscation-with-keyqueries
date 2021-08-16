package de.webis.keyqueries.generators.elasticsearch;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ElasticsearchDocumentTfIdfKeyQueryCandidateGeneratorIntegrationTest {

	@Test
	public void testWithTopDocumentForQueryPokemon() {
		ElasticsearchDocumentTfIdfKeyQueryCandidateGenerator kqGenerator = new ElasticsearchDocumentTfIdfKeyQueryCandidateGenerator(3, "webis_warc_clueweb09_003", "warcrecord");
		
		List<String> expected = Arrays.asList("pokemon", "ash", "pokemon ash", "pikachu", "pokemon pikachu", "ash pikachu", "pokemon ash pikachu");
		List<String> actual = kqGenerator.generateCandidates(new HashSet<>(Arrays.asList("2506c13f-5211-5630-a3a3-307bc707e564")));
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testWithTopDocumentForQueryHowToBuildABomb() {
		ElasticsearchDocumentTfIdfKeyQueryCandidateGenerator kqGenerator = new ElasticsearchDocumentTfIdfKeyQueryCandidateGenerator(4, "webis_warc_clueweb09_003", "warcrecord");
		
		List<String> expected = Arrays.asList("fusion", "fission", "fusion fission", "hydrogen", "fusion hydrogen", "fission hydrogen", "fusion fission hydrogen", "bomb", "fusion bomb", "fission bomb", "fusion fission bomb", "hydrogen bomb", "fusion hydrogen bomb", "fission hydrogen bomb", "fusion fission hydrogen bomb");
		List<String> actual = kqGenerator.generateCandidates(new HashSet<>(Arrays.asList("4f1e2bca-03f2-5e88-9a8a-1cac5f819f33")));
		
		Assert.assertEquals(expected, actual);
	}
}
