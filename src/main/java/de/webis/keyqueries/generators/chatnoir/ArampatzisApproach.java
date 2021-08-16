package de.webis.keyqueries.generators.chatnoir;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.webis.keyqueries.generators.chatnoir.CrypsorKeyQueryCandidateGenerator.InternalKeyQueryCandidateGenerator;

@SuppressWarnings("serial")
public class ArampatzisApproach implements Serializable, InternalKeyQueryCandidateGenerator {
	private static final int W_sizeOfSlidingWindow = 16; //size of sliding window
	private static final int l_maxLengthScrambledQueries = 3; //max length of scrambled queries
	private final Set<String> stopWords = StringOperations.getStopWords(null);

	@Override
	public List<String> getCandidates(SensitiveTerms sensitiveTerms, Set<String> targetDocuments) {
		List<String> texts = targetDocuments.stream()
				.map(i -> new ChatNoirDocument(i).mainContent())
				.collect(Collectors.toList());
		
		return getAllCandidates(texts).stream().collect(Collectors.toList());
	}
	
	public Set<String> getAllCandidates(List<String> texts) {
		Set<String> ret = new HashSet<>();
		
		for(String text: texts) {
			if(text != null) {
				List<String> docVector = getDocVector(text); 
				ret.addAll(generateScrambledQueries(docVector, W_sizeOfSlidingWindow, l_maxLengthScrambledQueries));
			}
		}

		return ret;
	}
	
	//Methode: Eingabe docVector; return Iterator ueber Windows
	static Iterable<List<String>> allSlidingWindows(List<String> docVector, int windowSize){
		if(docVector.size() < windowSize) {
			return Arrays.asList(docVector);
		}
		return IntStream.range(0, docVector.size()-windowSize+1).mapToObj(i->docVector.subList(i, i+windowSize)).collect(Collectors.toList());
	}
	
	static Set<String> generateScrambledQueries(List<String> docVector, int windowSize, int maxLengthOfScrambledQueries) {
		Set<Set<String>> combinations = new HashSet<>();
		for(List<String> window : allSlidingWindows(docVector, windowSize)) {
			for(int i=0; i<maxLengthOfScrambledQueries; i++) {
				combinations.addAll(extractAllPermutationsOfLength(window, i+1));
			}
		}
		
		return combinations.stream()
				.map(i -> i.stream().collect(Collectors.joining(" ")))
				.sorted()
				.collect(Collectors.toSet());
	}
	
	static Set<Set<String>> extractAllPermutationsOfLength(List<String> window, int length){
		Set<Set<String>> ret = new HashSet<>();
		
		if(length == 1) {
			for(String w: window) {
				HashSet<String> tmp = new HashSet<>();
				tmp.add(w);
				
				ret.add(tmp);
			}

			return ret;
		}
		
		Set<Set<String>> sub = extractAllPermutationsOfLength(window, length -1);
		
		for(String w: window) {
			for(Set<String> c: sub) {
				if(c.contains(w)) {
					continue;
				}
				Set<String> tmp = new HashSet<>(c);
				tmp.add(w);
				ret.add(tmp);
			}
		}
		
		return ret;
	}
	
	ArrayList<String> getDocVector(String text){
		text = StringOperations.replaceNonLetters(text);
		text = StringOperations.replaceMoreThanOneSpaces(text);
		ArrayList<String> docVector = new ArrayList<String>();

		for(String word : text.split(" ")) {
			if (!stopWords.contains(word) && !word.trim().equals("")) {
				docVector.add(word);
			}
		}
		
		return docVector;
	}
}
