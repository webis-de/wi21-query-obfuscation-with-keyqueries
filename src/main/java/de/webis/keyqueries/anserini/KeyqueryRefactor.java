package de.webis.keyqueries.anserini;

import java.util.Map;

import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.search.query.BagOfWordsQueryGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;

import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.combination.Interleaving;
import de.webis.keyqueries.generators.KeyQueryCandidateGenerator;
import de.webis.keyqueries.generators.lucene.NounPhrasesGenerator;
import de.webis.keyqueries.util.Util;


public class KeyqueryRefactor<T> implements Reranker<T>, RelevanceFeedbackAware<T> {
	private Map<String, Integer> relevanceFeedback;
	private ScoredDocuments relevanceFeedbackDocs;
	private List<KeyQueryCandidateGenerator<String>> candidateGenerator;
	@Override
	public void setRelevanceFeedback(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		this.relevanceFeedback = relevanceFeedback;
		this.relevanceFeedbackDocs = Util.relevanceFeedbackAsScoredDocs(relevanceFeedback, context);
	}

	@Override
	public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
		System.out.println("============"+" Query: " +context.getQueryText() +" ============\n");
		//declare variables
		String output = "============"+" Query: " +context.getQueryText() +" ============\n";
		Set<String> topQueries = new HashSet<String>();
		Set<String> bestPhrase = new HashSet<String>();
		Map<String, Float> termsAndWeights = new HashMap<String, Float>();
		String endQuery = "";
		List<Map<String, Float>> termMap = new ArrayList<Map<String, Float>>();
		Set<String> keyqueries_counting = new HashSet<String>();
		List<Set<String>> terms = new ArrayList<Set<String>>();
		List<List<ScoreDoc>> ret = new ArrayList<List<ScoreDoc>>();
		Set<String> checkedQueries = new HashSet<String>();
		Map<String, Integer> collect = relevanceFeedback.entrySet().stream()
				.filter(x -> x.getValue() > 0)
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
		LuceneSearcher searcher = new LuceneSearcher(context.getIndexSearcher(), Util.analyzer(context.getSearchArgs()));
		KeyQueryChecker checker = new KeyQueryChecker(collect.keySet(), searcher, context.getSearchArgs().keyquery_k, context.getSearchArgs().keyquery_l);
		Set<String> keyqueries = new HashSet<String>();
		if(context.getSearchArgs().rm3_term) {
			termsAndWeights = Util.getRM3TermsAndWeights(relevanceFeedbackDocs, context);
			termMap.add(termsAndWeights);
		}
		if(context.getSearchArgs().prf_term) {
			termMap.add(Util.getPRFTermsAndWeights(relevanceFeedbackDocs, context));
		}
		if(context.getSearchArgs().axiom_term) {
			termMap.add(Util.getAxiomTermsAndWeights(relevanceFeedbackDocs, context));
		}
		//select terms
		if(context.getSearchArgs().rm3keyquery) {
			termsAndWeights = Util.getRM3TermsAndWeights(relevanceFeedbackDocs, context);
			terms.add(Util.topTerms(termsAndWeights, context.getSearchArgs().term_depth));
			keyqueries.addAll(Util.calculateKeyqueryCandidates(terms.get(terms.size()-1)));
		} else if(context.getSearchArgs().axiomkeyquery) {
			terms.add(Util.getAxiomTerms(relevanceFeedbackDocs, context, context.getSearchArgs().term_depth));
			keyqueries.addAll(Util.calculateKeyqueryCandidates(terms.get(terms.size()-1)));
		} else if(context.getSearchArgs().prfkeyquery) {
			terms.add(Util.getPRFTerms(relevanceFeedbackDocs, context, context.getSearchArgs().term_depth));
			keyqueries.addAll(Util.calculateKeyqueryCandidates(terms.get(terms.size()-1)));
		} else {
			//tf-idf
			candidateGenerator = KeyQueryCandidateGenerator.anseriniKeyQueryCandidateGenerator(context);
			List<Set<String>>  tmp = Util.getTfIdfTermsDocument(candidateGenerator, collect.keySet());
			for(Set<String> term: tmp) {
				terms.add(term);
				keyqueries.addAll(Util.calculateKeyqueryCandidates(terms.get(terms.size()-1)));
			}
			terms.add(Util.getTfIdfTermsCollection(candidateGenerator, collect.keySet()));
			keyqueries.addAll(Util.calculateKeyqueryCandidates(terms.get(terms.size()-1)));
		}
		Set<String> combine = new HashSet<String>();
		for(Set<String> termset: terms) {
			combine.addAll(termset);
		}
		Map<String, String> unstemmed = Util.unstemm(combine, collect.keySet(), context);
		//use nounphrases
		if(context.getSearchArgs().nounphrases) {
			NounPhrasesGenerator noun = new NounPhrasesGenerator(collect, context);
			bestPhrase = noun.generateNounPhrases();
		}
		//select keyqueries
		BagOfWordsQueryGenerator querygenerator = new BagOfWordsQueryGenerator();
		if(context.getSearchArgs().selection_ndcg) {
			for(String query: keyqueries) {
				context.getSearchArgs().keyquery_count++;
				checker.issueQuery(query);
				
			}
			if(context.getSearchArgs().relaxedKeyquery) {
				topQueries = Util.selectRelaxedKeyqueries(checker,collect.keySet(),context.getSearchArgs().depth);
			} else {
				topQueries = Util.selectKeyqueries(checker, context.getSearchArgs().depth);
			}
			for(String query: topQueries) {
				System.out.println(query);
				//output += query +"\n";
					checkedQueries.add(query);
				
			}
		} else {
			for(String query: keyqueries) {
				if(!checker.isKeyQuery(query)) {
					continue;
				}
				System.out.println(query);
				checkedQueries.add(query);
			}
		}
		//select query combination
		List<ScoreDoc> merged = new ArrayList<ScoreDoc>();
		if(context.getSearchArgs().teamdraft) {
			merged = Interleaving.teamDraftInterleaving1(ret);
		} else if(context.getSearchArgs().boolquery){
			if(checkedQueries.size() == 0) {
				merged = Util.boolQuery(bestPhrase, context, true);
			} else {
				merged = Util.boolQuery(checkedQueries,bestPhrase, context, false);
			}
		} else if(context.getSearchArgs().greedyquery) {
			endQuery = Util.greedyQuery(checkedQueries, bestPhrase,context.getSearchArgs().greedy_depth, context, collect);
			System.out.println(endQuery);
			merged = Util.BagOfWordsQuery(endQuery, context, false);
		} else if(context.getSearchArgs().counting) { 
			Set<String> topTerms = Util.extractQueryFromKeyqueries(checkedQueries, context.getSearchArgs().counting_depth);
			Set<String> can = Util.calculateKeyqueryCandidates(new HashSet<String>(topTerms));
			for(String query: can) {
				if(checker.isKeyQuery(query)) {
					System.out.println(query);
					keyqueries_counting.add(query);
				}
			}
			merged = Util.boolQuery(keyqueries_counting, context, false);
		} else if(context.getSearchArgs().weighted) {
			merged = Util.boolAndWeightedQuery(termMap, checkedQueries, bestPhrase,context);
		} else {
			merged = Interleaving.balancedInterleaving1(ret);
		}
		ScoredDocuments res = new ScoredDocuments();
		if(merged.size() == 0) {
			System.out.println("Empty");
			res =  docs;
		} else {
			res.documents = new Document[merged.size()];
			res.ids = new int[merged.size()];
			res.scores = new float[merged.size()];
			for(int i=0; i<merged.size(); i++) {
				try {
					res.documents[i] = context.getIndexSearcher().doc(merged.get(i).doc);
				} catch(IOException e) {
					e.printStackTrace();
			        res.documents[i] = null;
				}
				res.scores[i] = merged.get(i).score;
			    res.ids[i] = merged.get(i).doc;
			}
		}
		if(context.getSearchArgs().nounphrases) {
			output+="============"+" NounPhrases: "+"============\n";
			for(String best: bestPhrase) {
				output+=best+"\n";
			}
		}
		output+="============"+ "Terms: " +"============\n";
		for(String term: combine) {
			output+=unstemmed.get(term)+"\n";
		}
		output+="============"+ " Keyqueries: " +"============\n";
		for(String query: checkedQueries) {
			String[] split = query.split(" ");
			String query_unstemmed = "";
			for(String s: split) {
				query_unstemmed+=" "+unstemmed.get(s); 
			}
			output+=query_unstemmed.stripLeading()+"\n";
		}
		if(context.getSearchArgs().greedyquery) {
			output+="============" +" Greedy: " +"============\n";
			String[] split = endQuery.split(" ");
			String query_unstemmed = "";
			for(String s: split) {
				query_unstemmed+=" "+unstemmed.get(s); 
			}
			output+=query_unstemmed.stripLeading()+"\n";
			if(checker.isKeyQuery(endQuery)) {
				output+="Is a keyquery\n";
			} else {
				output+="Is not a keyquery\n";
				if(merged.size() == 0) {
					output+="Empty\n";
				}
			}
		}
		if(context.getSearchArgs().counting) {
			output+="============" +" Counting: " +"============\n";
			for(String query: keyqueries_counting) {
				String[] split = query.split(" ");
				String query_unstemmed = "";
				for(String s: split) {
					query_unstemmed+=" "+unstemmed.get(s); 
				}
				output+=query_unstemmed.stripLeading()+"\n";
			}
		}
		output+="============"+" Begin Positions: "+"============\n";
		Map<String, Integer> pos = Util.positionOfTargetdocuments(collect, res);
		for(Map.Entry<String, Integer> entry : pos.entrySet()) {
			output+=entry.getKey()+" at Position " +entry.getValue() +"\n";
		}
		output+="============"+" End Positions: "+"============\n";
		/*File file = new File("debug_refactor.txt");
		try {
			file.createNewFile();
			FileWriter myWriter = new FileWriter(file, false);
			myWriter.write(output);
		    myWriter.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}*/
		return res;
	}
	
	private Set<String> generateKeyqueries(Map<String, Integer> relevanceFeedback2) {
		Set<String> ret = new HashSet();
		for(KeyQueryCandidateGenerator generator: candidateGenerator) {
			ret.addAll(generator.generateCandidates(relevanceFeedback2.keySet()));
		}
		return ret;
	}

	@Override
	public String tag() {
		return "keyquery-refactor";
	}
	
}



