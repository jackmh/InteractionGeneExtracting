package config;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import edu.stanford.nlp.io.IOUtils;
import normalization.processing.GeneSynonym;
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
	// 官方基因名称
	public static HashSet<String> geneOfficialSet = new HashSet<String>();
	// 关系词集合(转换成小写)
	public static HashSet<String> relationKeySet = new HashSet<String>();
	
	// <key: 官方基因名称 ---- value: 官方基因ID>
	public static HashMap<String, String> GeneOfficialName2GeneIDDict = new HashMap<String, String>();
	// <key: 基因官方名称 --- value: 基因完整信息>
	public static HashMap<String, geneToken> geneOfficial2GeneTokenDict = new HashMap<>();
	// <key: 蛋白质别名、同义词(全部转换成小写) --- value: 蛋白质官方基因名称>
	public static HashMap<String, String> geneSynonym2OfficialDict;
	// key:预处理之后的基因别名/全称; --- value:基因官方名
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
	
	private config() {
	}
	
	/**
	 * 输入文件 (/home/jack/Workspaces/HomoSapiens/gene_summary.result)
	 * config.geneOfficialSet: 人类所有基因官方名称
	 * config.geneSynProtein2OfficialDict: 字典(key: 基因官方名称; value: 基因完整信息)
	 * Gene_Synonyms(List<String>), Gene_Type<String>)
	 * 
	 * config.relationKeySet： 基因相互作用的关系词集合
	 */
	public static void loadGeneAndRelationWordsData() {
		/**
		 * 从文件中添加所有基因以及相对应的基因完整信息
		 */
		Iterable<String> allLines = IOUtils
				.readLines(config.geneHomisapiesSummaryFname);
		for (String line : allLines) {
			String text = line.trim();
			if (text == null || text.compareTo("") == 0
					|| text.substring(0, 1).compareTo("#") == 0)
				continue;
			// 基因官方名称
			String geneOfficialname = text.split("\t")[0];
			String geneOfficialID = text.split("\t")[1];
			config.geneOfficialSet.add(geneOfficialname);
			// 基因完整信息 (建立字典<基因官方名称, 基因完整信息>)
			geneToken geneInfo = new geneToken(text, "\t");
			config.geneOfficial2GeneTokenDict.put(geneOfficialname, geneInfo);
			config.GeneOfficialName2GeneIDDict.put(geneOfficialname, geneOfficialID);
//			config.GeneID2GeneOfficialNameDict.put(geneOfficialID, geneOfficialname);
		}
		config.geneSynonym2OfficialDict = geneSynonyms2GeneOfficial(config.geneOfficial2GeneTokenDict);
		config.genePostProcessingSynonym2OfficialDict = geneSynonymPreprocessing(config.geneSynonym2OfficialDict);

		/*
		 * Saving all Relation words set with it's low case.
		 */
		String[] arrs = null;
		Iterable<String> relationLines = IOUtils
				.readLines(config.relateionKeyFilename);
		String relationWord = "";
		Pattern pattern = Pattern.compile("\t|,");
		for (String tmpLine : relationLines) {
			arrs = pattern.split(tmpLine);
			for (String word : arrs) {
				relationWord = word.trim();
				relationWord = relationWord.toLowerCase();
				config.relationKeySet.add(relationWord);
			}
		}
	}
	
	/**
	 * 将原始基因数据集字典<String, geneToken>转换成字典<String, String> (基因/蛋白质别名 --> 基因官方名称)
	 * Build a dictionary with key: gene_synonym_name to value of
	 * gene_official_name key: gene_synonym_name (<String>) Value:
	 * gene_official_name (String), a List<String> save a String split with ";"
	 * 
	 * @param geneDict
	 *            (key: 基因别名/基因官方名/基因全称; value: 基因官方名)
	 * @return
	 */
	public static HashMap<String, String> geneSynonyms2GeneOfficial(
			HashMap<String, geneToken> geneDict) {
		if (geneDict.size() == 0)
			throw new IllegalArgumentException("Gene Dict is null.");

		HashMap<String, String> synonymDict = new HashMap<String, String>();
		// 遍历基因字典哈希表((key: 基因官方名称; value: 基因完整信息)), 建立以基因官方名称为value,
		// 基因别名/基因相似名为key的字典
		Iterator<String> iter = geneDict.keySet().iterator();
		while (iter.hasNext()) {
			String officialGeneKey = iter.next();
			geneToken GeneTokenValues = geneDict.get(officialGeneKey);
			List<String> synonyms = GeneTokenValues.getGeneSynonym();

			for (String synKey : synonyms) {
				synKey = synKey.trim();
				synKey = synKey.toLowerCase();
				if (synKey.compareTo("") == 0 || synKey.length() < 2) {
					continue;
				}
				if (!synonymDict.containsKey(synKey)) {
					synonymDict.put(synKey, officialGeneKey);
				} else {
					// 若字典中已经包含此基因别名为key, 则需要把新增加的基因官方名称加入作为value
					String preValueS = synonymDict.get(synKey);
					boolean flag = isStringInArrayList(officialGeneKey,
							preValueS, ";");
					if (!flag) {
						preValueS = preValueS + "; " + officialGeneKey;
					}
					synonymDict.remove(synKey);
					synonymDict.put(synKey, preValueS);
				}
			}
		}
		return synonymDict;
	}

	/**
	 * 预处理: 对原来(GeneSynonym--基因官方名称)字典中的所有keySets进行处理 (去符号、特殊单词...) key: 处理之前的
	 * GeneSymbolName; Value: 处理之后的NewGeneSybolName
	 * 
	 * 输入: 字典synonymDict(key: 基因别名/基因官方名/基因全称; value: 基因官方名) 输出:
	 * 字典processingSynonymKeyDict (key:预处理之后的基因别名/全称; value:基因官方名)
	 */
	private static HashMap<String, String> geneSynonymPreprocessing(
			HashMap<String, String> synonymDict) {
		if (synonymDict.size() == 0)
			throw new IllegalArgumentException("Gene Dict is null.");
		HashMap<String, String> processingSynonymKeyDict = new HashMap<String, String>();

		Iterator<String> iterator = synonymDict.keySet().iterator();
		while (iterator.hasNext()) {
			String oriGeneSynonym = iterator.next();
			String officialGeneName = synonymDict.get(oriGeneSynonym);

			GeneSynonym newGeneSynonym = new GeneSynonym(oriGeneSynonym);
			newGeneSynonym.postProcessingForOrigene(true);

			// 将预处理之后的同义基因加入到新创建的同义词字典中
			for (String newGene : newGeneSynonym.getPostGeneValueList()) {
				newGene = newGene.trim();
				if (newGene.compareTo("") == 0 || newGene == null)
					continue;

				if (!processingSynonymKeyDict.containsKey(newGene)) {
					processingSynonymKeyDict.put(newGene, officialGeneName);
				} else {
					String newOfficialValue = processingSynonymKeyDict
							.get(newGene);
					if (newOfficialValue == null)
						continue;
					// ---------------------------------
					boolean flag = isStringInArrayList(officialGeneName,
							newOfficialValue, ";");
					if (!flag) {
						newOfficialValue = newOfficialValue + "; "
								+ officialGeneName;
					}
					// ---------------------------------
					processingSynonymKeyDict.remove(newGene);
					processingSynonymKeyDict.put(newGene, newOfficialValue);
				}
			}
		}
		return processingSynonymKeyDict;
	}

	// 需要进一步处理, 比较字符串key中是否有重复gene在字符串geneSet中
	private static boolean isStringInArrayList(String key, String geneSet,
			String delimeter) {
		String[] keyList = key.split(delimeter);
		String[] geneList = geneSet.split(delimeter);
		for (String geneString : geneList) {
			geneString = geneString.trim();
			if (geneString.compareTo("") == 0)
				continue;
			for (String keyGene : keyList) {
				keyGene = keyGene.trim();
				if (keyGene.compareTo("") == 0)
					continue;
				if (keyGene.compareTo(geneString) == 0) {
					return true;
				}
			}
		}
		return false;
	}
}
