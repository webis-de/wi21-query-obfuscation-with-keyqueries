package de.webis.keyqueries.anserini;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;

import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.util.Util;
import io.anserini.rerank.Reranker;
import io.anserini.rerank.RerankerContext;
import io.anserini.rerank.ScoredDocuments;

public class ManualKeyquery<T> implements Reranker<T>, RelevanceFeedbackAware<T> {
	private Map<String, Integer> relevanceFeedback;
	@Override
	public void setRelevanceFeedback(Map<String, Integer> relevanceFeedback, RerankerContext<T> context) {
		this.relevanceFeedback = relevanceFeedback;
	}
	@Override
	public ScoredDocuments rerank(ScoredDocuments docs, RerankerContext<T> context) {
		String output = "";
		Map<String, Integer> collect = relevanceFeedback.entrySet().stream()
				.filter(x -> x.getValue() > 0)
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
		LuceneSearcher searcher = new LuceneSearcher(context.getIndexSearcher(), Util.analyzer(context.getSearchArgs()));
		KeyQueryChecker checker = new KeyQueryChecker(collect.keySet(), searcher, context.getSearchArgs().keyquery_k, context.getSearchArgs().keyquery_l);
		output+="============"+" Is Keyquery?: "+"============\n";
		Set<String> newQuery = Util.parseNewQuery(context.getSearchArgs().new_query);
		for(String query: newQuery) {
			if(checker.isKeyQuery(query)) {
				output+=query +": is a keyquery\n";
			} else {
				output+=query +": is not a keyquery\n";
			}
			output+=query +": " +checker.issueQuery(query).size() +" hits\n";
		}
		List<ScoreDoc> merged = Util.boolQuery(newQuery, context, true);
		if(merged.size() == 0) {
			System.out.println("No hits found.");
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
		output+="============"+" Begin Positions: "+"============\n";
		Map<String, Integer> pos = Util.positionOfTargetdocuments(collect, res);
		for(Map.Entry<String, Integer> entry : pos.entrySet()) {
			output+=entry.getKey()+"," +entry.getValue() +"\n";
		}
		output+="============"+" End Positions: "+"============\n";
		File file = new File("debug_manual.txt");
		try {
			file.createNewFile();
			FileWriter myWriter = new FileWriter(file, false);
			myWriter.write(output);
		    myWriter.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return res;
	}

	@Override
	public String tag() {
		return "manual-keyquery";
	}
	

}

