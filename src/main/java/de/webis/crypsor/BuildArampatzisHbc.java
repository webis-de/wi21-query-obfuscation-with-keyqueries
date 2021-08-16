package de.webis.crypsor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.webis.keyqueries.KeyQueryCheckerBase;
import lombok.SneakyThrows;


public class BuildArampatzisHbc {

	@SneakyThrows
	public static void main(String[] args) {
		for(String retrievalModel: CrypsorArgs.RETRIEVAL_METHODS) {
			for(int topic: CrypsorArgs.TOPICS) {
				process(topic, retrievalModel);
			}
		}
	}
	
	@SneakyThrows
	private static void process(int topic, String retrievalModel) {
		Path p = Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/scrambling-on-anserini").resolve("arampatzis-" + retrievalModel).resolve(topic + ".jsonl");
		System.out.println("Read results from " + p);
		List<String> queries = Files.readAllLines(p);
		
		Path resultPath = Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/scrambling-on-anserini/").resolve("arampatzisHbc-" + retrievalModel).resolve(topic + ".jsonl");
		resultPath.getParent().toFile().mkdirs();
		System.out.println("Write to " + resultPath);

		queries = queriesThatWouldBeSubmittedByHbc(queries);
		String out = queries.stream().collect(Collectors.joining("\n"));
		Files.write(resultPath, out.getBytes());	
	}

	@SneakyThrows
	@SuppressWarnings("unchecked")
	public static Set<String> docsWithTooFewResults(List<String> queries) {
		Set<String> ret = new HashSet<>();
		
		for(String query: queries) {
			Map<String, Object> parsedQuery = new ObjectMapper().readValue(query, Map.class);
			
			// keyquery must produce at least 20 results
			if((int) parsedQuery.get("hitsForScrambledQuery") < 20) {
				ret.add((String) parsedQuery.get("scrambledQuery"));
			}
		}
		
		return ret;
	}

	@SneakyThrows
	@SuppressWarnings("unchecked")
	public static Set<String> docsWithEnoughMatches(List<String> queries) {
		Set<String> ret = new HashSet<>();
		KeyQueryCheckerBase<String> kq = MainQuerySelection.kq(queries);
		
		for(String query: queries) {
			Map<String, Object> parsedQuery = new ObjectMapper().readValue(query, Map.class);
			query = (String) parsedQuery.get("scrambledQuery");
			
			if(kq.isKeyQuery(query)) {
				ret.add(query);
			}
		}
		
		return ret;
	}

	public static List<String> queriesThatWouldBeSubmittedByHbc(List<String> queries) {
		Set<String> toRemoveAtNextLevel = docsWithEnoughMatches(queries);
		toRemoveAtNextLevel.addAll(docsWithTooFewResults(queries));
		List<String> ret = new LinkedList<>();
		
		for(String query: queries) {
			if(hbcWouldHaveExpanded(query, toRemoveAtNextLevel)) {
				ret.add(query);
			}
		}
		
		return ret;
	}
	
	private static boolean hbcWouldHaveExpanded(String query, Set<String> toRemoveAtNextLevel) {
		for(String subQuery: subQueries(query)) {
			if(toRemoveAtNextLevel.contains(subQuery)) {
				return false;
			}
		}
		
		return true;
	}
	
	@SneakyThrows
	@SuppressWarnings("unchecked")
	public static List<String> subQueries(String query) {
		Map<String, Object> parsedQuery = new ObjectMapper().readValue(query, Map.class);
		query = (String) parsedQuery.get("scrambledQuery");
		
		String[] tokens = query.split("\\s+");
		
		if(tokens.length == 0) {
			throw new RuntimeException("Could not handle: " + query);
		} else if (tokens.length == 1) {
			return new ArrayList<>();
		} else if (tokens.length == 2) {
			return Arrays.asList(tokens[0], tokens[1]);
		} else if (tokens.length == 3) {
			return Arrays.asList(
				tokens[0], tokens[1], tokens[2],
				tokens[0] + " " + tokens[1], tokens[0] + " " + tokens[2], 
				tokens[1] + " " + tokens[2], tokens[1] + " " + tokens[0],
				tokens[2] + " " + tokens[0], tokens[2] + " " + tokens[1]
			);
		} else {
			throw new RuntimeException("Could not handle: " + query);
		}
	}
}
