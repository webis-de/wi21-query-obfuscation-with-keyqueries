package de.webis.keyqueries.generators;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import de.webis.keyqueries.generators.lucene.LuceneDocumentTfIdfKeyQueryCandidateGenerator;
import io.anserini.rerank.RerankerContext;

public interface KeyQueryCandidateGenerator<T> extends Serializable {
	public List<T> generateCandidates(Set<String> targetDocuments);
	
	public static List<KeyQueryCandidateGenerator<String>> anseriniKeyQueryCandidateGenerator(RerankerContext<?> rerankerContext) {
		return Arrays.asList(
			new LuceneDocumentTfIdfKeyQueryCandidateGenerator(rerankerContext.getSearchArgs().extraction, rerankerContext.getIndexSearcher(), rerankerContext.getSearchArgs().filterAlphaNumeric),
			new DocumentCollectionTfIdfKeyQueryCandidateGenerator(new LuceneDocumentTfIdfKeyQueryCandidateGenerator(rerankerContext.getSearchArgs().extraction_collection, rerankerContext.getIndexSearcher(), rerankerContext.getSearchArgs().filterAlphaNumeric), rerankerContext.getSearchArgs().extraction_collection)
		);
	}
}