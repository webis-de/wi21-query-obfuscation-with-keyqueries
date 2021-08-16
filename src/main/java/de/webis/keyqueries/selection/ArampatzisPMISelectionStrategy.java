package de.webis.keyqueries.selection;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.SneakyThrows;

public class ArampatzisPMISelectionStrategy {
	private static final int RETRIEVA_CUTOFF_THRESHOLD = 10;
	
	@SneakyThrows
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static double getMI(String scrambledQueryResultAsJson) {
		//cw09b and cw12b13 both have 50 million documents.
		int M = 50000000;
		Map<String, Object> parsedQuery = new ObjectMapper().readValue(scrambledQueryResultAsJson, Map.class);
		
		//hits for scrambled query
		int dfw = (int)parsedQuery.get("hitsForScrambledQuery");
		
		int hits = calculateHits((Map) parsedQuery.get("targetDocs"));
		
		//tq is the number of target documents
		int tq = ((Map) parsedQuery.get("targetDocs")).keySet().size();
		return Math.log(((double)hits/M)/(((double)dfw/M)*((double)tq/M)));
	}

	@SneakyThrows
	@SuppressWarnings("unchecked")
	public List<String> selectTop(List<String> scrambledQueriesResultsAsJson, int topK) {
		List<Pair<String, Double>> ret = new ArrayList<>();

		for(String query: scrambledQueriesResultsAsJson) {
			Map<String, Object> parsedQuery = new ObjectMapper().readValue(query, Map.class);
			
			ret.add(Pair.of((String) parsedQuery.get("scrambledQuery"), getMI(query)));
		}
		
		return ret.stream()
				.sorted((a,b) -> b.getRight().compareTo(a.getRight()))
				.map(i -> i.getKey())
				.limit(topK)
				.collect(Collectors.toList());
	}
	
	private static int calculateHits(Map<String, Integer> positionsOfTargetDocuments) {
		int ret = 0;
		
		for(Integer pos: positionsOfTargetDocuments.values()) {
			if(pos != null && pos <= RETRIEVA_CUTOFF_THRESHOLD) {
				ret += 1;
			}
		}
		
		return ret;
	}
}

/*
 * 
 * @param dfw |Hq| --> the document frequency of q (number of documents in the collection that q hits) 
--> number of results returned for query q
 * @param hits
 * @param tq

q := Private Query
N := Size of the document collection =? M
Hq := set of documents matching q
dfq := |Hq| --> the document frequency of q (number of documents in the collection that q hits) 
--> number of results returned for query q
dfw,q = Betrag der Schnittmenge von Hw und Hq --> gemeinsame Treffer von w und q
PMIw = log(N*dfq,w / (dfq * dfw))
dfw = number of Total results of scrambled Query
Single-Term scarmbled queries: can be determined directly from the document sample
multi-word scarmbled queries: 
 
 
 */
