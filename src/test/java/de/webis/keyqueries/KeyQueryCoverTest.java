package de.webis.keyqueries;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class KeyQueryCoverTest {
	
	private static final Set<String> 
			KW1 = new LinkedHashSet<>(Arrays.asList("keyword1")),
			KW2 = new LinkedHashSet<>(Arrays.asList("keyword2")),
			KW3 = new LinkedHashSet<>(Arrays.asList("keyword3")),
			KW4 = new LinkedHashSet<>(Arrays.asList("keyword4")),
			KW2_KW3 = new LinkedHashSet<>(Arrays.asList("keyword2", "keyword3")),
			KW1_KW2 = new LinkedHashSet<>(Arrays.asList("keyword1", "keyword2")),
			KW1_KW3 = new LinkedHashSet<>(Arrays.asList("keyword1", "keyword3")),
			KW1_KW3_KW4 = new LinkedHashSet<>(Arrays.asList("keyword1", "keyword3", "keyword4"));
	
	@Test
	public void checkKeyQueryCoverSolver() {
		int k = 2;
		int l = 5;
		Set<String> targetDocuments = new LinkedHashSet<>(Arrays.asList("a", "b"));
		Set<String> keywords = new LinkedHashSet<>(Arrays.asList("keyword1", "keyword2"));
		Map<Set<String>, List<String>> rankings = new HashMap<>();
		rankings.put(KW1, Arrays.asList("a"));
		rankings.put(KW2, Arrays.asList("b"));
		
		List<Set<String>> keyQueries = KeyQueryCoverSolver.getKeyQueryCoverTokens(
				keywords,
				keyqueryChecker(rankings, targetDocuments, k , l)
		);
		
		Assert.assertTrue(keyQueries.isEmpty());
	}
	
	@Test
	public void checkKeyQueryCoverSolver2() {
		int k = 2;
		int l = 5;
		Set<String> targetDocuments = new LinkedHashSet<>(Arrays.asList("a", "b"));
		Set<String> keywords = new LinkedHashSet<>(Arrays.asList("keyword1", "keyword2"));
		Map<Set<String>, List<String>> rankings = new HashMap<>();
		rankings.put(KW1, Arrays.asList("a", "b", "c", "d", "e"));
		rankings.put(KW2, Arrays.asList("b", "a", "c", "d", "e"));
		
		List<Set<String>> expected = Arrays.asList(KW1, KW2);
		List<Set<String>> actual = KeyQueryCoverSolver.getKeyQueryCoverTokens(
				keywords,
				keyqueryChecker(rankings, targetDocuments, k , l)
		);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void checkKeyQueryCoverSolver3() {
		int k = 2;
		int l = 5;
		Set<String> targetDocuments = new LinkedHashSet<>(Arrays.asList("a", "b"));
		Set<String> keywords = new LinkedHashSet<>(Arrays.asList("keyword1", "keyword2", "keyword3"));
		Map<Set<String>, List<String>> rankings = new HashMap<>();
		rankings.put(KW1, Arrays.asList("a", "b", "c", "d", "e"));
		rankings.put(KW2, Arrays.asList("b", "c", "a", "d", "e"));
		rankings.put(KW3, Arrays.asList("c", "a", "d", "e", "a"));
		rankings.put(KW2_KW3, Arrays.asList("b", "a", "c", "d", "e"));
		
		List<Set<String>> expected = Arrays.asList(KW1, KW2_KW3);
		List<Set<String>> actual = KeyQueryCoverSolver.getKeyQueryCoverTokens(
				keywords,
				keyqueryChecker(rankings, targetDocuments, k , l)
		);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void checkKeyQueryCoverSolver4() {
		int k = 2;
		int l = 5;
		Set<String> targetDocuments = new LinkedHashSet<>(Arrays.asList("a", "b"));
		Set<String> keywords = new LinkedHashSet<>(Arrays.asList("keyword1", "keyword2", "keyword3"));
		Map<Set<String>, List<String>> rankings = new HashMap<>();
		rankings.put(KW1, Arrays.asList("a", "b", "c", "d", "e"));
		rankings.put(KW2, Arrays.asList("b", "c", "a", "d", "e"));
		rankings.put(KW3, Arrays.asList("c", "a", "d", "e", "a"));
		rankings.put(KW2_KW3, Arrays.asList("b", "a", "c", "d"));

		List<Set<String>> expected = Arrays.asList(KW1);
		List<Set<String>> actual = KeyQueryCoverSolver.getKeyQueryCoverTokens(
				keywords,
				keyqueryChecker(rankings, targetDocuments, k , l)
		);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void checkKeyQueryCoverSolver5() {
		int k = 2;
		int l = 5;
		Set<String> targetDocuments = new LinkedHashSet<>(Arrays.asList("a", "b"));
		Set<String> keywords = new LinkedHashSet<>(Arrays.asList("keyword1", "keyword2", "keyword3"));
		Map<Set<String>, List<String>> rankings = new HashMap<>();
		rankings.put(KW1, Arrays.asList("c", "d", "e", "a", "b"));
		rankings.put(KW2, Arrays.asList("a", "a", "a", "a", "a"));
		
		rankings.put(KW1_KW2, Arrays.asList("a", "b", "c", "d", "e"));
		rankings.put(KW3, Arrays.asList("c", "a", "d", "e", "a"));
		rankings.put(KW1_KW3, Arrays.asList("b", "a", "c", "d"));
		rankings.put(KW2_KW3, Arrays.asList("b", "a", "c", "d", "e"));

		List<Set<String>> expected = Arrays.asList(KW1_KW2);
		List<Set<String>> actual = KeyQueryCoverSolver.getKeyQueryCoverTokens(
				keywords,
				keyqueryChecker(rankings, targetDocuments, k , l)
		);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void checkKeyQueryCoverSolver6() {
		int k = 2;
		int l = 5;
		Set<String> targetDocuments = new LinkedHashSet<>(Arrays.asList("a", "b"));
		Set<String> keywords = new LinkedHashSet<>(Arrays.asList("keyword1", "keyword2", "keyword3"));
		Map<Set<String>, List<String>> rankings = new HashMap<>();
		rankings.put(KW1, Arrays.asList("c", "d", "e", "a", "b"));
		rankings.put(KW2, Arrays.asList("a", "a", "a", "a", "a"));
		
		rankings.put(KW1_KW2, Arrays.asList("a", "b", "c", "d", "e"));
		rankings.put(KW3, Arrays.asList("c", "a", "d", "e", "a"));
		rankings.put(KW1_KW3, Arrays.asList("b", "a", "c", "d", "e"));
		rankings.put(KW2_KW3, Arrays.asList("b", "a", "c", "d", "e"));

		List<Set<String>> expected = Arrays.asList(KW1_KW2, KW1_KW3);
		List<Set<String>> actual = KeyQueryCoverSolver.getKeyQueryCoverTokens(
				keywords, 
				keyqueryChecker(rankings, targetDocuments, k , l)
		);
		
		Assert.assertEquals(expected, actual);
	}

	@Test
	@Ignore
	public void checkKeyQueryCoverSolver7() {
		int k = 2;
		int l = 5;
		Set<String> targetDocuments = new LinkedHashSet<>(Arrays.asList("a", "b"));
		Set<String> keywords = new LinkedHashSet<>(Arrays.asList("keyword1", "keyword2", "keyword3", "keyword4"));
		Map<Set<String>, List<String>> rankings = new HashMap<>();
		rankings.put(KW1, Arrays.asList("c", "d", "e", "a", "b"));
		rankings.put(KW2, Arrays.asList("a", "a", "a", "a", "a"));
		rankings.put(KW4, Arrays.asList("c", "a", "d", "e", "a"));
		
		rankings.put(KW1_KW2, Arrays.asList("a", "b", "c", "d", "e"));
		rankings.put(KW3, Arrays.asList("c", "a", "d", "e", "a"));
		rankings.put(KW1_KW3, Arrays.asList("b", "a", "c", "d"));
		rankings.put(KW2_KW3, Arrays.asList("a", "b", "c", "d"));
		rankings.put(KW1_KW3_KW4, Arrays.asList("b", "a", "e", "c", "d"));
		
		List<Set<String>> expected = Arrays.asList(KW1_KW2, KW1_KW3_KW4);
		List<Set<String>> actual = KeyQueryCoverSolver.getKeyQueryCoverTokens(
				keywords,
				keyqueryChecker(rankings, targetDocuments, k , l)
		);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	@Ignore
	public void checkKeyQueryCoverSolver8() {
		int k = 2;
		int l = 5;
		Set<String> targetDocuments = new LinkedHashSet<>(Arrays.asList("a", "b"));
		Set<String> keywords = new LinkedHashSet<>(Arrays.asList("keyword1", "keyword2", "keyword3", "keyword4"));
		Map<Set<String>, List<String>> rankings = new HashMap<>();
		rankings.put(KW1, Arrays.asList("c", "d", "e", "a", "b"));
		rankings.put(KW2, Arrays.asList("a", "a", "a", "a", "a"));
		rankings.put(KW4, Arrays.asList("c", "a", "d", "e", "a"));
		
		rankings.put(KW1_KW2, Arrays.asList("a", "b", "c", "d", "e"));
		rankings.put(KW3, Arrays.asList("c", "a", "d", "e", "a"));
		rankings.put(KW1_KW3, Arrays.asList("b", "a", "c", "d"));
		rankings.put(KW2_KW3, Arrays.asList("a", "b", "c", "d"));
		rankings.put(KW1_KW3_KW4, Arrays.asList("b", "a", "e", "c", "d"));
		
		List<String> expected = Arrays.asList("keyword1 keyword2", "keyword3 keyword4 keyword1");
		List<String> actual = KeyQueryCoverSolver.getKeyQueryCover(
				keywords,
				keyqueryChecker(rankings, targetDocuments, k , l)
		);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	@Ignore
	public void checkKeyQueryCoverSolver9() {
		int k = 2;
		int l = 5;
		int m = 1;
		Set<String> targetDocuments = new LinkedHashSet<>(Arrays.asList("a", "b"));
		Set<String> keywords = new LinkedHashSet<>(Arrays.asList("keyword2", "keyword1", "keyword3", "keyword4"));
		Map<Set<String>, List<String>> rankings = new HashMap<>();
		rankings.put(KW1, Arrays.asList("c", "d", "e", "a", "b"));
		rankings.put(KW2, Arrays.asList("a", "a", "a", "a", "a"));
		rankings.put(KW4, Arrays.asList("c", "a", "d", "e", "a"));
		
		rankings.put(KW1_KW2, Arrays.asList("a", "b", "c", "d", "e"));
		rankings.put(KW3, Arrays.asList("c", "a", "d", "e", "a"));
		rankings.put(KW1_KW3, Arrays.asList("b", "a", "c", "d"));
		rankings.put(KW2_KW3, Arrays.asList("a", "b", "c", "d"));
		rankings.put(KW1_KW3_KW4, Arrays.asList("b", "a", "e", "c", "d"));
		
		List<String> expected = Arrays.asList("keyword2", "keyword3 keyword4 keyword1");
		List<String> actual = KeyQueryCoverSolver.getKeyQueryCover(
				keywords,
				new KeyQueryChecker(targetDocuments, searcher(rankings), k, l, m)
		);
		
		Assert.assertEquals(expected, actual);
	}
	
	private static KeyQueryChecker keyqueryChecker(Map<Set<String>, List<String>> rankings, Set<String> targetDocuments, int k, int l) {
		return new KeyQueryChecker(targetDocuments, searcher(rankings), k, l);
	}
	
	private static Searcher<String> searcher(Map<Set<String>, List<String>> rankings) {
		return new Searcher<String>() {
			@Override
			public List<String> search(String query, int size) {
				List<String> ret = rankings.get(new HashSet<>(Arrays.asList(query.split("\\s"))));
				
				return ret == null ? Collections.emptyList() : ret;
			}
		};
	}
}
