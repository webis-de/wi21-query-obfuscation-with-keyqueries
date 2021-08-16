package de.webis.keyqueries.anserini;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;

import de.webis.keyqueries.util.Util;
import io.anserini.index.IndexArgs;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.search.SearchArgs;
import io.anserini.util.Qrels;

public class HybridRelevanceOrder<T> extends RelevanceFeedbackReranker<T> {

	public HybridRelevanceOrder(Reranker<T> internalReranker, Qrels qrels, int relevantDocuments) {
		super(internalReranker, qrels, relevantDocuments);
	}

	@Override
	public String tag() {
		return "HybridRelevanceOrder"+ ";rel=" + relevantDocuments +"(" +internalReranker.tag() +")";
	}

	@Override
	protected Map<String, Integer> getRelevanceFeedback(ScoredDocuments docs, T qid, Qrels qrels,
			int relevantDocuments, RerankerContext<T> context) {
		
		int relfound =0;
		Map<String, Integer> ret = new LinkedHashMap<>();
		Map<String,Integer> original = qrels.getDocMap(String.valueOf(qid));
		if(original == null) {
			return ret;
		}
		for(Map.Entry<String,Integer> entry: original.entrySet()) {
			ret.put(entry.getKey(), entry.getValue());
			relfound++;
			if(relfound >= relevantDocuments) {
				break;
			}
		}
		SearchArgs args = context.getSearchArgs();
		final Analyzer analyzer = Util.analyzer(args);
		List<ScoredDocuments> sdocs = new ArrayList<ScoredDocuments>();
		if(args.hybrid_rm3) {
			for (String fbTerms : args.rm3_fbTerms) {
		        for (String fbDocs : args.rm3_fbDocs) {
		          for (String originalQueryWeight : args.rm3_originalQueryWeight) {
		    			  RM3RelevanceFeedbackReranker<T> internalReranker = new RM3RelevanceFeedbackReranker<>(analyzer, IndexArgs.CONTENTS, Integer.valueOf(fbTerms),
		  		                Integer.valueOf(fbDocs), Float.valueOf(originalQueryWeight), args.rm3_outputQuery);
		    			  internalReranker.setRelevanceFeedback(ret, context);
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
			    			  PrfRelevanceFeedbackReranker<T> internalReranker = new PrfRelevanceFeedbackReranker<>(analyzer, IndexArgs.CONTENTS, Integer.valueOf(fbTerms),
			    	                    Integer.valueOf(fbDocs), Float.valueOf(k1), Float.valueOf(b), Float.valueOf(newTermWeight),
			    	                    args.bm25prf_outputQuery);
			    			  internalReranker.setRelevanceFeedback(ret, context);
			    			  sdocs.add(internalReranker.rerank(docs, context));
		              }
		            }
		          }
		        }
			}
		}
		if(args.hybrid_axiom) {
		  AxiomRelevanceFeedbackReranker<T> internalReranker = new AxiomRelevanceFeedbackReranker<>(args);
		  internalReranker.setRelevanceFeedback(ret, context);
		  sdocs.add(internalReranker.rerank(docs, context));
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
