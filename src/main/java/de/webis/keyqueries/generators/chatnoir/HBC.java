package de.webis.keyqueries.generators.chatnoir;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import de.webis.keyqueries.KeyQueryChecker;

public class HBC {
	private static final int NUMBERTOPKEYPHRASES = 20;
	private final ArrayList<String> topKeyphrases; // top-n keyphrases
	private final boolean combineWithoutCriteria;
	
	public HBC(ArrayList<String> nounPhrases) {
		this(nounPhrases, false);
	}
	
	public HBC(ArrayList<String> nounPhrases, boolean combineWihtoutCriteria) {
		this.combineWithoutCriteria = combineWihtoutCriteria;
		this.topKeyphrases = nounPhrases;
		failOnInvalidNounphrases(nounPhrases);
	}
	
	public ArrayList<String> getTopKeyphrases(){
		return this.topKeyphrases;
	}
	
	private void failOnInvalidNounphrases(ArrayList<String> nounPhrases) {
		failIfNounphraseListIsEmpty(nounPhrases);
		failIfNounphrasesContainTrailingSpaces(nounPhrases);
		failIfListContainsDuplicates(nounPhrases);
	}
	
	private void failIfNounphraseListIsEmpty(ArrayList<String> nounPhrasesToCheck) {
		if(nounPhrasesToCheck.isEmpty()) {
			throw new IllegalArgumentException("Nounphrase list is illegally empty.");
		}
	}
	
	private void failIfNounphrasesContainTrailingSpaces(ArrayList<String> nounPhrasesToCheck) {
		nounPhrasesToCheck.forEach(phrase -> {
			if(phrase.trim().length() < phrase.length()) {
				throw new IllegalArgumentException("Nounphrases contain illegal leading or trailing spaces.");
			}
		});
	}
	
	private void failIfListContainsDuplicates(ArrayList<String> nounPhrasesToCheck) {
		List<String> listWithoutDuplicates = nounPhrasesToCheck.stream()
	            .distinct()
	            .collect(Collectors.toList());
		if(listWithoutDuplicates.size() < nounPhrasesToCheck.size()) {
			throw new IllegalArgumentException("Nounphrases contain illegally duplicates.");
		}
	}
	
	public void runHbcAlgorithm(KeyQueryChecker keyQueryChecker) {
		ArrayList<ArrayList<String>> Candidates = new ArrayList<ArrayList<String>>();
		Candidates.add(0, topKeyphrases);
		ArrayList<String> candidatesNotRemove = new ArrayList<String>();

		// 1. Level
		int level = 0;
		ArrayList<String> candidate = Candidates.get(level);
		for (int j = 0; j < candidate.size(); j++) {
			String keyphrase = candidate.get(j);
			System.out.println(keyphrase);
			if (keyQueryChecker.isKeyQuery(keyphrase)) { // HBC-algorithm, line 4-7 (Check for conditions of keyquery)
				System.out.println(keyphrase+" is keyquery");
			} else {
				candidatesNotRemove.add(keyphrase);
			}
		}
		Candidates.add(candidatesNotRemove);
		level++;
		candidate = Candidates.get(level);
		ArrayList<String> candidateForNextI = new ArrayList<String>();

		// Next Levels
		while (candidate.size() > 0 && level <= 3) { // HBC-algorithm, line 10 (combine maximum 5 phrases) //TODO 4??
			for (int j = 0; j < candidate.size(); j++) { // HBC-algorithm, line 11
				String c = candidate.get(j);
				for (int j1 = j + 1; j1 < candidate.size(); j1++) {
					String c1 = candidate.get(j1);
					if (level == 1 || overlapBigEnough(c, c1, level)) { // 2. level: combination of all phrases; > 3. level:
																// overlap must be big enough (i-1), c != c1
						String cCombi = combine(c, c1); // HBC-algorithm, line 12 (phrases are ranked due to the
														// algorithm of combining)
						if(candidatePassesCriteria(cCombi, candidate, keyQueryChecker, candidateForNextI)) {
							candidateForNextI.add(cCombi);
						}
					}
				}
			}
			level++;
			Candidates.add(level, candidateForNextI);
			candidate.clear();
			for (String s : candidateForNextI) {
				candidate.add(s);
			}
			// System.out.println("Number candidates: "+candidate.size());
			candidateForNextI.clear();
		}
	}
	
	
	private boolean candidatePassesCriteria(String cCombi, ArrayList<String> candidate, KeyQueryChecker keyQueryChecker, ArrayList<String> candidateForNextI) {
		
		// FIXME: it sould not be possible that ccCombi is empty? or should it?
		if (!cCombi.trim().isEmpty() && !candidateForNextI.contains(cCombi)) { // candidate not generated yet
			 System.out.println(cCombi);
			if (!combineWithoutCriteria) {
				if (hitsFewer(cCombi, candidate, keyQueryChecker)) { // HBC-algorithm, line 13
					if (!keyQueryChecker.isKeyQuery(cCombi) && keyQueryChecker.parameterLIsSatisfied(cCombi)) {
						return true;
					} else {
						if(keyQueryChecker.isKeyQuery(cCombi)) {
							System.out.println(cCombi + " is keyquery");
						}
						return false;
					}
				} else {
					System.out.println(cCombi + ": longer word does not find more target documents");
					return false;
				}
			} else {
				return true;
			}
		}
		return false;

	}
	
	
	private boolean overlapBigEnough(String c, String c2, int level) { // TODO einfacher?
		// System.out.println("OVERLAP: " + c + " , "+ c2);
		int overlap = 0;
		ArrayList<String> keyphrasesCandidate1 = new ArrayList<String>();
		ArrayList<String> keyphrasesCandidate2 = new ArrayList<String>();

		for (String s : topKeyphrases) {
			if (c.contains(s)) {
				keyphrasesCandidate1.add(s);
			}
			if (c2.contains(s)) {
				keyphrasesCandidate2.add(s);
			}
		}
		for (String s : keyphrasesCandidate1) { // check, whether current keyphrase occures in c and c1 --> overlap?
			if (keyphrasesCandidate2.contains(s)) {
				overlap++;
			}
		}
		return overlap == level - 1; // if overlap == i-1 at i. level: true
	}

