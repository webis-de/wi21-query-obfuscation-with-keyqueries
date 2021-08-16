package de.webis.keyqueries.generators.chatnoir;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.search.IndexSearcher;

import lombok.Data;

@Data
public class CrypsorKeyQueryCandidateGenerator {
	
	private final String approach;
	
	private final IndexSearcher searcher;
	
	public CrypsorKeyQueryCandidateGenerator(String approach, IndexSearcher searcher) {
		this.approach = approach;
		this.searcher = searcher;
		
		if(approach() == null) {
			throw new RuntimeException("Can not handle xxx");
		}
	}
	
	public List<String> getCandidates(String privateQuery, Set<String> targetDocuments) {
		SensitiveTerms sensitiveTerms = SensitiveTerms.getSensitiveTermsWithSynonymsHyponymsAndHypernyms(privateQuery);
		List<String> ret = approach().getCandidates(sensitiveTerms, targetDocuments);
		
		return ret.stream().filter(i -> !sensitiveTerms.phraseIsDeniedByUser(i)).collect(Collectors.toList());
	}
	
	private InternalKeyQueryCandidateGenerator approach() {
		if("nounphrase".equalsIgnoreCase(approach)) {
			return new NounPhraseExtraction();
		} else if ("arampatzis".equalsIgnoreCase(approach)) {
			return new ArampatzisApproach();
		} else if("tf-idf".equalsIgnoreCase(approach)) {
			return new ChatNoirTfIdfApproach(searcher);
		} else if("hbc".equalsIgnoreCase(approach)) {
			return new HbcChatNoir(searcher);
		} else if("HbcTfIdf".equalsIgnoreCase(approach)) {
			return new HbcTfIdf(searcher);
		}
		
		return null;
	}
	
	public static interface InternalKeyQueryCandidateGenerator {
		public List<String> getCandidates(SensitiveTerms sensitiveTerms, Set<String> targetDocuments);
	}
}
