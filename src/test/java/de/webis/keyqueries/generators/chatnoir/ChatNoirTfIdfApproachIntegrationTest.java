package de.webis.keyqueries.generators.chatnoir;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.webis.crypsor.CrypsorArgs;
import de.webis.crypsor.CrypsorArgsTest;
import de.webis.crypsor.Main;
import io.anserini.IndexerWithEmptyDocumentTestBase;
import io.anserini.index.IndexArgs;

public class ChatNoirTfIdfApproachIntegrationTest extends IndexerWithEmptyDocumentTestBase {

	@Test
	public void testForSampleDocument3() throws IOException {
		String privateQuery = "test";
		CrypsorKeyQueryCandidateGenerator gen = generator(privateQuery);

		List<String> expected = new ArrayList<>(Arrays.asList());
		List<String> actual = gen.getCandidates(privateQuery,
				new HashSet<>(Arrays.asList("clueweb09-en0051-90-00849")));

		Collections.sort(expected);
		Collections.sort(actual);

		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testForSampleDocument() throws IOException {
		String privateQuery = "hello world";
		CrypsorKeyQueryCandidateGenerator gen = generator(privateQuery);

		List<String> expected = new ArrayList<>(Arrays.asList("test"));
		List<String> actual = gen.getCandidates(privateQuery,
				new HashSet<>(Arrays.asList("clueweb09-en0051-90-00849")));

		Collections.sort(expected);
		Collections.sort(actual);

		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testForSampleDocument22() throws IOException {
		String privateQuery = "hello world";
		CrypsorKeyQueryCandidateGenerator gen = generator(privateQuery);

		List<String> expected = new ArrayList<>(
				Arrays.asList("Cities", "Some", "Some Cities", "Some text", "Some text Cities", "text", "text Cities"));
		List<String> actual = gen.getCandidates(privateQuery,
				new HashSet<>(Arrays.asList("clueweb09-enwp01-81-22839")));

		Collections.sort(expected);
		Collections.sort(actual);

		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testForSampleDocument23() throws IOException {
		String privateQuery = "some cities";
		CrypsorKeyQueryCandidateGenerator gen = generator(privateQuery);

		List<String> expected = new ArrayList<>(Arrays.asList("text"));
		List<String> actual = gen.getCandidates(privateQuery,
				new HashSet<>(Arrays.asList("clueweb09-enwp01-81-22839")));

		Collections.sort(expected);
		Collections.sort(actual);

		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testForSampleDocument24() throws IOException {
		String privateQuery = "hello world";
		CrypsorKeyQueryCandidateGenerator gen = generator(privateQuery);

		List<String> expected = new ArrayList<>(Arrays.asList("Cities", "test", "Some", "Some Cities", "Some text",
				"Some text Cities", "text", "text Cities"));
		List<String> actual = gen.getCandidates(privateQuery,
				new HashSet<>(Arrays.asList("clueweb09-enwp01-81-22839", "clueweb09-en0051-90-00849")));

		Collections.sort(expected);
		Collections.sort(actual);

		Assert.assertEquals(expected, actual);
	}

	private CrypsorKeyQueryCandidateGenerator generator(String query) {
		CrypsorArgs args = CrypsorArgsTest.args("-topic", "17", "-bm25", "-index", tempDir1.toString(), "-output", "a",
				"-scramblingApproach", "tf-idf");
		Main m = new Main(args) {
			@Override
			public String readTopic(CrypsorArgs crypsorArgs) {
				return query;
			}
		};

		return m.generator();
	}

	@Before
	@Override
	public void setUp() throws Exception {
		super.setUp();
		buildTestIndex();
	}

	// A very simple example of how to build an index.
	// Creates an index similar to IndexerTestBase, but adds an empty document to
	// test error handling.
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
		// https://www.chatnoir.eu/cache?uuid=4e84a94f-5bb2-5671-94c0-ed15a28e5e6e&index=cw09
		doc1.add(new StringField(IndexArgs.ID, "clueweb09-enwp01-81-22839", Field.Store.YES));
		doc1.add(new SortedDocValuesField(IndexArgs.ID, new BytesRef("doc1".getBytes())));
		doc1.add(new Field(IndexArgs.CONTENTS, doc1Text, textOptions));
		doc1.add(new StoredField(IndexArgs.RAW, doc1Text));
		writer.addDocument(doc1);

		Document doc2 = new Document();
		String doc2Text = "more texts";
		doc2.add(new StringField(IndexArgs.ID, "doc2", Field.Store.YES));
		doc2.add(new SortedDocValuesField(IndexArgs.ID, new BytesRef("doc2".getBytes())));
		doc2.add(new Field(IndexArgs.CONTENTS, doc2Text, textOptions)); // Note plural, to test stemming
		doc2.add(new StoredField(IndexArgs.RAW, doc2Text));
		writer.addDocument(doc2);

		Document doc3 = new Document();
		String doc3Text = "here is a test";
		// https://www.chatnoir.eu/cache?uuid=fb0815ff-0059-59cc-8d50-86f1c60264a9&index=cw09
		doc3.add(new StringField(IndexArgs.ID, "clueweb09-en0051-90-00849", Field.Store.YES));
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

		for (int i = 10; i < 30; i++) {
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