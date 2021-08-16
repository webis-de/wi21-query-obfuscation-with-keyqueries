package de.webis.crypsor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.KeyQueryCheckerBase;
import de.webis.keyqueries.Searcher;
import de.webis.keyqueries.selection.ArampatzisPMISelectionStrategy;
import de.webis.keyqueries.selection.CrypsorQuerySelectionStrategy;
import de.webis.keyqueries.selection.NdcgSelectionStrategy;
import lombok.SneakyThrows;

public class MainQuerySelection {
	@SneakyThrows
	public static void main(String[] args) {
		if(args.length != 3) {
			throw new RuntimeException("Expect 3 args: approach retrievalModel querySelectionApproach");
		}
		
		for(int topic: CrypsorArgs.TOPICS) {
			process(args[0], args[1], topic, args[2]);
		}
	}
	
	@SneakyThrows
	private static void process(String scramblingApproach, String retrievalModel, int topic, String querySelectionApproach) {
		Path p = Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/scrambling-on-anserini").resolve(scramblingApproach + "-" + retrievalModel).resolve(topic + ".jsonl");
		System.out.println("Read results from " + p);
		List<String> queries = Files.readAllLines(p);
		String out = selectTop(queries, 25, querySelectionApproach, scramblingApproach);
		
		Path resultPath = Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/selected-scrambled-queries/").resolve(scramblingApproach + "-" + retrievalModel + "-" + querySelectionApproach).resolve(topic + ".jsonl");
		resultPath.getParent().toFile().mkdirs();
		System.out.println("Write to " + resultPath);
		Files.write(resultPath, out.getBytes());	
	}
	
	public static KeyQueryCheckerBase<String> kq(List<String> jsonLines) {
		Set<String> targetDocuments = targetDocuments(jsonLines.get(0));
		int k = 10; // target document must be in the top 10 of a keyquery
		int l = 20; // keyquery must produce at least 20 results
		int m = 3; // only one target document required in a key query
		
		return new KeyQueryChecker(targetDocuments, searcher(jsonLines), k, l, m) {
			@Override
			protected boolean noSubQueryIsKeyQuery(String query) {
				//already checked in HBC :)
				return true;
			}
		};
	}

	@SneakyThrows
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Searcher<String> searcher(List<String> jsonLines) {
		Map<String, Map<Integer, String>> queryToRanking = new HashMap<>();
		
		for(String q: jsonLines) {
			Map<String, Object> parsedQuery = new ObjectMapper().readValue(q, Map.class);
			q = (String) parsedQuery.get("scrambledQuery");
			Map<Integer, String> positionToTargetDocs = extractPpositionToTargetDocs((Map) parsedQuery.get("targetDocs"));
			
			if(!positionToTargetDocs.isEmpty()) {
				queryToRanking.put(q, positionToTargetDocs);
			}
		}
		
		return new Searcher<>() {
			@Override
			public List<String> search(String query, int size) {
				Map<Integer, String> ranking = queryToRanking.get(query);
				
				if(ranking == null) {
					return new ArrayList<>();
				}
				
				List<String> ret = new ArrayList<>();
				
				for(int i=0; i< size; i++) {
					String doc = ranking.getOrDefault(i+1, "does-not-exist");
					ret.add(doc);
				}
				
				return ret;
			}
			
		};
	}

	@SneakyThrows
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Map<Integer, String> extractPpositionToTargetDocs(String json) {
		Map<String, Object> parsedQuery = new ObjectMapper().readValue(json, Map.class);
		
		return extractPpositionToTargetDocs((Map) parsedQuery.get("targetDocs"));
	}
	
	private static Map<Integer, String> extractPpositionToTargetDocs(Map<String, Integer> docToPosition) {
		Map<Integer, String> ret = new HashMap<>();
		
		for(Map.Entry<String, Integer> i: docToPosition.entrySet()) {
			if(i != null && i.getValue() != null && i.getValue() <= 100) {
				if(ret.containsKey(i.getValue())) {
					throw new RuntimeException("Duplicate: " + i);
				}
				
				if(i.getValue() < 1) {
					throw new RuntimeException("I assume it starts wit position 1");
				}
				ret.put(i.getValue(), i.getKey());
			}
		}
		
		return ret;
	}

