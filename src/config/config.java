package config;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import normalization.processing.geneToken;

public class config {
	
	public static boolean __DEBUG__ = true;
	public static boolean __WriteIntoFileFlag__ = true;
	public static boolean __ANALYSISFLAG__ = true;
	public static boolean __PUBMEDIDLISTFROMFNAME__ = true;
	
	public static int THREAD_NUM = 6;
	
	public static String BaseDIR = "./proteinPI";
	public static final String geneDictFilename = config.BaseDIR + File.separator + "dictOfAllMergeGeneProtein.name";
	public static final String pubmedIDFname = config.BaseDIR + File.separator + "pubmedIDList";
	
	public static String srcPubmedText = "/home/jack/Workspaces/expPPI/20MillionPubmedTextData";
	public static String DstDIR = "/home/jack/Workspaces/recognizedPPI";
	public static String allRecognitionPPIFname = "/home/jack/Workspaces/result/PPIOfpubmedRecognition.result";
	
	
	public static String srcDataDir = "/home/jack/Workspaces/expPPI/resultData";
	public static String pubmed2GenesIntFname = srcDataDir + File.separator + "pubmedId2Gene_201108.intm";
	
	
	/***************** USED *******************/
	public static HashSet<String> geneOfficialSet = new HashSet<String>();
	public static HashSet<String> relationKeySet = new HashSet<String>();
	
	public static HashMap<String, geneToken> geneOfficial2GeneTokenDict = new HashMap<>();
	public static HashMap<String, String> geneSynonym2OfficialDict;
	public static HashMap<String, String> genePostProcessingSynonym2OfficialDict;
	
	/*************************************************/
	private static final String dataCenterDir = "/home/jack/Workspaces/HomoSapiens";
	public static final String geneHomisapiesSummaryFname = dataCenterDir + File.separator + "gene_summary.result";
	public static final String relateionKeyFilename = dataCenterDir + File.separator + "relationKeys.name";
	
	/********************************************/
	public static String specialCharaters = new String("`~!@#$%^&*()-=_[]\\{}|;':\",./<>?");
	public static HashSet<String> stopWords = new HashSet<String>(Arrays.asList("a",
			"able", "about", "across", "after", "all", "almost", "also",
			"am", "among", "an", "and", "any", "are", "as", "at", "be",
			"because", "been", "but", "by", "can", "cannot", "could",
			"dear", "did", "do", "does", "either", "else", "ever", "every",
			"for", "from", "get", "got", "had", "has", "have", "he", "her",
			"hers", "him", "his", "how", "however", "i", "if", "in",
			"into", "is", "it", "its", "just", "least", "let", "like",
			"likely", "may", "me", "might", "most", "must", "my",
			"neither", "no", "nor", "not", "of", "off", "often", "on",
			"only", "or", "other", "our", "own", "rather", "said", "say",
			"says", "she", "should", "since", "so", "some", "than", "that",
			"the", "their", "them", "then", "there", "these", "they",
			"this", "tis", "to", "too", "twas", "us", "wants", "was", "we",
			"were", "what", "when", "where", "which", "while", "who",
			"whom", "why", "will", "with", "would", "yet", "you", "your",
			"ain't", "aren't", "can't", "could've", "couldn't", "didn't",
			"doesn't", "don't", "hasn't", "he'd", "he'll", "he's", "how'd",
			"how'll", "how's", "i'd", "i'll", "i'm", "i've", "isn't",
			"it's", "might've", "mightn't", "must've", "mustn't", "shan't",
			"she'd", "she'll", "she's", "should've", "shouldn't",
			"that'll", "that's", "there's", "they'd", "they'll", "they're",
			"they've", "wasn't", "we'd", "we'll", "we're", "weren't",
			"what'd", "what's", "when'd", "when'll", "when's", "where'd",
			"where'll", "where's", "who'd", "who'll", "who's", "why'd",
			"why'll", "why's", "won't", "would've", "wouldn't", "you'd",
			"you'll", "you're", "you've"));
}
