/*package de.webis.keyqueries.selection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.webis.keyqueries.selection.CrypsorQuerySelectionStrategy;
import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.selection.NdcgSelectionStrategy.NdcgSelectionComparator;

public class NdcgSelectionStrategyTest {
	@Test
	public void ensureThatEmptyListObtainsZeroNdcg() {
		KeyQueryChecker kq = kq(
			new HashSet<>(Arrays.asList("doc-1", "doc-2")),
			null
		);
		Set<String> alreadyCoveredTargetDocuments = Collections.emptySet();
		
		NdcgSelectionComparator comparator = new NdcgSelectionComparator(kq, alreadyCoveredTargetDocuments);
		
		Assert.assertEquals(0.0, comparator.ndcg("abcd"), 0.00000001);
		Assert.assertEquals(0.0, comparator.ndcg("1234"), 0.00000001);
	}
	
	@Test
	public void ensureThatListWithoutRelevantDocumentsObtainsZeroNdcg() {
		Map<String, List<String>> rankings = new HashMap<>();
		rankings.put("abcd", Arrays.asList("doc-1", "doc-2"));
		rankings.put("1234", Arrays.asList("doc-2", "doc-1"));
		
		KeyQueryChecker kq = kq(
			new HashSet<>(Arrays.asList("doc-1", "doc-2")),
			rankings
		);
		Set<String> alreadyCoveredTargetDocuments = new HashSet<>(Arrays.asList("doc-1", "doc-2"));
		
		NdcgSelectionComparator comparator = new NdcgSelectionComparator(kq, alreadyCoveredTargetDocuments);
		
		Assert.assertEquals(0.0, comparator.ndcg("abcd"), 0.00000001);
		Assert.assertEquals(0.0, comparator.ndcg("1234"), 0.00000001);
	}
	
	@Test
	public void ensureThatListWithoutRelevantDocumentsObtainsZeroNdcg2() {
		Map<String, List<String>> rankings = new HashMap<>();
		rankings.put("abcd", Arrays.asList("doc-1", "doc-2"));
		rankings.put("1234", Arrays.asList("doc-2", "doc-1"));
		
		KeyQueryChecker kq = kq(
			new HashSet<>(Arrays.asList("doc-3", "doc-4")),
			rankings
		);
		Set<String> alreadyCoveredTargetDocuments = new HashSet<>(Arrays.asList("doc-4", "doc-5"));
		
		NdcgSelectionComparator comparator = new NdcgSelectionComparator(kq, alreadyCoveredTargetDocuments);
		
		Assert.assertEquals(0.0, comparator.ndcg("abcd"), 0.00000001);
		Assert.assertEquals(0.0, comparator.ndcg("1234"), 0.00000001);
	}
	
	@Test
	public void ensureThatPerfectRankingsObtainNdcgOfOne() {
		Map<String, List<String>> rankings = new HashMap<>();
		rankings.put("abcd", Arrays.asList("doc-1", "doc-2"));
		rankings.put("1234", Arrays.asList("doc-2", "doc-1"));
		
		KeyQueryChecker kq = kq(
			new HashSet<>(Arrays.asList("doc-1", "doc-2")),
			rankings
		);
		Set<String> alreadyCoveredTargetDocuments = new HashSet<>(Arrays.asList("doc-4", "doc-5"));
		
		NdcgSelectionComparator comparator = new NdcgSelectionComparator(kq, alreadyCoveredTargetDocuments);
		
		Assert.assertEquals(1.0, comparator.ndcg("abcd"), 0.00000001);
		Assert.assertEquals(1.0, comparator.ndcg("1234"), 0.00000001);
	}
	
	@Test
	public void ensureThatRankingsWithLocallyPerfectJudgmentsDoNotObtainPerfectNdcg() {
		Map<String, List<String>> rankings = new HashMap<>();
		rankings.put("abcd", Arrays.asList("doc-1", "doc-3", "doc-4"));
		
		KeyQueryChecker kq = kq(
			new HashSet<>(Arrays.asList("doc-1", "doc-2")),
			rankings
		);
		Set<String> alreadyCoveredTargetDocuments = new HashSet<>();
		
		NdcgSelectionComparator comparator = new NdcgSelectionComparator(kq, alreadyCoveredTargetDocuments);
		
		Assert.assertEquals(0.6131471927, comparator.ndcg("abcd"), 0.00000001);
	}
	
	@Test
	public void ensureThatMediumRankingsObtainMediumNdcg() {
		Map<String, List<String>> rankings = new HashMap<>();
		rankings.put("abcd", Arrays.asList("doc-1", "doc-2"));
		
		KeyQueryChecker kq = kq(
			new HashSet<>(Arrays.asList("doc-2")),
			rankings
		);
		Set<String> alreadyCoveredTargetDocuments = new HashSet<>(Arrays.asList("doc-4", "doc-5"));
		
		NdcgSelectionComparator comparator = new NdcgSelectionComparator(kq, alreadyCoveredTargetDocuments);
		
		Assert.assertEquals(0.630929, comparator.ndcg("abcd"), 0.000001);
	}
	
	@Test
	public void largerIntegrationTest() {
		List<String> expected = Arrays.asList("query-1", "query-3");
		
		Map<String, List<String>> rankings = new HashMap<>();
		rankings.put("query-1", Arrays.asList("rel-1", "rel-2", "doc-3"));
		rankings.put("query-2", Arrays.asList("doc-4", "rel-1", "rel-2"));
		rankings.put("query-3", Arrays.asList("doc-5", "rel-3", "doc-6"));
		KeyQueryChecker kq = kq(
				new HashSet<>(Arrays.asList("rel-1", "rel-2", "rel-3")),
				rankings
			);
		
		CrypsorQuerySelectionStrategy selection = new CrypsorQuerySelectionStrategy(new NdcgSelectionStrategy());
		List<String> actual = selection.selectTop(kq, 5);
		
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void largerIntegrationTest2() {
		List<String> expected = Arrays.asList("query-1", "query-3", "query-2");
		
		Map<String, List<String>> rankings = new HashMap<>();
		rankings.put("query-1", Arrays.asList("rel-1", "rel-2", "doc-3"));
		rankings.put("query-2", Arrays.asList("doc-4", "rel-1", "rel-2", "doc-7", "rel-4"));
		rankings.put("query-3", Arrays.asList("doc-5", "rel-3", "doc-6"));
		KeyQueryChecker kq = kq(
				new HashSet<>(Arrays.asList("rel-1", "rel-2", "rel-3", "rel-4")),
				rankings
			);
		
		CrypsorQuerySelectionStrategy selection = new CrypsorQuerySelectionStrategy(new NdcgSelectionStrategy());
		List<String> actual = selection.selectTop(kq, 5);
		
		Assert.assertEquals(expected, actual);
	}
	
	private static KeyQueryChecker kq(Set<String> relevantDocs, Map<String, List<String>> rankings) {
		KeyQueryChecker ret = Mockito.mock(KeyQueryChecker.class);
		Mockito.when(ret.getTargetDocuments()).thenReturn(relevantDocs);
		Mockito.when(ret.issueQuery(ArgumentMatchers.anyString())).then(new Answer<List<String>>() {
			@Override
			public List<String> answer(InvocationOnMock invocation) throws Throwable {
				return rankings == null ? Collections.emptyList(): rankings.getOrDefault(invocation.getArguments()[0], Collections.emptyList());
			}
			
		});
		Mockito.when(ret.isKeyQuery(ArgumentMatchers.anyString())).thenReturn(Boolean.TRUE);
		Mockito.when(ret.submittedQueries()).thenReturn(rankings == null ? Collections.emptySet():rankings.keySet());
		Mockito.when(ret.targetDocumentsInResult(ArgumentMatchers.anyString())).then(new Answer<Set<String>>() {
			@Override
			public Set<String> answer(InvocationOnMock invocation) throws Throwable {
				List<String> ranking = rankings == null ? Collections.emptyList(): rankings.getOrDefault(invocation.getArguments()[0], Collections.emptyList());
				
				return ranking.stream().filter(i -> relevantDocs != null && relevantDocs.contains(i)).collect(Collectors.toSet());
			}
		});
		
		return ret;
	}
}*/
