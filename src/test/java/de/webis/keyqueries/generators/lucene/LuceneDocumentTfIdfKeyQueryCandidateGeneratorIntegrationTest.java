package de.webis.keyqueries.generators.lucene;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.webis.keyqueries.generators.DocumentTfIdfKeyQueryCandidateGenerator;
import de.webis.keyqueries.generators.lucene.LuceneDocumentTfIdfKeyQueryCandidateGenerator.ResolveHumanReadableWord;
import io.anserini.IndexerWithEmptyDocumentTestBase;
import io.anserini.index.IndexArgs;

public class LuceneDocumentTfIdfKeyQueryCandidateGeneratorIntegrationTest extends IndexerWithEmptyDocumentTestBase {
	@Test
	public void testForSampleDocument1() throws IOException {
		DocumentTfIdfKeyQueryCandidateGenerator kqGenerator = new LuceneDocumentTfIdfKeyQueryCandidateGenerator(3,
				searcher(), false);

		List<String> expected = new ArrayList<>(
				Arrays.asList("citi", "citi text", "some", "some citi", "some citi text", "some text", "text"));
		List<String> actual = kqGenerator.generateCandidates(new HashSet<>(Arrays.asList("doc1")));

		Collections.sort(expected);
		Collections.sort(actual);

		Assert.assertEquals(expected, actual);
	}

