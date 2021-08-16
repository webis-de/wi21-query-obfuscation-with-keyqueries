package de.webis.keyqueries.generators.lucene;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;

import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.anserini.LuceneSearcher;
import de.webis.keyqueries.selection.CrypsorQuerySelectionStrategy;
import de.webis.keyqueries.selection.NdcgSelectionStrategy;
import de.webis.keyqueries.util.Util;
import io.anserini.rerank.RerankerContext;
import io.anserini.search.SimpleSearcher;
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;

public class NounPhrasesGenerator<T> {
	Map<String, Integer> relevancefeedback;
	RerankerContext<T> context;
	final String[] stopwords = new String[]{"i", "me", "my", "myself", "we", "our", "ours", "ourselves", "you", "you're", "you've",
			"you'll", "you'd", "your", "yours", "yourself", "yourselves", "he", "him", "his", "himself", "she", "she's", "her", "hers",
			"herself", "it", "it's", "its", "itself", "they", "them", "their", "theirs", "themselves", "what", "which", "who", "whom",
			"this", "that", "that'll", "these", "those", "am", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
			"having", "do", "does", "did", "doing", "a", "an", "the", "and", "but", "if", "or", "because", "as", "until", "while", "of",
			"at", "by", "for", "with", "about", "against", "between", "into", "through", "during", "before", "after", "above", "below",
			"to", "from", "up", "down", "in", "out", "on", "off", "over", "under", "again", "further", "then", "once", "here", "there",
			"when", "where", "why", "how", "all", "any", "both", "each", "few", "more", "most", "other", "some", "such", "no", "nor",
			"not", "only", "own", "same", "so", "than", "too", "very", "s", "t", "can", "will", "just", "don", "don't", "should",
			"should've", "now", "d", "ll", "m", "o", "re", "ve", "y", "ain", "aren", "aren't", "couldn", "couldn't", "didn", "didn't",
			"doesn", "doesn't", "hadn", "hadn't", "hasn", "hasn't", "haven", "haven't", "isn", "isn't", "ma", "mightn", "mightn't", "mustn"
			, "mustn't", "needn", "needn't", "shan", "shan't", "shouldn", "shouldn't", "wasn", "wasn't", "weren", "weren't", "won", "won't",
			"wouldn", "wouldn't"}; //NLTK stopword list
	public NounPhrasesGenerator(Map<String, Integer> relevancefeedback, RerankerContext<T> context) {
		this.relevancefeedback = relevancefeedback;
		this.context = context;
	}
	public Set<String> generateNounPhrases() {
		Set<String> bestPhrase = new HashSet<String>();
		try {
			LuceneSearcher searcher = new LuceneSearcher(context.getIndexSearcher(), Util.analyzer(context.getSearchArgs()));
			SimpleSearcher simple = new SimpleSearcher(context.getSearchArgs().index);
			InputStream modelInParse = new FileInputStream("en-parser-chunking.bin"); //http://opennlp.sourceforge.net/models-1.5/
			ParserModel model = new ParserModel(modelInParse);
			//create parse tree
			Parser parser = ParserFactory.create(model);
			for(String id: this.relevancefeedback.keySet()) {
				KeyQueryChecker checker = new KeyQueryChecker(this.relevancefeedback.keySet(), searcher, context.getSearchArgs().keyquery_k, context.getSearchArgs().keyquery_l);
				List<String> topPhrases = null;
				ArrayList<Parse[]> parsed = new ArrayList<Parse[]>();
				String rawdoc = simple.documentRaw(id).replace("\n", " ");
				String cleaned = rawdoc.substring(rawdoc.indexOf("[Text]")+6, rawdoc.length()-7);
				String[] split = cleaned.split(Pattern.quote("."));
				if(split.length >= 1000) {
					String[] cropped = Arrays.copyOf(split, 1000);
					split = cropped;
				} 
				System.out.println(split.length);
				for(String s: split) {
					parsed.add(ParserTool.parseLine(s, parser, 1));
				}
				Map<String, Double> nounPhrases = new LinkedHashMap<String,Double>();
				for(int i=0; i<parsed.size(); i++) {
					for (Parse p : parsed.get(i)) {
						nounPhrases = getNounPhrases(p, nounPhrases);
					}	
				}
				double max = Double.MIN_VALUE;
				ArrayList<String> candidates = new ArrayList<String>(); 
				for(Map.Entry<String, Double> entry : nounPhrases.entrySet()) {
					if(entry.getValue() >= max) {
						if(entry.getValue() > max) {
							candidates.clear();
							max = entry.getValue();
						}
						if(NumberUtils.isCreatable(entry.getKey())) {
							continue;
						}
						if(Arrays.stream(this.stopwords).anyMatch(entry.getKey().toLowerCase()::equals)) {
							continue;
						}
						if(entry.getKey().contains("<")) {
							continue;
						}
						checker.issueQuery(entry.getKey());
					}
				}
				CrypsorQuerySelectionStrategy selection = new CrypsorQuerySelectionStrategy(new NdcgSelectionStrategy());
				topPhrases = selection.TopNounPhrases(checker, context.getSearchArgs().noun_depth);
				bestPhrase.addAll(topPhrases);
			}
			modelInParse.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return bestPhrase;
	}
	
	//recursively loop through tree, extracting noun phrases
	private Map<String, Double> getNounPhrases(Parse p, Map<String, Double> phrases) {
	    if (p.getType().equals("NP")) { //NP=noun phrase
	    	if(!phrases.containsKey(p.getCoveredText())) {
	    		phrases.put(p.getCoveredText(), p.getProb());
	    	}
	    }
	    for (Parse child : p.getChildren())
	         getNounPhrases(child, phrases);
	    return phrases;
	}
}
