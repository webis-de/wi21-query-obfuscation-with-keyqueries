package de.webis.crypsor;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;

import de.webis.keyqueries.generators.chatnoir.CrypsorKeyQueryCandidateGenerator;
import io.anserini.search.SimpleSearcher;
import io.anserini.search.SimpleSearcher.Result;
import io.anserini.search.topicreader.WebxmlTopicReader;
import lombok.Data;
import lombok.SneakyThrows;

@Data
public class Main {
	
	private final String privateQuery;
	
	private final long hitsForPrivateQuery;
	
	private final Set<String> targetDocs;
	
	private final SimpleSearcher simpleSearcher;
	
	private final CrypsorArgs args;
	
	@SneakyThrows
	public Main(CrypsorArgs args) {
		this.args = args;
		this.privateQuery = readTopic(args);
		this.simpleSearcher = args.getSimpleSearcher();
		
		Result[] results = simpleSearcher.search(privateQuery);
		this.hitsForPrivateQuery = results.length == 0 ? 0 : results[0].totalHits;
		Set<String> tmp = new LinkedHashSet<>();
		
		for(Result r: results) {
			tmp.add(r.docid);
		}
		
		targetDocs = Collections.unmodifiableSet(tmp);
	}
	
	public static void main(String[] args) throws Exception {
		CrypsorArgs crypsorArgs = CrypsorArgs.parse(args);
		if(crypsorArgs == null) {
			return;
		}
		
		new Main(crypsorArgs).run();
	}
	
	@SneakyThrows
	private void run() {
		System.out.println("Generate Candidates for " + targetDocs.size() + " documents.");
		Paths.get(args.getOutputFile()).resolve("..").toFile().mkdirs();
		CrypsorKeyQueryCandidateGenerator candGenerator = generator();
		List<String> queries = candGenerator.getCandidates(privateQuery, targetDocs);
		
		if(args.isDebug) {
			System.out.println("Dbug only: I would evaluate with " + queries.size() + " candidate queries "
					+ "(Topic: " + args.topic +"; Query: " + privateQuery + "). Would write results to " + args.getOutputFile());
			return;
		}
		
		
		if (args.partitions <= 1) {
			System.out.println("I evaluate " + queries.size() + " candidate queries "
					+ "(Topic: " + args.topic +"; Query: " + privateQuery + "). Write results to " + args.getOutputFile());
		
			List<String> output = runScrambledQueries(queries);
			Files.write(Paths.get(args.getOutputFile()), output.stream().collect(Collectors.joining("\n")).getBytes());
		} else {
			List<List<String>> partitions = Lists.partition(queries, args.partitions);
			System.out.println("I evaluate " + queries.size() + " candidate queries in " + partitions.size() + " partitions "
					+ "(Topic: " + args.topic +"; Query: " + privateQuery + ").");
			for(int i=0; i<partitions.size(); i++) {
				List<String> partition = partitions.get(i);
				String out = args.getOutputFile() + "-part-" + i;
				System.out.println("I evaluate " + partition.size() + " candidate queries "
						+ "(Topic: " + args.topic +"; Query: " + privateQuery + "). Write results to " + out);
				
				List<String> output = runScrambledQueries(partition);
				Files.write(Paths.get(out), output.stream().collect(Collectors.joining("\n")).getBytes());
			}
		}
		
	}
	
	public CrypsorKeyQueryCandidateGenerator generator() {
		return new  CrypsorKeyQueryCandidateGenerator(args.scramblingApproach, simpleSearcher.getIndexSearcher());
	}

	public String readTopic(CrypsorArgs crypsorArgs) {
		return readTopicPublic(crypsorArgs);
	}
	
	@SneakyThrows
	public static String readTopicPublic(CrypsorArgs crypsorArgs) {
		String cwVersion = crypsorArgs.topic <=200 ? "09" : "12";
		WebxmlTopicReader reader = new WebxmlTopicReader(Paths.get("src/main/resources/topics-and-qrels/topics.clueweb" + cwVersion + "-private.txt"));
		Map<String, String> ret = reader.read().get(crypsorArgs.topic);
		
		if(ret == null || ret.get("title") == null || ret.get("title").isBlank()) {
			throw new RuntimeException("Got: " + ret);
		}
		
		return ret.get("title");
	}

	public List<String> runScrambledQueries(List<String> queries) {
		Map<String, Result[]> ret = simpleSearcher.batchSearch(queries, queries, 100, args.threads);
		
		return formatScrambledQueries(ret);
	}
	
	private List<String> formatScrambledQueries(Map<String, Result[]> scrambedQueryResults) {
		return scrambedQueryResults.keySet().stream().sorted()
				.map(i -> formatScrambedQuery(i, scrambedQueryResults.get(i)))
				.collect(Collectors.toList());
	}
	
	@SneakyThrows
	private String formatScrambedQuery(String query, Result[] result) {
		Map<String, Object> ret = new LinkedHashMap<>();
		ret.put("privateQuery", privateQuery);
		ret.put("scrambledQuery", query);
		ret.put("approach", args.scramblingApproach);
		ret.put("hitsForPrivateQuery", hitsForPrivateQuery);
		ret.put("hitsForScrambledQuery", result.length == 0 ? 0 : result[0].totalHits);
		ret.put("targetDocs", positionOfTargetDocs(result));
		
		return new ObjectMapper().writeValueAsString(ret);
	}
	
	private Map<String, Integer> positionOfTargetDocs(Result[] result) {
		Map<String, Integer> ret = new LinkedHashMap<>();
		
		for(String targetDoc: targetDocs) {
			Integer pos = null;
			for(int i=0; i< result.length; i++) {
				if(targetDoc.equals(result[i].docid)) {
					pos = 1+i;
				}
			}
			
			ret.put(targetDoc, pos);
		}
		
		return ret;
	}
}
