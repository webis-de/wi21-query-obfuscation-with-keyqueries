package de.webis.crypsor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

public class UserStudyCsvGenerator {
	private static List<String> SELECTION_APPROACHES = Arrays.asList("ndcg", "pmi");
	
	public static void main(String[] args) {
		Set<Set<String>> alreadyJudged = judgedFromPreviousRound();
		for(int topic: CrypsorArgs.TOPICS) {
			System.out.println("Process " + topic);
			process(topic, alreadyJudged);
		}
	}
	
	@SneakyThrows
	private static Set<Set<String>> judgedFromPreviousRound() {
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
	
	@SneakyThrows
	@SuppressWarnings("unchecked")
	private static void process(int topic, Set<Set<String>> judgedFromPreviousRound) {
		CrypsorArgs a = new CrypsorArgs();
		a.topic = topic;
		String privateQuery = Main.readTopicPublic(a);
		
		Set<String> ret = new HashSet<>();
		
		for(String retrievalModel: CrypsorArgs.RETRIEVAL_METHODS) {
			for(String selectionApproach: SELECTION_APPROACHES) {
				for(String scramblingApproach: CrypsorArgs.APPROACHES) {
					Path p = Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/selected-scrambled-queries").resolve(scramblingApproach + "-" + retrievalModel + "-" + selectionApproach).resolve(topic +".jsonl");
					Map<String, String> scrambledQueries = new ObjectMapper().readValue(p.toFile(), Map.class);
					
					ret.addAll(scrambledQueries.values());
				}
			}
		}
		
		List<String> out = new ArrayList<>();
		Set<Set<String>> alreadyCovered = new HashSet<>();
		int alreadyJudged = 0;
		
		for(String query: ret) {
			if(!alreadyCovered.contains(q(query))) {
				alreadyCovered.add(q(query));
				
				if(judgedFromPreviousRound.contains(q(query))) {
					alreadyJudged += 1;
				} else {
					out.add(query);
				}
			}
		}

		if(alreadyJudged > 0) {
			System.out.println("Skip " + alreadyJudged + " judged queries");
		}
		
		Collections.shuffle(out);

		//# 1 OK
		// 0 Sensitiv
		// -1 sensitiv uznd/oder garbage
		String finalOut = "privateQuery,scrambledQuery,score\n" + out.stream()
			.map(i -> privateQuery + "," + i +",")
			.collect(Collectors.joining("\n"));
		
		Files.write(Paths.get("src/main/resources/crypsor-query-user-study/" + topic + "-user-study.csv"), finalOut.getBytes());
	}

	public static Set<Set<String>> alreadyJudgedQueries(List<String> input) {
		Set<Set<String>> ret = new HashSet<>();

		for(String query: input) {
			String[] parts = (query + " ").split(",");
			
			if(parts.length != 3) {
				throw new RuntimeException("Could not handle: " + query);
			}
			
			if(isJudged(parts[2])) {
				ret.add(q(parts[1]));
			}
		}
		
		return ret;
	}
	
	private static boolean isJudged(String i) {
		try {
			Integer.parseInt(i.trim());
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	private static Set<String> q(String query) {
		return new HashSet<>(Arrays.asList(query.split("\\s+")));
	}
}
