package interaction;

import interaction.getRelationParseTree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import config.config;
import normalization.ElemOfSentence;
import normalization.NormSentence;
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
		config.loadGeneAndRelationWordsData();
		LexicalizedParser lParser = LexicalizedParser
				.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

		List<Sentence> geneTagSentenceList = geneTagger("banner.properties", "model_BC2GM.bin",
//				"/home/jack/Workspaces/expPPI/20MillionPubmedTextData/17342744");
		 "/home/jack/Workspaces/tarDIR/test.1734274444");
		String newPubmedText = "", newTextOfRecProtein = "";
		HashSet<String> interactiveProteinPairSet = new HashSet<String>();
		for (Sentence sentence : geneTagSentenceList) {
			/**
			 * 分析摘要中的每一句，这里已经标识出句子中存在的蛋白质
			 */
			// 对识别出来的蛋白质进行标准化转换，转换成官方名称
			NormSentence normSentence = new NormSentence(sentence.getText());
			ElemOfSentence SentElem = new ElemOfSentence();
			
			normSentence.HPNormlization(sentence, config.geneSynonym2OfficialDict);
			
			System.out.println("\n--------------------");
			System.out.println(normSentence.getNormSentText());
			// 若一句话中识别出的蛋白质个数有2个以上, 则才对此句作进一步分析
			if (normSentence.GetNumberOfProtein2OfficialGene() >= 2)
				if (config.__DEBUG__ == true) {
					System.out.println("*********************  "
							+ sentence.getMentions().size() + " ("
							+ printAllGeneMentions(sentence.getMentions()) + ")");
				}
				interactiveProteinPairSet.clear();
				List<List<HasWord>> sentenceListWord = new LinkedList<List<HasWord>>();

				// 将每一句中的每一个单词转换成HasWord格式的List
				// 这里sentenceListWord其实只有一句
				Reader paraReader = new StringReader(normSentence.getNormSentText());
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

					for (List<HasWord> sentHasWords : sentListTmp)
					{
						/**
						 * 先识别出句子当中蛋白质、关系词、否定词的位置
						 */
						SentElem.proteinRecognition(sentHasWords,
								config.geneOfficialSet, config.relationKeySet);
						newPubmedText += normSentence.getNormSentText() + " ";
						if (config.__DEBUG__ == true)
						{
							System.out.println(SentElem.GetNumberOfRecognizedProtein()
									+ "\t"
									+ SentElem.GetNumberOfRecognizedRelationWords() + "\t"
									+ normSentence.GetNumberOfProtein2OfficialGene() + "\n"
									+ normSentence.getPreSentText() + "\n"
									+ normSentence.getNormSentText());
						}
						/**
						 * 若当前句中满足条件: 1. 所含蛋白质个数为2个以上; 2. 关系词个数超过1个. 则对此句进行句法分析
						 */
						if (normSentence.GetNumberOfProtein2OfficialGene() >= 2
								&& SentElem.GetNumberOfRecognizedRelationWords() >= 1)
						{
							// newTextOfRecProtein +=
							// recGene.getOriSentenceText() +
							// recGene.getNewSentenceText() + "\n";
							getRelationParseTree geneRelationExtracTree = new getRelationParseTree(normSentence.getNormSentText());
							
							newTextOfRecProtein = geneRelationExtracTree.getRelateion(lParser, config.geneOfficialSet, config.relationKeySet, SentElem);
							if (geneRelationExtracTree.getNumberOfInteractionPair() > 0)
							{
								HashSet<String> sentenceInteracticePairSet = geneRelationExtracTree.getInteractionPairSet();
								for (String pairStr : sentenceInteracticePairSet)
								{
									String[] GenePair = pairStr.split("\\|");
									if (GenePair.length < 2)
										continue;
									String GeneA = GenePair[0].trim();
									String GeneB = GenePair[1].trim();
									String oppositeGenePair = GeneB + "|"
											+ GeneA;
									if (!interactiveProteinPairSet.contains(pairStr) && !interactiveProteinPairSet.contains(oppositeGenePair)) 
									{
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
		if (config.__DEBUG__ == true) {
			System.out.println("\n--------------------------------------\n"
					+ newPubmedText
					+ "--------------------------------------\n");
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