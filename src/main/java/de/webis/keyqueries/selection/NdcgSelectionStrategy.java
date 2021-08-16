package de.webis.keyqueries.selection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import lombok.Data;
import de.webis.keyqueries.KeyQueryCheckerBase;
import de.webis.keyqueries.selection.CrypsorQuerySelectionStrategy.SelectionStrategy;
import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.DenseDataPoint;
import ciir.umass.edu.learning.RankList;
import ciir.umass.edu.metric.NDCGScorer;

@Data
@SuppressWarnings("serial")
public class NdcgSelectionStrategy<T> implements SelectionStrategy<T> {
	@Override
	public Comparator<T> comparator(KeyQueryCheckerBase<T> kq, List<T> usedQueries) {
		Set<String> alreadyCoveredTargetDocuments = new LinkedHashSet<>();
		if(usedQueries != null) {
			for(T usedQuery: usedQueries) {
				alreadyCoveredTargetDocuments.addAll(kq.targetDocumentsInResult(usedQuery));
			}
		}
		return new NdcgSelectionComparator<>(kq, alreadyCoveredTargetDocuments);
	}
	
	@Data
	public static class NdcgSelectionComparator<T> implements Comparator<T>, Serializable {
		private final KeyQueryCheckerBase<T> kq;
		private final Set<String> alreadyCoveredTargetDocuments;
		
		public NdcgSelectionComparator(KeyQueryCheckerBase<T> kq, Set<String> alreadyCoveredTargetDocuments) {
			this.kq = kq;
			this.alreadyCoveredTargetDocuments = alreadyCoveredTargetDocuments;
		}
		@Override
		public int compare(T q1, T q2) {
			Double q1Ndcg = Double.valueOf(ndcg(q1));
			Double q2Ndcg = Double.valueOf(ndcg(q2));
			int ret = q2Ndcg.compareTo(q1Ndcg);
			
			return ret;
		}
		
		public double ndcg(T query) {
			//Set<String> relevantDocs = relevantDocs();
			Set<String> relevantDocs = kq.getTargetDocuments();
			RankList rl = new RankList(dataPoints(query, relevantDocs));
			
			return new NDCGScorer().score(rl);
		}
		
		private List<DataPoint> dataPoints(T query, Set<String> relevantDocs) {
			List<String> ret = new ArrayList<>(kq.issueQuery(query));
			Set<String> originalCoveredRelevantDocs = new HashSet<>(ret);
			
			for(int i=0; i< 100; i++) {
				ret.add("irrelevant");
			}
			
			if(relevantDocs != null) {
				for(String relevant: relevantDocs) {
					if(!originalCoveredRelevantDocs.contains(relevant)) {
						ret.add(relevant);
					}
				}
			}
			
			return ret.stream()
					.map(i -> toDataPoint(i, relevantDocs))
					.collect(Collectors.toList());
		}
		
		private DataPoint toDataPoint(String i, Set<String> relevantDocs) {
			String label = relevantDocs != null && relevantDocs.contains(i) ? "1" : "0";
			// + " qid:1 1:1 2:1 3:1 # Dummy data point"
			return new DenseDataPoint(label + " qid:1 1:1 2:1 3:1 # Dummy data point");
		}
	}

	public List<String> selectTop(KeyQueryCheckerBase<String> kq, int topK) {
		List<Pair<String, Double>> ret = new ArrayList<>();
		NdcgSelectionComparator<String> internal = new NdcgSelectionComparator<>(kq, new HashSet<>());
		
		for(String query: kq.submittedQueries()) {
			ret.add(Pair.of(query, internal.ndcg(query)));
			
			if((ret.size() % 1000) == 0) {
				System.out.println("Evaluated " + ret.size() + " Queries.");
			}
		}
		
		return ret.stream()
				.sorted((a,b) -> b.getRight().compareTo(a.getRight()))
				.map(i -> i.getKey())
				.limit(topK)
				.collect(Collectors.toList());
	}
}