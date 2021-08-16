package de.webis.keyqueries.generators.chatnoir;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.search.IndexSearcher;

import de.webis.keyqueries.KeyQueryChecker;
import de.webis.keyqueries.Searcher;
import de.webis.keyqueries.anserini.LuceneSearcher;
import de.webis.keyqueries.generators.chatnoir.CrypsorKeyQueryCandidateGenerator.InternalKeyQueryCandidateGenerator;
import io.anserini.index.IndexCollection;
import lombok.Data;

@Data
public class HbcChatNoir implements InternalKeyQueryCandidateGenerator {

	private final IndexSearcher searcher;
	
	@Override
	public List<String> getCandidates(SensitiveTerms sensitiveTerms, Set<String> targetDocuments) {
		List<String> nounPhrases = nounPhrases(sensitiveTerms, targetDocuments);
		HBC hbc = new HBC(new ArrayList<>(nounPhrases));
		KeyQueryChecker keyQueryChecker = keyQueryChecker(targetDocuments);
		
		hbc.runHbcAlgorithm(keyQueryChecker);
		
		return keyQueryChecker.submittedQueries().stream().collect(Collectors.toList());
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

	private List<String> nounPhrases(SensitiveTerms sensitiveTerms, Set<String> targetDocuments) {
		String[] texts = targetDocuments.stream()
				.map(i -> new ChatNoirDocument(i).mainContent())
				.collect(Collectors.toList()).toArray(new String[targetDocuments.size()]);
		
		return new NounPhraseExtraction().nounPhrasesSortedFromOftenToRare(sensitiveTerms, 10, texts);
	}
}
