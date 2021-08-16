package de.webis.keyqueries.anserini;

import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

import io.anserini.index.IndexArgs;
import io.anserini.search.query.BagOfWordsQueryGenerator;

public class LuceneSearcher extends LuceneSearcherBase<String> {

	private final Analyzer analyzer;
	
	public LuceneSearcher(IndexSearcher searcher, Analyzer analyzer) {
		super(searcher);
		this.analyzer = analyzer;
	}
	
	@Override
	public List<String> search(String query, int size) {
		return searchWithLucene(bowQuery(query), size);
	}
	
	private Query bowQuery(String query) {
		return new BagOfWordsQueryGenerator().buildQuery(IndexArgs.CONTENTS, analyzer, query);
	}
}
