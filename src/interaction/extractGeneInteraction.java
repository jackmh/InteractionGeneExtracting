package interaction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import config.config;

import normalization.SentenceNM;
import normalization.processing.GeneSynonym;
import normalization.processing.SimGeneSynonyms;
import normalization.processing.geneToken;

import banner.BannerProperties;
import banner.Sentence;
import banner.processing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.Mention;
import banner.tokenization.Tokenizer;

public class extractGeneInteraction {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		loadGeneAndRelationWordsData();
		LexicalizedParser lParser = LexicalizedParser
				.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

		List<Sentence> geneTagSentenceList = geneTagger("banner.properties",
				"/home/jack/Workspaces/tarDIR/model_BC2GM.bin",
				"/home/jack/Workspaces/expPPI/20MillionPubmedTextData/17342744");
		// "/home/jack/Workspaces/tarDIR/15208391");
		String newPubmedText = "", newTextOfRecProtein = "";
		HashSet<String> interactiveProteinPairSet = new HashSet<String>();
		for (Sentence sentence : geneTagSentenceList) {
			/**
			 * 分析摘要中的每一句，这里已经标识出句子中存在的蛋白质
			 */
			// 对识别出来的蛋白质进行标准化转换，转换成官方名称
			geneOfSentence recGene = new geneOfSentence(sentence);
			SentenceNM sentNM = geneNormalization(sentence,
					config.geneSynonym2OfficialDict, recGene);
			System.out.println("\n--------------------");
			System.out.println(sentNM.getText());
			// 若一句话中识别出的蛋白质个数有2个以上, 则才对此句作进一步分析
			if (recGene.getNumberOfRecognitionGenePair() >= 2) {
				if (config.__DEBUG__ == true) {
					System.out.println("*********************  "
							+ sentNM.getMentions().size() + " ("
							+ printAllGeneMentions(sentNM.getMentions()) + ")");
				}
				System.out.println(sentNM.getNormSentence());
				interactiveProteinPairSet.clear();
				List<List<HasWord>> sentenceListWord = new LinkedList<List<HasWord>>();

				// 将每一句中的每一个单词转换成HasWord格式的List
				// 这里sentenceListWord其实只有一句
				Reader paraReader = new StringReader(sentNM.getNormSentence());
				DocumentPreprocessor dPreprocessor = new DocumentPreprocessor(
						paraReader);
				Iterator<List<HasWord>> it = dPreprocessor.iterator();
				sentenceListWord.clear();
				while (it.hasNext()) {
					List<HasWord> sentenceHasWords = it.next();
					sentenceListWord.add(sentenceHasWords);
				}

				// convert the list into string, append it into newAbstractText
				// 对每一句子使用Stanford Parser进行解析，若一句话超过40个单词，则取其前36个
				newTextOfRecProtein = "";
				int RADIXNUM = 36;
				for (List<HasWord> sent : sentenceListWord) {
					List<List<HasWord>> sentListTmp = new LinkedList<List<HasWord>>();
					/**
					 * if sentence in sentencelist and len(sentence) > 40; split
					 * this sentece into several part. 改进地方：
					 * 这里如果一个句子长度超过40个单词，则可以用截取句子包括基因、关系词在内的40个单词.
					 * (以第一个关系词/基因为开始, 最后一个基因或关系词为结束)
					 */
					if (sent.size() > RADIXNUM) {
						int k = 0, i = 0;
						int num = (sent.size() + RADIXNUM - 1) / RADIXNUM;
						while (k < num) {
							i = 0;
							List<HasWord> sentTmp = new LinkedList<HasWord>();
							while (i < RADIXNUM
									&& (i + k * RADIXNUM) < sent.size()) {
								HasWord tmpHasWord = sent.get(i + k * RADIXNUM);
								sentTmp.add(tmpHasWord);
								i += 1;
							}
							sentListTmp.add(sentTmp);
							k += 1;
						}
					} else {
						sentListTmp.add(sent);
					}

					for (List<HasWord> sentHasWords : sentListTmp) {
						/**
						 * 先识别出句子当中蛋白质、关系词、否定词的位置
						 */
						recGene.proteinRecognition(sentHasWords,
								config.geneOfficialSet, config.relationKeySet);
						newPubmedText += recGene.getNewSentenceText() + " ";
						if (config.__DEBUG__ == true) {
							System.out.println(recGene
									.getNumberOfRecognitionProteins()
									+ "\t"
									+ recGene.getNumberOfRelationWords()
									+ "\t"
									+ recGene.getNumberOfRecognitionGenePair()
									+ "\n"
									+ recGene.getOriSentenceText()
									+ "\n" + recGene.getNewSentenceText());
						}
						/**
						 * 若当前句中满足条件: 1. 所含蛋白质个数为2个以上; 2. 关系词个数超过1个. 则对此句进行句法分析
						 */
						if (recGene.getNumberOfRecognitionProteins() >= 2
								&& recGene.getNumberOfRelationWords() >= 1) {
							// newTextOfRecProtein +=
							// recGene.getOriSentenceText() +
							// recGene.getNewSentenceText() + "\n";
							getRelationParseTree geneRelationExtracTree = new getRelationParseTree(
									recGene.getNewSentenceText());
							newTextOfRecProtein = geneRelationExtracTree
									.getRelateion(lParser,
											config.geneOfficialSet,
											config.relationKeySet, recGene);
							if (geneRelationExtracTree
									.getNumberOfInteractionPair() > 0) {
								HashSet<String> sentenceInteracticePairSet = geneRelationExtracTree
										.getInteractionPairSet();
								for (String pairStr : sentenceInteracticePairSet) {
									String[] GenePair = pairStr.split("\\|");
									if (GenePair.length < 2)
										continue;
									String GeneA = GenePair[0].trim();
									String GeneB = GenePair[1].trim();
									String oppositeGenePair = GeneB + "|"
											+ GeneA;
									if (!interactiveProteinPairSet
											.contains(pairStr)
											&& !interactiveProteinPairSet
													.contains(oppositeGenePair)) {
										interactiveProteinPairSet.add(pairStr);
									}
								}
							}
						}
					}
				}
				if (interactiveProteinPairSet.size() > 0) {
					// 输出蛋白质相互作用对辅助信息
					if (config.__DEBUG__ == true) {
						System.out.println(newTextOfRecProtein);
					}
					// 输出蛋白质相互作用对
					System.out
							.println(printProteinInteractivePairs(interactiveProteinPairSet));
					System.out.println("-----------------");
				}
			}
			newPubmedText = newPubmedText.trim();
			newPubmedText += "\n";
		}
		if (config.__DEBUG__ == true) {
			System.out.println("\n--------------------------------------\n"
					+ newPubmedText
					+ "\n--------------------------------------\n");
		}
	}

	/**
	 * 将识别出的蛋白质实体进行标准化，转换成基因官方名称
	 */
	public static SentenceNM geneNormalization(Sentence geneTagSentence,
			HashMap<String, String> synonymDict, geneOfSentence recGene) {
		String preText = geneTagSentence.getText();
		SentenceNM sentence = new SentenceNM(preText);
		sentence.setMentions(geneTagSentence.getMentions());

		String newText = new String(preText);
		String substituteSubstr = "";
		for (Mention mention : geneTagSentence.getMentions()) {
			substituteSubstr = mention.getText();
			if (synonymDict.containsKey(mention.getText())) {
				// 若有多个, 这里暂时只取第一个
				substituteSubstr = synonymDict.get(mention.getText());
				String[] substitutestrList = substituteSubstr.split(";");
				substituteSubstr = substitutestrList[0].trim();
				recGene.addNewRespStr2GeneMap(mention.getText(),
						substituteSubstr);
			} else {
				/**
				 * 如果当前识别出来的基因不在建立的字典中(key: 基因别名/基因官方名/基因全称; value: 基因官方名)
				 * 找出其相似的基因(去特殊字符、符号等)
				 */
				SimGeneSynonyms simGene = new SimGeneSynonyms(mention.getText());
				simGene.geneSynonymsDictHashMap(synonymDict);
				Map<String, Double> simGeneSynonymsMap = simGene
						.getSimGeneSynonymsMap();
				if (!simGeneSynonymsMap.isEmpty()) {
					substituteSubstr = simGene
							.getMaxSimilarityGene(synonymDict);
					// 若此蛋白质别名含有多个官方基因名称
					String[] simSynonymList = substituteSubstr.split(";");
					substituteSubstr = simSynonymList[0].trim();
					recGene.addNewRespStr2GeneMap(mention.getText(),
							substituteSubstr);
				}
			}
			substituteSubstr = substituteSubstr.trim();
			newText = newText.replace(mention.getText(), substituteSubstr);
		}
		sentence.setNormSentence(newText);
		recGene.setNewSentenceText(newText);
		return sentence;
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

	/**
	 * 输出蛋白质相互作用对
	 */
	public static String printProteinInteractivePairs(
			HashSet<String> interactiveProteinPairSet) {
		String result = "";
		for (String proteinPairs : interactiveProteinPairSet) {
			result += proteinPairs + "; ";
		}
		return result.trim();
	}

	/**
	 * 输出文本中所有识别出的基因
	 */
	public static String printAllGeneMentions(List<Mention> geneMentionList) {
		String resultString = "";
		for (Mention men : geneMentionList) {
			resultString += men.getText() + "; ";
		}
		return resultString.trim();
	}
}