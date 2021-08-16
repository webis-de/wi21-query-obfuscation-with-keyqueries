package de.webis.keyqueries.generators.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.util.BytesRef;

import com.google.common.collect.ImmutableList;

import de.webis.keyqueries.generators.DocumentTfIdfKeyQueryCandidateGenerator;
import io.anserini.analysis.AnalyzerUtils;
import io.anserini.index.IndexArgs;

@SuppressWarnings("serial")
public class LuceneDocumentTfIdfKeyQueryCandidateGenerator extends DocumentTfIdfKeyQueryCandidateGenerator {
	 
	private final IndexSearcher searcher;
	private final boolean alphaNumeric;
	private final ResolveHumanReadableWord resolver;
	
	public LuceneDocumentTfIdfKeyQueryCandidateGenerator(int topCandidates, IndexSearcher searcher, boolean alphaNumeric) {
		this(topCandidates, searcher, alphaNumeric, (a,b) -> a);
	}
	
	public LuceneDocumentTfIdfKeyQueryCandidateGenerator(int topCandidates, IndexSearcher searcher, boolean alphaNumeric, ResolveHumanReadableWord resolver) {
		super(topCandidates);
		this.alphaNumeric = alphaNumeric;
		this.searcher = searcher;
		this.resolver = resolver;
	}

