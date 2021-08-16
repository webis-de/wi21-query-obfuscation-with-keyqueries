package de.webis.keyqueries.anserini;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.webis.keyqueries.Candidate;
import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.generators.KeyQueryCandidateGenerator;
import de.webis.keyqueries.util.Util;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;

public class Oracle <T> implements Reranker<T>, RelevanceFeedbackAware<T> {
	private Map<String, Integer> relevanceFeedback;
	private List<KeyQueryCandidateGenerator<String>> candidateGenerator;
	
	@Override
	public void setRelevanceFeedback(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		this.relevanceFeedback = relevanceFeedback;	
	}

	@Override
	public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
		System.out.println("Query : " +context.getQueryText());
		if(relevanceFeedback == null) {
			return docs;
		}
		candidateGenerator = KeyQueryCandidateGenerator.anseriniKeyQueryCandidateGenerator(context);
		Set<String> keyqueries = generateKeyqueries(relevanceFeedback);
		LuceneSearcher searcher = new LuceneSearcher(context.getIndexSearcher(), Util.analyzer(context.getSearchArgs()));
		Map<Candidate, Integer> occurence = new HashMap<Candidate, Integer>();
		KeyQueryChecker kq = new KeyQueryChecker(relevanceFeedback.keySet(), searcher, context.getSearchArgs().keyquery_k, 10);
		for(String query: keyqueries) {
			int counter = 0;
			List<String> res = searcher.search(query, context.getSearchArgs().keyquery_k);
			for(String hit: res) {
				if(relevanceFeedback.keySet().contains(hit)) {
					counter++;
				}
			}
			Candidate cand = new Candidate(query, kq);
			occurence.put(cand, counter);
		}
		if(context.getSearchArgs().keyquery_out.equals("")) {
			occurence.entrySet()
			  .stream()
			  .sorted((Map.Entry.comparingByValue()))
			  .forEach(System.out::println);
		} else {
			List<String> list = occurence.entrySet()
			  .stream()
			  .sorted((Map.Entry.comparingByValue()))
			  .map(i -> i.toString())
			  .collect(Collectors.toList());
			String out = "Query : " +context.getQueryText() +"\n";
			for(String s: list) {
				out += s+"\n";
			}
			File file = new File(context.getSearchArgs().keyquery_out);
			try {
				file.createNewFile();
				FileWriter myWriter = new FileWriter(file, true);
				myWriter.write(out);
			    myWriter.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		return docs;
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
		return "oracle";
	}

}
