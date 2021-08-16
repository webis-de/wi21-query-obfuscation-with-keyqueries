package de.webis.keyqueries.generators.chatnoir;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.search.IndexSearcher;

import de.webis.keyqueries.generators.chatnoir.CrypsorKeyQueryCandidateGenerator.InternalKeyQueryCandidateGenerator;
import de.webis.keyqueries.generators.lucene.LuceneDocumentTfIdfKeyQueryCandidateGenerator;
import lombok.Data;

@Data
public class ChatNoirTfIdfApproach implements InternalKeyQueryCandidateGenerator {

	private final IndexSearcher searcher;
	
	@Override
	public List<String> getCandidates(SensitiveTerms sensitiveTerms, Set<String> targetDocuments) {
		return getCandidates(sensitiveTerms, docIdToText(targetDocuments));
	}

	private List<String> getCandidates(SensitiveTerms sensitiveTerms, Map<String, String> docIdToText) {
		LuceneDocumentTfIdfKeyQueryCandidateGenerator gen = generator(docIdToText, sensitiveTerms, searcher);
		return gen.generateCandidates(docIdToText.keySet());
	}
	
	public static Map<String, String> docIdToText(Set<String> targetDocuments) {
		Map<String, String> ret = new LinkedHashMap<>();
		for(String targetDoc: targetDocuments) {
			ret.put(targetDoc, new ChatNoirDocument(targetDoc).mainContent());
		}
		
		return ret;
	}
	
	@SuppressWarnings("serial")
	public static LuceneDocumentTfIdfKeyQueryCandidateGenerator generator(Map<String, String> docIdToText, SensitiveTerms sensitiveTerms, IndexSearcher searcher) {
		return new LuceneDocumentTfIdfKeyQueryCandidateGenerator(7, searcher, true, new LuceneDocumentTfIdfKeyQueryCandidateGenerator.MapResolveHumanReadableWord(docIdToText)) {
			public List<String> topTermVectors(String docId) {
				List<String> ret = allTermVectorsSortedByScore(docId).stream()
						.filter(i -> !sensitiveTerms.phraseIsDeniedByUser(i))
						.limit(super.topCandidates).collect(Collectors.toList());
				
				System.out.println("Terms for " + docId +": " + ret);
				
				return ret;
			}
		};
	}
}
