package de.webis.keyqueries.generators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Sets;

import de.webis.keyqueries.generators.DocumentTfIdfKeyQueryCandidateGenerator.TermWithScore;

@SuppressWarnings("serial")
public class DocumentCollectionTfIdfKeyQueryCandidateGenerator implements KeyQueryCandidateGenerator<String> {

	private final DocumentTfIdfKeyQueryCandidateGenerator tfIdfGenerator;

	private final int topCandidates;
	
	public DocumentCollectionTfIdfKeyQueryCandidateGenerator(DocumentTfIdfKeyQueryCandidateGenerator tfIdfGenerator, int topCandidates) {
		super();
		this.topCandidates = topCandidates;
		this.tfIdfGenerator = tfIdfGenerator;
	}
	
	public DocumentTfIdfKeyQueryCandidateGenerator getTfIdfGenerator() {
		return tfIdfGenerator;
	}
	
	public int getTopCandidates() {
		return topCandidates;
	}
	
	@Override
	public List<String> generateCandidates(Set<String> targetDocuments) {
		List<List<TermWithScore>> termsOfTerms = new ArrayList<>();
		
		for(String docId: targetDocuments) {
			termsOfTerms.add(tfIdfGenerator.termsSortedByScore(docId));
		}

		List<CombinedTerm> combined = combine(termsOfTerms);
		return Sets.powerSet(new HashSet<>(selectTop(combined, topCandidates))).stream()
				.filter(i -> !i.isEmpty())
				.map(j -> j.stream().collect(Collectors.joining(" ")))
				.collect(Collectors.toList());
	}
	
	public static List<CombinedTerm> combine(List<List<TermWithScore>> termsOfTerms) {
		Map<String, Pair<Integer, Float>> current = new HashMap<>();
		
		for(List<TermWithScore> terms: termsOfTerms) {
			for(TermWithScore term: terms) {
				Pair<Integer, Float> oldP = current.getOrDefault(term.getTerm(), Pair.of(0, Float.MIN_VALUE));
				Pair<Integer, Float> newP = Pair.of(oldP.getLeft()+1, Math.max(oldP.getRight(), term.getScore()));
				
				current.put(term.getTerm(), newP);
			}
		}
		
		return current.entrySet().stream()
				.map(i -> new CombinedTerm(i.getKey(), i.getValue().getLeft(), i.getValue().getRight()))
				.collect(Collectors.toList());
	}

	public static class CombinedTerm {
		@Override
		public String toString() {
			return "CombinedTerm [term=" + term + ", matchedDocuments=" + matchedDocuments + ", maxScore=" + maxScore
					+ "]";
		}
		public CombinedTerm(String term, Integer matchedDocuments, Float maxScore) {
			super();
			this.term = term;
			this.matchedDocuments = matchedDocuments;
			this.maxScore = maxScore;
		}
		private final String term;
		private final Integer matchedDocuments;
		private final Float maxScore;
		
		public String getTerm() {
			return term;
		}
		public Integer getMatchedDocuments() {
			return matchedDocuments;
		}
		public Float getMaxScore() {
			return maxScore;
		}
	}

	public static List<String> selectTop(List<CombinedTerm> input, int k) {
		input = new ArrayList<>(input);
		Collections.sort(input, (a,b) -> compare(a,b));
		
		
		return input.stream()
				.limit(k)
				.map(i -> i.getTerm())
				.collect(Collectors.toList());
	}
	
	private static int compare(CombinedTerm a, CombinedTerm b) {
		if(b.getMatchedDocuments().compareTo(a.getMatchedDocuments()) != 0) {
			return b.getMatchedDocuments().compareTo(a.getMatchedDocuments());
		} else {
			return b.getMaxScore().compareTo(a.getMaxScore());
		}
	}
}