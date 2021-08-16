package de.webis.keyqueries.anserini;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.Assert;
import org.junit.Test;

import de.webis.keyqueries.Searcher;
import io.anserini.IndexerWithEmptyDocumentTestBase;

public class LuceneSearcherTest extends IndexerWithEmptyDocumentTestBase {
	@Test
	public void testForNonExistingQuery() {
		Searcher searcher = new LuceneSearcher(searcher(), new EnglishAnalyzer());
		List<String> expected = Collections.emptyList();
		
		List<String> actual = searcher.search("not existing", 10);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testForQueryThatMatchesOnlyOneDocument() {
		Searcher searcher = new LuceneSearcher(searcher(), new EnglishAnalyzer());
		List<String> expected = Arrays.asList("doc1");
		
		List<String> actual = searcher.search("city", 10);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testForQueryThatMatchesNoDocumentDueToWrongAnalyzer() {
		Searcher searcher = new LuceneSearcher(searcher(), new StandardAnalyzer());
		List<String> expected = Collections.emptyList();
		
		List<String> actual = searcher.search("city", 10);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testForQueryThatMatchesMultipleDocuments() {
		Searcher searcher = new LuceneSearcher(searcher(), new EnglishAnalyzer());
		List<String> expected = Arrays.asList("doc2", "doc1");
		
		List<String> actual = searcher.search("text", 10);
		
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testForQueryThatMatchesMultipleDocumentsWithSize1() {
		Searcher searcher = new LuceneSearcher(searcher(), new EnglishAnalyzer());
		List<String> expected = Arrays.asList("doc2");
		
		List<String> actual = searcher.search("text", 1);
		
		Assert.assertEquals(expected, actual);
	}
	
	private IndexSearcher searcher() {
		try {
			return new IndexSearcher(reader());
		} catch(IOException e) {
			throw new RuntimeException(e);
		}
	}

	private IndexReader reader() throws IOException {
		Directory dir = FSDirectory.open(Paths.get(tempDir1.toString()));
		return DirectoryReader.open(dir);
	}
}
