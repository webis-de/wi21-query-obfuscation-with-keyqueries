//package de.webis.keyqueries.generators;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import io.anserini.rerank.RerankerContext;
//
//public class CandidateTerms {
//	public static final List<String> getCandidateTerms(RerankerContext<?> context, Map<String, Integer> rf) {
//		List<KeyQueryCandidateGenerator> candidateGenerator = KeyQueryCandidateGenerator.anseriniKeyQueryCandidateGenerator(context);
//		Set<String> ret = new HashSet();
//		for(KeyQueryCandidateGenerator generator: candidateGenerator) {
//			ret.addAll(generator.generateCandidates(rf.keySet()));
//		}
//		List<String> terms = new ArrayList<String>();
//		for(String candidate: ret) {
//			String[] split = candidate.split(" ");
//			for(String s: split) {
//				if(!terms.contains(s)) {
//					terms.add(s);
//				}
//			}
//		}
//		for(String term: terms) {
//			//zurück stemmen
//		}
//		return null;	
//		
//	}
//}
