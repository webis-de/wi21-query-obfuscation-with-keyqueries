package de.webis.keyqueries.generators.lucene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.google.common.collect.Sets;

import de.webis.keyqueries.anserini.RM3KeyqueryReranker;
import de.webis.keyqueries.generators.KeyQueryCandidateGenerator;
import de.webis.keyqueries.util.Util;
import io.anserini.index.IndexArgs;
import io.anserini.rerank.RerankerContext;

@SuppressWarnings("serial")
public class RelevanceFeedbackQueryCandidateGenerator<T> implements KeyQueryCandidateGenerator<Query> {

	private final RM3KeyqueryReranker<T> reranker;

	private final RerankerContext<T> context;

	public RelevanceFeedbackQueryCandidateGenerator(RM3KeyqueryReranker<T> reranker,
			RerankerContext<T> rerankerContext) {
		if (reranker.getRelevanceFeedback() == null) {
			throw new RuntimeException("I need relevance feedback.");
		}

		this.reranker = reranker;
		this.context = rerankerContext;
	}

	@Override
	public List<Query> generateCandidates(Set<String> targetDocuments) {
		if (targetDocuments != null) {
			throw new RuntimeException(
					"This query generator uses target documents that are passed to the constructor, hence, the we only accept null as input here");
		}
		Set<Set<String>> queryCandidates = qeueryTermCandidatesForExpansion();
		List<Query> ret = queryCandidates.stream()
				.map(i -> weightedQueryOf(i))
				.collect(Collectors.toList());

		return distinct(ret);
	}
	
	private List<Query> distinct(List<Query> queries) {
		Set<String> existingQueries = new HashSet<>();
		List<Query> ret = new ArrayList<>();
		
		for(Query query: queries) {
			String queryStr = query.toString();
			if(!existingQueries.contains(queryStr)) {
				existingQueries.add(queryStr);
				ret.add(query);
			}
		}
		
		return ret;
	}

	private Query weightedQueryOf(Set<String> queryTerms) {
		if(queryTerms == null || queryTerms.isEmpty()) {
			queryTerms = new HashSet<>(Arrays.asList("iNeedASingleNonExistingTermLikeThisOneToBuildDisableExpansion"));
		}
		
		Map<String, Float> termsAndWeights = reranker.getTermsAndWeights(context, queryTerms);
		BooleanQuery.Builder ret = new BooleanQuery.Builder();

		for (Map.Entry<String, Float> termToWeight : termsAndWeights.entrySet()) {
			ret.add(new BoostQuery(new TermQuery(new Term(IndexArgs.CONTENTS, termToWeight.getKey())), termToWeight.getValue()), BooleanClause.Occur.SHOULD);
		}

		return ret.build();
	}

	private Set<Set<String>> qeueryTermCandidatesForExpansion() {
		Set<String> cancidateTerms = cancidateTerms(); 
		return Sets.powerSet(cancidateTerms).stream().filter(i -> i.size() == reranker.query_length)
				.collect(Collectors.toSet());
	}

	private Set<String> cancidateTerms() {
		return Util.topTerms(reranker.getTermsAndWeights(context), reranker.candidate_terms);
	}
}
