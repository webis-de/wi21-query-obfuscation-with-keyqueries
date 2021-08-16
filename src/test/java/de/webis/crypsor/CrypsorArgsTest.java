package de.webis.crypsor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import io.anserini.IndexerTestBase;

public class CrypsorArgsTest extends IndexerTestBase {

	@Test(expected = Exception.class)
	public void checkThatExceptionIsThrownForNonExistingTopic215() {
		CrypsorArgs args = args("-topic", "215", "-bm25", "-index", tempDir1.toString(), "-output", "a", "-scramblingApproach", "a");
		new Main(args);
	}
	
	@Test
	public void approveTopic17() {
		String expected = "poker tournaments";
		CrypsorArgs args = args("-topic", "17", "-bm25", "-index", tempDir1.toString(), "-output", "a", "-scramblingApproach", "a");
		String actual = new Main(args).getPrivateQuery();
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void approveTopic213() {
		String expected = "carpal tunnel syndrome";
		CrypsorArgs args = args("-topic", "213", "-bm25", "-index", tempDir1.toString(), "-output", "a", "-scramblingApproach", "a");
		String actual = new Main(args).getPrivateQuery();
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void checkConstructionOfMainArgsForQueryCity() {
		CrypsorArgs args = args("-topic", "215", "-bm25", "-index", tempDir1.toString(), "-output", "a", "-scramblingApproach", "a");
		Main m = new Main(args) {
			public String readTopic(CrypsorArgs crypsorArgs) {
				return "city";
			}
		};
		
		Assert.assertEquals(1l , m.getHitsForPrivateQuery());
		Assert.assertEquals(new HashSet<>(Arrays.asList("doc1")) , m.getTargetDocs());
	}
	
	@Test
	public void checkConstructionOfMainArgsForQueryText() {
		CrypsorArgs args = args("-topic", "215", "-bm25", "-index", tempDir1.toString(), "-output", "a", "-scramblingApproach", "a");
		Main m = new Main(args) {
			public String readTopic(CrypsorArgs crypsorArgs) {
				return "text";
			}
		};
		
		Assert.assertEquals(2l , m.getHitsForPrivateQuery());
		Assert.assertEquals(new HashSet<>(Arrays.asList("doc1", "doc2")) , m.getTargetDocs());
	}
	
	@Test
	public void checkScrambledQueryResults() {
		CrypsorArgs args = args("-topic", "215", "-scramblingApproach", "non-existing", "-bm25", "-index", tempDir1.toString(), "-output", "a");
		Main m = new Main(args) {
			public String readTopic(CrypsorArgs crypsorArgs) {
				return "city";
			}
		};
		
		Assert.assertEquals(1l , m.getHitsForPrivateQuery());
		Assert.assertEquals(new HashSet<>(Arrays.asList("doc1")) , m.getTargetDocs());
		
		List<String> expected = Arrays.asList("{\"privateQuery\":\"city\",\"scrambledQuery\":\"test\",\"approach\":\"non-existing\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":1,\"targetDocs\":{\"doc1\":null}}");
		List<String> actual = m.runScrambledQueries(Arrays.asList("test"));
		
		Assert.assertEquals(expected.toString(), actual.toString());
	}
	
	@Test
	public void checkScrambledQueryResultsForQueryYork() {
		CrypsorArgs args = args("-topic", "215", "-scramblingApproach", "non-existing", "-bm25", "-index", tempDir1.toString(), "-output", "a");
		Main m = new Main(args) {
			public String readTopic(CrypsorArgs crypsorArgs) {
				return "city";
			}
		};
		
		Assert.assertEquals(1l , m.getHitsForPrivateQuery());
		Assert.assertEquals(new HashSet<>(Arrays.asList("doc1")) , m.getTargetDocs());
		
		List<String> expected = Arrays.asList("{\"privateQuery\":\"city\",\"scrambledQuery\":\"york\",\"approach\":\"non-existing\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":0,\"targetDocs\":{\"doc1\":null}}");
		List<String> actual = m.runScrambledQueries(Arrays.asList("york"));
		
		Assert.assertEquals(expected.toString(), actual.toString());
	}
	
	@Test
	public void checkScrambledQueryResultsForQueryText() {
		CrypsorArgs args = args("-topic", "215", "-scramblingApproach", "non-existing", "-bm25", "-index", tempDir1.toString(), "-output", "a");
		Main m = new Main(args) {
			public String readTopic(CrypsorArgs crypsorArgs) {
				return "city";
			}
		};
		
		Assert.assertEquals(1l , m.getHitsForPrivateQuery());
		Assert.assertEquals(new HashSet<>(Arrays.asList("doc1")) , m.getTargetDocs());
		
		List<String> expected = Arrays.asList("{\"privateQuery\":\"city\",\"scrambledQuery\":\"text\",\"approach\":\"non-existing\",\"hitsForPrivateQuery\":1,\"hitsForScrambledQuery\":2,\"targetDocs\":{\"doc1\":1}}");
		List<String> actual = m.runScrambledQueries(Arrays.asList("text"));
		
		Assert.assertEquals(expected.toString(), actual.toString());
	}
	
	@Test
	public void checkScrambledQueryResultsForQueryTextMultipleMatches() {
		CrypsorArgs args = args("-topic", "215", "-scramblingApproach", "non-existing", "-bm25", "-index", tempDir1.toString(), "-output", "a");
		Main m = new Main(args) {
			public String readTopic(CrypsorArgs crypsorArgs) {
				return "text";
			}
		};
		
		Assert.assertEquals(2l , m.getHitsForPrivateQuery());
		Assert.assertEquals(new HashSet<>(Arrays.asList("doc1", "doc2")) , m.getTargetDocs());
		
		List<String> expected = Arrays.asList("{\"privateQuery\":\"text\",\"scrambledQuery\":\"text\",\"approach\":\"non-existing\",\"hitsForPrivateQuery\":2,\"hitsForScrambledQuery\":2,\"targetDocs\":{\"doc1\":1,\"doc2\":2}}");
		List<String> actual = m.runScrambledQueries(Arrays.asList("text"));
		
		Assert.assertEquals(expected.toString(), actual.toString());
	}
	
	@Test
	public void testOutputDir() {
		CrypsorArgs args = args("-topic", "215", "-scramblingApproach", "non-existing", "-bm25", "-index", tempDir1.toString(), "-output", "a");
		Assert.assertEquals("a/non-existing-bm25/215.jsonl", args.getOutputFile());
	}
	
	public static CrypsorArgs args(String...args) {
		CrypsorArgs ret = CrypsorArgs.parse(args);
		if(ret == null) {
			throw new RuntimeException("");
		}
		
		return ret;
	}
}
