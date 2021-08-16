package de.webis.keyqueries.anserini;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.junit.Assert;
import org.junit.Test;

import io.anserini.IndexerTestBase;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;

public class MatchAllDocumtsRerankerTest<T> extends IndexerTestBase {

  @Test
  public void testNullIsReranked() throws IOException {
    Reranker<T> reranker = new MatchAllDocumtsReranker<>();
    Set<String> expected = new HashSet<>(Arrays.asList("doc1", "doc2", "doc3"));

    ScoredDocuments docs = reranker.rerank(null, context());

    Assert.assertEquals(expected, toSet(docs));
  }

  @Test(expected=AssertionError.class)
  public void test() throws IOException {
    Reranker<T> reranker = new MatchAllDocumtsReranker<>();
    Set<String> expected = new HashSet<>(Arrays.asList("doc21", "doc2", "doc3"));

    ScoredDocuments docs = reranker.rerank(null, context());

    Assert.assertEquals(expected, toSet(docs));
  }

  private RerankerContext<T> context() throws IOException {
    IndexReader indexReader = DirectoryReader.open(FSDirectory.open(tempDir1));
    return new RerankerContext<T>(new IndexSearcher(indexReader), null, null, null, null, null, null, null);
  }

  private static Set<String> toSet(ScoredDocuments docs) {
    return Stream.of(docs.documents).map(i -> i.get("id")).collect(Collectors.toSet());
  }
}
