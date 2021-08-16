package de.webis.keyqueries;

import java.util.Set;

import org.apache.lucene.search.Query;

public class LuceneKeyQueryChecker extends KeyQueryCheckerBase<Query> {

	public LuceneKeyQueryChecker(Set<String> targetDocuments, Searcher<Query> searcher, int k, int l) {
		super(targetDocuments, searcher, k, l, targetDocuments.size());
	}

	@Override
	protected boolean noSubQueryIsKeyQuery(Query query) {
		// For the moment, we dont want to test subqueries for weighted queries
		return true;
	}
}
