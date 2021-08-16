package de.webis.keyqueries.anserini;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.combination.Interleaving;
import de.webis.keyqueries.generators.DocumentTfIdfKeyQueryCandidateGenerator.TermWithScore;
import de.webis.keyqueries.generators.KeyQueryCandidateGenerator;
import de.webis.keyqueries.generators.lucene.LuceneDocumentTfIdfKeyQueryCandidateGenerator;
import de.webis.keyqueries.selection.CrypsorQuerySelectionStrategy;
import de.webis.keyqueries.selection.NdcgSelectionStrategy;
import de.webis.keyqueries.util.Util;
import io.anserini.index.IndexArgs;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.search.SearchArgs;
import io.anserini.search.SimpleSearcher;
import io.anserini.search.query.BagOfWordsQueryGenerator;
import io.anserini.search.similarity.DocumentSimilarityScore;

public class KeyqueryRelevanceReranker<T> implements Reranker<T>, RelevanceFeedbackAware<T> {
	private Map<String, Integer> relevanceFeedback;
	private List<KeyQueryCandidateGenerator<String>> candidateGenerator;
	@Override
	public void setRelevanceFeedback(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		this.relevanceFeedback = relevanceFeedback;
	}
	@Override
	public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
		String output = "============"+" Query: " +context.getQueryText() +" ============\n";
		System.out.print(output);
		System.out.println("============"+" Keyqueries: "+"============");
		output+= "============"+" Keyqueries: "+"============\n";
		List<String> topQueries = null;
		Map<String, Integer> collect = relevanceFeedback.entrySet().stream()
				.filter(x -> x.getValue() > 0)
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
		candidateGenerator = KeyQueryCandidateGenerator.anseriniKeyQueryCandidateGenerator(context);
		Set<String> keyqueries = generateKeyqueries(collect);
		// checken ob tatsächlich keyquery -> KeyQueryChecker
		LuceneSearcher searcher = new LuceneSearcher(context.getIndexSearcher(), Util.analyzer(context.getSearchArgs()));
		KeyQueryChecker checker = new KeyQueryChecker(collect.keySet(), searcher, context.getSearchArgs().keyquery_k, context.getSearchArgs().keyquery_l);
		// auswählen welche Keyqueries behalten werden
		// Ergebnisse mergen
		BagOfWordsQueryGenerator querygenerator = new BagOfWordsQueryGenerator();
		List<List<ScoreDoc>> ret = new ArrayList<List<ScoreDoc>>();
		Set<String> checkedQueries = new HashSet<String>();
		if(context.getSearchArgs().selection_ndcg) {
			for(String query: keyqueries) {
				context.getSearchArgs().keyquery_count++;
				if(checker.isKeyQuery(query)) {
				}
			}
			//System.out.println(context.getSearchArgs().keyquery_count);
			CrypsorQuerySelectionStrategy selection = new CrypsorQuerySelectionStrategy(new NdcgSelectionStrategy());
			topQueries = selection.selectTop(checker, context.getSearchArgs().depth, false);
			// prepare training data
			if(!context.getSearchArgs().train.equals("")) {
				String traindata = "";
				DocumentSimilarityScore sim = new DocumentSimilarityScore(context.getIndexSearcher().getIndexReader());
				for(String doc: relevanceFeedback.keySet()) {
					List<Float> scores = sim.bm25Similarity(context.getSearchArgs().depth ,topQueries, doc);
					String tmp = "";
					for(int i=0; i<scores.size(); i++) {
						tmp+= (i+1) +":" +scores.get(i) +" ";
					}
					traindata += relevanceFeedback.get(doc) +" qid:" +context.getQueryId() +" " +tmp +"\n";
				}
				File file = new File(context.getSearchArgs().train);
				try {
					file.createNewFile();
					FileWriter myWriter = new FileWriter(file, true);
					myWriter.write(traindata);
				    myWriter.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			for(String query: topQueries) {
				System.out.println(query);
				output += query +"\n";
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
				output += query +"\n";
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
		if(context.getSearchArgs().combineQuery) {
			checkedQueries = Util.extractQueryFromKeyqueries(checkedQueries, context.getSearchArgs().combineQuery_limit);
			System.out.println("============" +" Combined Query: " +"============");
			System.out.println(String.join(" ", checkedQueries));
		}
		List<ScoreDoc> merged = null;
		if(context.getSearchArgs().teamdraft) {
			merged = Interleaving.teamDraftInterleaving1(ret);
		} else if(context.getSearchArgs().boolquery){
			merged = Util.boolQuery(checkedQueries, context, false);
		} else {
			merged = Interleaving.balancedInterleaving1(ret);
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
		if(merged.size() == 0) res = docs;
		if(context.getSearchArgs().toTop) {
			RelevantToTopReranker<T> internalReranker = new RelevantToTopReranker<>();
			internalReranker.setRelevanceFeedback(collect, context);
			ScoredDocuments rankedToTop = internalReranker.rerank(docs, context);
			ArrayList<Document> documents = new ArrayList<Document>();
			ArrayList<Float> scores = new ArrayList<Float>();
			ArrayList<String> ids = new ArrayList<String>();
			ArrayList<Integer> id = new ArrayList<Integer>();
			for(int i=0; i<Math.min(collect.keySet().size(),rankedToTop.documents.length); i++) {
				documents.add(rankedToTop.documents[i]);
				scores.add(rankedToTop.scores[i]+10000);
				ids.add(rankedToTop.documents[i].get("id"));
				id.add(rankedToTop.ids[i]);
			}
			for(int i=0; i<res.documents.length; i++) {
				if(ids.contains(res.documents[i].get("id"))) {
					continue;
				}
				ids.add(res.documents[i].get("id"));
				documents.add(res.documents[i]);
				scores.add(res.scores[i]);
				id.add(res.ids[i]);
			}
			ScoredDocuments tmp = new ScoredDocuments();
			tmp.ids = new int[id.size()];
			int j = 0;
			for(Integer in: id) {
				tmp.ids[j++] = in;
			}
			tmp.documents = documents.toArray(new Document[documents.size()]);
			tmp.scores = new float[scores.size()];
			int i = 0;
			for (Float f : scores) {
			  tmp.scores[i++] = f;
			}
			res = tmp;
		} else if(context.getSearchArgs().toTop1) {
			Set<String> top = new HashSet<String>();
			top.addAll(collect.keySet());
			List<Document> documents = new ArrayList<Document>();
			List<Float> scores = new ArrayList<Float>();
			if(top.size() == 0) {
				res = docs;
			} else {
				try {
					SimpleSearcher simplesearch = new SimpleSearcher("/home/johannes/Bachelorarbeit/thesis-huck/web-search-anserini-sandbox/indexes/robust04.pos+docvectors+rawdocs");
					for(String id: top) {
						documents.add(simplesearch.document(id));
						scores.add(10000.0f);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if(merged.size() == 0) res = docs;
				for(int i=0; i<res.documents.length; i++) {
					if(top.contains(res.documents[i].get("id")) || res.documents[i].get("id") == null) {
						continue;
					}
					top.add(res.documents[i].get("id"));
					documents.add(res.documents[i]);
					scores.add(res.scores[i]);
				}
				ScoredDocuments tmp = new ScoredDocuments();
				tmp.ids = new int[scores.size()];
				tmp.documents = documents.toArray(new Document[documents.size()]);
				tmp.scores = new float[scores.size()];
				int i = 0;
				for (Float f : scores) {
				  tmp.scores[i++] = f;
				}
				res = tmp;
			}
		}
		if(!context.getSearchArgs().test.equals("")) {
			DocumentSimilarityScore sim = new DocumentSimilarityScore(context.getIndexSearcher().getIndexReader());
			String testdata = "";
			for(int i=0; i<res.documents.length; i++) {
				List<Float> scores = sim.bm25Similarity(context.getSearchArgs().depth, topQueries, res.documents[i].get("id"));
				String tmp = "";
				for(int j=0; j<scores.size(); j++) {
					tmp+= (j+1) +":" +scores.get(j) +" ";
				}
				testdata += "0 qid:" +context.getQueryId() + " " +tmp +"#" +res.documents[i].get("id") +"\n";
			}
			File file = new File(context.getSearchArgs().test);
			try {
				file.createNewFile();
				FileWriter myWriter = new FileWriter(file, true);
				myWriter.write(testdata);
			    myWriter.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
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
		if(context.getSearchArgs().debug) {
			Map<String, Float> termsWeights = getTermsAndWeights(collect, context);
			Set<String> terms = termsWeights.keySet();
			output+="============"+" Terms: "+"============\n";
			for(String term: terms) {
				output+=term+"\n";
			}
			output+="============"+" RM3-Terms: "+"============\n";
			SearchArgs args = context.getSearchArgs();
			final Analyzer analyzer = Util.analyzer(context.getSearchArgs());
			for (String fbTerms : args.rm3_fbTerms) {
		        for (String fbDocs : args.rm3_fbDocs) {
		          for (String originalQueryWeight : args.rm3_originalQueryWeight) {
		    			  RM3RelevanceFeedbackReranker<T> internalReranker = new RM3RelevanceFeedbackReranker<>(analyzer, IndexArgs.CONTENTS, Integer.valueOf(fbTerms),
		  		                Integer.valueOf(fbDocs), Float.valueOf(originalQueryWeight), args.rm3_outputQuery);
		    			  internalReranker.setRelevanceFeedback(collect, context);
		    			  output+=internalReranker.reformulatedQuery(docs, context)+"\n";
		          }
		        }
			}
			output+="============"+" Begin Positions: "+"============\n";
			Map<String, Integer> pos = Util.positionOfTargetdocuments(collect, res);
			for(Map.Entry<String, Integer> entry : pos.entrySet()) {
				output+=entry.getKey()+"," +entry.getValue() +"\n";
			}
			output+="============"+" End Positions: "+"============\n";
			File file = new File("debug.txt");
			try {
				file.createNewFile();
				FileWriter myWriter = new FileWriter(file, false);
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
	
	public Map<String, Float> getTermsAndWeights(Map<String, Integer> targetDocuments, RerankerContext<T> context) {
		Map<String, Float> terms = new HashMap<String, Float>();
		candidateGenerator = KeyQueryCandidateGenerator.anseriniKeyQueryCandidateGenerator(context);
		List<TermWithScore> kqterms = new ArrayList<TermWithScore>();
		for(KeyQueryCandidateGenerator<String> generator: candidateGenerator) {
			if(generator instanceof LuceneDocumentTfIdfKeyQueryCandidateGenerator) {
				int limit = context.getSearchArgs().debug_extraction;
				for(String doc: targetDocuments.keySet()) {
					int length = ((LuceneDocumentTfIdfKeyQueryCandidateGenerator) generator).termsSortedByScore(doc).size();
					List<TermWithScore> tmp = (((LuceneDocumentTfIdfKeyQueryCandidateGenerator) generator).termsSortedByScore(doc).subList(0, Math.min(limit, length)));
					for(TermWithScore entry: tmp) {
						if(!kqterms.contains(entry)) {
							kqterms.add(entry);
							terms.put(entry.getTerm(), entry.getScore());
						}
					}
				}
			}
		}
		double L1Norm = computeL1Norm(terms);
		for(String term: terms.keySet()) {
			terms.put(term, (float) (terms.get(term) / L1Norm));
		}
		return terms;
	}
	
	public double computeL1Norm(Map<String, Float> terms) {
		double norm = 0.0;
		for(String term: terms.keySet()) {
			norm+= Math.abs(terms.get(term));
		}
		return norm;
	}
	
	@Override
	public String tag() {
		return "keyquery-relevance-feedback";
	}
	

}
