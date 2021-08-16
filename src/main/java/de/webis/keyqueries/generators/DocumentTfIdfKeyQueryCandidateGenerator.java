package de.webis.keyqueries.generators;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

@SuppressWarnings("serial")
public abstract class DocumentTfIdfKeyQueryCandidateGenerator implements KeyQueryCandidateGenerator<String> {

	protected final int topCandidates;
	
	public DocumentTfIdfKeyQueryCandidateGenerator(int topCandidates) {
		this.topCandidates = topCandidates;
	}
	
	public final List<String> generateCandidates(Set<String> targetDocuments) {
		Set<String> ret = new LinkedHashSet<>();
		
		for(String doc: targetDocuments) {
			for(Set<String> candidate: Sets.powerSet(new HashSet<>(topTermVectors(doc)))) {
				if(!candidate.isEmpty()) {
					ret.add(candidate.stream().collect(Collectors.joining(" ")));
				}
			}
		}

		return ret.stream().collect(Collectors.toList());
	}
	// filter top terms here
	public List<String> topTermVectors(String docId) {
		return allTermVectorsSortedByScore(docId).stream()
				.limit(topCandidates).collect(Collectors.toList());
	}
	
	protected final List<String> allTermVectorsSortedByScore(String docId) {
		return termsSortedByScore(docId)
				.stream().map(i -> i.getTerm())
				.collect(Collectors.toList());
	}
	
	public final List<TermWithScore> termsSortedByScore(String docId) {
		List<TermWithScore> terms = terms(docId);
		
		Collections.sort(terms, (a,b) -> b.getScore().compareTo(a.getScore()));
		return terms;
	}
	
	protected abstract List<TermWithScore> terms(String docId);
	
	public static class TermWithScore {
		private final String term;
		private final float score;
		
		public TermWithScore(String term, float score) {
			this.term = term;
			this.score = score;
		}
		
		public String getTerm() {
			return term;
		}
		
		public Float getScore() {
			return score;
		}
		
		@Override
		public String toString() {
			return "TermWithScore [" + term + "; " + score + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((term == null) ? 0 : term.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof TermWithScore)) {
				return false;
			}
			TermWithScore other = (TermWithScore) obj;
			if (term == null) {
				if (other.term != null) {
					return false;
				}
			} else if (!term.equals(other.term)) {
				return false;
			}
			return true;
		}
	}
}
