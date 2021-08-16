package de.webis.keyqueries.generators.chatnoir;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import de.webis.keyqueries.generators.chatnoir.CrypsorKeyQueryCandidateGenerator.InternalKeyQueryCandidateGenerator;
import lombok.Data;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;

@Data
public class NounPhraseExtraction implements InternalKeyQueryCandidateGenerator {
	
	private static final String POS_MODEL_FILE = "/en-pos-maxent.bin";
	private static final String SENTENCE_MODEL = "/en-sent.bin";
	
	public static void main(String[] args) {
		NounPhraseExtraction npe = new NounPhraseExtraction();
		Set<String> targetDocs = new HashSet<>(Arrays.asList("clueweb12-1811wb-16-29005"));
		SensitiveTerms sensitiveTerms = SensitiveTerms.getSensitiveTermsWithSynonyms("pokemon");
		
		npe.getCandidates(sensitiveTerms, targetDocs);
	}
	
	@Override
	public List<String> getCandidates(SensitiveTerms sensitiveTerms, Set<String> targetDocuments) {
		String[] texts = targetDocuments.stream()
			.map(i -> new ChatNoirDocument(i).mainContent())
			.collect(Collectors.toList()).toArray(new String[targetDocuments.size()]);
		List<String> ret = nounPhrasesSortedFromOftenToRare(sensitiveTerms, 10, texts);

		
		return Sets.powerSet(new HashSet<>(ret)).stream()
				.map(i -> combine(i))
				.filter(i -> !i.trim().isEmpty())
				.collect(Collectors.toList());
	}
	
	private static String combine(Set<String> i) {
		String ret = "";
		for (String t: i) {
			ret += " " + t;
		}
		
		return ret.trim();
	}

	public List<String> nounPhrasesSortedFromOftenToRare(SensitiveTerms sensitiveTerms, int top, String...texts) {
		return nounPhrasesSortedFromOftenToRare(texts).stream()
				.filter(i -> !sensitiveTerms.phraseIsDeniedByUser(i))
				.limit(top)
				.collect(Collectors.toList());
	}
	
	public List<String> nounPhrasesSortedFromOftenToRare(String...texts) {
		return calculateNounPhrases(texts).entrySet().stream().map(i -> i.getKey()).collect(Collectors.toList());
	}
	
	public Map<String, Integer> calculateNounPhrases(String...texts) {
		Map<String, Integer> ret = new LinkedHashMap<>();
		
		for(String text: texts) {
			text = StringOperations.replaceMoreThanOneSpaces(text).trim();
			text = StringOperations.removeStopwords(text);
			Map<String, Integer> current = calculateNounPhrasesInternal(tag(text));
			
			for(Map.Entry<String, Integer> i: current.entrySet()) {
				ret.put(i.getKey(), i.getValue() + ret.getOrDefault(i.getKey(), 0));
			}
		}
		
		List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(ret.entrySet());
		Collections.sort(list, new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		
		ret = new LinkedHashMap<>();
		for(Entry<String, Integer> i: list) {
			ret.put(i.getKey(), i.getValue());
		}
		
		return ret;
	}
	
	private String tag(String paragraph) {
		paragraph = paragraph.replaceAll("[^A-za-z ]", " . ").replaceAll("\\[", " . ").replaceAll("\\]", " . ");
		paragraph = paragraph.replaceAll(" +", " ");

		String tagged = "";
		POSTaggerME tagger = new POSTaggerME(posModel());
		// Call Sentence Detector
		String[] sentences = getSentences(paragraph);
		            
		for (String sentence: sentences) {
			String whitespaceTokenizerLine[] = WhitespaceTokenizer.INSTANCE.tokenize(sentence);
		    String[] tags = tagger.tag(whitespaceTokenizerLine);
		    for (int i = 0; i < whitespaceTokenizerLine.length; i++) {
		    	String word = whitespaceTokenizerLine[i].trim();
		        String tag = tags[i].trim();
		        
		        tagged += word + "_" + tag + " ";
		    }
		}
		
		return tagged;
	}
	
	private static POSModel posModel() {
		try {
		InputStream posModel = NounPhraseExtraction.class.getResourceAsStream(POS_MODEL_FILE);
			return new POSModel(posModel);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String[] getSentences(String paragraph) {
		
		SentenceModel model = null;
		String [] sentencesReturn = null;

		// Load model object
		try {
				
			InputStream sentenceModel = NounPhraseExtraction.class.getResourceAsStream(SENTENCE_MODEL);
		
			model = new SentenceModel(sentenceModel);
		} catch (Exception e) {
		    e.printStackTrace();
		}

		// Sentence detection
		try {
		    if (model != null) {
		        SentenceDetectorME sdetector = new SentenceDetectorME(model);
		        String[] sentences = sdetector.sentDetect(paragraph);
		        sentencesReturn = sentences;
		    }
		} catch (Exception e) {
		    e.printStackTrace();
		}
		return sentencesReturn;
	}
	
	private static Map<String, Integer> calculateNounPhrasesInternal(String tagged) {
		HashMap<String, Integer> ret = new HashMap<>();
		String recentPhrase = "";
		String [] splitted = StringOperations.stringToArrayBySplitting(tagged, " ");
		for(int i = 0; i < splitted.length; i++) {
			
			if(splitted[i].contains("_JJ") || splitted[i].contains("_NN")) {
				//Remove plural
				if(splitted[i].endsWith("S")) {
					splitted[i] = StringOperations.removePlural(splitted[i]);
				}
				
				recentPhrase += " " + splitted[i];
				
			}else{
				if(recentPhrase.contains("_NN")) {
					recentPhrase = StringOperations.removeMoreThanOneStrings(recentPhrase, new String[]{"_NNP", "_NNS", "_NN", "_JJR", "_JJS", "_JJ"});
					recentPhrase = recentPhrase.toLowerCase().trim();
					
					if(recentPhrase.contains(" ")) {
						if(recentPhrase.split(" ").length <=5) {
							if(ret.containsKey(recentPhrase)){
								ret.put(recentPhrase, (ret.get(recentPhrase) + 1));
							}else {
								ret.put(recentPhrase, 10000);
							}
						}
					}
				}
				recentPhrase = "";
			}
		}
		
		return ret;
	}
}

