package de.webis.keyqueries.anserini;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import io.anserini.IndexerTestBase;
import io.anserini.index.IndexArgs;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.search.SearchArgs;

public class RM3KeyqueryRerankerIntegrationTest<T> extends IndexerTestBase {
	
	@Before
	public void setUp() throws Exception {
		super.setUp();
		
		//add some more documents to have non-stopwords during the tests
	    Analyzer analyzer = new EnglishAnalyzer();
	    IndexWriterConfig config = new IndexWriterConfig(analyzer);

	    IndexWriter writer = new IndexWriter(FSDirectory.open(tempDir1), config);

	    FieldType textOptions = new FieldType();
	    textOptions.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
	    textOptions.setStored(true);
	    textOptions.setTokenized(true);
	    textOptions.setStoreTermVectors(true);
	    textOptions.setStoreTermVectorPositions(true);

	    for(int i=0; i<7; i++) {
	    	Document doc1 = new Document();
	    	String doc1Text = "Uninteresting.";
	    	doc1.add(new StringField(IndexArgs.ID, "uninteresting", Field.Store.YES));
	    	doc1.add(new SortedDocValuesField(IndexArgs.ID, new BytesRef("doc1".getBytes())));
	    	doc1.add(new Field(IndexArgs.CONTENTS, doc1Text , textOptions));

	    	doc1.add(new StoredField(IndexArgs.RAW, String.format("{\"contents\": \"%s\"}", doc1Text)));
	    	writer.addDocument(doc1);
	    }
	    
	    writer.commit();
	    writer.forceMerge(1);
	    writer.close();
	}
	
