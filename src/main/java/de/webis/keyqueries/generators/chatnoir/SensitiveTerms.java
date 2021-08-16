package de.webis.keyqueries.generators.chatnoir;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.anserini.analysis.AnalyzerUtils;
import lombok.Data;

@Data
@SuppressWarnings("serial")
public class SensitiveTerms implements Serializable {
	private final List<String> approvedByUser;
	private final List<String> deniedByUser;
	
	public SensitiveTerms(List<String> approvedByUser, List<String> deniedByUser) {
		this.approvedByUser = approvedByUser;
		this.deniedByUser = deniedByUser;
		for(String term: new ArrayList<>(this.deniedByUser)) {
			term = stem(term);
			if(term != null) {
				this.deniedByUser.add(term);
			}
		}
	}
	
	public static SensitiveTerms getSensitiveTermsWithSynonyms(String query) {
		Set<String> denyList = new HashSet<>();
		denyList.addAll(banAllQueryTokens(query));
		
		for(String queryToken: extractTokens(query)) {
			JawsSynonyms synonyms = new JawsSynonyms(queryToken);
			
			WordField wordField = synonyms.getWordField();

			denyList.addAll(extractTokens(wordField.getAllSynonyms())); // same level
		}
		
		return new SensitiveTerms(Collections.emptyList(), new ArrayList<>(denyList));
	}
	
	public static SensitiveTerms getSensitiveTermsWithSynonymsHyponymsAndHypernyms(String query) {
		Set<String> denyList = new HashSet<>();
		denyList.addAll(banAllQueryTokens(query));
		for(String queryToken: extractTokens(query)) {
			JawsSynonyms synonyms = new JawsSynonyms(queryToken);
			WordField wordField = synonyms.getWordField();
			denyList.addAll(extractTokens(wordField.getAllSynonyms())); // same level
			denyList.addAll(extractTokens(wordField.getAllHyponyms())); // more specifical
			denyList.addAll(extractTokens(wordField.getAllHypernyms())); // more general
		}
		
		return new SensitiveTerms(Collections.emptyList(), new ArrayList<>(denyList));
	}

	public boolean deniedByUser(String term) {
		return term != null && deniedByUser.contains(term.toLowerCase().trim());
	}

	public boolean approvedByUser(String term) {
		return term != null && approvedByUser.contains(term.toLowerCase().trim());
	}

	public boolean phraseIsDeniedByUser(String phrase) {
		if(phrase == null ||phrase.trim().isEmpty()) {
			return Boolean.TRUE;
		}
		
		for(String w : extractTokens(phrase)) {
			if(deniedByUser(w)) {	
				return true;
			}
			
			w = stem(w);
			
			if(deniedByUser(w)) {
				return true;
			}
		}

		return false;
	}
	
	private static Set<String> extractTokens(Collection<String> a) {
		Set<String> ret = new HashSet<>();
		for(String str: a) {
			ret.addAll(extractTokens(str));
		}
		
		return ret;
	}
	
	private static Set<String> extractTokens(String str) {
		return new HashSet<>(Arrays.asList(str.trim().split("\\s+")));
	}
	
	public static SensitiveTerms simpleBanQueryTokens(String privateQuery) {
		List<String> deniedByUser = banAllQueryTokens(privateQuery);
		
		return new SensitiveTerms(Collections.emptyList(), deniedByUser) {
			@Override
			public boolean approvedByUser(String term) {
				return !deniedByUser(term);
			}
		};
	}
	
	private static List<String> banAllQueryTokens(String privateQuery) {
		return Arrays.asList(StringUtils.split(privateQuery))
				.stream().map(i -> i.toLowerCase()).collect(Collectors.toList());
	}
	
	private static String stem(String text) {
		if(text != null) {
			List<String> ret = AnalyzerUtils.analyze(text);
			
			if(ret.size() > 0 && ret.get(0).trim().length() > 2) {
				return ret.get(0).trim().toLowerCase();
			}
		}
		
		return null;
	}
}