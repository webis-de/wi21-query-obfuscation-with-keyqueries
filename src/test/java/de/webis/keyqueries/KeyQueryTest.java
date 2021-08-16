package de.webis.keyqueries;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class KeyQueryTest {
	@Test
	public void testThatEmptyRankingIsNotAKeyQuery() {
		KeyQueryChecker kq = kq(6, 2, 1);
		List<String> ranking = Collections.emptyList();
		
		Assert.assertFalse(kq.isKeyQuery(ranking, ""));
	}
	
	@Test
	public void testThatRankingWithoutTargetDocumentsIsNotAKeyQuery() {
		KeyQueryChecker kq = kq(6, 2, 1, "y", "z");
		List<String> ranking = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
		
		Assert.assertFalse(kq.isKeyQuery(ranking, ""));
	}
	
	@Test
	public void testThatRankingWithTargetDocumentAtFirstPositionIsAKeyQuery() {
		KeyQueryChecker kq = kq(6, 2, 1, "a");
		List<String> ranking = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
		
		Assert.assertTrue(kq.isKeyQuery(ranking, ""));
	}
	
	@Test
	public void testThatRankingWithTargetDocumentAtPositionKPlusOneIsNotAKeyQuery() {
		KeyQueryChecker kq = kq(6, 2, 1, "a");
		List<String> ranking = Arrays.asList("b", "c", "d", "e", "f", "g", "a", "h", "i", "j");
		
		Assert.assertFalse(kq.isKeyQuery(ranking, ""));
	}
	
	@Test
	public void testThatTooShortRankingWithTargetDocumentAtFirstPositionIsNotAKeyQuery() {
		KeyQueryChecker kq = kq(6, 11, 1, "a");
		List<String> ranking = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
		
		Assert.assertFalse(kq.isKeyQuery(ranking, ""));
	}
	
	@Test
	public void testThatRankingWithTooFewTargetDocumentsIsNotAKeyQuery() {
		KeyQueryChecker kq = kq(6, 2, 2, "a");
		List<String> ranking = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
		
		Assert.assertFalse(kq.isKeyQuery(ranking, ""));
	}
	
	@Test
	@SuppressWarnings("unchecked")
	public void testThatQueryCacheIsWorking() {
		List<String> q1Result = Arrays.asList("a", "b");
		List<String> q2Result = Arrays.asList("c", "a", "e", "f", "g", "h");
		List<String> q3Result = Arrays.asList("c", "d", "e", "f", "g", "h");
		
		Searcher<String> searcher = Mockito.mock(Searcher.class);
		Mockito.when(searcher.search("q1", 6)).thenReturn(q1Result);
		Mockito.when(searcher.search("q2", 6)).thenReturn(q2Result);
		Mockito.when(searcher.search("q3", 6)).thenReturn(q3Result);
		
		KeyQueryChecker checker = new KeyQueryChecker("q1", searcher, 4, 4, 1);
		
		Assert.assertTrue(checker.isKeyQuery("q2"));
		Assert.assertFalse(checker.isKeyQuery("q3"));
		
		Assert.assertEquals(new HashSet<>(Arrays.asList("q2", "q3")), checker.submittedQueries());
		Assert.assertEquals(Arrays.asList("q2"), checker.getKnownKeyqueries());
	}
	
	private static KeyQueryChecker kq(int k, int l, int m, String...targetDocuments) {
		Set<String> targetDocumentSet = new HashSet<>(Arrays.asList(targetDocuments));
		
		return new KeyQueryChecker(targetDocumentSet, null, k, l, m);
	}
}
