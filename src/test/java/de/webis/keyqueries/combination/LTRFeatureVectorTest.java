/*package de.webis.keyqueries.combination;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

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
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.LuceneTestCase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.anserini.index.IndexArgs;
import io.anserini.search.similarity.DocumentSimilarityScore;


public class LTRFeatureVectorTest extends LuceneTestCase {
	protected Path tempDir1;
	
	public void buildTestIndex() throws IOException {
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
	    String doc1Text = "here is some text here is some more text. city.";
	    doc1.add(new StringField(IndexArgs.ID, "doc1", Field.Store.YES));
	    doc1.add(new SortedDocValuesField(IndexArgs.ID, new BytesRef("doc1".getBytes())));
	    doc1.add(new Field(IndexArgs.CONTENTS, doc1Text , textOptions));
	    // specifically demonstrate how "contents" and "raw" might diverge:
	    doc1.add(new StoredField(IndexArgs.RAW, String.format("{\"contents\": \"%s\"}", doc1Text)));
	    writer.addDocument(doc1);

	    Document doc2 = new Document();
	    String doc2Text = "more texts";
	    doc2.add(new StringField(IndexArgs.ID, "doc2", Field.Store.YES));
	    doc2.add(new SortedDocValuesField(IndexArgs.ID, new BytesRef("doc2".getBytes())));
	    doc2.add(new Field(IndexArgs.CONTENTS, doc2Text, textOptions));  // Note plural, to test stemming
	    // specifically demonstrate how "contents" and "raw" might diverge:
	    doc2.add(new StoredField(IndexArgs.RAW, String.format("{\"contents\": \"%s\"}", doc2Text)));
	    writer.addDocument(doc2);

	    Document doc3 = new Document();
	    String doc3Text = "here is a test";
	    doc3.add(new StringField(IndexArgs.ID, "doc3", Field.Store.YES));
	    doc3.add(new SortedDocValuesField(IndexArgs.ID, new BytesRef("doc3".getBytes())));
	    doc3.add(new Field(IndexArgs.CONTENTS, doc3Text, textOptions));
	    // specifically demonstrate how "contents" and "raw" might diverge:
	    doc3.add(new StoredField(IndexArgs.RAW, String.format("{\"contents\": \"%s\"}", doc3Text)));
	    writer.addDocument(doc3);

	    writer.commit();
	    writer.forceMerge(1);
	    writer.close();

	    dir.close();
	}
	
	@Test
	public void checkFeatureVector() throws IOException {
		Directory d = FSDirectory.open(tempDir1);
		DirectoryReader reader = DirectoryReader.open(d);
		DocumentSimilarityScore sim = new DocumentSimilarityScore(reader);
		//doc1
		String expected = "0 qid:0 1:2 2:2 3:1";
		List<String> keyqueries = Arrays.asList("some text", "city", "here test");
		int size = keyqueries.size();
		String documentId = "doc1";
		String actual = FeatureVectorToString(sim.bm25Similarity(size, keyqueries, documentId)).strip();
		Assert.assertEquals(expected, actual);
		//doc2
		expected = "0 qid:0 1:1 2:0 3:0";
		documentId = "doc2";
		actual = FeatureVectorToString(sim.bm25Similarity(size, keyqueries, documentId)).strip();
		Assert.assertEquals(expected, actual);
		//doc3
		expected = "0 qid:0 1:0 2:0 3:2";
		documentId = "doc3";
		actual = FeatureVectorToString(sim.bm25Similarity(size, keyqueries, documentId)).strip();
		Assert.assertEquals(expected, actual);
	}
	
	public String FeatureVectorToString(List<Float> input) {
		String tmp = "";
		for(int i=0; i<input.size(); i++) {
			tmp+= (i+1) +":" +input.get(i) +" ";
		}
		return tmp;
	}
	
	@Before
	@Override
	public void setUp() throws Exception {
	   super.setUp();
	   tempDir1 = createTempDir();
	   buildTestIndex();
	}

	@After
	@Override
	public void tearDown() throws Exception {
	  // Call garbage collector for Windows compatibility
	  System.gc();
	  super.tearDown();
	}
}*/
