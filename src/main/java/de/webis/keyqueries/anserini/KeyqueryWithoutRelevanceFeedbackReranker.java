package de.webis.keyqueries.anserini;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;

import de.webis.keyqueries.util.Util;
import io.anserini.index.IndexArgs;
import io.anserini.index.IndexCollection;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.rerank.lib.AxiomReranker;
import io.anserini.rerank.lib.BM25PrfReranker;
import io.anserini.rerank.lib.Rm3Reranker;
import io.anserini.search.SearchArgs;
import io.anserini.util.Qrels;

public class KeyqueryWithoutRelevanceFeedbackReranker<T> extends RelevanceFeedbackReranker<T> {

	public KeyqueryWithoutRelevanceFeedbackReranker(Reranker<T> internalReranker, Qrels qrels, int relevantDocuments) {
		super(internalReranker, qrels, relevantDocuments);
	}

	@Override
	public String tag() {
		return "KeyqueryHybridReranker";
	}

	@Override
	protected Map<String, Integer> getRelevanceFeedback(ScoredDocuments docs, T qid, Qrels qrels,
			int relevantDocuments, RerankerContext<T> context) {
		
		Map<String, Integer> ret = new LinkedHashMap<>();
		SearchArgs args = context.getSearchArgs();
		final Analyzer analyzer = Util.analyzer(args);
		List<ScoredDocuments> sdocs = new ArrayList<ScoredDocuments>();
		if(args.hybrid_rm3) {
			for (String fbTerms : args.rm3_fbTerms) {
		        for (String fbDocs : args.rm3_fbDocs) {
		          for (String originalQueryWeight : args.rm3_originalQueryWeight) {
		    			  Rm3Reranker internalReranker = new Rm3Reranker(analyzer, IndexArgs.CONTENTS, Integer.valueOf(fbTerms),
		  		                Integer.valueOf(fbDocs), Float.valueOf(originalQueryWeight), args.rm3_outputQuery);
		    			  sdocs.add(internalReranker.rerank(docs, context));
		          }
		        }
			}
		}
		if(args.hybrid_prf) {
			for (String fbTerms : args.bm25prf_fbTerms) {
		        for (String fbDocs : args.bm25prf_fbDocs) {
		          for (String k1 : args.bm25prf_k1) {
		            for (String b : args.bm25prf_b) {
		              for (String newTermWeight : args.bm25prf_newTermWeight) {
			    			  BM25PrfReranker internalReranker = new BM25PrfReranker(analyzer, IndexArgs.CONTENTS, Integer.valueOf(fbTerms),
			    	                    Integer.valueOf(fbDocs), Float.valueOf(k1), Float.valueOf(b), Float.valueOf(newTermWeight),
			    	                    args.bm25prf_outputQuery);
			    			  sdocs.add(internalReranker.rerank(docs, context));
		              }
		            }
		          }
		        }
			}
		}
		if(args.hybrid_axiom) {
			for (String r : args.axiom_r) {
		        for (String n : args.axiom_n) {
		          for (String beta : args.axiom_beta) {
		            for (String top : args.axiom_top) {
		              for (String seed : args.axiom_seed) {
			    			  AxiomReranker<T> internalReranker;
							try {
								internalReranker = new AxiomReranker<>(args.index, args.axiom_index, IndexArgs.CONTENTS,
								            args.axiom_deterministic, Integer.valueOf(seed), Integer.valueOf(r),
								            Integer.valueOf(n), Float.valueOf(beta), Integer.valueOf(top),
								            args.axiom_docids, args.axiom_outputQuery, args.searchtweets);
				    			sdocs.add(internalReranker.rerank(docs, context));
							} catch (NumberFormatException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
		              }
		            }
		          }
		        }
			}
		}
		int found = 0;
		if(sdocs != null) {
			if(args.hybrid_selection_qrel) {
				for(ScoredDocuments scored: sdocs) {
					found=0;
					if(scored == null) continue;
					for(int i=0; i<scored.documents.length; i++) {
						String id = scored.documents[i].get(IndexArgs.ID);
						Integer currentRelevanceLabel = qrels.getRelevanceGrade(String.valueOf(qid), id);
						if(currentRelevanceLabel > 0 && !ret.keySet().contains(id)) {
							ret.put(id, currentRelevanceLabel);
							found++;
						}
						if(found >= args.hybrid_amount) break;
					}
				}
			} else {
				for(ScoredDocuments scored: sdocs) {
					found=0;
					if(scored == null) continue;
					for(int i=0; i<scored.documents.length; i++) {
						String id = scored.documents[i].get(IndexArgs.ID);
						if(!ret.keySet().contains(id)) {
							ret.put(id, 1);
							found++;
						}
						if(found >= args.hybrid_amount) break;
					}
				}
			}
		}
		return ret;
	}

}
