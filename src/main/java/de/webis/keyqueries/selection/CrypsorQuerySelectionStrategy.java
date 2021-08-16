package de.webis.keyqueries.selection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import lombok.Data;
import de.webis.keyqueries.KeyQueryCheckerBase;
import de.webis.keyqueries.selection.NdcgSelectionStrategy.NdcgSelectionComparator;

@Data
@SuppressWarnings("serial")
public class CrypsorQuerySelectionStrategy<T> implements Serializable {

	private final SelectionStrategy<T> selectionStrategy;
	
	public CrypsorQuerySelectionStrategy(NdcgSelectionStrategy<T> ndcg) {
		selectionStrategy = ndcg;
	}
	
	public List<T> Top(KeyQueryCheckerBase<T> kq, int k) {
		List<T> ret = new ArrayList<>(kq.submittedQueries());
		Comparator<T> cmp = selectionStrategy.comparator(kq, new ArrayList<>());
		Collections.sort(ret, cmp);
		if(cmp instanceof NdcgSelectionComparator) {
			NdcgSelectionComparator<T> ndcgComp = (NdcgSelectionComparator<T>) cmp;
			List<Pair<T, Double>> tmp = ret.stream().map(i -> Pair.of(i, ndcgComp.ndcg(i))).collect(Collectors.toList());
				tmp = tmp.stream()
						.filter(i -> kq.isKeyQuery(i.getLeft()))
						.collect(Collectors.toList());
			ret = tmp.stream()
					.map(i -> i.getLeft())
					.collect(Collectors.toList());
		}
		return ret.stream().limit(k).collect(Collectors.toList());
	}
	
	public List<T> TopNounPhrases(KeyQueryCheckerBase<T> kq, int k) {
		List<T> ret = new ArrayList<>(kq.submittedQueries());
		Comparator<T> cmp = selectionStrategy.comparator(kq, new ArrayList<>());
		Collections.sort(ret, cmp);
		if(cmp instanceof NdcgSelectionComparator) {
			NdcgSelectionComparator<T> ndcgComp = (NdcgSelectionComparator<T>) cmp;
			List<Pair<T, Double>> tmp = ret.stream().map(i -> Pair.of(i, ndcgComp.ndcg(i))).collect(Collectors.toList());
			tmp = tmp.stream()
					.filter(i -> i.getRight() > 0)
					.collect(Collectors.toList());
			ret = tmp.stream()
					.map(i -> i.getLeft())
					.collect(Collectors.toList());
		
		}
		return ret.stream().limit(k).collect(Collectors.toList());
	}
	
	public List<Pair<T, Double>> TopWithValue(KeyQueryCheckerBase<T> kq, int k, boolean phrase) {
		List<T> ret = new ArrayList<>(kq.submittedQueries());
		List<Pair<T, Double>> tmp = new ArrayList<>();
		Comparator<T> cmp = selectionStrategy.comparator(kq, new ArrayList<>());
		Collections.sort(ret, cmp);
		if(cmp instanceof NdcgSelectionComparator) {
			if(!phrase) {
				NdcgSelectionComparator<T> ndcgComp = (NdcgSelectionComparator<T>) cmp;
			    tmp = ret.stream().map(i -> Pair.of(i, ndcgComp.ndcg(i))).collect(Collectors.toList());
				tmp = tmp.stream()
						.filter(i -> kq.isKeyQuery(i.getLeft()))
						.collect(Collectors.toList());
				
			} else {
				NdcgSelectionComparator<T> ndcgComp = (NdcgSelectionComparator<T>) cmp;
			    tmp = ret.stream().map(i -> Pair.of(i, ndcgComp.ndcg(i))).collect(Collectors.toList());
				tmp = tmp.stream()
						.filter(i -> i.getRight() > 0)
						.collect(Collectors.toList());
			}
			
		}
		
		
		return tmp.stream().limit(k).collect(Collectors.toList());
	}
	
	public List<T> selectTop(KeyQueryCheckerBase<T> kq, int k, boolean relaxed) {
		List<T> ret = new ArrayList<>();
		for(int i=0; i<k; i++) {
			T candidate = remainingTopCandidate(kq, ret, relaxed);
			
			if(candidate != null && !ret.contains(candidate)) {
				ret.add(candidate);
			}
		}
		
		return ret;
	}
	
	public List<T> selectTopOracle(KeyQueryCheckerBase<T> kq, int k) {
		List<T> ret = new ArrayList<>();
		for(int i=0; i<k; i++) {
			T candidate = remainingTopCandidateOracle(kq, ret);
			
			if(candidate != null && !ret.contains(candidate)) {
				ret.add(candidate);
			}
		}
		
		return ret;
	}
	
	public List<T> selectTopNounPhrase(KeyQueryCheckerBase<T> kq, int k) {
		List<T> ret = new ArrayList<>();
		for(int i=0; i<k; i++) {
			T candidate = remainingTopCandidateNounPhrase(kq, ret);
			
			if(candidate != null && !ret.contains(candidate)) {
				ret.add(candidate);
			}
		}
		return ret;
	}
	
	public List<Pair<T, Double>> selectTopKeyqueryWithValue(KeyQueryCheckerBase<T> kq, int k, boolean phrase) {
		List<T> ret = new ArrayList<>();
		List<Pair<T, Double>> pairs = new ArrayList<>();
		for(int i=0; i<k; i++) {
			Pair<T, Double> candidate = remainingTopCandidateKeyquery(kq, ret, phrase);
			
			if(candidate != null && !ret.contains(candidate.getLeft())) {
				ret.add(candidate.getLeft());
				pairs.add(candidate);
			}
		}
		return pairs;
	}
	
