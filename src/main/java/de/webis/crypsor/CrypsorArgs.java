package de.webis.crypsor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;
import org.kohsuke.args4j.ParserProperties;

import io.anserini.search.SimpleSearcher;
import lombok.SneakyThrows;

public class CrypsorArgs {
	@Option(name = "-index", metaVar = "[path]", required = true, usage = "Path to Lucene index")
	public String index;

	@Option(name = "-topic", metaVar = "[Number]", required = true, usage = "topic to retrieve")
	public int topic = -1;

	@Option(name = "-bm25", forbids = { "-qld" }, usage = "ranking model: BM25")
	public boolean bm25 = false;

	@Option(name = "-qld", forbids = { "-bm25" }, usage = "ranking model: query likelihood with Dirichlet smoothing")
	public boolean qld = false;

	@Option(name = "-threads", metaVar = "[Number]", usage = "Number of Threads")
	public int threads = 1;

	@Option(name = "-partitions", metaVar = "[Number]", usage = "Number of partitions")
	public int partitions = 0;
	
	@Option(name = "-output", metaVar = "[file]", required = true, usage = "output file")
	public String output;

	@Option(name = "-scramblingApproach", required = true, usage = "The scramblingApproach to use.")
	public String scramblingApproach = null;
	
	@Option(name = "-isDebug")
	public boolean isDebug = false;

	public static final List<String> RETRIEVAL_METHODS = Collections.unmodifiableList(Arrays.asList("bm25", "qld"));
	
	public static final List<String> APPROACHES = Arrays.asList("nounphrase", "arampatzis", "arampatzisHbc", "tf-idf", "HbcTfIdf", "hbc");

	public static final List<Integer> TOPICS = Arrays.asList(2, 8, 11, 17, 18, 26, 30, 33, 38, 40, 46, 47, 50, 57, 59, 61, 62, 66, 67, 78, 82, 88, 89, 95, 98, 104, 105, 109, 111, 117, 119, 121, 123, 128, 131, 136, 140, 142, 147, 152, 156, 162, 168, 173, 175, 177, 182, 196, 199, 207, 213, 222, 236, 253, 254, 262, 266, 273, 286, 287, 209, 214);
	
	@SneakyThrows
	public SimpleSearcher getSimpleSearcher() {
		SimpleSearcher ret = new SimpleSearcher(index);

		if (bm25) {
			ret.setBM25(0.9f, 0.4f);
		} else if (qld) {
			ret.setQLD(1000);
		} else {
			throw new RuntimeException("eee");
		}

		return ret;
	}

	private String retrievalName() {
		if (bm25) {
			return "bm25";
		} else if (qld) {
			return "qld";
		} else {
			throw new RuntimeException("eee");
		}
	}
	
	public static CrypsorArgs parse(String[] args) {
		CrypsorArgs crypsorArgs = new CrypsorArgs();
		CmdLineParser parser = new CmdLineParser(crypsorArgs, ParserProperties.defaults().withUsageWidth(100));

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			parser.printUsage(System.err);
			System.err.println("Example: SearchCollection" + parser.printExample(OptionHandlerFilter.REQUIRED));
			return null;
		}

		return crypsorArgs;
	}

	public String getOutputFile() {
		return output +"/" + scramblingApproach +"-" + retrievalName() + "/" + topic + ".jsonl";
	}
}
