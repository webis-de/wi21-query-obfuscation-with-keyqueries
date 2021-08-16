package de.webis.keyqueries.generators.chatnoir;
import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringOperations {
	private static final String[] STOPWORDS_FILES = {"/stopwords-lemur", "/stopwords-tcbook", "/stopwords-web"};
	private static final Set<String> ALL_STOP_WORDS = getStopWords(null);
	
	
	
	public static String replaceSpecialChars(String text) {
		return text.replaceAll("[^A-za-z, ������]", "");
	}
	
	public static String replaceNonLetters(String text) {
		return text.replaceAll("[^A-za-z \r\n]", "");
	}
	
	public static String replaceNonFigures(String text) {
		return text.replaceAll("[^0-9]", "");
	}
	
	public static String replaceMoreThanOneSpaces(String text) {
		return text.replaceAll(" +", " ");
	}
	
	public static String[] stringToArrayBySplitting(String text, String splitsequence) {
		return text.split(splitsequence);
	}
	
	public static String removeMoreThanOneStrings(String text, String [] toRemove) {
		for(String s : toRemove) {
			text = text.replaceAll(s, "");
		}
		text = replaceMoreThanOneSpaces(text).trim();
		return text;
	}
		
	public static String removePlural(String word) {
		if(word.endsWith("_NNS")) {						
			word = word.replaceAll("_NNS", "");
			if(word.charAt(word.length()-1) == 's' || word.charAt(word.length()-1) == 'S') {
				word = word.substring(0, word.length()-1) + "_NN";
			}
		}else if(word.endsWith("_NNPS")) {
			word = word.replaceAll("_NNPS", "");
			if(word.charAt(word.length()-1) == 's' || word.charAt(word.length()-1) == 'S') {
				word = word.substring(0, word.length()-1) + "_NN";
			}
		}
		return word;
	}
	
	public static Set<String> getStopWords(String query) {
		LinkedList<String> stopwords = new LinkedList<String>();
		for(String file : STOPWORDS_FILES) {
				InputStream is = StringOperations.class.getResourceAsStream(file);
				Scanner s = new Scanner(is);
				while(s.hasNextLine()) {
					String sWord = s.nextLine();
					if (!stopwords.contains(sWord)) {
						if(query == null) {
							stopwords.add(sWord);
							continue;
						}
						
						Pattern p = Pattern.compile("\\b" + sWord + "\\b"); // \\b makes sure that "female cat" is not matched instead of "male cat"
						Matcher m = p.matcher(query);
						if (!m.find()) {
							stopwords.add(sWord);
						}
					}
				}
				s.close();
			}
		return new HashSet<>(stopwords);
	}
	
	public static String removeStopwords(String phrase) {
		for(String s : ALL_STOP_WORDS) { //remove all stopwords
			phrase = phrase.replaceAll((" " + s + " "), " "); //replaceAll((" "+ "(?i)" + s+" "), " ") would ignore capitalized letters
			phrase = removeUnexpectedCases(phrase, s);
		}
		phrase = replaceMoreThanOneSpaces(phrase).trim();
		return phrase;
	}
	
	public static boolean isCommonWords(String s) {
		s = s.trim();
		String[] commonWords = {"com", "privacy policy", "right reserved", "username password", "free online", "email address", "sitemap register", "privacy statement", "javascript view", "site map", "blog comment", "free newsletter", "ad by trafficjunky", "close embed video close", "ntilde"};
		for(String cw : commonWords) {
			if(s.contains(cw)) {
				return true;
			}
		}
		return false;
	}
	
	
	public static String removeUnexpectedCases(String phrase, String s) { //removes stopwords which are not seggregated by spaces
		phrase = phrase.replaceAll(("[^A-Za-z ]" + s + " "), " "); //Sonderzeichen - stopword- space
		phrase = phrase.replaceAll((" " + s + "[^A-Za-z ]"), " "); //space - stopword - Sonderzeichen
		phrase = phrase.replaceAll(("[^A-Za-z ]" + s + "[^A-Za-z ]"), " "); //Sonderzeichen - stopword - Sonderzeichen
		return phrase;
	}
	
	public static String removeHtmlTags(String text) {
		return text.replaceAll("\\<.*?\\>", "").trim();
	}
	
	
	
	public static String removeQuery(String text, String query) {
		query = query.replaceAll("%20", " "); 
		//text = text.replaceAll("(?i)"+query+"(?i)s", ".").replaceAll("(?i)"+query, ".");		//replaces query from webtext
		text = text.replaceAll("(?i)"+query+"(?i)s", ".");				//replaces query from webtext				((?i) means ignoring case
		text = text.replaceAll("(?i)"+query, ".");
		
		//check with words
		String[]singleWords = query.split(" ");
		boolean isContained = true;
		for(int i = 0; i < singleWords.length; i++) {			//check, whether all words of query are contained in text
			if(! text.contains(singleWords[i])) {
				isContained = false;
			}
		}
		if(isContained) {
			for(int i = 0; i < singleWords.length; i++) {	
				text = text.replaceAll(singleWords[i], ".");
			}
		}
	
		return text;
	}
	
	
	public static String[] getTextBetweenCurlyBrackets(String[] text) { 
		// get text between curly brackets from meta[1] (single results)
		if(text[1].length() > 10) {
			text[1] = text[1].replaceFirst("\\[    \\{      ", "");		
			text[1] = text[1].substring(0, text[1].length()-10);
			return text[1].split("    \\},    \\{      ");
		}
		return null;
	}
	
	
	public static String addUserQueryToText(String query, String[] metaTemp) {
		String text = "";
		text += query + " " + StringOperations.replaceNonFigures(metaTemp[1]) + " ";
		if (metaTemp[2].contains("cw09")) {
			text += "cw09";
		}
		if (metaTemp[2].contains("cw12")) {
			text += "cw12";
		}
		if (metaTemp[2].contains("cc1511")) {
			text += "cc1511";
		}
		return text;
	}
	
	
	public static String[] getArampatzisQueries() {
		return new String[]{"acute hepatitis", "advantages of vaginal birth", "alcohol interactions with cortisone", "antibiotics", "anticholinergic", "anxiety medicine", "atlanta shemale escorts", "baby names", "bartholomeus abscess medical", "blood pressure", "call girls", "cancer society", "car radar detectors", "celebrity pictures", "cheating husbands", "children who have died from moms postpartum depression", "definition of chamblee cancer", "dripping penis but no pain", "drugs and athletes", "electronic personnel folder", "erikson stages of development", "erotic stories", "find lost family burial site", "fitness models", "foods have vitamin d", "foot fetish", "free porn movies", "get access to adoption records of dead father", "getting over anorexia", "good nutrition for people with infection", "gun racks", "hacking yahoo passwords", "hardcore blowjobs", "hazardous materials", "hepatitis b vaccine safety infants", "herpes", "hot sexy singles", "how can aids be transferred", "how to humiliate someone", "how to make bombs", "how to take optygen", "how to write a will", "human factors psychology", "hydrocortisone", "illegal drugs", "infection in blood", "israel political system", "jobs with no drug testing", "lawyers for victims of child rape", "leukemia", "leukemia symptoms teens", "loan office", "loans for young farmers", "local dating", "medicine to drop blood pressure", "military car rental benefits", "military porn", "neck problem", "new life baptist church", "nurses against unions", "online music sites", "oral sex positions", "physicians role in a diabetic patient", "pictures of mary magdalene", "police scanner", "post traumatic stress", "preparing for bird flu", "psychiatric disorders", "radar detection see through walls", "radiation facilities los angeles", "rapid release of histamine", "real hot naked babes", "recent aids research", "reducing hot flashes using home remedies", "rehabs in harrisburg pa", "restaurant general manager job description", "revenge tactics", "romantic love letters", "sex toys", "strontium applicator", "surgery center fees", "symptoms of bone infection", "threesomes", "transsexual", "truck drivers salaries", "tubes tied can i get pregnant", "unemployment office", "upper respiratory resistance syndrome", "ways to make money without money", "wedding invitations", "weight gaining problems", "welfare fraud", "what does fear of success really mean", "what is a diabetic diet exchange list", "what is deep vein thrombosis",
	    };
	}
	
	
	
	public static HashMap<String, Integer> sortHashMap(HashMap<String, Integer> hashMapToSort) {
		//Sort keyqueries after their values
		List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(hashMapToSort.entrySet());
		Collections.sort(list, new Comparator<Entry<String, Integer>>() {

			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});
		hashMapToSort.clear();
		
		for(Entry<String, Integer> item: list) {
			hashMapToSort.put(item.getKey(), item.getValue());
		}
		return hashMapToSort;
	}
	
	public static String getForbidden() {
		return "https://html.spec.whatwg.org/"+"; "+"https://www.barnesandnoble.com/detail46.xml"+"; "+"https://www.barnesandnoble.com/detail70.xml"+"; "+"http://www.taunuskaefer.de/gaestebuch/gaestebuch.txt"+"; "+"http://www.taunuskaefer.de/gaestebuch/gaestebuch.txt"+"; "+"https://www.pinterest.de/AWDFFM/interior-decor-living-room/"+"; "+"http://web.mit.edu/~ecprice/Public/wordlist.ranked"+"; "+"https://www.barnesandnoble.com/detail81.xml"+"; "+"https://www.sciencedirect.com/science/article/pii/S0272735817303951"+"; "+"https://www.hollandamerica.com/en_US/faq.html" + "; "+"https://www.lowes.com/l/shipping-delivery" +"; " + "https://www.tripplite.com/smartonline-208-230v-1kva-900w-double-conversion-ups-2u-extended-run-snmp-card-option-lcd-usb-db9-energy-star~SUINT1000LCD2U" + "; " + "https://www.adidas-group.com/media/filer_public/2014/03/17/adidas_lb_2013_en.pdf" +"; "+ "https://www.dhgate.com/wholesale/car%2Breverse%2Bparking%2Bsensor%2Bsystem.html" +"; "+ "https://www.lowes.com/l/appliance-delivery-haulaway.html"+"; "+ "https://www.costco.com/car-electronics.html%3Ftype%3Dradar-detector%26refine%3Dads_f59501_ntk_cs%25253A%252522Radar%252BDetector%252522"+"; "+ "https://www.jdsports.co.uk/product/grey-calvin-klein-fleece-pants/249978/"+"; " + "https://packages.debian.org/stable/allpackages"+"; "+"https://www.techwalla.com/articles/how-to-find-a-prepaid-cell-phone-number"+"; "+"https://www.radissonhotels.com/en-us/faq"+"; "+"https://www.babycenter.com/0_precocious-early-puberty-in-girls_68661.bc"+"; "+"https://www.techwalla.com/articles/how-to-accept-collect-calls-on-a-cell-phone-for-free"+"; "+"https://www.dyson.com/register.html"+"; " +"https://www.dysoncanada.ca/content/dyson/ca/en.html"+"; "+"https://www.techwalla.com/articles/how-to-get-free-caller-id"+"; "+"https://tjmaxx.tjx.com/store/shop/womens-jeans/_/N-3838481960"+"; "+"https://www.dyson.co.uk/support/quick-links/contact-us.html"+"; "+"https://www.dyson.com/footer-primary-links/repairs-and-servicing-information.html"+"; "+"https://www.radissonhotels.com/en-us/brand/radisson"+"; ";
	}
	
	
	

}
