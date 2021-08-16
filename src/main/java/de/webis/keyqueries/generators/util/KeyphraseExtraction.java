package de.webis.keyqueries.generators.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;


public class KeyphraseExtraction {
	private final String query;
	private final int sizeT;
	private final String index;
	private final List<String> topKeyphrases = new ArrayList<>();
	private final int numberOfTopKeyPhrases;

	private HashMap<String, Integer> keyphrasesToValue = new HashMap<String, Integer>();
	private HashMap<String, String> stemToKeyphrases = new HashMap<String, String>(); // stem, keyphrase
	private HashMap<String, String> keyphrasesToStem = new HashMap<String, String>(); // keyphrase, stem#
	
	public KeyphraseExtraction(String query, int sizeT, String index, int numberOfTopKeyPhrases) {
		this.query = query;
		this.sizeT = sizeT;
		this.index = index;
		this.numberOfTopKeyPhrases = numberOfTopKeyPhrases;
	}
	
	private void setKeyphrasesToValue(HashMap<String, Integer> keyphrasesToValue) {
		this.keyphrasesToValue = keyphrasesToValue;
	}
//	
//	private void getKeyphrasesFromChatNoir(HashMap<String, ChatNoirDocument> docs, String query, int sizeT) {
//		for (String doc : docs.keySet()) {
//			String url = JSoupSearch.CHATNOIRDOCUMENTURL + docs.get(doc).getUuid() + "&index=" + docs.get(doc).getIndex()
//					+ "&raw&plain";
//			String response = JSoupSearch.issueQueryWithChatNoirFailsave(url);
//			keyphrasesToValue = getKeyphrases(doc, query, sizeT, response);
//			keyphrasesToValue = removeStemOfWordsFromAHashMap(keyphrasesToValue, query.split(" "));
//		}
//	}
//
//	private HashMap<String, Integer> getKeyphrases(String uuidAndIndex, String query, int sizeT, String response) {
//		try {
//			String nounphrase = StringOperations.removeHtmlTags(response); // TODO besser loesen?
//
//			if (nounphrase != "") {
//				KeyPhraseExtractor kpe = new KeyPhraseExtractor(nounphrase, query, sensitiveTerms);
//				CalculateKeyPhrases ckp = new CalculateKeyPhrases(keyphrasesToValue, kpe.getNounPhrases());
//				return ckp.keyphrasestemp;
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
//	
//	public HashMap<String, Integer> removeStemOfWordsFromAHashMap(HashMap<String, Integer> hashmap, String... words){
//		hashmap.entrySet().removeIf(entry -> containsAllWords(entry.getKey(), words));
//		return hashmap;
//	}
//	
//	
//	private boolean containsAllWords(String hashmapKey, String... words) {
//		for(String word : words) {
//			if(! hashmapKey.contains(word) && ! hashmapKey.contains(stemOf(word))) {
//				return false;
//			}
//		}
//		return true;
//	}
//
//	private String stemOf(String word) {
//		return StringOperations.getStemmedPhrase(word);
//	}
//
//	private void uniteKeyphrases() { //TODO vereinfachen
//		String[] kp = StringOperations.removeMoreThanOneStrings(keyphrasesToValue.toString(), new String[] { "\\{", "\\}" }).split(",");
//		String[] kpStem = new String[kp.length];
//
//		for (int i = 0; i < kp.length; i++) {
//			kp[i] = StringOperations.replaceNonLetters(kp[i]).trim();
//			kpStem[i] = kp[i];
//			kpStem[i] = StringOperations.getStemmedPhrase(kpStem[i]);
//
//			stemToKeyphrases.put(kpStem[i], kp[i]);
//			keyphrasesToStem.put(kp[i], kpStem[i]);
//
//			for (int j = 0; j < i; j++) {
//				if (kpStem[i] != null && kpStem[j] != null) {
//					if (kpStem[i].equals(kpStem[j])) {
//						if (kp[i].length() > kp[j].length()) {
//							stemToKeyphrases.remove(kpStem[i]);
//							keyphrasesToStem.remove(kp[i]);
//
//							keyphrasesToValue.put(kp[j], keyphrasesToValue.get(kp[i]) + keyphrasesToValue.get(kp[j]));
//							keyphrasesToValue.remove(kp[i]);
//							kpStem[i] = null;
//						} else {
//							stemToKeyphrases.remove(kpStem[j]);
//							keyphrasesToStem.remove(kp[j]);
//
//							keyphrasesToValue.put(kp[i], keyphrasesToValue.get(kp[i]) + keyphrasesToValue.get(kp[j]));
//							keyphrasesToValue.remove(kp[j]);
//							kpStem[j] = null;
//						}
//					}
//				}
//			}
//		}
//	}
//	
//	private HashMap<String, Integer> sortPhrasesToValues(HashMap<String, Integer> phrases, int size) {
//		topKeyphrases.clear();
//		List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(phrases.entrySet());
//		Collections.sort(list, new Comparator<Entry<String, Integer>>() {
//			@Override
//			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
//				return o2.getValue().compareTo(o1.getValue());
//			}
//		});
//		phrases.clear();
//		int i = 0;
//		for (Entry<String, Integer> item : list) {
//			if (i < 10 * size) {
//				phrases.put(item.getKey(), item.getValue());
//				if (i < numberOfTopKeyPhrases) {
//					topKeyphrases.add(item.getKey());
//				}
//				i++;
//			} else {
//				return phrases;
//			}
//		}
//		return phrases;
//	}
//	
//	public List<String> getTopKeyphrases() {
//		topKeyphrases.clear();
//		issueQuery(query, sizeT, index);
//			
//		getKeyphrasesFromChatNoir(getCurTopDocs(), query, sizeT);
//		uniteKeyphrases();
//	
//		setKeyphrasesToValue(sortPhrasesToValues(keyphrasesToValue, sizeT));
//	
//		return topKeyphrases;
//	}
}