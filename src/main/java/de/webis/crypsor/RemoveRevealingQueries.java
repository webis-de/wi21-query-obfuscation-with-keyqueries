package de.webis.crypsor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

public class RemoveRevealingQueries {

	public static void main(String[] args) {
		for(String approach: CrypsorArgs.APPROACHES) {
			for(String retrievalModel: CrypsorArgs.RETRIEVAL_METHODS) {
				for(String selectionMethod: Arrays.asList("ndcg", "pmi")) {
					for(int topic: CrypsorArgs.TOPICS) {
						process(approach, retrievalModel, selectionMethod, topic);
					}
				}
			}
		}
	}
	
	@SneakyThrows
	private static void process(String approach, String retrievalModel, String selectionMethod, int topic) {
		String tmp = approach + "-" + retrievalModel + "-" + selectionMethod;
		Path inputPath = Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/BACKUPS/23-05-2021-submitted-scrambled-queries").resolve(tmp).resolve(topic + ".jsonl");
		Path outputPath = Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/submitted-scrambled-queries").resolve(tmp).resolve(topic + ".jsonl");
		
		System.out.println("Read: " + inputPath);
		System.out.println("Write: " + outputPath);
		
		outputPath.getParent().toFile().mkdirs();
		String output = removeRevealingQueries(Files.readString(inputPath));
		
		Files.writeString(outputPath, output);
	}

	private static final Set<Set<String>> REVEALING_QUERIES = revealingQueries();
	
	@SneakyThrows
	@SuppressWarnings("unchecked")
	public static String removeRevealingQueries(String input) {
		Map<String, Object> ret = new ObjectMapper().readValue(input, Map.class);
		
		List<Map<String, Object>> tmp = new ArrayList<>();
		for(int i=1; i< 26; i++) {
			Map<String, Object> data = (Map<String, Object>) ret.get("" + i);
			
			if(data != null && !REVEALING_QUERIES.contains(q((String) data.get("query")))) {
				tmp.add(data);
			}
		}
		
		ret = new LinkedHashMap<>();
		
		for(int i=0; i< tmp.size(); i++) {
			ret.put("" + (i +1), tmp.get(i));
		}
		
		return new ObjectMapper().writeValueAsString(ret);
	}

	@SneakyThrows
	private static Set<Set<String>> revealingQueries() {
		File f = Paths.get("src/main/resources/crypsor-query-user-study").toFile();
		Set<Set<String>> ret = new HashSet<>();
		for(String subFile: f.list()) {
			if(subFile.endsWith(".csv")) {
				List<String> lines = Files.readAllLines(f.toPath().resolve(subFile));
				ret.addAll(alreadyJudgedQueries(lines));
			}
		}
		
		System.out.println("Already judged: " + ret.size());
		
		return ret;
	}
	
	public static Set<Set<String>> alreadyJudgedQueries(List<String> input) {
		Set<Set<String>> ret = new HashSet<>();

		for(String query: input) {
			String[] parts = (query + " ").split(",");
			
			if(parts.length != 3) {
				throw new RuntimeException("Could not handle: " + query);
			}
			
			if(isRevealing(parts[2])) {
				ret.add(q(parts[1]));
			}
		}
		
		return ret;
	}
	
	private static Set<String> q(String query) {
		return new HashSet<>(Arrays.asList(query.split("\\s+")));
	}
	
	private static boolean isRevealing(String i) {
		try {
			return Integer.parseInt(i.trim()) <= 0;
		} catch(Exception e) {
			return false;
		}
	}
}
