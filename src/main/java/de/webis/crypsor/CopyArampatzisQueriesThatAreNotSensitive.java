package de.webis.crypsor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.webis.keyqueries.generators.chatnoir.SensitiveTerms;
import lombok.SneakyThrows;

public class CopyArampatzisQueriesThatAreNotSensitive {
	public static void main(String[] args) {
		for (String retrievalModel: CrypsorArgs.RETRIEVAL_METHODS) {
			for(int topic: CrypsorArgs.TOPICS) {
				System.out.println("Process " + retrievalModel + " on " + topic);
				process(retrievalModel, topic);
			}
		}
	}
	
	@SneakyThrows
	private static void process(String retrievalModel, int topic) {
		Path p = Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/scrambling-on-anserini").resolve("arampatzis-" + retrievalModel).resolve(topic + ".jsonl");
		
		List<String> result = retainNonSensitiveQueries(Files.readAllLines(p));
		
		System.out.println("Write " + result.size() + " queries to " + p);
		Files.write(p, result.stream().collect(Collectors.joining("\n")).getBytes());
	}
	
	public static List<String> retainNonSensitiveQueries(List<String> queries) {
		SensitiveTerms sensitiveTerms = sensitiveTerms(queries);
		int numQueries = EvaluateQuerySavingsByHbc.parseQueriesAndSortByLengthOfScrambledQuery(queries).size();
		System.out.println("Evaluate Queries: " + numQueries);
		
		List<String> ret = new ArrayList<>();
		for(String q: queries) {
			String tmp = EvaluateQuerySavingsByHbc.privateQueryToScrambledQuery(q).getValue();
			
			if(!sensitiveTerms.phraseIsDeniedByUser(tmp)) {
				ret.add(q);
			}
		}
		
		return ret;
	}

	private static SensitiveTerms sensitiveTerms(List<String> queries) {
		String privateQuery = EvaluateQuerySavingsByHbc.privateQueryToScrambledQuery(queries.get(0)).getKey();
		
		return SensitiveTerms.getSensitiveTermsWithSynonymsHyponymsAndHypernyms(privateQuery);
	}
}
