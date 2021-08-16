package de.webis.keyqueries;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class KeyQueryCoverSolver {

	private KeyQueryCoverSolver() {
		// hide utility class constructor
	}
	
	/**
	 * 
	 * @param keywords
	 * @param keyQueryChecker
	 * @return A list of keyqueries.
	 */
	public static List<String> getKeyQueryCover(Set<String> keywords, KeyQueryChecker keyQueryChecker) {
		return getKeyQueryCoverTokens(keywords, keyQueryChecker).stream()
			.map(i -> i.stream().collect(Collectors.joining(" ")))
			.collect(Collectors.toSet())
			.stream().collect(Collectors.toList());
	}

	/**
	 * 
	 * @param keywords
	 * @param keyQueryChecker
	 * @return A list of keyqueries. Each keyquery is represented as a set of strings (i.e. tokens that make up the keyquery).
	 */
	public static List<Set<String>> getKeyQueryCoverTokens(Set<String> keywords, KeyQueryChecker keyQueryChecker) {
		keywords = removeAllKeyWordsWithTooFewResults(keyQueryChecker, keywords);
		List<Set<String>> ret = new ArrayList<>();
		
		Set<String> currentQuery = new HashSet<>();
		for(String keyword: keywords) {
			currentQuery.add(keyword);
			if(keyQueryChecker.isKeyQuery(currentQuery)) {
				ret.add(currentQuery);
				currentQuery = new HashSet<>();
			}
		}
		
		if(!currentQuery.isEmpty()) {
			for(String keyword: keywords) {
				Set<String> tmpQuery = new HashSet<>(currentQuery);
				tmpQuery.add(keyword);
				
				if(queryIsNotMinimal(tmpQuery, ret)) {
					continue;
				} else {
					currentQuery = tmpQuery;
				}
				
				if(keyQueryChecker.isKeyQuery(currentQuery)) {
					ret.add(currentQuery);
					break;
				}
			}
		}
		
		return ret;
	}

	private static Set<String> removeAllKeyWordsWithTooFewResults(KeyQueryChecker keyQueryChecker, Set<String> keywords) {
		return keywords.stream()
				.filter(kw -> keyQueryChecker.parameterLIsSatisfied(kw))
				.collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
	}
	
	private static boolean queryIsNotMinimal(Set<String> tmpQuery, List<Set<String>> ret) {
		for(Set<String> minimalQuery: ret) {
			if(tmpQuery.containsAll(minimalQuery)) {
				return true;
			}
		}
		
		return false;
	}
}
