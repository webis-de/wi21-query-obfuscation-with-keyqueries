package de.webis.crypsor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.anserini.analysis.AnalyzerUtils;
import lombok.SneakyThrows;

public class AnalyzeRunFile {
	@SneakyThrows
	public static void main(String[] args) {
		List<String> ret = new ArrayList<>();

		for(String scramblingApproach: CrypsorArgs.APPROACHES) {
			for(String retrievalModel: CrypsorArgs.RETRIEVAL_METHODS) {
				System.out.println("Eval: " + scramblingApproach + " on " + retrievalModel);
				for(int topic: CrypsorArgs.TOPICS) {
					List<String> tmp = Files.readAllLines(p(scramblingApproach, retrievalModel, topic));
					ret.add(queryStatistics(tmp,topic, scramblingApproach, retrievalModel));
				}
			}
		}
		
		Files.write(Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/scrambling-on-anserini/per-query-statistics.jsonl"),
			ret.stream().collect(Collectors.joining("\n")).getBytes()
		);
	}
	
	private static Path p(String scramblingApproach, String retrievalModel, int topic) {
		return Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/scrambling-on-anserini").resolve(scramblingApproach + "-" + retrievalModel).resolve(topic + ".jsonl");
	}
	
	public static List<String> tokensInQuery(String query) {
		return AnalyzerUtils.analyze(query);
	}

	@SneakyThrows
	public static String queryStatistics(List<String> queries, int topic, String scramblingApproach, String retrievalModel) {
		List<Integer> tokensInQuery = new ArrayList<>();
		Set<String> tokens = new HashSet<>();
		
		queries = EvaluateQuerySavingsByHbc.parseQueriesAndSortByLengthOfScrambledQuery(queries);
		
		for(String query: queries) {
			List<String> tokenizedQuery = tokensInQuery(query);
			tokensInQuery.add(tokenizedQuery.size());
			tokens.addAll(tokenizedQuery);
		}
		
		Map<String, Object> ret = new LinkedHashMap<>();
		ret.put("topic", topic);
		ret.put("vocabularySize", tokens.size());
		ret.put("meanTokensInQuery", tokensInQuery.stream().mapToInt(i -> i).average().getAsDouble());
		ret.put("retrievalModel", retrievalModel);
		ret.put("queries", tokensInQuery.size());
		ret.put("scramblingApproach", scramblingApproach);
		
		return new ObjectMapper().writeValueAsString(ret);
	}
}