	public String combine(String p, String q) { // combines two phrases; removes overlapping phrase
		String combinedPhrase = "";
		for (String s : topKeyphrases) {
			// add all phrases in p and q to combinedPhrase. Does not add overlapping phrase
			// twice because of if/else
			if (p.contains(s) || q.contains(s)) {
				combinedPhrase += s + " ";
			}
		}
		
		if(combinedPhrase.contains("  ")) {
			throw new RuntimeException("---> '" + p +"', '" + q + "'. ---> " + combinedPhrase);
		}
		return combinedPhrase.trim(); // returns combinedPhrase. Combined Keyphrases are ranked by their
										// keyphrase-scores because of the way they are combined
	}

	private boolean hitsFewer(String cCombi, ArrayList<String> candidate, KeyQueryChecker kq) { // TODO einfacher?
		for (String p : topKeyphrases) { // iterate through all keyphrases that are combined
			if (cCombi.contains(p)) { // check, whether current candidat cCombi contains current keyphrase
				String candidateWithoutCurrentPhrase = removePhraseFromCandidate(cCombi, p);
				if (candidateWithoutCurrentPhrase != null && !candidateWithoutCurrentPhrase.trim().isEmpty() && kq.isKeyQuery(candidateWithoutCurrentPhrase)) {
					if (kq.isKeyQuery(cCombi)) {
						if (kq.targetDocumentsInResult(candidateWithoutCurrentPhrase).size() >= kq.targetDocumentsInResult(cCombi).size()) { // checks, if candidate reduced by phrase hits less documents than current
												// Candidates
							return false; // if not, return immediately false (because every phrase must find fewer
											// documents than cCombi)
						}
					} else { // current phrase hits at least one original document, but cCombi does not
						// System.out.println("Candidate findet mehr als");
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public static String removePhraseFromCandidate(String query, String phrase) {
		return query.replaceFirst(phrase, "").trim().replaceAll("\\s+", " ");
	}

	public ArrayList<String> sortPhrasesToValues(HashMap<String, Integer> phrases, int size) {
		ArrayList<String> sortedPhrases = new ArrayList<>();
		List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(phrases.entrySet());
		Collections.sort(list, new Comparator<Entry<String, Integer>>() {

			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		phrases.clear();
		int i = 0;
		for (Entry<String, Integer> item : list) {
//			if (i < 10 * size) { // FIXME size??
				phrases.put(item.getKey(), item.getValue());
				if (i < NUMBERTOPKEYPHRASES) {
					sortedPhrases.add(item.getKey());
				}
				i++;
//			} else {
//				return sortedPhrases;
//			}
		}
		return sortedPhrases;
	}
	//NOTE: sortedPhrases only contains the top-NUMBERTOPKEYPHRASES phrases; phrases will contain all phrases in sorted order

}