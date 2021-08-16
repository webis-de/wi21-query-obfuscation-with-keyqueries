package de.webis.keyqueries.anserini;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import de.webis.keyqueries.Searcher;
import io.anserini.index.IndexArgs;

public abstract class LuceneSearcherBase<T> implements Searcher<T> {

	private final IndexSearcher searcher;
	
	public LuceneSearcherBase(IndexSearcher searcher) {
		this.searcher = searcher;
	}
	
	protected List<String> searchWithLucene(Query query, int size) {
		try {List<String> ret = new ArrayList<>();
			TopDocs topDocs = searcher.search(query, size);
		
			for(ScoreDoc doc: topDocs.scoreDocs) {
				ret.add(docId(doc));
			}
		
			return ret;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private String docId(ScoreDoc doc) throws IOException {
		return searcher.getIndexReader().document(doc.doc, new HashSet<>(Arrays.asList(IndexArgs.ID))).get(IndexArgs.ID);
	}
}
