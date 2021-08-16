package de.webis.keyqueries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class KeyQueryCheckerBase<T> {
	
	protected Set<String> targetDocuments;
	protected int k, l, m;
	private final Searcher<T> searcher;
	private final Map<T, List<String>> queryCache = new LinkedHashMap<>();
	
	public static final int TOP_DOCUMENTS_FOR_KEYQUERY_BUILDING = 10;

	public KeyQueryCheckerBase(Set<String> targetDocuments, Searcher<T> searcher, int k, int l, int m) {
		this.targetDocuments = targetDocuments;
		this.k = k;
		this.l = l;
		this.m = m;
		this.searcher = searcher;
	}

	public Set<String> getTargetDocuments() {
		return Collections.unmodifiableSet(targetDocuments);
	}
	
	public boolean isKeyQuery(T query) {
		return isKeyQuery(issueQuery(query), query);
	}
	
	protected abstract boolean noSubQueryIsKeyQuery(T query);

	public boolean isRelaxedKeyQuery(T query, int difficulty) {
		return isRelaxedKeyQuery(issueQuery(query), difficulty);
	}
	
	public boolean isRelaxedKeyQuery(T query, List<Integer> difficulty) {
		for(Integer entry: difficulty) {
			if(isRelaxedKeyQuery(issueQuery(query), entry)) {
				return true;
			}
		}
		return false;
	}
	
	public Set<String> targetDocumentsInResult(T query) {
		return targetDocumentsInResult(issueQuery(query));
	}
	
	protected Set<String> targetDocumentsInResult(List<String> ranking) {
		Set<String> tmp = new HashSet<>(ranking.subList(0, Math.min(k, ranking.size())));
		
		return targetDocuments.stream()
				.filter(i -> tmp.contains(i))
				.collect(Collectors.toSet());
	}

	public boolean isKeyQuery(List<String> ranking, T query) {
		return targetDocuments != null
				&& parameterLIsSatisfied(ranking)
				&& targetDocumentsInResult(ranking).size() >= m
				&& noSubQueryIsKeyQuery(query);
	}
	
	public boolean isRelaxedKeyQuery(List<String> ranking, int difficulty) {
		return targetDocuments != null
				&& parameterLIsSatisfied(ranking)
				&& targetDocumentsInResult(ranking).size() >= difficulty;
	}
	
	public synchronized List<String> issueQuery(T query) {
		if(!queryCache.containsKey(query)) {
			queryCache.put(query, issueQueryWithoutCache(query));	
		}		
		
		return queryCache.get(query);
	}
	
	protected List<String> issueQueryWithoutCache(T query) {
		List<String> ret = new ArrayList<String>();
		ret = searcher.search(query, targetResultSetSize());
		return Collections.unmodifiableList(ret);
	}
	
	public int targetResultSetSize() {
		return Math.max(l, k) +2;
	}

	public boolean parameterLIsSatisfied(T query) {
		return parameterLIsSatisfied(issueQuery(query));
	}
	
	public boolean parameterLIsSatisfied(List<String> ranking) {
		return ranking != null && ranking.size() >= l;
	}

	public List<T> getKnownKeyqueries() {
		return submittedQueries().stream()
				.filter(i -> isKeyQuery(issueQuery(i), i))
				.collect(Collectors.toList());
	}

	public Set<T> submittedQueries() {
		return Collections.unmodifiableSet(queryCache.keySet());
	}
	
	public Searcher<T> getSearcher() {
		return searcher;
	}
	
	public int getTargetDocumentSize() {
		return targetDocuments.size();
	}
	public void setTargetDocuments(Set<String> targetdocuments) {
		this.targetDocuments = targetdocuments;
		this.m = this.targetDocuments.size();
	}
	
}