	private Pair<T, Double> remainingTopCandidateKeyquery(KeyQueryCheckerBase<T> kq, List<T> alreadyRanked, boolean phrase) {
		List<T> ret = new ArrayList<>(kq.submittedQueries());
		ret.removeAll(alreadyRanked);
		List<Pair<T, Double>> tmp = new ArrayList<>();
		Comparator<T> cmp = selectionStrategy.comparator(kq, alreadyRanked);
		Collections.sort(ret, cmp);
		if(cmp instanceof NdcgSelectionComparator) {
			if(!phrase) {
				NdcgSelectionComparator<T> ndcgComp = (NdcgSelectionComparator<T>) cmp;
			    tmp = ret.stream().map(i -> Pair.of(i, ndcgComp.ndcg(i))).collect(Collectors.toList());
				tmp = tmp.stream()
						.filter(i -> kq.isKeyQuery(i.getLeft()))
						.collect(Collectors.toList());
				
			if(tmp.size() > 1) {
				//System.out.println("I select '" + tmp.get(0).getLeft() + "' (ndcg=" +  tmp.get(0).getRight() + ") from " + tmp);
			}
			} else {
				NdcgSelectionComparator<T> ndcgComp = (NdcgSelectionComparator<T>) cmp;
			    tmp = ret.stream().map(i -> Pair.of(i, ndcgComp.ndcg(i))).collect(Collectors.toList());
				tmp = tmp.stream()
						.filter(i -> i.getRight() > 0)
						.collect(Collectors.toList());
			}
			
		}
		
		
		return tmp.size() >= 1 ? tmp.get(0) : null;
	}
	
	private T remainingTopCandidateOracle(KeyQueryCheckerBase<T> kq, List<T> alreadyRanked) {
		List<T> ret = new ArrayList<>();
		ret.removeAll(alreadyRanked);
		Comparator<T> cmp = selectionStrategy.comparator(kq, alreadyRanked);
		Collections.sort(ret, cmp);
		if(cmp instanceof NdcgSelectionComparator) {
			NdcgSelectionComparator<T> ndcgComp = (NdcgSelectionComparator<T>) cmp;
			List<Pair<T, Double>> tmp = ret.stream().map(i -> Pair.of(i, ndcgComp.ndcg(i))).collect(Collectors.toList());
			tmp = tmp.stream()
					.filter(i -> i.getRight() > 0)
					.collect(Collectors.toList());
			ret = tmp.stream()
					.map(i -> i.getLeft())
					.collect(Collectors.toList());
		}
		return ret.size() >= 1 ? ret.get(0) : null;
	}
	
	private T remainingTopCandidate(KeyQueryCheckerBase<T> kq, List<T> alreadyRanked, boolean relaxed) {
		List<T> ret = new ArrayList<>(kq.submittedQueries());
		ret.removeAll(alreadyRanked);
		Comparator<T> cmp = selectionStrategy.comparator(kq, alreadyRanked);
		Collections.sort(ret, cmp);
		if(cmp instanceof NdcgSelectionComparator) {
			NdcgSelectionComparator<T> ndcgComp = (NdcgSelectionComparator<T>) cmp;
			List<Pair<T, Double>> tmp = ret.stream().map(i -> Pair.of(i, ndcgComp.ndcg(i))).collect(Collectors.toList());
			if(!relaxed) {
				tmp = tmp.stream()
						.filter(i -> kq.isKeyQuery(i.getLeft()))
						.collect(Collectors.toList());
			} else {
				List<Integer> difficulty = new ArrayList<Integer>();
				for(int i=kq.getTargetDocumentSize(); i>0; i--) {
					difficulty.add(i);
				}
				tmp = tmp.stream()
						.filter(i -> i.getRight() > 0 && kq.isRelaxedKeyQuery(i.getLeft(), difficulty))
						.collect(Collectors.toList());
			}
			if(tmp.size() > 1) {
				//System.out.println("I select '" + tmp.get(0).getLeft() + "' (ndcg=" +  tmp.get(0).getRight() + ") from " + tmp);
			}
			ret = tmp.stream()
					.map(i -> i.getLeft())
					.collect(Collectors.toList());
		}
		
		
		return ret.size() >= 1 ? ret.get(0) : null;
	}

	private T remainingTopCandidateNounPhrase(KeyQueryCheckerBase<T> kq, List<T> alreadyRanked) {
		List<T> ret = new ArrayList<>(kq.submittedQueries());
		ret.removeAll(alreadyRanked);
		Comparator<T> cmp = selectionStrategy.comparator(kq, alreadyRanked);
		Collections.sort(ret, cmp);
		if(cmp instanceof NdcgSelectionComparator) {
			NdcgSelectionComparator<T> ndcgComp = (NdcgSelectionComparator<T>) cmp;
			List<Pair<T, Double>> tmp = ret.stream().map(i -> Pair.of(i, ndcgComp.ndcg(i))).collect(Collectors.toList());
			tmp = tmp.stream()
					.filter(i -> i.getRight() > 0)
					.collect(Collectors.toList());
			if(tmp.size() > 1) {
				//System.out.println("I select '" + tmp.get(0).getLeft() + "' (ndcg=" +  tmp.get(0).getRight() + ") from " + tmp);
			}
			ret = tmp.stream()
					.map(i -> i.getLeft())
					.collect(Collectors.toList());
		
		}
		return ret.size() >= 1 ? ret.get(0) : null;
	}
	public static interface SelectionStrategy<T> extends Serializable {
		Comparator<T> comparator(KeyQueryCheckerBase<T> kq, List<T> usedQueries);
	}
}

