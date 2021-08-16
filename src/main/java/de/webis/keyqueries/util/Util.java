package de.webis.keyqueries.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.ar.ArabicAnalyzer;
import org.apache.lucene.analysis.bn.BengaliAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.hi.HindiAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MultiPhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;

import com.google.common.collect.Sets;

import org.apache.commons.lang3.tuple.Pair;

import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.KeyQueryCheckerBase;
import de.webis.keyqueries.anserini.AxiomRelevanceFeedbackReranker;
import de.webis.keyqueries.anserini.LuceneSearcher;
import de.webis.keyqueries.anserini.PrfRelevanceFeedbackReranker;
import de.webis.keyqueries.anserini.RM3RelevanceFeedbackReranker;
import de.webis.keyqueries.combination.Interleaving;
import de.webis.keyqueries.generators.DocumentCollectionTfIdfKeyQueryCandidateGenerator;
import de.webis.keyqueries.generators.KeyQueryCandidateGenerator;
import de.webis.keyqueries.generators.DocumentCollectionTfIdfKeyQueryCandidateGenerator.CombinedTerm;
import de.webis.keyqueries.generators.DocumentTfIdfKeyQueryCandidateGenerator.TermWithScore;
import de.webis.keyqueries.generators.lucene.LuceneDocumentTfIdfKeyQueryCandidateGenerator;
import de.webis.keyqueries.selection.CrypsorQuerySelectionStrategy;
import de.webis.keyqueries.selection.NdcgSelectionStrategy;
import io.anserini.analysis.AnalyzerUtils;
import io.anserini.analysis.DefaultEnglishAnalyzer;
import io.anserini.analysis.TweetAnalyzer;
import io.anserini.index.IndexArgs;
import io.anserini.index.IndexCollection;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.search.SearchArgs;
import io.anserini.search.SimpleSearcher;
import io.anserini.search.query.BagOfWordsQueryGenerator;
import io.anserini.search.query.QueryGenerator;

