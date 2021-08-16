package de.webis.crypsor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.webis.keyqueries.generators.chatnoir.ChatNoirDocument;
import lombok.SneakyThrows;

public class LoadChatNoirDocs {
	
	private static final String DIR = "/mnt/ceph/storage/data-in-progress/data-research/web-search/private-web-search-with-keyqueries/reranking-index-anserini/";
	
	private static final String INPUT_DIR = DIR + "doc-ids/";
	
	private static final String OUTPUT_DIR = DIR + "documents/";
	
	public static void main(String[] args) {
		for(String part: args) {
			if(outputExists(part)) {
				System.out.println("Skip Existing Part: " + part);
				continue;
			}
			process(part);
		}
	}
	
	@SneakyThrows
	private static void process(String part) {
		Path inFile = Paths.get(INPUT_DIR).resolve(part);
		List<String> docIds = Files.readAllLines(inFile);
		String output = docIds.stream()
			.map(i -> loadDoc(i))
			.collect(Collectors.joining("\n"));
		
		Path outFile = Paths.get(OUTPUT_DIR).resolve(part +".jsonl");
		Files.write(outFile, output.getBytes());
	}

	private static boolean outputExists(String part) {
		return Paths.get(OUTPUT_DIR).resolve(part).toFile().exists();
	}

	@SneakyThrows
	private static String loadDoc(String docId) {
		Map<String, String> ret = new LinkedHashMap<>();
		ret.put("id", docId);
		ret.put("contents", new ChatNoirDocument(docId).mainContent());
		
		return new ObjectMapper().writeValueAsString(ret);
	}
}