	@Override
	protected List<TermWithScore> terms(String docId) {
		try {
			return termsWithTfIdfScore(docId);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private int docId(String docId) throws IOException {
		BooleanQuery queryForDoc = new BooleanQuery.Builder()
				.add(idIs(docId), org.apache.lucene.search.BooleanClause.Occur.FILTER)
				.build();
		TopDocs docs = searcher.search(queryForDoc, 10);
		if(docs.scoreDocs.length != 1 ) {
			throw new RuntimeException("Fix this: " + docId + " should be unique");
		}
		
		return docs.scoreDocs[0].doc;
	}

	private static Query idIs(String documentId) {
		return new TermQuery(new Term(IndexArgs.ID, documentId));
	}

	private List<TermWithScore> termsWithTfIdfScore(String docId) throws IOException {
		return termsWithTfIdfScore(docId(docId));
	}

	private List<TermWithScore> termsWithTfIdfScore(int docId) throws IOException {
		List<TermWithScore> ret = new ArrayList<>();
		IndexReader reader = searcher.getIndexReader();
		Terms terms = reader.getTermVector(docId, IndexArgs.CONTENTS);
		
		if(terms == null) {
			return new ArrayList<>();
		}
		// Liste von validen Termen erzeugen
		List<String> validTerms = validTerms(terms, reader);
		if(validTerms == null) {
			return Collections.emptyList();
		}
		
		TermsEnum termsEnum = terms.iterator();
		int docCount = reader.getDocCount(IndexArgs.CONTENTS);
		PostingsEnum postings = null;
		BytesRef text;

		while ((text = termsEnum.next()) != null) {
			String term = text.utf8ToString();
			if (term.length() == 0) {
				continue;
			}

			postings = termsEnum.postings(postings, PostingsEnum.FREQS);
			postings.nextDoc();
			int docFrequency = reader.docFreq(new Term(IndexArgs.CONTENTS, term));
			int termFrequency = postings.freq();
			float score = ((float) termFrequency) * new ClassicSimilarity().idf(docFrequency, docCount);
			// ist term in Liste von validen termen enthalten
			if(validTerms.contains(term)) {
				term = resolver.token(term, searcher.doc(docId).get(IndexArgs.ID));
				if(term != null) {
					ret.add(new TermWithScore(term, score));
				}
			}
		}

		return ret;
	}
	private List<String> validTerms(Terms terms, IndexReader reader) {
		   List<String> validTerms = new ArrayList<String>();

		   try {
		     int numDocs = reader.numDocs();
		     TermsEnum termsEnum = terms.iterator();

		     BytesRef text;
		     while ((text = termsEnum.next()) != null) {
		       String term = text.utf8ToString();
		       if (term.length() < 2 || term.length() > 20) continue;
		       if(this.alphaNumeric) {
		    	   Pattern p = Pattern.compile("[a-z]+");
			       Pattern p2 = Pattern.compile("[0-9]+");
			       Matcher m = p.matcher(term);
			       Matcher m2 = p2.matcher(term);
			       if (m.find() && m2.find()) continue;
		       }
		       if(!term.matches("[a-z0-9]+")) continue;

		       // This seemingly arbitrary logic needs some explanation. See following PR for details:
		       //   https://github.com/castorini/Anserini/pull/289
		       //
		       // We have long known that stopwords have a big impact in RM3. If we include stopwords
		       // in feedback, effectiveness is affected negatively. In the previous implementation, we
		       // built custom stopwords lists by selecting top k terms from the collection. We only
		       // had two stopwords lists, for gov2 and for Twitter. The gov2 list is used on all
		       // collections other than Twitter.
		       //
		       // The logic below instead uses a df threshold: If a term appears in more than n percent
		       // of the documents, then it is discarded as a feedback term. This heuristic has the
		       // advantage of getting rid of collection-specific stopwords lists, but at the cost of
		       // introducing an additional tuning parameter.
		       //
		       // Cognizant of the dangers of (essentially) tuning on test data, here's what I
		       // (@lintool) did:
		       //
		       // + For newswire collections, I picked a number, 10%, that seemed right. This value
		       //   actually increased effectiveness in most conditions across all newswire collections.
		       //
		       // + This 10% value worked fine on web collections; effectiveness didn't change much.
		       //
		       // Since this was the first and only heuristic value I selected, we're not really tuning
		       // parameters.
		       //
		       // The 10% threshold, however, doesn't work well on tweets because tweets are much
		       // shorter. Based on a list terms in the collection by df: For the Tweets2011 collection,
		       // I found a threshold close to a nice round number that approximated the length of the
		       // current stopwords list, by eyeballing the df values. This turned out to be 1%. I did
		       // this again for the Tweets2013 collection, using the same approach, and obtained a value
		       // of 0.7%.
		       //
		       // With both values, we obtained effectiveness pretty close to the old values with the
		       // custom stopwords list.
		       int df = reader.docFreq(new Term(IndexArgs.CONTENTS, term));
		       float ratio = (float) df / numDocs;
		       
		       if (ratio > 0.1f) continue;
		       validTerms.add(term);
		     }
		   } catch (Exception e) {
		     throw new RuntimeException(e);
		   }

		   return validTerms;
		 }
	
	public static interface ResolveHumanReadableWord {
		public String token(String stemmedWord, String id);
	}
	
	public static class MapResolveHumanReadableWord implements ResolveHumanReadableWord {
		private final Map<String, Map<String, String>> docIdToTermToHumanReadable;
		
		public MapResolveHumanReadableWord(Map<String, String> docIdToText) {
			docIdToTermToHumanReadable = new HashMap<>();
			
			for(String docId: docIdToText.keySet()) {
				docIdToTermToHumanReadable.put(docId, termToHumanReadable(docIdToText.get(docId)));
			}
		}
		
		public static Map<String, String> termToHumanReadable(String text) {
			Map<String, List<String>> termToWords = new HashMap<>();
			
			for(Object token: ImmutableList.copyOf(new StringTokenizer(text).asIterator())) {//text.split("\\s+")) {
				token = ((String)token).replaceAll("\\W", "");
				String term = token((String)token);
				if(term != null && !term.isEmpty()) {
					if(!termToWords.containsKey(term)) {
						termToWords.put(term, new ArrayList<>());
					}
					
					termToWords.get(term).add((String) token);
				}
			}

			Map<String, String> ret = new LinkedHashMap<>();
			termToWords.keySet().stream().sorted().forEach(i -> ret.put(i, mostFrequentWord(termToWords.get(i))));
			
			return ret;
		}

		private static String mostFrequentWord(List<String> words) {
			Map<String, Integer> tmp = new HashMap<>();
			
			for(String k: words) {
				if(!tmp.containsKey(k)) {
					tmp.put(k, 0);
				}
				
				tmp.put(k, tmp.get(k) +1);
			}

			return tmp.entrySet().stream().sorted((a,b) -> b.getValue().compareTo(a.getValue())).findFirst().get().getKey();
		}

		private static String token(String text) {
			List<String> ret = AnalyzerUtils.analyze(text);
			
			if(ret.size() < 1) {
				return null;
			}
			
			return ret.get(0);
		}
		
		@Override
		public String token(String stemmedWord, String id) {
			return docIdToTermToHumanReadable.get(id).get(stemmedWord);
		}
	}
}
