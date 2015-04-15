package normalization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import normalization.processing.GeneSynonym;
import normalization.processing.geneToken;
import config.config;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import banner.BannerProperties;
import banner.Sentence;
import banner.processing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.Mention;
import banner.tokenization.Tokenizer;

public class NERTest {

	public static void main(String[] args) throws IOException {

//		loadGeneAndRelationWordsData();
		
		long startTime = (int) System.currentTimeMillis(); 
		LexicalizedParser lParser = LexicalizedParser
				.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");
		
		List<String> pubmedList = new ArrayList<String>();
		
		// Get all file in dir.
		ArrayList<File> files = getFilesList("/home/jack/Workspaces/expPPI/pubmedtextTestData");
		System.out.println(files.size());
		String AllGeneMentions = "";
		
		for (File file : files) {
			List<Sentence> geneTagSentenceList = geneTagger("banner.properties",
					"/home/jack/Workspaces/tarDIR/model_BC2GM.bin",
					file.getAbsolutePath());
			String pubmedMentions = "";
			String newPubmedText = "";
			for (Sentence sent : geneTagSentenceList) {
				newPubmedText += sent.getSGML();
				for (Mention mention : sent.getMentions()) {
					pubmedMentions += mention.getText() + ";";
				}
			}
			String newPubmedTextPath = "/home/jack/Workspaces/expPPI/NERPubmedText" + File.separator + file.getName();
			writeStringIntoFile(newPubmedTextPath, newPubmedText);
			AllGeneMentions += file.getName() + "\t" + pubmedMentions + "\n";
		}
		String newNERFilePath = "/home/jack/Workspaces/expPPI/ProteinNERProtein.result";
		writeStringIntoFile(newNERFilePath, AllGeneMentions.trim());
		
		long endTime=System.currentTimeMillis(); //获取结束时间  毫秒
		System.out.println("程序运行时间： "+((endTime-startTime)/1000)+"秒"); 
	}
	
	public static ArrayList<File> getFilesList(Object obj) {
		File Directory = null;
		if (obj instanceof File) {
			Directory = (File) obj;
		}
		else {
			Directory = new File(obj.toString());
		}
		ArrayList<File> files = new ArrayList<File>();
		if (Directory.isFile()) {
			files.add(Directory);
		}
		else if (Directory.isDirectory()) {
			File[] fileArr = Directory.listFiles();
			for (int i = 0; i < fileArr.length; i ++) {
				File fileOneFile = fileArr[i];
				files.addAll(getFilesList(fileOneFile));
			}
		}
		return files;
	}
	
	public static void writeStringIntoFile(String filePath, String fileContent) {
		try {
			File file = new File(filePath);
			PrintStream ps = new PrintStream(new FileOutputStream(file));
			ps.println(fileContent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 函数作用: 识别出自然文本句子中的基因
	 */
	public static List<Sentence> geneTagger(String propertiesFilename,
			String modelFilename, String inputFilename) throws IOException {
		// Get the properties and create the tagger
		BannerProperties properties = BannerProperties.load(propertiesFilename);
		Tokenizer tokenizer = properties.getTokenizer();
		CRFTagger tagger = CRFTagger.load(new File(modelFilename),
				properties.getLemmatiser(), properties.getPosTagger());
		PostProcessor postProcessor = properties.getPostProcessor();
		// Get the input text
		BufferedReader inputReader = new BufferedReader(new FileReader(
				inputFilename));
		String text = "";
		String line = inputReader.readLine();
		while (line != null) {
			text += line.trim() + " ";
			line = inputReader.readLine();
		}
		inputReader.close();

		// Break the input into sentences, tag
		List<Sentence> geneTagSentenceList = new ArrayList<Sentence>();
		BreakIterator breaker = BreakIterator.getSentenceInstance();
		breaker.setText(text);
		int start = breaker.first();
		for (int end = breaker.next(); end != BreakIterator.DONE; start = end, end = breaker
				.next()) {
			String sentenceText = text.substring(start, end).trim();
			if (sentenceText.length() > 0) {
				Sentence sentence = new Sentence(null, sentenceText);
				tokenizer.tokenize(sentence);
				tagger.tag(sentence);
				if (postProcessor != null)
					postProcessor.postProcess(sentence);
				geneTagSentenceList.add(sentence);
			}
		}
		return geneTagSentenceList;
	}

	/**
	 * 输入文件 (/home/jack/Workspaces/HomoSapiens/gene_summary.result)
	 * config.geneOfficialSet: 人类所有基因官方名称 config.geneSynProtein2OfficialDict:
	 * 字典(key: 基因官方名称; value: 基因完整信息) Gene_Synonyms(List<String>),
	 * Gene_Type<String>)
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
			config.geneOfficialSet.add(geneOfficialname);
			// 基因完整信息 (建立字典<基因官方名称, 基因完整信息>)
			geneToken geneInfo = new geneToken(text, "\t");
			config.geneOfficial2GeneTokenDict.put(geneOfficialname, geneInfo);
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
