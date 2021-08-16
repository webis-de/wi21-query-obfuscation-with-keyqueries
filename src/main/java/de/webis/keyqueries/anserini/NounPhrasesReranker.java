package de.webis.keyqueries.anserini;

import java.util.Map;

import io.anserini.index.IndexArgs;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.search.query.BagOfWordsQueryGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import com.google.common.collect.Sets;

import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.combination.Interleaving;
import de.webis.keyqueries.generators.KeyQueryCandidateGenerator;
import de.webis.keyqueries.selection.CrypsorQuerySelectionStrategy;
import de.webis.keyqueries.selection.NdcgSelectionStrategy;
import de.webis.keyqueries.util.Util;


public class NounPhrasesReranker<T> implements Reranker<T>, RelevanceFeedbackAware<T> {
	private Map<String, Integer> relevanceFeedback;
	private List<KeyQueryCandidateGenerator<String>> candidateGenerator;
	@Override
	public void setRelevanceFeedback(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		this.relevanceFeedback = relevanceFeedback;	
	}

	@Override
	public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
		System.out.println("============"+" Query: " +context.getQueryText() +" ============\n");
		List<String> topQueries = new ArrayList<String>();
		List<List<ScoreDoc>> ret = new ArrayList<List<ScoreDoc>>();
		Set<String> checkedQueries = new HashSet<String>();
		Map<String, Integer> collect = relevanceFeedback.entrySet().stream()
				.filter(x -> x.getValue() > 0)
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
		//NounPhrasesGenerator noun = new NounPhrasesGenerator(collect, context);
		//List<String> bestPhrase = noun.generateNounPhrases();
		candidateGenerator = KeyQueryCandidateGenerator.anseriniKeyQueryCandidateGenerator(context);
		Set<String> keyqueries = generateKeyqueries(collect);
		// checken ob tatsächlich keyquery -> KeyQueryChecker
		LuceneSearcher searcher = new LuceneSearcher(context.getIndexSearcher(), Util.analyzer(context.getSearchArgs()));
		KeyQueryChecker checker = new KeyQueryChecker(collect.keySet(), searcher, context.getSearchArgs().keyquery_k, context.getSearchArgs().keyquery_l);
		// auswählen welche Keyqueries behalten werden
		// Ergebnisse mergen
		BagOfWordsQueryGenerator querygenerator = new BagOfWordsQueryGenerator();
		if(context.getSearchArgs().selection_ndcg) {
			for(String query: keyqueries) {
				context.getSearchArgs().keyquery_count++;
				checker.issueQuery(query);
				
			}
			CrypsorQuerySelectionStrategy selection = new CrypsorQuerySelectionStrategy(new NdcgSelectionStrategy());
			//relaxed keyquery permutations of relevance feedback  
			Set<Set<String>> permutation = Sets.powerSet(collect.keySet());
			List<Set<String>> combination = permutation.stream().
					sorted(Comparator.comparing(Set<String>::size).reversed()).
					collect(Collectors.toList());
			for(Set<String> comb: combination) {
				if(comb.isEmpty()) break;
				checker.setTargetDocuments(comb);
				List<String> topkeyqueries = selection.selectTop(checker, context.getSearchArgs().depth, false);
				for(String query: topkeyqueries) {
					if(!topQueries.contains(query)) {
						topQueries.add(query);
					}
				}
				if(topQueries.size() >= 1) {
					break;
				}
			}
			
			for(String query: topQueries) {
				System.out.println(query);
				//output += query +"\n";
				if(!context.getSearchArgs().boolquery) {
					Query q = querygenerator.buildQuery(IndexArgs.CONTENTS, Util.analyzer(context.getSearchArgs()), query);
					try {
						TopDocs tmp = context.getIndexSearcher().search(q, context.getSearchArgs().rerankcutoff);
						List<ScoreDoc> hits = new ArrayList<ScoreDoc>();
						for(ScoreDoc doc: tmp.scoreDocs) {
							hits.add(doc);
						}
						ret.add(hits);
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				} else {
					checkedQueries.add(query);
				}
				
			}
		} else {
			for(String query: keyqueries) {
				if(!checker.isKeyQuery(query)) {
					continue;
				}
				System.out.println(query);
				if(!context.getSearchArgs().boolquery) {
					Query q = querygenerator.buildQuery(IndexArgs.CONTENTS, Util.analyzer(context.getSearchArgs()), query);
					try {
						TopDocs tmp = context.getIndexSearcher().search(q, context.getSearchArgs().rerankcutoff);
						List<ScoreDoc> hits = new ArrayList<ScoreDoc>();
						for(ScoreDoc doc: tmp.scoreDocs) {
							hits.add(doc);
						}
						ret.add(hits);
					} catch(IOException e) {
						throw new RuntimeException(e);
					}
				} else {
					checkedQueries.add(query);
				}
			}
		}
		/*List<Pair<String, Double>> pairs = Util.getListOfPairs(checkedQueries, bestPhrase, context, collect);
		for(Pair<String, Double> pair: pairs) {
			System.out.println(pair.getLeft() +" " +pair.getRight());
		}*/
		List<ScoreDoc> merged = null;
		if(context.getSearchArgs().teamdraft) {
			merged = Interleaving.teamDraftInterleaving1(ret);
		} else if(context.getSearchArgs().boolquery){
			if(checkedQueries.size() == 0) {
				return docs;
				//merged = Util.boolQuery(bestPhrase, context, true);
			} else {
				merged = Util.boolQuery(checkedQueries,new HashSet<String>(), context, false);
			}
		} else {
			merged = Interleaving.balancedInterleaving1(ret);
		}
		if(merged.size() == 0) {
			System.out.println("Empty");
			return docs;
		}
		ScoredDocuments res = new ScoredDocuments();
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
		/*output+="============"+" NounPhrases: "+"============\n";
		for(String best: bestPhrase) {
			output+=best+"\n";
		}
		output+="============"+" Begin Positions: "+"============\n";
		Map<String, Integer> pos = Util.positionOfTargetdocuments(collect, res);
		for(Map.Entry<String, Integer> entry : pos.entrySet()) {
			output+=entry.getKey()+"," +entry.getValue() +"\n";
		}
		output+="============"+" End Positions: "+"============\n";
		File file = new File("debug_nounphrases.txt");
		try {
			file.createNewFile();
			FileWriter myWriter = new FileWriter(file, false);
			myWriter.write(output);
		    myWriter.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println(output);*/
		return res;
	}
	
	private Set<String> generateKeyqueries(Map<String, Integer> relevanceFeedback2) {
		Set<String> ret = new HashSet<>();
		for(KeyQueryCandidateGenerator<String> generator: candidateGenerator) {
			ret.addAll(generator.generateCandidates(relevanceFeedback2.keySet()));
		}
		return ret;
	}

	@Override
	public String tag() {
		return "noun-phrases";
	}
	
}