public interface Util {
	/**
	 * Moves documents on top of an already existing result set
	 * @param docs Initial scored documents
	 * @param relevantdocs Set with IDs of relevant documents
	 * @param rs Result set 
	 * @return TopDocs with documents specified in <code>relevantdocs</code> on top of <code>rs</code>
	 */
	public static TopDocs moveToTop(ScoredDocuments docs, Set<String> relevantdocs, TopDocs rs) {
		List<ScoreDoc> sdocs = new ArrayList<ScoreDoc>();
	    List<Integer> ids = new ArrayList<Integer>();
	    for (int i = 0; i < docs.ids.length; i++){
	    	String id = docs.documents[i].get(IndexArgs.ID);
	    	if(relevantdocs.contains(id)) {
	    		sdocs.add(new ScoreDoc(docs.ids[i], 100+i));
	    	  	ids.add(docs.ids[i]);
	    	}
	    }
	    for(int i = 0; i<rs.scoreDocs.length; i++) {
	    	if(!ids.contains(rs.scoreDocs[i].doc)) {
	    		sdocs.add(rs.scoreDocs[i]);
	    	}
	    }
	    ScoreDoc[] scoredocs  = new ScoreDoc[sdocs.size()];
	    for(int i = 0; i<sdocs.size(); i++) {
	    	scoredocs[i] = sdocs.get(i);
	    }
	    TopDocs newrs = new TopDocs(rs.totalHits, scoredocs);
	    return newrs;
	}
	public static List<ScoreDoc> moveToTop(ScoredDocuments docs, List<ScoreDoc> original) {
		List<ScoreDoc> ret = new ArrayList<ScoreDoc>();
		List<Integer> idsOriginal = new ArrayList<Integer>();
		for(int i=0; i<original.size(); i++) {
			idsOriginal.add(original.get(i).doc);
			ret.add(new ScoreDoc(original.get(i).doc, original.get(i).score+100));
		}
		for(int i=0; i<docs.documents.length; i++) {
			if(!idsOriginal.contains(docs.ids[i])) {
				ret.add(new ScoreDoc(docs.ids[i], docs.scores[i]));
			}
		}
		return ret;
	}
	private static <T> BooleanQuery.Builder generateBooleanQueryBuilder(Set<String> queries, boolean stem) {
		BooleanQuery.Builder builder = new BooleanQuery.Builder();
		BagOfWordsQueryGenerator querygenerator = new BagOfWordsQueryGenerator();
		Query q = null;
		for(String query: queries) {
			if(!stem) {
				q = querygenerator.buildQuery(IndexArgs.CONTENTS, DefaultEnglishAnalyzer.newStemmingInstance("none"), query);
			} else {
				q = querygenerator.buildQuery(IndexArgs.CONTENTS, IndexCollection.DEFAULT_ANALYZER, query);
			}
			builder.add(q, BooleanClause.Occur.SHOULD);
		}
		return builder;
	}
	public static <T> BooleanQuery generateBooleanQuery(Set<String> queries, boolean stem) {
		BooleanQuery.Builder builder = generateBooleanQueryBuilder(queries, stem);
		return builder.build();
	}
	public static <T> List<ScoreDoc> boolQuery(Set<String> queries, RerankerContext<T> context, boolean stem) {
		List<ScoreDoc> ret = new ArrayList<ScoreDoc>();
		BooleanQuery boolq = generateBooleanQuery(queries, stem);
		try {
			TopDocs tmp = context.getIndexSearcher().search(boolq, context.getSearchArgs().rerankcutoff);
			for(ScoreDoc doc: tmp.scoreDocs) {
				ret.add(doc);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return ret;
	}
	
	public static <T> List<ScoreDoc> weightedQuery(Set<String> queries, Set<String> nounphrases,Map<String, Float> terms, RerankerContext<T> context) {
		List<ScoreDoc> ret = new ArrayList<ScoreDoc>();
		List<MultiPhraseQuery> multi = generateMultiPhraseQueries(nounphrases);
		BooleanQuery.Builder feedbackQueryBuilder = new BooleanQuery.Builder();
		IndexSearcher rs = context.getIndexSearcher();
		for(String query: queries) {
			 String[] split = query.split(" ");
			 float sum = 0;
			 for(String term: split) {
				 sum+=terms.get(term);
			 }
			 if(sum == 0) break;
			 BooleanQuery.Builder tmp = new BooleanQuery.Builder();
			 for(String term: split) {
				 
				 tmp.add(new BoostQuery(new TermQuery(new Term(IndexArgs.CONTENTS, term)), (float) terms.get(term)/sum), BooleanClause.Occur.SHOULD);
			 }
			 BooleanQuery tmpQuery = tmp.build();
			 feedbackQueryBuilder.add(tmpQuery, BooleanClause.Occur.SHOULD);
		}
		for(MultiPhraseQuery m: multi) {
			feedbackQueryBuilder.add(m, BooleanClause.Occur.SHOULD);
		}
			 Query feedbackQuery = feedbackQueryBuilder.build();
			 Query finalQuery = feedbackQuery;
			 if (context.getFilter() != null) {
				   System.out.println(context.getFilter().toString());
			       BooleanQuery.Builder bqBuilder = new BooleanQuery.Builder();
			       bqBuilder.add(context.getFilter(), BooleanClause.Occur.FILTER);
			       bqBuilder.add(feedbackQuery, BooleanClause.Occur.MUST);
			       finalQuery = bqBuilder.build();
			     }
			 try {
				 TopDocs topDocs = rs.search(finalQuery, context.getSearchArgs().rerankcutoff);
				 for(ScoreDoc doc: topDocs.scoreDocs) {
						ret.add(doc);
					}
			 } catch(Exception e) {
				 e.printStackTrace();
			 }

		return ret;
	}
	
	
	public static <T> List<ScoreDoc> boolQuery(Set<String> queries, Set<String> nounphrases, RerankerContext<T> context, boolean stem) {
		List<ScoreDoc> ret = new ArrayList<ScoreDoc>();
		List<MultiPhraseQuery> multi = generateMultiPhraseQueries(nounphrases);
		BooleanQuery.Builder builder = generateBooleanQueryBuilder(queries, stem);
		for(MultiPhraseQuery query: multi) {
			builder.add(query, BooleanClause.Occur.SHOULD);
		}
		BooleanQuery boolq = builder.build();
		try {
			TopDocs tmp = context.getIndexSearcher().search(boolq, context.getSearchArgs().rerankcutoff);
			for(ScoreDoc doc: tmp.scoreDocs) {
				ret.add(doc);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return ret;
	}
	public static <T> List<MultiPhraseQuery> generateMultiPhraseQueries(Set<String> queries) {
		List<MultiPhraseQuery> multiphrasequeries = new ArrayList<MultiPhraseQuery>();
		for(String query: queries) {
			MultiPhraseQuery.Builder builder = new MultiPhraseQuery.Builder();
			List<String> tokens = AnalyzerUtils.analyze(query);
			for(String token: tokens) {
				builder.add(new Term(IndexArgs.CONTENTS, token));
			}
			MultiPhraseQuery multi = builder.build();
			multiphrasequeries.add(multi);
		}
		return multiphrasequeries;
	}
	public static <T> List<ScoreDoc> multiPhraseQuery(ScoredDocuments docs,Set<String> queries, RerankerContext<T> context) {
		List<ScoreDoc> ret = new ArrayList<ScoreDoc>();
		List<List<ScoreDoc>> queryList = new ArrayList<List<ScoreDoc>>();
		List<MultiPhraseQuery> multi = generateMultiPhraseQueries(queries);
		for(MultiPhraseQuery query: multi) {
			List<ScoreDoc> tmpres = new ArrayList<ScoreDoc>();
			try {
				TopDocs tmp = context.getIndexSearcher().search(query, context.getSearchArgs().rerankcutoff);
				for(ScoreDoc doc: tmp.scoreDocs) {
					tmpres.add(doc);
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			queryList.add(tmpres);
		}
		ret = Interleaving.balancedInterleaving1(queryList);
		return moveToTop(docs, ret);
	}
	
	public static <T> String greedyQuery(Set<String> queries, Set<String> nounphrases ,int complexity, RerankerContext<T> context, Map<String, Integer> rf) {
		List<Pair<String, Double>> pairs = getListOfPairs(queries, nounphrases ,context, rf);
		pairs = pairs.stream()
				.sorted(Collections.reverseOrder(Comparator.comparing(Pair::getValue)))
				.collect(Collectors.toList());
		Set<String> query = new HashSet<String>();
		String result = "";
		for(Pair<String, Double> entry: pairs) {
			if(query.size() <= complexity) {
				query.addAll(Arrays.asList(entry.getLeft().split(" ")));
			} else {
				break;
			}
		}
		result = query.stream().limit(complexity).collect(Collectors.joining(" "));
		return result;
	}
	
	public static <T> List<ScoreDoc> BagOfWordsQuery(String result, RerankerContext<T> context,  boolean stem) {
		List<ScoreDoc> ret = new ArrayList<ScoreDoc>();
		try {
			BagOfWordsQueryGenerator querygenerator = new BagOfWordsQueryGenerator();
			Query q = null;
			if(!stem) {
				q = querygenerator.buildQuery(IndexArgs.CONTENTS, DefaultEnglishAnalyzer.newStemmingInstance("none"), result);
			} else {
				q = querygenerator.buildQuery(IndexArgs.CONTENTS, IndexCollection.DEFAULT_ANALYZER, result);
			}
			TopDocs tmp = context.getIndexSearcher().search(q, context.getSearchArgs().rerankcutoff);
			for(ScoreDoc doc: tmp.scoreDocs) {
				ret.add(doc);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return ret;
	}	
	
	public static Query originalQuery(RerankerContext<?> context) {
		try {
			QueryGenerator generator = (QueryGenerator) Class.forName("io.anserini.search.query." + context.getSearchArgs().queryGenerator)
					.getConstructor().newInstance();
			return generator.buildQuery(IndexArgs.CONTENTS, Util.analyzer(context.getSearchArgs()), context.getQueryText());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Set<String> extractQueryFromKeyqueries(Set<String> queries, int limit) {
		List<String> sortedQueries = new ArrayList<String>();
		Map<String, Integer> freq = new HashMap<String,Integer>();
		for(String query: queries) {
			String[] split = query.split(" ");
			for(String elem: split) {
				if(!freq.containsKey(elem)) {
					freq.put(elem,1);
				} else {
					freq.put(elem, freq.get(elem)+1);
				}
			}
		}
		freq.entrySet().stream()
		.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
		.forEach(e -> sortedQueries.add(e.getKey()));
		return new HashSet<String>(sortedQueries.subList(0, Math.min(sortedQueries.size(), limit)));
	}
	
	public static Map<String, Integer> positionOfTargetdocuments(Map<String, Integer> targetDocuments, ScoredDocuments docs) {
		Map<String, Integer> pos = new LinkedHashMap<String, Integer>();
		Set<String> ids = targetDocuments.keySet();
		for(int i=0; i<docs.documents.length; i++) {
			if(ids.contains(docs.documents[i].get(IndexArgs.ID))) {
				pos.put(docs.documents[i].get(IndexArgs.ID),i+1);
			}
		}
		return pos;
	}
	public static Set<String> parseNewQuery(String[] cmd) {
		Set<String> newQuery = new HashSet<String>();
		for(String queryWithUnderscore: cmd) {
			newQuery.add(queryWithUnderscore.replace("_", " "));
		}
		return newQuery;
	}
	
	public static <T> List<Pair<String, Double>> getListOfPairs(Set<String> keyqueries, RerankerContext<T> context, Map<String, Integer> collect) {
		return Util.getListOfPairs(keyqueries, new HashSet<String>(), context, collect);
	}
	
	public static <T> List<Pair<String, Double>> getListOfPairs(Set<String> keyqueries,Set<String> nounphrases, RerankerContext<T> context, Map<String, Integer> collect) {
		LuceneSearcher searcher = new LuceneSearcher(context.getIndexSearcher(), Util.analyzer(context.getSearchArgs()));
		KeyQueryChecker checker = new KeyQueryChecker(collect.keySet(), searcher, context.getSearchArgs().keyquery_k, context.getSearchArgs().keyquery_l);
		for(String keyquery: keyqueries) {
			checker.issueQuery(keyquery);
		}
		KeyQueryChecker checker_noun = new KeyQueryChecker(collect.keySet(), searcher, context.getSearchArgs().keyquery_k, context.getSearchArgs().keyquery_l);
		for(String noun: nounphrases) {
			checker_noun.issueQuery(noun);
		}
		CrypsorQuerySelectionStrategy<String> selection = new CrypsorQuerySelectionStrategy<>(new NdcgSelectionStrategy<>());
		List<Pair<String, Double>> pairs = selection.TopWithValue(checker, 5, false);
		pairs.addAll(selection.TopWithValue(checker_noun, 5, true));
		return pairs;
	}
	
	public static Set<String> calculateKeyqueryCandidates(Set<String> terms) {
		Set<Set<String>> powerset = Sets.powerSet(terms);
		Set<String> candidates = new HashSet<String>();
		for(Set<String> set: powerset) {
			if(set.isEmpty()) continue;
			candidates.add(set.stream().collect(Collectors.joining(" ")));
		}
		return candidates;
	}
	
	public static <T> Set<String> calculateKeyqueryCandidatesExpanded(Set<String> terms, Set<String> originalQueryTerms, RerankerContext<T> context, int query_length) {
		Set<Set<String>> powerset = Sets.powerSet(terms);
		Set<String> candidates = new HashSet<String>();
		for(Set<String> set: powerset) {
			if(set.isEmpty() || set.size() != query_length) continue;
			set = new HashSet<>(set);
			if(Double.parseDouble(context.getSearchArgs().rm3_originalQueryWeight[0]) > 0) {
				set.addAll(originalQueryTerms);
			}
			candidates.add(set.stream().collect(Collectors.joining(" ")));
		}
		return candidates;
	}
	
	public static Set<String> selectRelaxedKeyqueries(KeyQueryChecker checker, Set<String> docId, int depth)  {
		CrypsorQuerySelectionStrategy<String> selection = new CrypsorQuerySelectionStrategy<>(new NdcgSelectionStrategy<>());
		Set<String> relaxed = new HashSet<String>();
		Set<Set<String>> permutation = Sets.powerSet(docId);
		List<Set<String>> combination = permutation.stream().
				sorted(Comparator.comparing(Set<String>::size).reversed()).
				collect(Collectors.toList());
		for(Set<String> comb: combination) {
			if(comb.isEmpty()) break;
			checker.setTargetDocuments(comb);
			relaxed.addAll(selection.Top(checker, depth));
			if(relaxed.size() >= depth) {
				break;
			}
		}
		return relaxed;
	}

	public static <T> Map<String, Float> getRM3TermsAndWeights(ScoredDocuments relevantDocuments, RerankerContext<T> context) {
		return getRM3TermsAndWeights(relevantDocuments, context, null);
	}
	
	public static <T> Map<String, Float> getRM3TermsAndWeights(ScoredDocuments relevantDocuments, RerankerContext<T> context, Set<String> terms) {
		RM3RelevanceFeedbackReranker<T> internalReranker = new RM3RelevanceFeedbackReranker<>(context.getSearchArgs());
		internalReranker.setRelevanceFeedback(asRelevantDocs(relevantDocuments), context);
		
		return internalReranker.getTermsAndWeights(relevantDocuments, context, terms);
	}
	
	public static Map<String, Integer> asRelevantDocs(ScoredDocuments relevantDocuments) {
		return Stream.of(relevantDocuments.documents)
				.map(i -> i.get(IndexArgs.ID))
				.collect(Collectors.toMap(i -> i, i -> 1));
	}
	
	public static Analyzer analyzer(SearchArgs args) {
		//copied from SearchCollection
	    if (args.searchtweets) {
	        return new TweetAnalyzer();
	      } else if (args.language.equals("zh")) {
	        return new CJKAnalyzer();
	      } else if (args.language.equals("ar")) {
	        return new ArabicAnalyzer();
	      } else if (args.language.equals("fr")) {
	        return new FrenchAnalyzer();
	      } else if (args.language.equals("hi")) {
	        return new HindiAnalyzer();
	      } else if (args.language.equals("bn")) {
	        return new BengaliAnalyzer();
	      } else if (args.language.equals("de")) {
	        return new GermanAnalyzer();
	      } else if (args.language.equals("es")) {
	        return new SpanishAnalyzer();
	      } else {
	        // Default to English
	    	return args.keepstop ?
	            DefaultEnglishAnalyzer.newStemmingInstance(args.stemmer, CharArraySet.EMPTY_SET) :
	            DefaultEnglishAnalyzer.newStemmingInstance(args.stemmer);
	      }
	}
	
	public static int uniqueInt(String[] choices) {
		return Integer.parseInt(unique(choices));
	}
	
	public static float uniqueFloat(String[] choices) {
		return Float.parseFloat(unique(choices));
	}
	
	public static String unique(String[] choices) {
		if (choices == null || choices.length != 1) {
			throw new RuntimeException("Couldnt handle:" + Arrays.asList(choices));
		}
		
		return choices[0];
	}
	
	public static Set<String> topTerms(Map<String, Float> terms, int limit) {
		return terms.entrySet().stream()
			.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
			.limit(limit)
			.map(i -> i.getKey())
			.collect(Collectors.toSet());
	}
	
	public static <T> Set<String> getAxiomTerms(ScoredDocuments relevantDocuments, RerankerContext<T> context, int limit) {
		Map<String, Float> terms = getAxiomTermsAndWeights(relevantDocuments, context);
		return topTerms(terms, limit);
	}
	
	public static <T> Map<String, Float> getAxiomTermsAndWeights(ScoredDocuments relevantDocuments, RerankerContext<T> context) {
      AxiomRelevanceFeedbackReranker<T> internalReranker = new AxiomRelevanceFeedbackReranker<>(context.getSearchArgs());
      internalReranker.setRelevanceFeedback(asRelevantDocs(relevantDocuments), context);

      return internalReranker.getTermsAndWeights(relevantDocuments, context);
	}
	
	public static <T> ScoredDocuments relevanceFeedbackAsScoredDocs(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		List<ScoreDoc> relevantDocs = relevanceFeedback.entrySet().stream()
			.filter(x -> x.getValue() > 0)
			.map(i -> scoreForDoc(i.getKey(), context))
			.filter(i -> i != null)
			.sorted((a,b) -> Double.compare(b.score, a.score))
			.collect(Collectors.toList());
		mapZeroScoresToMinFloat(relevantDocs);
		
		TopDocs topDocs = new TopDocs(null, relevantDocs.toArray(new ScoreDoc[relevantDocs.size()]));
		return ScoredDocuments.fromTopDocs(topDocs, context.getIndexSearcher());
	}
	
	public static void mapZeroScoresToMinFloat(List<ScoreDoc> docs) {
		for(ScoreDoc doc: docs) {
			if(doc.score <= 0.0000001) {
				doc.score = 0.0000001f;
			}
		}
	}
	
	private static <T> ScoreDoc scoreForDoc(String docId, RerankerContext<T> context) {
		try {
			BooleanQuery scoreForDoc = new BooleanQuery.Builder()
				      .add(idIs(docId), Occur.FILTER)
				      .add(Util.originalQuery(context), Occur.SHOULD)
				      .build();
			
			TopDocs ret = context.getIndexSearcher().search(scoreForDoc, 10);
			if (ret != null && ret.scoreDocs != null && ret.scoreDocs.length == 1) {
				return ret.scoreDocs[0];
			} else {
				return null;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Query idIs(String documentId) {
		return new TermQuery(new Term(IndexArgs.ID, documentId));
	}
	
	public static <T> Set<String>  getPRFTerms(ScoredDocuments relevantDocuments, RerankerContext<T> context, int limit) {
		Map<String, Float> terms = getPRFTermsAndWeights(relevantDocuments, context);
		
		return topTerms(terms, limit);
	}

	public static <T> Map<String, Float> getPRFTermsAndWeights(ScoredDocuments relevantDocuments, RerankerContext<T> context) {
		return getPRFTermsAndWeights(relevantDocuments, context, null); 
	}
	
	public static <T> Map<String, Float> getPRFTermsAndWeights(ScoredDocuments relevantDocuments, RerankerContext<T> context, Set<String> allowedTerms) {
		PrfRelevanceFeedbackReranker<T> internalReranker = new PrfRelevanceFeedbackReranker<>(context.getSearchArgs());
		internalReranker.setRelevanceFeedback(asRelevantDocs(relevantDocuments), context);
		
		return internalReranker.getTermsAndWeights(relevantDocuments, context, allowedTerms); 
	}
	
	public static List<Set<String>> getTfIdfTermsDocument(List<KeyQueryCandidateGenerator<String>> generator, Set<String> targetdocuments) {
		List<Set<String>> terms = new ArrayList<Set<String>>();
		for(KeyQueryCandidateGenerator<String> gen: generator) {
			if(gen instanceof LuceneDocumentTfIdfKeyQueryCandidateGenerator) {
				for(String doc: targetdocuments) {
					terms.add(new HashSet<String>(((LuceneDocumentTfIdfKeyQueryCandidateGenerator) gen).topTermVectors(doc)));
				}
			}
		}
		return terms;
	}
	
	public static Set<String> getTfIdfTermsCollection(List<KeyQueryCandidateGenerator<String>> generator, Set<String> targetdocuments) {
		Set<String> terms = new HashSet<String>();
		List<List<TermWithScore>> termsOfTerms = new ArrayList<>();
		for(KeyQueryCandidateGenerator<String> gen: generator) {
			if(gen instanceof DocumentCollectionTfIdfKeyQueryCandidateGenerator) {
				for(String doc: targetdocuments) {
					termsOfTerms.add(((DocumentCollectionTfIdfKeyQueryCandidateGenerator) gen).getTfIdfGenerator().termsSortedByScore(doc));
				}
				List<CombinedTerm> combined = DocumentCollectionTfIdfKeyQueryCandidateGenerator.combine(termsOfTerms);
				terms.addAll(DocumentCollectionTfIdfKeyQueryCandidateGenerator.selectTop(combined, ((DocumentCollectionTfIdfKeyQueryCandidateGenerator) gen).getTopCandidates()));
			}
		}
		return terms;
	}
	
	public static <T> Set<T> selectKeyqueries(KeyQueryCheckerBase<T> checker, int depth) {
		CrypsorQuerySelectionStrategy<T> selection = new CrypsorQuerySelectionStrategy<>(new NdcgSelectionStrategy<>());
		Set<T> keyqueries = new HashSet<>();
		keyqueries.addAll(selection.Top(checker, depth));
		return keyqueries;
	}
	
	public static <T> Map<String, String> unstemm(Set<String> terms, Set<String> docId ,RerankerContext<T> context) {
		Map<String, String> unstemmedTerms= new HashMap<String, String>();
		Map<String, Map<String, Integer>> possibleForms = new HashMap<String, Map<String, Integer>>();
		try(SimpleSearcher simple = new SimpleSearcher(context.getSearchArgs().index);) {
			for(String id: docId) {
				String rawdoc = simple.documentRaw(id).replace("\n", " ");
				List<String> tokens = AnalyzerUtils.analyze(rawdoc);
				List<String> notstemmed = AnalyzerUtils.analyze(DefaultEnglishAnalyzer.newStemmingInstance("none"), rawdoc);
				for(String term: terms) {
					for(int i=0; i<tokens.size(); i++) {
						if(term.equals(tokens.get(i))) {
							if(!possibleForms.containsKey(term)) {
								Map<String, Integer> tmp = new HashMap<String, Integer>();
								tmp.put(notstemmed.get(i), 1);
								possibleForms.put(term, tmp);
							} else {
								Map<String, Integer> tmp = possibleForms.get(term);
								if(!tmp.containsKey(notstemmed.get(i))) {
									tmp.put(notstemmed.get(i),1);
								} else {
									tmp.put(notstemmed.get(i), tmp.get(notstemmed.get(i))+1);
								}
								possibleForms.put(term, tmp);
							}
						}
					}
				}
			}
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		for(Map.Entry<String, Map<String, Integer>> entry : possibleForms.entrySet()) {
			Map<String, Integer> tmp = entry.getValue();
			if(tmp.size() == 0) {
				unstemmedTerms.put(entry.getKey(), "");
				continue;
			}
			Optional<Entry<String, Integer>> maxEntry = tmp.entrySet()
			        .stream()
			        .max(Comparator.comparing(Map.Entry::getValue));
			unstemmedTerms.put(entry.getKey(), maxEntry.get().getKey());
		}
		unstemmedTerms.put("","");
		return unstemmedTerms;
	}
	
	public static <T> List<ScoreDoc> boolAndWeightedQuery(List<Map<String, Float>> weights, Set<String> queries, Set<String> noun,RerankerContext<T> context) {
		List<ScoreDoc> ret = new ArrayList<ScoreDoc>();
		BooleanQuery.Builder feedbackQueryBuilder = new BooleanQuery.Builder();
		BooleanQuery.setMaxClauseCount(100000);
		for(Map<String, Float> map: weights) {
			for(Map.Entry<String, Float> entry: map.entrySet()) {
				feedbackQueryBuilder.add(new BoostQuery(new TermQuery(new Term(IndexArgs.CONTENTS, entry.getKey())), entry.getValue()), BooleanClause.Occur.SHOULD);
			}
		}
		BooleanQuery boolq = feedbackQueryBuilder.build();
		BooleanQuery.Builder endQueryBuilder = new BooleanQuery.Builder();
		endQueryBuilder.add(new BoostQuery(boolq, (float)0.75), BooleanClause.Occur.SHOULD);
		BagOfWordsQueryGenerator querygenerator = new BagOfWordsQueryGenerator();
		float boost = 0;
		if(queries.size()+noun.size() != 0) {
			boost = (float)0.25/(queries.size()+noun.size());
		}
		for(String query: queries) {
			Query q = querygenerator.buildQuery(IndexArgs.CONTENTS, DefaultEnglishAnalyzer.newStemmingInstance("none"), query);
			endQueryBuilder.add(new BoostQuery(q, boost), BooleanClause.Occur.SHOULD);
		}
		List<MultiPhraseQuery> multi= Util.generateMultiPhraseQueries(noun);
		for(MultiPhraseQuery m: multi) {
			endQueryBuilder.add(new BoostQuery(m, boost), BooleanClause.Occur.SHOULD);
		}
		BooleanQuery endq = endQueryBuilder.build();
		try {
			TopDocs tmp = context.getIndexSearcher().search(endq, context.getSearchArgs().rerankcutoff);
			for(ScoreDoc doc: tmp.scoreDocs) {
				ret.add(doc);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return ret;
	}
}
