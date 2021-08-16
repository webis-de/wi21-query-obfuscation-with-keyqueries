package de.webis.crypsor;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

public class EnrichSelectedQueriesWithRankings {
	
	private static final String CHATNOIR_SEARCH_URL = "https://www.chatnoir.eu/api/v1/_search?apikey=f5e62422-9169-4210-bed0-36017de61bf7";
	
	public static void main(String[] args) {
		process("orig");

		process("hbc-bm25-keyqueryNdcg");
		process("tf-idf-bm25-keyqueryNdcg");
		process("nounphrase-bm25-keyqueryNdcg");
		process("hbc-qld-keyqueryNdcg");
		process("tf-idf-qld-keyqueryNdcg");
		process("nounphrase-qld-keyqueryNdcg");
		
		process("hbc-bm25-keyqueryNdcgRelaxed");
		process("tf-idf-bm25-keyqueryNdcgRelaxed");
		process("nounphrase-bm25-keyqueryNdcgRelaxed");
		process("hbc-qld-keyqueryNdcgRelaxed");
		process("tf-idf-qld-keyqueryNdcgRelaxed");
		process("nounphrase-qld-keyqueryNdcgRelaxed");
		
		
		process("arampatzis-bm25-ndcg");
		process("arampatzis-qld-ndcg");
		process("hbc-qld-ndcg");
		process("tf-idf-qld-ndcg");
		process("nounphrase-qld-ndcg");
		
		process("hbc-bm25-ndcg");
		process("tf-idf-bm25-ndcg");
		process("nounphrase-bm25-ndcg");
		
		process("arampatzis-bm25-keyqueryNdcg");
		process("arampatzis-qld-keyqueryNdcg");
		process("arampatzis-bm25-keyqueryNdcgRelaxed");
		process("arampatzis-qld-keyqueryNdcgRelaxed");
		
		process("arampatzis-qld-pmi");
		process("arampatzis-bm25-pmi");

		process("tf-idf-qld-pmi");
		process("tf-idf-bm25-pmi");
		
		process("hbc-qld-pmi");
		process("hbc-bm25-pmi");
		
		process("nounphrase-qld-pmi");
		process("nounphrase-bm25-pmi");
	}
	
	@SneakyThrows
	private static void process(String inputCombination) {
		for(int topic: CrypsorArgs.TOPICS) {
			Path p = Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/selected-scrambled-queries").resolve(inputCombination).resolve(topic +".jsonl");
			Path out = Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/submitted-scrambled-queries").resolve(inputCombination).resolve(topic +".jsonl");
			out.getParent().toFile().mkdirs();

			if(out.toFile().exists()) {
				System.out.println(inputCombination + " Skip " + topic);
				continue;
			}
			
			System.out.println(inputCombination + " Process: " + topic);
			
			String in = Files.readString(p);
			Files.write(out, enrichWithRankings(in, topic).getBytes());
		}
	}

	@SneakyThrows
	@SuppressWarnings("unchecked")
	public static String enrichWithRankings(String input, int topic) {
		Map<String, String> tmp = new LinkedHashMap<>(new ObjectMapper().readValue(input, Map.class));
		Map<String, Object> ret = new LinkedHashMap<>();
		
		for(String pos: tmp.keySet()) {
			ret.put(pos, rankingForQuery(tmp.get(pos), topic));
		}
		
		return new ObjectMapper().writeValueAsString(ret);
	}

	private static Map<String, Object> rankingForQuery(String query, int topic) {
		Map<String, Object> ret = new LinkedHashMap<>();
		ret.put("query", query);
		ret.put("ranking", query(query, topic).stream().map(i -> i.getLeft()).collect(Collectors.toList()));
		
		return ret;
	}
	
	@SneakyThrows
	@SuppressWarnings("unchecked")
	private static List<Pair<String, Double>> query(String query,  int topic) {
		Map<String, Object> response = new ObjectMapper().readValue(submitQueryToChatNoir(query, topic), Map.class);
		List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
		List<Pair<String, Double>> ret = new ArrayList<>();
		
		for(Map<String, Object> result: results) {
			ret.add(Pair.of((String) result.get("trec_id"), (Double) result.get("score")));
		}
		
		return ret.stream()
			.sorted((a,b) -> b.getRight().compareTo(a.getRight()))
			.collect(Collectors.toList());
	}
	
	@SneakyThrows
	private static String submitQueryToChatNoir(String query,  int topic) {
		String index = topic <= 200 ? "cw09" : "cw12";
		URI u = new URI(CHATNOIR_SEARCH_URL + "&index=" + index + "&query=" + urlEncode(query) + "&size=" + 100 + "&pretty"); 
		
		return IOUtils.toString(u, StandardCharsets.UTF_8);
	}
	
	@SneakyThrows
	private static String urlEncode(String query) {
		try {
			return URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return null;
		}
	}
}