	@Test
	public void testRelevanceFeedback() throws Exception {
		String expected = "{\"doc1\":0.391}";
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		String actual = setRelevanceFeedback(relevanceFeedback, "city");
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testRelevanceFeedbackWithNonMatchingDocument() throws Exception {
		String expected = "{\"doc2\":0.000}";
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 0);
		relevanceFeedback.put("doc2", 1);
		String actual = setRelevanceFeedback(relevanceFeedback, "city");
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testRelevanceFeedbackForMultipleMatchingDocuments() throws Exception {
		String expected = "{\"doc2\":0.659,\"doc1\":0.487}";
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		relevanceFeedback.put("doc2", 1);
		String actual = setRelevanceFeedback(relevanceFeedback, "text");
		
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void testRelevanceFeedbackForMultipleMatchingDocuments2() throws Exception {
		String expected = "{\"doc1\":0.487}";
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		relevanceFeedback.put("doc2", 0);
		String actual = setRelevanceFeedback(relevanceFeedback, "text");
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testRelevanceFeedbackForMultipleMatchingDocuments3() throws Exception {
		String expected = "{\"doc3\":0.887,\"doc2\":0.659,\"doc1\":0.487}";
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		relevanceFeedback.put("doc2", 1);
		relevanceFeedback.put("doc3", 1);
		String actual = setRelevanceFeedback(relevanceFeedback, "test text");
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testRelevanceFeedbackForMultipleMatchingDocuments4() throws Exception {
		String expected = "{\"doc3\":0.887,\"doc1\":0.487}";
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		relevanceFeedback.put("doc3", 1);
		String actual = setRelevanceFeedback(relevanceFeedback, "test text");
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void testRM3Expansion() throws Exception {
		String expected = "{\"some\":0.333,\"test\":0.250,\"text\":0.250,\"citi\":0.167}";
		SearchArgs args = new SearchArgs();
		args.rm3_term = true;
		RerankerContext<T> context = context("test text", args);
		
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		
		String actual = actualRM3Expansion(relevanceFeedback, context);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testRM3Expansion2() throws Exception {
		//more is a stopword
		String expected = "{\"test\":0.250,\"text\":0.250}";
		SearchArgs args = new SearchArgs();
		args.rm3_term = true;
		RerankerContext<T> context = context("test text", args);
		
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc2", 1);
		
		String actual = actualRM3Expansion(relevanceFeedback, context);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testRM3Expansion3() throws Exception {
		String expected = "{\"text\":0.500,\"some\":0.333,\"citi\":0.167}";
		SearchArgs args = new SearchArgs();
		args.rm3_term = true;
		RerankerContext<T> context = context("text", args);
		
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		String actual = actualRM3Expansion(relevanceFeedback, context);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testRM3Expansion4() throws Exception {
		String expected = "{\"text\":0.500,\"some\":0.333,\"citi\":0.167,\"test\":0.000}";
		SearchArgs args = new SearchArgs();
		args.rm3_term = true;
		RerankerContext<T> context = context("text", args);
		
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		relevanceFeedback.put("doc2", 1);
		relevanceFeedback.put("doc3", 1);
		String actual = actualRM3Expansion(relevanceFeedback, context);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testRM3Expansion5() throws Exception {
		String expected = "{\"test\":1.000}";
		SearchArgs args = new SearchArgs();
		args.rm3_term = true;
		RerankerContext<T> context = context("test", args);
		
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc3", 1);
		String actual = actualRM3Expansion(relevanceFeedback, context);
		
		assertEquals(expected, actual);
	}
	
	@Test
	@Ignore
	public void testRM3Expansion6() throws Exception {
		String expected = "{\"test\":1.000}";
		SearchArgs args = new SearchArgs();
		args.rm3_term = true;
		RerankerContext<T> context = context("test", args);
		
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		String actual = actualRM3Expansion(relevanceFeedback, context);
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testRM3ExpansionWithAllowedWords() throws Exception {
		String expected = "{\"text\":0.500,\"citi\":0.167}";
		SearchArgs args = new SearchArgs();
		args.rm3_term = true;
		RerankerContext<T> context = context("text", args);
		
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		relevanceFeedback.put("doc2", 1);
		relevanceFeedback.put("doc3", 1);
		String actual = actualRM3Expansion(relevanceFeedback, context, "citi");
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testRM3ExpansionWithNonExistingAllowedWords() throws Exception {
		String expected = "{\"text\":0.500}";
		SearchArgs args = new SearchArgs();
		args.rm3_term = true;
		RerankerContext<T> context = context("text", args);
		
		Map<String, Integer> relevanceFeedback = new LinkedHashMap<>();
		relevanceFeedback.put("doc1", 1);
		relevanceFeedback.put("doc2", 1);
		relevanceFeedback.put("doc3", 1);
		String actual = actualRM3Expansion(relevanceFeedback, context, "CiTi");
		
		assertEquals(expected, actual);
	}
	
	private String actualRM3Expansion(Map<String, Integer> relevanceFeedback, RerankerContext<T> context, String...allowedTerms) {
		RM3KeyqueryReranker<T> reranker = new RM3KeyqueryReranker<>(2, 3, 10, 0.5);
		reranker.setRelevanceFeedback(relevanceFeedback, context);

		Set<String> terms = Stream.of(allowedTerms).collect(Collectors.toSet());
		Map<String, Float> actual = reranker.getTermsAndWeights(context, terms);
		
		String ret = actual.entrySet().stream()
			.sorted((a,b) -> Double.compare(b.getValue(), a.getValue()))
			.map(i -> "\""+ i.getKey() +"\":" + String.format("%.3f", i.getValue()).replace(",", "."))
			.collect(Collectors.joining(",")); 
		
		return "{" + ret + "}";
	}
	
	private String setRelevanceFeedback(Map<String, Integer> relevanceFeedback, String query) throws IOException {
		RM3KeyqueryReranker<T> reranker = new RM3KeyqueryReranker<>(2, 3, 10, 0.5);
		RerankerContext<T> context = context(query, new SearchArgs());
		
		reranker.setRelevanceFeedback(relevanceFeedback, context);
		ScoredDocuments retDocs = reranker.getRelevanceFeedback();
		List<String> kv = new ArrayList<>();
		for(int i=0; i< retDocs.ids.length; i++) {
			kv.add("\""+ retDocs.documents[i].get("id") +"\":" + String.format("%.3f", retDocs.scores[i]).replace(",", ".")); 
		}
		
		return "{" + kv.stream().collect(Collectors.joining(",")) + "}";
	}
	
	protected RerankerContext<T> context(String query, SearchArgs args) {
		try {
			IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(tempDir1)));
			return new RerankerContext<>(searcher, null, null, null, query, null, null, args);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
