package de.webis.keyqueries.anserini;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

import de.webis.keyqueries.LuceneKeyQueryChecker;
import de.webis.keyqueries.generators.KeyQueryCandidateGenerator;
import de.webis.keyqueries.generators.lucene.RelevanceFeedbackQueryCandidateGenerator;
import de.webis.keyqueries.util.Util;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;
import io.anserini.search.SearchArgs;

public class RM3KeyqueryReranker<T> implements Reranker<T>, RelevanceFeedbackAware<T> {
	private ScoredDocuments relevanceFeedback;
	public final int query_length;
	public final int candidate_terms;
	public final int query_amount;
	public final double originalQueryWeight;
	
	public RM3KeyqueryReranker(int query_length, int candidate_terms, int query_amount, double originalQueryWeight) {
		this.query_length = query_length;
		this.candidate_terms = candidate_terms;
		this.query_amount = query_amount;
		this.originalQueryWeight = originalQueryWeight;
	}
	
	@Override
	public void setRelevanceFeedback(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		this.relevanceFeedback = Util.relevanceFeedbackAsScoredDocs(relevanceFeedback, context);
	}

	@Override
	public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
		KeyQueryCandidateGenerator<Query> queryCandidateGenerator = relevanceFeedbackQueryCandidateGenerator(context);
		LuceneSearcherRaw searcher = new LuceneSearcherRaw(context.getIndexSearcher());
		List<Query> candidates = queryCandidateGenerator.generateCandidates(null);
		LuceneKeyQueryChecker checker = new LuceneKeyQueryChecker(Util.asRelevantDocs(getRelevanceFeedback()).keySet(), searcher, context.getSearchArgs().keyquery_k, context.getSearchArgs().keyquery_l);
		for(Query query: candidates) {
			checker.issueQuery(query); 
		}
		
		Set<Query> keyQueries = Util.selectKeyqueries(checker, this.query_amount);
		
		return submitQueries(keyQueries, context); 
	}

	private ScoredDocuments submitQueries(Set<Query> keyQueries, RerankerContext<T> context) {
		if(keyQueries == null || keyQueries.size()< 1) {
			throw new RuntimeException("Illegal input.");
		}
		
		if(keyQueries.size() == 1) {
			return submitQuery(keyQueries.iterator().next(), context);
		}

		BooleanQuery.Builder ret = new BooleanQuery.Builder();

		for (Query query : keyQueries) {
			ret.add(query, BooleanClause.Occur.SHOULD);
		}
		
		return submitQuery(ret.build(), context);
	}
	
	private ScoredDocuments submitQuery(Query query, RerankerContext<T> context) {
		try {
			TopDocs topDocs = context.getIndexSearcher().search(query, context.getSearchArgs().rerankcutoff);
			if(context.getSearchArgs().rm3_outputQuery || context.getSearchArgs().bm25prf_outputQuery || context.getSearchArgs().axiom_outputQuery) {
				System.out.println("Original Query: '" + context.getQueryText() +"'; Used Query: " + query);
			}
			
			return ScoredDocuments.fromTopDocs(topDocs, context.getIndexSearcher());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String tag() {
		return "Rm3KeyqueryReranker(query_length="+query_length+",candidate_terms="+candidate_terms+",query_amount="+query_amount+",originalQueryWeight="+originalQueryWeight+")";
	}
	
	public Map<String, Float> getTermsAndWeights(RerankerContext<T> context) {
		return getTermsAndWeights(context, null);
	}
	
	public Map<String, Float> getTermsAndWeights(RerankerContext<T> context, Set<String> terms) {
		SearchArgs args = context.getSearchArgs();
		if(args.rm3_term) {
			return Util.getRM3TermsAndWeights(getRelevanceFeedback(), context, terms);
		} else if(args.prf_term) {
			return Util.getPRFTermsAndWeights(getRelevanceFeedback(), context, terms);
		} else if(args.axiom_term) {
			if(terms != null && !terms.isEmpty()) {
				throw new RuntimeException("Implement this");
			}
			
			return Util.getAxiomTermsAndWeights(getRelevanceFeedback(), context);
		}
		return new HashMap<String, Float>();
	}

	public ScoredDocuments getRelevanceFeedback() {
		return relevanceFeedback;
	}

	public KeyQueryCandidateGenerator<Query> relevanceFeedbackQueryCandidateGenerator(RerankerContext<T> context) {
		return new RelevanceFeedbackQueryCandidateGenerator<>(this, context);
	}
}
