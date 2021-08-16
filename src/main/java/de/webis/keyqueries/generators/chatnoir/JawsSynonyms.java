package de.webis.keyqueries.generators.chatnoir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class JawsSynonyms {
	WordField wordField;
	
	public static void main(String[] args) {
		String wordForm = "heart";
		JawsSynonyms jawsSynonyms = new JawsSynonyms(wordForm);
		WordField wordField = jawsSynonyms.getWordField();
		
		System.out.println("Synonyms");
		wordField.getAllSynonyms();
		System.out.println("Hyponyms");
		wordField.getAllHyponyms();
		System.out.println("Hypernyms");
		wordField.getAllHypernyms();
		System.out.println("Definition");
		wordField.getAllDefinitions();
	}
	
	public JawsSynonyms (String wordForm) {
		System.setProperty("wordnet.database.dir", "src/main/resources/wordnet/WordNet-3.0/dict");
		wordField = new WordField(wordForm);
		
		// Get the synsets containing the word form
		WordNetDatabase database = WordNetDatabase.getFileInstance();
		Synset[] synsets = getSynsets(database, wordForm);
		wordField.addSynsets(synsets);

		for(Synset synset : synsets) {
			wordField.addDefinition(getDefinition(synset));
			if(synset instanceof NounSynset) {
				NounSynset nounSynset = (NounSynset) (synset);
				wordField.addNounSynSets(nounSynset);
				wordField.addHyponyms(getHyponyms(nounSynset));
				wordField.addHypernyms(getHypernyms(nounSynset));
			}
		}
	}
	public WordField getWordField() {
		return wordField;
	}
	
	private static Synset[] getSynsets(WordNetDatabase database, String wordForm) {
		Synset[] synsets = database.getSynsets(wordForm);
		if (synsets.length > 0) {
			for (int i = 0; i < synsets.length; i++) {
				String[] wordForms = synsets[i].getWordForms();
				for (int j = 0; j < wordForms.length; j++) {
				}				
			}
		} else {
			System.err.println("No synsets exist that contain " + "the word form '" + wordForm + "'");
		}
		return synsets;
	}
	
	private static String getDefinition(Synset synset) {
		return synset.getDefinition();
	}
	
	private static NounSynset[] getHyponyms(NounSynset nounSynset) { //semantically under given word (heart --> athlete's heart/city center)
		return nounSynset.getHyponyms();
	}
	
	private static NounSynset[] getHypernyms(NounSynset nounSynset) { //semantically over given word (heart --> organ)
		return nounSynset.getHypernyms();
	}
}

class WordField{
	String word;
	ArrayList<Synset[]> synsets;
	ArrayList<String> definitions;
	ArrayList<NounSynset> nounsynsets;
	ArrayList<NounSynset[]> hyponyms, hypernyms;
	
	
	public WordField(String wordForm){
		this.word = wordForm;
		this.synsets = new ArrayList<>();
		this.definitions = new ArrayList<>();
		this.nounsynsets = new ArrayList<>();
		this.hyponyms = new ArrayList<>();
		this.hypernyms = new ArrayList<>();
	}
	
	public void addSynsets(Synset[] synsets) {
		this.synsets.add(synsets);
	}
	public void addDefinition(String definition) {
		this.definitions.add(definition);
	}
	public void addNounSynSets(NounSynset nounsynsets) {
		this.nounsynsets.add(nounsynsets);
	}
	public void addHyponyms(NounSynset[] hyponyms) {
		this.hyponyms.add(hyponyms);
	}
	public void addHypernyms(NounSynset[] hypernyms) {
		this.hypernyms.add(hypernyms);
	}
	
	public Set<String> getAllDefinitions(){
		Set<String> allDefinitions = new HashSet<>();
		for(int i = 0; i < synsets.size(); i++) {
			for(Synset syn :synsets.get(i)) {
				allDefinitions.add(syn.getDefinition());
			}
		}
		return allDefinitions;
	}
	
	public Set<String> getAllSynonyms() {
		Set<String> allSynonyms = new HashSet<>();
		for(int i = 0; i < synsets.size(); i++) {
			for(Synset syn : synsets.get(i)) {
				for(String s : syn.getWordForms()) {
					allSynonyms.add(s);
				}
			}
		}
		return allSynonyms;
	}
	
	public Set<String> getAllHyponyms(){
		Set<String> allHyponyms = new HashSet<>();
		for(int i = 0; i < hyponyms.size(); i++) {
			for(NounSynset hyponym : hyponyms.get(i)) {
				for(String s : hyponym.getWordForms()) {
					allHyponyms.add(s);
				}
			}
		}
		return allHyponyms;
	}
	
	public Set<String> getAllHypernyms(){
		Set<String> allHypernyms = new HashSet<>();
		for(int i = 0; i < hypernyms.size(); i++) {
			for(NounSynset hypernym : hypernyms.get(i)) {
				for(String s : hypernym.getWordForms()) {
					allHypernyms.add(s);
				}
			}
		}
		return allHypernyms;
	}
	
	public void print() {
		System.out.println("Word: " + word);
		for(int i = 0; i < synsets.size(); i++) {
			
			for(Synset syn : synsets.get(i)) {
				System.out.println("Synset " + i);
				for(String s : syn.getWordForms()) {
					System.out.println(s);
				}
			}
			
			System.out.println("Definition: " + definitions.get(i));
			
			System.out.println("Nounsynset: ");
			for(String s : nounsynsets.get(i).getWordForms()) {
				System.out.println(s);
			}
			
			System.out.println("Hyponym");
			for(NounSynset hyponym : hyponyms.get(i)) {
				System.out.println("Hyponym " + i);
				for(String s : hyponym.getWordForms()) {
					System.out.println(s);
				}
			}
			
			for(NounSynset hypernym : hypernyms.get(i)) {
				System.out.println("Hypernym " + i);
				for(String s : hypernym.getWordForms()) {
					System.out.println(s);
				}
			}
		}
	}
}