	@SneakyThrows
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Set<String> targetDocuments(String json) {
		Map<String, Object> ret = new ObjectMapper().readValue(json, Map.class);
		ret = (Map) ret.get("targetDocs");
		
		return ret.keySet();
	}

	public static String selectTop(List<String> queries, int topK, String selectionApproach) {
		return selectTop(queries, topK, selectionApproach, null);
	}
	
	@SneakyThrows
	public static String selectTop(List<String> queries, int topK, String selectionApproach, String scramblingApproach) {
		queries = queries.stream()
				.filter(i -> !extractPpositionToTargetDocs(i).isEmpty())
				.collect(Collectors.toList());
		KeyQueryCheckerBase<String> kq = kq(queries);
		ensureAllQueriesAreSubmitted(kq, EvaluateQuerySavingsByHbc.parseQueriesAndSortByLengthOfScrambledQuery(queries));

		if(scramblingApproach != null && selectionApproach.toLowerCase().contains("ndcg") && scramblingApproach.toLowerCase().contains("arampatzis")) {
			List<String> top1000Queries = selectWithStrategy(kq, 1025, "ndcg", queries);
			
			kq = kq(queries);
			ensureAllQueriesAreSubmitted(kq, top1000Queries);
		
			System.out.println("For Arampatzis: use only top " + kq.submittedQueries().size() + " Queries for retrieval.");
		}
		
		List<String> topQueries = selectWithStrategy(kq, topK, selectionApproach, queries);
		
		Map<Integer, String> ret = new LinkedHashMap<>();
		
		for(int i=0; i<topQueries.size(); i++) {
			ret.put(1+i, topQueries.get(i));
		}
		
		return new ObjectMapper().writeValueAsString(ret);
	}
	
	private static void ensureAllQueriesAreSubmitted(KeyQueryCheckerBase<String> kq, List<String> queries) {
		for(String q: queries) {
			kq.issueQuery(q);
		}
	}

	private static List<String> selectWithStrategy(KeyQueryCheckerBase<String> kq, int topK, String approach, List<String> rawQueries) {
		if("keyqueryNdcgRelaxed".equalsIgnoreCase(approach)) {
			CrypsorQuerySelectionStrategy<String> selectionStrategy = new CrypsorQuerySelectionStrategy<>(new NdcgSelectionStrategy<>());
			return selectionStrategy.selectTop(kq, topK, true);
		} else if ("keyqueryNdcg".equalsIgnoreCase(approach)){
			CrypsorQuerySelectionStrategy<String> selectionStrategy = new CrypsorQuerySelectionStrategy<>(new NdcgSelectionStrategy<>());
			return selectionStrategy.selectTop(kq, topK, false);
		} else if ("ndcg".equalsIgnoreCase(approach)) {
			NdcgSelectionStrategy<String> selectionStrategy = new NdcgSelectionStrategy<>();
			return selectionStrategy.selectTop(kq, topK);
		} else if ("pmi".equalsIgnoreCase(approach)) {
			return new ArampatzisPMISelectionStrategy().selectTop(rawQueries, topK);
		}
		
		else throw new RuntimeException("Could not handle: '" + approach +"'.");
	}
	
//	/**
//	 * Original Approach for comparison
//	 * @param args
//	 */
//	@SneakyThrows
//	public static void main(String[] args) {
//		for(int topic: CrypsorArgs.TOPICS) {
//			CrypsorArgs crypsorArgs = new CrypsorArgs();
//			crypsorArgs.topic = topic;
//			String queryForTopic = Main.readTopic(crypsorArgs);
//			
//			Path resultPath = Paths.get("/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/selected-scrambled-queries/").resolve("orig").resolve(topic + ".jsonl");
//			resultPath.getParent().toFile().mkdirs();
//			
//			Map<String, String> ret = new LinkedHashMap<>();
//			ret.put("" + topic, queryForTopic);
//			
//			String out = new ObjectMapper().writeValueAsString(ret);
//			
//			Files.write(resultPath, out.getBytes());
//		}
//	}
}
