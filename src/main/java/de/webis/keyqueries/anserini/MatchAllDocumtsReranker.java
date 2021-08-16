package de.webis.keyqueries.anserini;

import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TopDocs;

public class MatchAllDocumtsReranker<T> implements Reranker<T> {

  @Override
  public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
    ScoredDocuments ret = new ScoredDocuments();
    ret.ids = null;
    ret.scores = null;
    ret.documents = allDocs(context);
    
    return ret;
  }
  
  private Document[] allDocs(RerankerContext<T> context) {
    try {
      TopDocs topDocs = context.getIndexSearcher().search(new MatchAllDocsQuery(), Integer.MAX_VALUE);
      Document[] ret = new Document[topDocs.scoreDocs.length];
      for(int i=0; i< ret.length; i++) {
        ret[i] = context.getIndexSearcher().getIndexReader().document(topDocs.scoreDocs[i].doc);
      }
      
      return ret;
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String tag() {
    return "match-all-docs";
  }
}
