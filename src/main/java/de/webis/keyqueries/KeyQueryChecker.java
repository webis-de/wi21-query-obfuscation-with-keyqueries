package de.webis.keyqueries;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

public class KeyQueryChecker extends KeyQueryCheckerBase<String> {

	public KeyQueryChecker(String query, Searcher<String> searcher, int k, int l, int m) {
		this(new HashSet<>(), searcher, k, l, m);
		
		targetDocuments.addAll(issueQueryWithoutCache(query).stream().limit(TOP_DOCUMENTS_FOR_KEYQUERY_BUILDING).collect(Collectors.toList()));
	}
	
	public KeyQueryChecker(Set<String> targetDocuments, Searcher<String> searcher, int k, int l) {
		this(targetDocuments, searcher, k, l, targetDocuments.size());
	}
	
	public KeyQueryChecker(Set<String> targetDocuments, Searcher<String> searcher, int k, int l, int m) {
		super(targetDocuments, searcher, k, l, m);
	}


	@Override
	protected boolean noSubQueryIsKeyQuery(String query) {
		String[] split = query.split(" ");
		Set<Set<String>> subsets = Sets.powerSet(new HashSet<>(Arrays.asList(split)));
		for(Set<String> subset: subsets) {
			String set = subset.stream().collect(Collectors.joining(" "));
			if(set.equals(query) || set.isEmpty()) {
				continue;
			}
			if(targetDocumentsInResult(issueQuery(set)).size() >= m) {
				return false;
			}
		}
		
		return true;
	}

	public boolean isKeyQuery(Set<String> query) {
		return isKeyQuery(query.stream().collect(Collectors.joining(" ")));
	}
}
