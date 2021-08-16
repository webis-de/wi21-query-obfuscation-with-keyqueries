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

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.generators.chatnoir.HBC;
import lombok.SneakyThrows;

public class EvaluateQuerySavingsByHbc {
	
	@SneakyThrows
	public static void main(String[] args) {
		List<String> ret = new ArrayList<>();
		
		for (String method: CrypsorArgs.RETRIEVAL_METHODS) {
			for(int topic: CrypsorArgs.TOPICS) {
				System.out.println("Analyze " + method + " on " + topic);
				ret.add(analyzeTopic(topic, method));
			}
		}
		
		Files.write(Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/scrambling-on-anserini/hbc-query-savings-overview.jsonl"), ret.stream().collect(Collectors.joining("\n")).getBytes());
	}
	
//	public static void main(String[] args) {
//		System.out.println(analyzeTopic(262, "bm25"));
//	}
	
	private static Path hbcPathForMethod(String method) {
		return Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/scrambling-on-anserini").resolve("hbc-" + method);
	}
	
	@SneakyThrows
	private static String analyzeTopic(int topic, String method) {
		List<String> queries = Files.readAllLines(hbcPathForMethod(method).resolve(topic + ".jsonl"));
		Map<String, Object> ret = analyzeTopic(queries);
		ret.put("topic", topic);
		ret.put("method", method);
		
		return new ObjectMapper().writeValueAsString(ret);
	}
	
	public static Map<String, Object> analyzeTopic(List<String> queries) {
		Map<String, Object> ret = new LinkedHashMap<>();
		ret.put("submittedQueries", queries.size());
		ret.put("exhaustiveSearchNumberOfQueries", hbc(new ArrayList<>(startupQueries(queries))));
		
		return ret;
	}

	private static int hbc(List<String> nounPhrases) {
		HBC hbc = new HBC(new ArrayList<>(nounPhrases));
		KeyQueryChecker keyQueryChecker = keyQueryChecker();
		
		hbc.runHbcAlgorithm(keyQueryChecker);
		
		return keyQueryChecker.submittedQueries().stream().collect(Collectors.toList()).size();
	}
	
	private static KeyQueryChecker keyQueryChecker() {
		Set<String> submittedQueries = new HashSet<>();
		
		return new KeyQueryChecker(new HashSet<>(), null, 10, 20, 3) {
			@Override
			public Set<String> targetDocumentsInResult(String query) {
				return new HashSet<>();
			}
			
			@Override
			public boolean parameterLIsSatisfied(String query) {
				return true;
			}
			
			@Override
			public boolean isKeyQuery(String query) {
				submittedQueries.add(query);
				return false;
			}
			
			@Override
			public Set<String> submittedQueries() {
				return submittedQueries;
			}
		};
	}

	public static Set<String> startupQueries(List<String> queries) {
		queries = parseQueriesAndSortByLengthOfScrambledQuery(queries);
		Set<String> ret = new HashSet<>();
		
		for(String query: queries) {
			if(isNewQuery(ret, query)) {
				ret.add(query);
			}
		}
		
		return ret;
	}
	
	private static boolean isNewQuery(Set<String> existingQueries, String query) {
		for(String existingQuery: existingQueries) {
			if(query.contains(existingQuery)) {
				return false;
			}
		}
		
		return true;
	}
	
	static List<String> parseQueriesAndSortByLengthOfScrambledQuery(List<String> queries) {
		List<Pair<String, String>> ret = queries.stream()
			.map(i -> privateQueryToScrambledQuery(i))
			.sorted((a,b) -> Integer.valueOf(a.getRight().length()).compareTo(b.getRight().length()))
			.collect(Collectors.toList());

		failWhenMultiplePrivateQueriesOccur(ret);
		
		return ret.stream().map(i -> i.getRight()).collect(Collectors.toList());
	}
	
	private static void failWhenMultiplePrivateQueriesOccur(List<Pair<String, String>> privateQueriesToScrambledQuery) {
		int privateQueries = privateQueriesToScrambledQuery.stream().map(i -> i.getLeft()).collect(Collectors.toSet()).size();
		
		if(privateQueries != 1) {
			throw new RuntimeException("Found more than one private query: " + privateQueries);
		}
	}

	@SneakyThrows
	@SuppressWarnings("unchecked")
	static Pair<String, String> privateQueryToScrambledQuery(String json) {
		Map<String, Object> parsedJson = new ObjectMapper().readValue(json, Map.class);
		
		return Pair.of((String) parsedJson.get("privateQuery"), (String) parsedJson.get("scrambledQuery"));
	}
}
