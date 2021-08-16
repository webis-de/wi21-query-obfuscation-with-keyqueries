package de.webis.keyqueries.generators.chatnoir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.search.IndexSearcher;

import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.Searcher;
import de.webis.keyqueries.anserini.LuceneSearcher;
import de.webis.keyqueries.generators.chatnoir.CrypsorKeyQueryCandidateGenerator.InternalKeyQueryCandidateGenerator;
import de.webis.keyqueries.generators.lucene.LuceneDocumentTfIdfKeyQueryCandidateGenerator;
import io.anserini.index.IndexCollection;
import lombok.Data;

@Data
public class HbcTfIdf implements InternalKeyQueryCandidateGenerator {
	private final IndexSearcher searcher;
	
	@Override
	public List<String> getCandidates(SensitiveTerms sensitiveTerms, Set<String> targetDocuments) {
		return getCandidates(sensitiveTerms, ChatNoirTfIdfApproach.docIdToText(targetDocuments));
	}
	
	public List<String> getCandidates(SensitiveTerms sensitiveTerms, Map<String, String> docIdToText) {
		Set<String> ret = new HashSet<>();
		
		for(String doc: docIdToText.keySet()) {
			LuceneDocumentTfIdfKeyQueryCandidateGenerator gen = ChatNoirTfIdfApproach.generator(docIdToText, sensitiveTerms, searcher);
			List<String> topTermVectors = gen.topTermVectors(doc);
			if(topTermVectors == null || topTermVectors.isEmpty()) {
				continue;
			}
			
			HBC hbc = new HBC(new ArrayList<>(topTermVectors));
			KeyQueryChecker keyQueryChecker = keyQueryChecker(docIdToText.keySet());
			
			hbc.runHbcAlgorithm(keyQueryChecker);
			
			ret.addAll(keyQueryChecker.submittedQueries().stream().collect(Collectors.toList()));
		}
		
		return ret.stream().collect(Collectors.toList());
	}
	
	private KeyQueryChecker keyQueryChecker(Set<String> targetDocuments) {
		int k = 10; // target document must be in the top 10 of a keyquery
		int l = 20; // keyquery must produce at least 20 results
		int m = 3; // only one target document required in a key query
		
		return new KeyQueryChecker(targetDocuments, searcher(), k, l, m) {
			@Override
			protected boolean noSubQueryIsKeyQuery(String query) {
				//already checked in HBC :)
				return true;
			}
		};
	}

	private Searcher<String> searcher() {
		return new LuceneSearcher(searcher, IndexCollection.DEFAULT_ANALYZER);
	}
}
