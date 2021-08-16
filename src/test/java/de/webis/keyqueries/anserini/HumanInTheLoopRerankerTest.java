package de.webis.keyqueries.anserini;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.util.Qrels;


public class HumanInTheLoopRerankerTest<T> {

  @SuppressWarnings("unchecked")
  private final RerankerContext<T> context = Mockito.mock(RerankerContext.class);

  @Test
  public void humanInTheLoopWithAscendingInternalK1() {
    HumanInTheLoopTestReranker<T> internalReranker = new HumanInTheLoopTestReranker<>();
    internalReranker.comparator = (a,b) -> a.compareTo(b);
    List<RerankerExecutionStage> expectedStages = Arrays.asList(
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("a", "b", "c", "d")),
        Map.of()
      ),
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("a", "b", "c", "d")),
        Map.of("a", 1)
      ),
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("b", "c", "d")),
        Map.of("a", 1)
      ),
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("c", "d")),
        Map.of("a", 1, "b", 2)
      ),
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("d")),
        Map.of("a", 1, "b", 2, "c", 3)
      )
    );
    
    HumanInTheLoopReranker<T> humanInTheLoop = new HumanInTheLoopReranker<>(internalReranker, qrels(Map.of("a", 1, "b", 2, "c", 3, "d", 4)), 1);
    ScoredDocuments reranked = humanInTheLoop.rerank(scoreDocs("a", "c", "b", "a", "d"), context);

    assertDocsAreRankedAsExpected(reranked, "a", "a", "b", "c", "d");
    assertRerankStagesAreAsExpected(expectedStages, internalReranker.executionStages);
  }

  @Test
  public void humanInTheLoopWithAscendingInternalK2() {
    HumanInTheLoopTestReranker<T> internalReranker = new HumanInTheLoopTestReranker<>();
    internalReranker.comparator = (a,b) -> a.compareTo(b);
    List<RerankerExecutionStage> expectedStages = Arrays.asList(
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("a", "b", "c", "d")),
        Map.of()
      ),
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("b", "c", "d")),
        Map.of("a", 1)
      ),
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("d")),
        Map.of("a", 1, "b", 2, "c", 3)
      )
    );
    
    HumanInTheLoopReranker<T> humanInTheLoop = new HumanInTheLoopReranker<>(internalReranker, qrels(Map.of("a", 1, "b", 2, "c", 3, "d", 4)), 2);
    ScoredDocuments reranked = humanInTheLoop.rerank(scoreDocs("a", "c", "b", "a", "d"), context);
    
    assertDocsAreRankedAsExpected(reranked, "a", "a", "b", "c", "d");
    assertRerankStagesAreAsExpected(expectedStages, internalReranker.executionStages);
  }

  @Test
  public void humanInTheLoopWithAscendingInternalK2Reverse() {
    HumanInTheLoopTestReranker<T> internalReranker = new HumanInTheLoopTestReranker<>();
    internalReranker.comparator = (a,b) -> b.compareTo(a);
    List<RerankerExecutionStage> expectedStages = Arrays.asList(
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("a", "b", "c", "d")),
        Map.of()
      ),
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("a", "b")),
        Map.of("d", 4, "c", 3)
      ),
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("a")),
        Map.of("a", 1, "b", 2, "c", 3, "d", 4)
      )
    );
    
    HumanInTheLoopReranker<T> humanInTheLoop = new HumanInTheLoopReranker<>(internalReranker, qrels(Map.of("a", 1, "b", 2, "c", 3, "d", 4)), 2);
    ScoredDocuments reranked = humanInTheLoop.rerank(scoreDocs("a", "c", "b", "a", "d"), context);
    
    assertDocsAreRankedAsExpected(reranked, "d", "c", "b", "a", "a");
    assertRerankStagesAreAsExpected(expectedStages, internalReranker.executionStages);
  }
  
  @Test
  public void humanInTheLoopWithAscendingInternalK3Reverse() {
    HumanInTheLoopTestReranker<T> internalReranker = new HumanInTheLoopTestReranker<>();
    internalReranker.comparator = (a,b) -> b.compareTo(a);
    List<RerankerExecutionStage> expectedStages = Arrays.asList(
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("a", "b", "c", "d")),
        Map.of()
      ),
      new RerankerExecutionStage(
        new HashSet<>(Arrays.asList("a")),
        Map.of("b", 2, "c", 3, "d", 4)
      )
    );
    
    HumanInTheLoopReranker<T> humanInTheLoop = new HumanInTheLoopReranker<>(internalReranker, qrels(Map.of("a", 1, "b", 2, "c", 3, "d", 4)), 3);
    ScoredDocuments reranked = humanInTheLoop.rerank(scoreDocs("a", "c", "b", "a", "d"), context);
    
    assertDocsAreRankedAsExpected(reranked, "d", "c", "b", "a", "a");
    assertRerankStagesAreAsExpected(expectedStages, internalReranker.executionStages);
  }

  static void assertDocsAreRankedAsExpected(ScoredDocuments docs, String...expected) {
    Assert.assertEquals(docs.documents.length, expected.length);

    for(int i=0; i<expected.length; i++) {
      Assert.assertEquals("Rank " + i + " is not as expected.", docs.documents[i].get("id"), expected[i]);
    }
  }
  
  static ScoredDocuments scoreDocs(String...args) {
    ScoredDocuments ret = new ScoredDocuments();
    ret.documents = new Document[args.length];
    ret.ids = new int[args.length];

    for(int i=0; i<args.length; i++) {
      ret.documents[i] = doc(args[i]);
      ret.ids[i] = -1;
    }

    return ret;
  }
  
  private static Document doc(String id) {
    Document ret = new Document();
    ret.add(new StringField("id", id, Store.YES));

    return ret;
  }
  
  private void assertRerankStagesAreAsExpected(List<RerankerExecutionStage> expected, List<RerankerExecutionStage> actual) {
    Assert.assertEquals(expected.size(), actual.size());

    for(int i=0; i<expected.size(); i++) {
      Assert.assertEquals("Reranker-Stage has unexpected input at batch " + i, expected.get(i), actual.get(i));
    }
  }

  public static class HumanInTheLoopTestReranker<T> implements Reranker<T>, HumanInTheLoopAware<T> {
    private final List<RerankerExecutionStage> executionStages = new ArrayList<>();
    private Comparator<String> comparator;
    private HumanInTheLoopReranker<T> humanInTheLoop;

    @Override
    public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
      ScoredDocuments ret = new ScoredDocuments();
      ret.ids = null;
      ret.scores = null;
      ret.documents = rerank(docs.documents);
      
      return ret;
    }

	private Document[] rerank(Document[] documents) {
      documents = Arrays.copyOf(documents, documents.length);
      Arrays.sort(documents, (a,b) -> comparator.compare(a.get("id"), b.get("id")));
      Set<String> idsToRerank = Stream.of(documents).map(i -> i.get("id")).collect(Collectors.toSet());
      executionStages.add(new RerankerExecutionStage(idsToRerank, humanInTheLoop.availableGroundTruth()));

      return documents;
	}

	@Override
    public String tag() {
      return null;
    }

    @Override
    public void setHumanInTheLoop(HumanInTheLoopReranker<T> humanInTheLoop) {
      this.humanInTheLoop = humanInTheLoop;
    }
  }

  public static class RerankerExecutionStage {
    private final Set<String> idsToRerank;
    private final Map<String, Integer> docToJudgment;

    public RerankerExecutionStage(Set<String> idsToRerank, Map<String, Integer> docToJudgment) {
      this.docToJudgment = docToJudgment;
      this.idsToRerank = idsToRerank;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof RerankerExecutionStage
             && ((RerankerExecutionStage) obj).docToJudgment.equals(this.docToJudgment)
             && ((RerankerExecutionStage) obj).idsToRerank.equals(this.idsToRerank);
    }

    @Override
    public String toString() {
      return idsToRerank + ";" + docToJudgment;
    }
  }

  static Qrels qrels(Map<String, Integer> qrels) {
    Qrels ret = Mockito.mock(Qrels.class);
    Mockito.when(ret.getRelevanceGrade(Matchers.any(), Matchers.any())).then(new Answer<Integer>() {
      @Override
      public Integer answer(InvocationOnMock invocation) throws Throwable {
        String docId = (String) invocation.getArguments()[1];

        return qrels.get(docId); 
      }
    });
    
    return ret;
  }
}
