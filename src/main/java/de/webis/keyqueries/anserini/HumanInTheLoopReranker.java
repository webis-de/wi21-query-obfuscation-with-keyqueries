package de.webis.keyqueries.anserini;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.document.Document;

import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.util.Qrels;

public class HumanInTheLoopReranker<T> implements Reranker<T> {

  private final Reranker<T> internalReranker;

  private final int k;
  
  private final Qrels qrels;

  private T currentTopic = null;
  
  private Set<String> rerankedIds = null;
  
  public HumanInTheLoopReranker(Reranker<T> internalReranker, Qrels qrels, int k) {
    this.internalReranker = internalReranker;
    this.k = k;
    this.qrels = qrels;
  }

  @Override
  public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
    Document[] rerankedDocs = new Document[docs.documents.length];
    Document[] toRerank = docs.documents;
    currentTopic = context.getQueryId();
    
    for(int position=0; position < rerankedDocs.length; position += k) {
      rerankedIds = new HashSet<>(Stream.of(rerankedDocs).filter(i -> i != null).map(i -> i.get("id")).collect(Collectors.toList()));
      initializeReranker();
      ScoredDocuments nextToRerank = new ScoredDocuments();
      nextToRerank.documents = toRerank;

      ScoredDocuments tmp = internalReranker.rerank(nextToRerank, context);
      toRerank = Arrays.copyOfRange(tmp.documents, Integer.min(k, tmp.documents.length), tmp.documents.length);
      for(int offset=0; offset < Integer.min(k, tmp.documents.length); offset++) {
        rerankedDocs[position + offset] = tmp.documents[offset];
      }
    }

    currentTopic = null;
    rerankedIds = null;

    ScoredDocuments ret = new ScoredDocuments();
    ret.documents = rerankedDocs;

    return ret;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void initializeReranker() {
    if(internalReranker != null && internalReranker instanceof HumanInTheLoopAware) {
      ((HumanInTheLoopAware)internalReranker).setHumanInTheLoop(this);
    }
  }
  
  @Override
  public String tag() {
    return "human-in-the-loop(" + internalReranker.tag() + ")";
  }

  public Map<String, Integer> availableGroundTruth() {
    Map<String, Integer> ret = new HashMap<>();

    for(String id : rerankedIds) {
      ret.put(id, qrels.getRelevanceGrade(currentTopic, id));
    }

    return ret;
  }
}
