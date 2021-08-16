package de.webis.keyqueries;

import static io.anserini.search.SearchCollection.BREAK_SCORE_TIES_BY_DOCID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

import io.anserini.index.IndexArgs;
import io.anserini.rerank.RerankerContext;

public class TermWeightKeyQueryChecker <T> extends KeyQueryChecker {
	Map<String, Float> terms;
	RerankerContext<T> context;
	Set<String> originalQuery;
	int query_length;

	public TermWeightKeyQueryChecker(Set<String> targetDocuments, Searcher searcher, int k, int l, Map<String, Float> terms, RerankerContext<T> context, Set<String> originalQuery, int query_length) {
		super(targetDocuments, searcher, k, l);
		this.terms = terms;
		this.context = context;
		this.originalQuery = originalQuery;
		this.query_length = query_length;
	}
	
	@Override
	public boolean isKeyQuery(String query) {
		return query.split(" ").length >= this.query_length;
	}
	
	
	
	@Override
	protected List<String> issueQueryWithoutCache(String query) {
		List<String> ret = new ArrayList<String>();
		IndexSearcher rs = context.getIndexSearcher();
		 BooleanQuery.Builder feedbackQueryBuilder = new BooleanQuery.Builder();
		 String[] split = query.split(" ");
		 float sum = 0;
		 for(String term: split) {
			 sum+=terms.get(term);
		 }
		 if(sum == 0) return ret;
		 for(String term: split) {
			 feedbackQueryBuilder.add(new BoostQuery(new TermQuery(new Term(IndexArgs.CONTENTS, term)), (float) terms.get(term)/sum), BooleanClause.Occur.SHOULD);
		 }
		 Query feedbackQuery = feedbackQueryBuilder.build();
		 try {
			 TopDocs topDocs = rs.search(feedbackQuery, targetResultSetSize(), BREAK_SCORE_TIES_BY_DOCID, true);
			 for(ScoreDoc doc: topDocs.scoreDocs) {
					ret.add(docId(doc, rs));
				}
		 } catch(Exception e) {
			 throw new RuntimeException(e);
		 }
		 return ret;
	}
	private String docId(ScoreDoc doc, IndexSearcher searcher) throws IOException {
		return searcher.getIndexReader().document(doc.doc, new HashSet<>(Arrays.asList(IndexArgs.ID))).get(IndexArgs.ID);
	}
	
}
