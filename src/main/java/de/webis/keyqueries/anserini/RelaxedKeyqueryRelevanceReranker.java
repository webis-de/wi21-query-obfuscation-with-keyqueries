package de.webis.keyqueries.anserini;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.combination.Interleaving;
import de.webis.keyqueries.generators.KeyQueryCandidateGenerator;
import de.webis.keyqueries.selection.CrypsorQuerySelectionStrategy;
import de.webis.keyqueries.selection.NdcgSelectionStrategy;
import de.webis.keyqueries.util.Util;
import io.anserini.index.IndexArgs;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.search.query.BagOfWordsQueryGenerator;

public class RelaxedKeyqueryRelevanceReranker<T> implements Reranker<T>, RelevanceFeedbackAware<T> {
	private Map<String, Integer> relevanceFeedback;
	private List<KeyQueryCandidateGenerator<String>> candidateGenerator;
	@Override
	public void setRelevanceFeedback(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		this.relevanceFeedback = relevanceFeedback;
	}

	@Override
	public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
		String output = "Query: " +context.getQueryText() +"\n";
		System.out.print(output);
		Map<String, Integer> collect = relevanceFeedback.entrySet().stream()
				.filter(x -> x.getValue() > 0)
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
		relevanceFeedback = collect;
		candidateGenerator = KeyQueryCandidateGenerator.anseriniKeyQueryCandidateGenerator(context);
		Set<String> keyqueries = generateKeyqueries(relevanceFeedback);
		// checken ob tatsächlich keyquery -> KeyQueryChecker
		LuceneSearcher searcher = new LuceneSearcher(context.getIndexSearcher(), Util.analyzer(context.getSearchArgs()));
		KeyQueryChecker checker = new KeyQueryChecker(relevanceFeedback.keySet(), searcher, context.getSearchArgs().keyquery_k, 10);
		// auswählen welche Keyqueries behalten werden
		// Ergebnisse mergen
		BagOfWordsQueryGenerator querygenerator = new BagOfWordsQueryGenerator();
		List<List<ScoreDoc>> ret = new ArrayList<List<ScoreDoc>>();
		boolean empty = true;
		int count = 0;
		if(context.getSearchArgs().selection_ndcg) {
			for(String query: keyqueries) {
				if(checker.isKeyQuery(query)) {
				}
			}
			CrypsorQuerySelectionStrategy selection = new CrypsorQuerySelectionStrategy(new NdcgSelectionStrategy());
			List<String> topQueries = selection.selectTop(checker, context.getSearchArgs().depth, true);
			for(String query: topQueries) {
				System.out.println(query);
				output += query +"\n";
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
			}
		} else {
			while(empty) {
				for(String query: keyqueries) {
					if(!checker.isRelaxedKeyQuery(query, relevanceFeedback.keySet().size()-count)) {
						continue;
					}
					System.out.println(query);
					output += query +"\n";
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
				}
				if(ret.size() != 0) {
					empty = false;
				}
				count++;
				if(count == relevanceFeedback.keySet().size()) {
					break;
				}
			}
		}
		List<ScoreDoc> merged = Interleaving.balancedInterleaving1(ret);
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
		if(context.getSearchArgs().toTop) {
			RelevantToTopReranker<T> internalReranker = new RelevantToTopReranker<>();
			internalReranker.setRelevanceFeedback(relevanceFeedback, context);
			ScoredDocuments rankedToTop = internalReranker.rerank(docs, context);
			ArrayList<Document> documents = new ArrayList<Document>();
			ArrayList<Float> scores = new ArrayList<Float>();
			ArrayList<String> ids = new ArrayList<String>();
			for(int i=0; i<relevanceFeedback.keySet().size(); i++) {
				documents.add(rankedToTop.documents[i]);
				scores.add(rankedToTop.scores[i]+100);
				ids.add(rankedToTop.documents[i].get("id"));
			}
			if(merged.size() == 0) res = docs;
			for(int i=0; i<res.documents.length; i++) {
				if(ids.contains(res.documents[i].get("id"))) {
					continue;
				}
				ids.add(res.documents[i].get("id"));
				documents.add(res.documents[i]);
				scores.add(res.scores[i]);
			}
			ScoredDocuments tmp = new ScoredDocuments();
			tmp.ids = null;
			tmp.documents = documents.toArray(new Document[documents.size()]);
			tmp.scores = new float[scores.size()];
			int i = 0;
			for (Float f : scores) {
			  tmp.scores[i++] = f;
			}
			res = tmp;
		}
		if(!context.getSearchArgs().keyquery_out.equals("")) {
			File file = new File(context.getSearchArgs().keyquery_out);
			try {
				file.createNewFile();
				FileWriter myWriter = new FileWriter(file, true);
				myWriter.write(output);
			    myWriter.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
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
		return "relaxed-keyquery-relevance-feedback";
	}

}
