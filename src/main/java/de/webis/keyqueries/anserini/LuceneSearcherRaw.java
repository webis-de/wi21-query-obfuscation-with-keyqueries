package de.webis.keyqueries.anserini;

import java.util.List;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

public class LuceneSearcherRaw extends LuceneSearcherBase<Query> {

	public LuceneSearcherRaw(IndexSearcher searcher) {
		super(searcher);
	}

	@Override
	public List<String> search(Query query, int size) {
		return searchWithLucene(query, size);
	}
}
