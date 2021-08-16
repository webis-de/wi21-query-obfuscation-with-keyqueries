package de.webis.keyqueries.generators.elasticsearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.TermVectorsRequest;
import org.elasticsearch.client.core.TermVectorsResponse;

import de.webis.keyqueries.generators.DocumentTfIdfKeyQueryCandidateGenerator;

@SuppressWarnings("serial")
public class ElasticsearchDocumentTfIdfKeyQueryCandidateGenerator extends DocumentTfIdfKeyQueryCandidateGenerator {
	
	private final String esIndex;
	
	private final String esType;
	
	public ElasticsearchDocumentTfIdfKeyQueryCandidateGenerator(int topCandidates, String esIndex, String esType) {
		super(topCandidates);
		this.esIndex = esIndex;
		this.esType = esType;
	}

	@Override
	protected List<TermWithScore> terms(String docId) {
		try (RestHighLevelClient client = client()) {
			TermVectorsResponse response = client.termvectors(termVectorRequest(docId), RequestOptions.DEFAULT);
			if(response.getTermVectorsList().size() != 1) {
				throw new RuntimeException("TBD");
			}
			
			return new ArrayList<>(response.getTermVectorsList().get(0).getTerms()).stream()
					.map(i -> new TermWithScore(i.getTerm(), i.getScore()))
					.collect(Collectors.toList());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("deprecation")
	private TermVectorsRequest termVectorRequest(String docId) {
		docId = docId.replace(esIndex, "");
		
		TermVectorsRequest ret = new TermVectorsRequest(esIndex, esType, docId);
		ret.setFields("body_lang.en");
		ret.setTermStatistics(Boolean.TRUE);
		ret.setFieldStatistics(Boolean.TRUE);
		ret.setFilterSettings(filterSettings());
		
		return ret;
	}
	
	private static Map<String, Integer> filterSettings() {
		Map<String, Integer> ret = new HashMap<>();
		ret.put("min_doc_freq", 100);
		ret.put("max_doc_freq", 100000);
		
		return ret;
	}
	
	private RestHighLevelClient client() {
		return new RestHighLevelClient(RestClient.builder(new HttpHost("betaweb023.medien.uni-weimar.de", 9200, "http")));
	}
}