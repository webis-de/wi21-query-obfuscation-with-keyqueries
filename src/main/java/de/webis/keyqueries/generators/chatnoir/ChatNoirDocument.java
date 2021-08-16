package de.webis.keyqueries.generators.chatnoir;

import java.io.FileNotFoundException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.webis.WebisUUID;
import lombok.SneakyThrows;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;


public class ChatNoirDocument {
	
	private final String documentId;
	
	public ChatNoirDocument(String documentId) {
		this.documentId = documentId;
	}
	
	public static void main(String[] args) {
		System.out.println(new ChatNoirDocument("clueweb09-en0032-54-18523").mainContent());
	}
	
	public static final RetryPolicy<String> RETRY_POLICY = new RetryPolicy<String>()
			  .handle(Exception.class)
			  .withBackoff(2, 10, ChronoUnit.SECONDS)
			  .withMaxRetries(5);
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String mainContent() {
		String ret = "";
		Map<String, Object> tmp = ((Map)parse().get("_source"));
		
		if(tmp == null) {
			return ret;
		}
		
		for(String k: tmp.keySet()) {
			if(k.startsWith("body_lang")) {
				ret += " " + tmp.get(k);
			}
		}
		
		return ret.trim();
	}
	
	@SuppressWarnings("unchecked")
	@SneakyThrows
	private Map<String, Object> parse() {
		System.out.println("http://betaweb023.medien.uni-weimar.de:9200/" + getIndex() + "/warcrecord/" + getUuid() + "\t\t(ID:" + documentId + ")");

		String src = Failsafe.with(RETRY_POLICY)
				.get(() -> IOUtils.toString(inputStream("http://betaweb023.medien.uni-weimar.de:9200/" + getIndex() + "/warcrecord/" + getUuid()),
						StandardCharsets.UTF_8));
		try {
			return new ObjectMapper().readValue(src, Map.class);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@SneakyThrows
	private java.io.InputStream inputStream(String url) {
		HttpURLConnection conn = (HttpURLConnection) new URL("http://betaweb023.medien.uni-weimar.de:9200/" + getIndex() + "/warcrecord/" + getUuid()).openConnection();
		try {
			return conn.getInputStream();
		} catch(FileNotFoundException e) {
			return conn.getErrorStream();
		}
	}
	
	private String getUuid() {
		return webisUUID(documentId);
	}
	
	private static String webisUUID(String documentId) {
		return webisUUID(longChatNoirId(documentId), documentId).toString();
	}

	private static UUID webisUUID(String prefix, String documentId) {
		return new WebisUUID(prefix).generateUUID(documentId);
	}

	private static String longChatNoirId(String documentId) {
		if (documentId.startsWith("clueweb09")) {
			return "clueweb09";
		} else if (documentId.startsWith("clueweb12")) {
			return "clueweb12";
		} else {
			return "commoncrawl";
		}
	}
	
	private String getIndex() {
		if (documentId.startsWith("clueweb09")) {
			return "webis_warc_clueweb09_003";
		} else if(documentId.startsWith("clueweb12")) {
			return "webis_warc_clueweb12_011";
		} else {
			throw new RuntimeException("Could not handle:" + documentId);
		}
	}
}