	@Test
	public void approveTermVectors() throws IOException {
		DocumentTfIdfKeyQueryCandidateGenerator kqGenerator = new LuceneDocumentTfIdfKeyQueryCandidateGenerator(3,
				searcher(), false);
		List<String> expected = Arrays.asList("TermWithScore [some; 6.9698133]", "TermWithScore [text; 6.158883]", "TermWithScore [citi; 3.4849067]", "TermWithScore [more; 3.0794415]");
		List<String> actual = kqGenerator.termsSortedByScore("doc1").stream().map(i -> i.toString())
				.collect(Collectors.toList());

		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void approveTermVectors2() throws IOException {
		Map<String, String> docIdToText = new HashMap<>();
		docIdToText.put("doc1", "here is some texts here is some more texts. city.");
		
		LuceneDocumentTfIdfKeyQueryCandidateGenerator.MapResolveHumanReadableWord tmp = new LuceneDocumentTfIdfKeyQueryCandidateGenerator.MapResolveHumanReadableWord(docIdToText);
		
		DocumentTfIdfKeyQueryCandidateGenerator kqGenerator = new LuceneDocumentTfIdfKeyQueryCandidateGenerator(3,
				searcher(), false, tmp);
		List<String> expected = Arrays.asList("TermWithScore [some; 6.9698133]", "TermWithScore [texts; 6.158883]", "TermWithScore [city; 3.4849067]", "TermWithScore [more; 3.0794415]");
		List<String> actual = kqGenerator.termsSortedByScore("doc1").stream().map(i -> i.toString())
				.collect(Collectors.toList());

		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testResolvingOfWordsAllLowerCount() {
		Map<String, String> docIdToText = new HashMap<>();
		docIdToText.put("doc-1", "here is some texts here is some more texts. city.");
		
		String expected = "{citi=city, here=here, more=more, some=some, text=texts}";
		String actual = LuceneDocumentTfIdfKeyQueryCandidateGenerator.MapResolveHumanReadableWord.termToHumanReadable(docIdToText.get("doc-1")).toString();
		Assert.assertEquals(expected, actual);
		
		ResolveHumanReadableWord resolver = new LuceneDocumentTfIdfKeyQueryCandidateGenerator.MapResolveHumanReadableWord(docIdToText);
		
		Assert.assertNull(resolver.token("not-existing-word", "doc-1"));
		Assert.assertEquals("city", resolver.token("citi", "doc-1"));
		Assert.assertEquals("texts", resolver.token("text", "doc-1"));
		Assert.assertEquals("here", resolver.token("here", "doc-1"));
	}
	
	@Test
	public void testResolvingOfWordsSomeUpperCase() {
		Map<String, String> docIdToText = new HashMap<>();
		docIdToText.put("doc-1", "here is some Texts here is some more Texts. City.");
		
		String expected = "{citi=City, here=here, more=more, some=some, text=Texts}";
		String actual = LuceneDocumentTfIdfKeyQueryCandidateGenerator.MapResolveHumanReadableWord.termToHumanReadable(docIdToText.get("doc-1")).toString();
		Assert.assertEquals(expected, actual);
		
		ResolveHumanReadableWord resolver = new LuceneDocumentTfIdfKeyQueryCandidateGenerator.MapResolveHumanReadableWord(docIdToText);
		
		Assert.assertNull(resolver.token("not-existing-word", "doc-1"));
		Assert.assertEquals("City", resolver.token("citi", "doc-1"));
		Assert.assertEquals("Texts", resolver.token("text", "doc-1"));
		Assert.assertEquals("here", resolver.token("here", "doc-1"));
	}

	@Test
	public void testResolvingOfWordsWithMultpleCases() {
		Map<String, String> docIdToText = new HashMap<>();
		docIdToText.put("doc-2", "here is some Texts here is some more Texts. texte. texte. texte. City.");
		
		String expected = "{citi=City, here=here, more=more, some=some, text=texte}";
		String actual = LuceneDocumentTfIdfKeyQueryCandidateGenerator.MapResolveHumanReadableWord.termToHumanReadable(docIdToText.get("doc-2")).toString();
		Assert.assertEquals(expected, actual);
		
		ResolveHumanReadableWord resolver = new LuceneDocumentTfIdfKeyQueryCandidateGenerator.MapResolveHumanReadableWord(docIdToText);
		
		Assert.assertNull(resolver.token("not-existing-word", "doc-2"));
		Assert.assertEquals("City", resolver.token("citi", "doc-2"));
		Assert.assertEquals("texte", resolver.token("text", "doc-2"));
		Assert.assertEquals("here", resolver.token("here", "doc-2"));
	}
	

	@Test
	public void testResolvingOfWordsWithMultpleCases2() {
		Map<String, String> docIdToText = new HashMap<>();
		docIdToText.put("doc-2", "here texts, texts texts, texts. texts. is some Texts here is some more Texts. texte. texte. texte. City.");
		
		String expected = "{citi=City, here=here, more=more, some=some, text=texts}";
		String actual = LuceneDocumentTfIdfKeyQueryCandidateGenerator.MapResolveHumanReadableWord.termToHumanReadable(docIdToText.get("doc-2")).toString();
		Assert.assertEquals(expected, actual);
		
		ResolveHumanReadableWord resolver = new LuceneDocumentTfIdfKeyQueryCandidateGenerator.MapResolveHumanReadableWord(docIdToText);
		
		Assert.assertNull(resolver.token("not-existing-word", "doc-2"));
		Assert.assertEquals("City", resolver.token("citi", "doc-2"));
		Assert.assertEquals("texts", resolver.token("text", "doc-2"));
		Assert.assertEquals("here", resolver.token("here", "doc-2"));
	}

	@Test
	public void approveTermVectorsForDoc2() throws IOException {
		DocumentTfIdfKeyQueryCandidateGenerator kqGenerator = new LuceneDocumentTfIdfKeyQueryCandidateGenerator(3,
				searcher(), false);
		List<String> expected = Arrays.asList("TermWithScore [more; 3.0794415]", "TermWithScore [text; 3.0794415]");
		List<String> actual = kqGenerator.termsSortedByScore("doc2").stream().map(i -> i.toString())
				.collect(Collectors.toList());

		Assert.assertEquals(expected, actual);
	}

	@Test
	public void approveTermVectorsForDoc3() throws IOException {
		DocumentTfIdfKeyQueryCandidateGenerator kqGenerator = new LuceneDocumentTfIdfKeyQueryCandidateGenerator(3,
				searcher(), false);
		List<String> expected = Arrays.asList("TermWithScore [test; 3.4849067]");
		List<String> actual = kqGenerator.termsSortedByScore("doc3").stream().map(i -> i.toString())
				.collect(Collectors.toList());

		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testForSampleDocument4() throws IOException {
		DocumentTfIdfKeyQueryCandidateGenerator kqGenerator = new LuceneDocumentTfIdfKeyQueryCandidateGenerator(3,
				searcher(), false);

		List<String> expected = new ArrayList<>();
		List<String> actual = kqGenerator.generateCandidates(new HashSet<>(Arrays.asList("doc4")));

		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testForSampleDocument3() throws IOException {
		DocumentTfIdfKeyQueryCandidateGenerator kqGenerator = new LuceneDocumentTfIdfKeyQueryCandidateGenerator(3,
				searcher(), false);

		List<String> expected = new ArrayList<>(Arrays.asList(
				"test"
		));
		List<String> actual = kqGenerator.generateCandidates(new HashSet<>(Arrays.asList("doc3")));

		Collections.sort(expected);
		Collections.sort(actual);

		Assert.assertEquals(expected, actual);
	}

	private IndexSearcher searcher() throws IOException {
		return new IndexSearcher(reader());
	}

	private IndexReader reader() throws IOException {
		Directory dir = FSDirectory.open(Paths.get(tempDir1.toString()));
		return DirectoryReader.open(dir);
	}
	
	  @Before
	  @Override
	  public void setUp() throws Exception {
		  super.setUp();
	    buildTestIndex();
	  }
	  
	  // A very simple example of how to build an index.
	  // Creates an index similar to IndexerTestBase, but adds an empty document to test error handling.
	  private void buildTestIndex() throws IOException {
	    Directory dir = FSDirectory.open(tempDir1);

	    Analyzer analyzer = new EnglishAnalyzer();
	    IndexWriterConfig config = new IndexWriterConfig(analyzer);
	    config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

	    IndexWriter writer = new IndexWriter(dir, config);

	    FieldType textOptions = new FieldType();
	    textOptions.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
	    textOptions.setStored(true);
	    textOptions.setTokenized(true);
	    textOptions.setStoreTermVectors(true);
	    textOptions.setStoreTermVectorPositions(true);

	    Document doc1 = new Document();
	    String doc1Text = "here is some texts here is some more texts. city.";
	    doc1.add(new StringField(IndexArgs.ID, "doc1", Field.Store.YES));
	    doc1.add(new SortedDocValuesField(IndexArgs.ID, new BytesRef("doc1".getBytes())));
	    doc1.add(new Field(IndexArgs.CONTENTS, doc1Text , textOptions));
	    doc1.add(new StoredField(IndexArgs.RAW, doc1Text));
	    writer.addDocument(doc1);

	    Document doc2 = new Document();
	    String doc2Text = "more texts";
	    doc2.add(new StringField(IndexArgs.ID, "doc2", Field.Store.YES));
	    doc2.add(new SortedDocValuesField(IndexArgs.ID, new BytesRef("doc2".getBytes())));
	    doc2.add(new Field(IndexArgs.CONTENTS, doc2Text, textOptions));  // Note plural, to test stemming
	    doc2.add(new StoredField(IndexArgs.RAW, doc2Text));
	    writer.addDocument(doc2);

	    Document doc3 = new Document();
	    String doc3Text = "here is a test";
	    doc3.add(new StringField(IndexArgs.ID, "doc3", Field.Store.YES));
	    doc3.add(new SortedDocValuesField(IndexArgs.ID, new BytesRef("doc3".getBytes())));
	    doc3.add(new Field(IndexArgs.CONTENTS, doc3Text, textOptions));
	    doc3.add(new StoredField(IndexArgs.RAW, doc3Text));
	    writer.addDocument(doc3);

	    Document doc4 = new Document();
	    String doc4Text = "";
	    doc4.add(new StringField(IndexArgs.ID, "doc4", Field.Store.YES));
	    doc4.add(new SortedDocValuesField(IndexArgs.ID, new BytesRef("doc4".getBytes())));
	    doc4.add(new Field(IndexArgs.CONTENTS, doc4Text, textOptions));
	    doc4.add(new StoredField(IndexArgs.RAW, doc4Text));
	    writer.addDocument(doc4);

	    for(int i=10; i<30; i++) {
	    	Document doc = new Document();
	    	doc.add(new StringField(IndexArgs.ID, "doc" + i, Field.Store.YES));
	    	doc.add(new SortedDocValuesField(IndexArgs.ID, new BytesRef("doc4".getBytes())));
	    	doc.add(new Field(IndexArgs.CONTENTS, "foo bar here should become a stopword", textOptions));
	    	doc.add(new StoredField(IndexArgs.RAW, "foo bar here should become a stopword"));
		    writer.addDocument(doc);
	    }
	    
	    writer.commit();
	    writer.forceMerge(1);
	    writer.close();

	    dir.close();
	  }
}
