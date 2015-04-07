package interaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import banner.Sentence;

import config.config;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer;

/**
 * input: 摘要中的每一个句子
 * output: 将每一个句子中的蛋白质识别出来,
 * 
 *  功能说明:
 *  给定识别出来的基因序列句子， 找出对应识别出的基因位置、关系词位置、否定词位置，并简化句子结构以便解析耗时和空间小(去掉否定词、停顿词等)
 */

public class geneOfSentence {
	/********************* private variable ********************************************/

	private String oriSentenceText;
	private String newSentenceText;
	
	/**
	 * First parameter: HashMap [key: original string; values: newGene]
	 * Following: the location of ths hashmap in this sentence.
	 */
	private HashMap<String, String> respStr2GeneMap = new HashMap<String, String>();
	
	private HashMap<Integer, String> proteinMap = new HashMap<Integer, String>();
	private HashMap<Integer, String> relationWordsMap = new HashMap<Integer, String>();
	private HashMap<Integer, String> negativeWordsMap = new HashMap<Integer, String>();
	private List<HasWord> newSimpleSentenceList = new ArrayList<HasWord>();

	/******************* end private variable *****************************************/

	public geneOfSentence() {
		oriSentenceText = newSentenceText = "";
		clear();
	}

	public geneOfSentence(String sentence) {
		oriSentenceText = newSentenceText = sentence;
		clear();
	}

	public geneOfSentence(List<HasWord> oldSentenceList) {
		oriSentenceText = newSentenceText = PTBTokenizer
				.labelList2Text(oldSentenceList);
		clear();		
	}
	
	public geneOfSentence(Sentence sent)
	{
		oriSentenceText = newSentenceText = sent.getText();
		clear();
	}
	
	private void clear() {
		respStr2GeneMap.clear();
		newSimpleSentenceList.clear();
		relationWordsMap.clear();
		proteinMap.clear();
		negativeWordsMap.clear();
	}
	
	public void addNewRespStr2GeneMap(String subStr, String Gene) {
		if (!respStr2GeneMap.containsKey(subStr)) {
			respStr2GeneMap.put(subStr, Gene);
		}
	}
	
	public String printAllRecognizedGene()
	{
		String result = "";
		Iterator<String> iter = respStr2GeneMap.keySet().iterator();
		
		int FirstFlag = 1;
		while (iter.hasNext())
		{
			String key = iter.next();
			String Gene = respStr2GeneMap.get(key);
			if (FirstFlag != 1) {
				result += " || ";
			}
			result += key + " -> " + Gene;
			FirstFlag = 0;
		}
		
		return result;
	}

	/****************************************************************************************/

	/**
	 * @param sentenceList
	 *            : 句子列表，包含句子中的一个个单词
	 * @param allKeysSets
	 *            : 所有蛋白质和基因集合
	 * @param firstCharDict
	 *            : 当蛋白质全称并非一个单词时, 我们需要比较全称是否存在与句子列表中
	 * @param geneSynProteinDict
	 *            : 蛋白质和基因集合作为key, 对应的官方基因名称作为Value
	 *            从原始句子列表中识别出蛋白质、保存于字典proteinMap中，并将识别出的个数保存此中.
	 */
	/*
	 * Input: OldSentence Hasword List; Output: NewSentence (if Old_sentence ！= NewSentence
	 * and VariedWords>=2, then print this sentence.) Deal with each sentence:
	 * (At first, we should tokenized)
	 * 1. 首先对每一个句子进行分词. 对每一个单词, 先判断它是否存在于allKeysSets集合中: 
	 * 	{若存在, 则直接修改该单词, 且VariedWords+1; 
	 * 	 若不存在, 则判断firstCharDict的所有keys中是否包含的此单词:
			{	若不包含, 直接返回; 
			 	若包含, 则判断此单词后序单词是否和firstCharDict.get(firstWord)中某一个相等:
					{	若相等, 则返回当前单词以及后续相等单词的个数num, 且VariedWords+1;
						若不相等, 则直接返回1.} } } 
		2.
	 * 返回一个句子中出现两个或两个以上蛋白质的句子
	 * 
	 * curSentenceList: 已经识别出的句子序列
	 * allGeneSet: 所有基因集合
	 * relationWordsSet: 所有关系词集合
	 */

	public void proteinRecognition(List<HasWord> curSentenceList,
			HashSet<String> allGeneSet,
			HashSet<String> relationWordsSet)
	{
		newSentenceText = PTBTokenizer.labelList2Text(curSentenceList);
		int index = 0, numOfList = curSentenceList.size();

		/*************
		 * 需要改进的地方 1. 根据词性来识别出句子中的蛋白质实体 2. 利用CRF来识别蛋白质.
		 * --------------------------------> 后期改进
		 ***********************************************************************************/
		HashSet<String> negativeWordsSet = new HashSet<String>(Arrays.asList(
				"no", "not", "neither", "nor", "n't"));

		/******************************************************************************
		 * BUG1: Fixed. 进一步改进，识别出蛋白质就行，主要参数是列表，识别结束继续返回Hasword列表
		 * 注意这里需要理解Hasword和String之间是如何转换的. 
		 * 分词程序有缺陷: try this sentence: Interaction between HSPB1 and MME.
		 *****************************************************************************/
		String key, word;
		while (index < numOfList) {
			HasWord wordInSent = curSentenceList.get(index);
			word = wordInSent.word();
			key = word.toLowerCase();
			
			HasWord wordHastype = new Word(word);
			newSimpleSentenceList.add(wordHastype);
			// 去掉特殊字符, 例如，.-等等
			if (key.length() == 1 && config.specialCharaters.indexOf(key) != -1)
			{
				index += 1;
				continue;
			}
			// 先判断此词是否为否定词，若是则记录位置和否定词信息
			if (negativeWordsSet.contains(key)) {
				negativeWordsMap.put(index, word);
			}
			// 确定句中的关系词位置和关系词
			else if (relationWordsSet.contains(key)) {
				relationWordsMap.put(index + 1, word);
				
			}
			// 找出句中的基因
			else if (allGeneSet.contains(word)) {
				proteinMap.put(index+1, word);
			}
			// 去掉停顿词(and is等等)，减少解析树所需要耗费的时间和内存空间
//			else if (!config.stopWords.contains(key))
//			{
//				newSimpleSentenceList.add(wordHastype);
//			}
			index += 1;
		}
	}

	public List<HasWord> getNewSentenceList() {
		return newSimpleSentenceList;
	}

	public void setNewSentenceList(List<HasWord> newSentenceList) {
		this.newSimpleSentenceList = newSentenceList;
	}
	
	public HashMap<Integer, String> getProteinMap() {
		return proteinMap;
	}

	public HashMap<String, String> setProtein(String oldPhrases, String newGene) {
		HashMap<String, String> newProtein = new HashMap<String, String>();
		newProtein.put(oldPhrases, newGene);
		return newProtein;
	}

	// 添加识别出来的蛋白质, 保存蛋白质-Gene对以及其在句子中的位置
//	public void addProteinMap(int index, String newProteinMap) {
//		if (!proteinMap.containsKey(newProteinMap)) {
//			proteinMap.put(Integer.valueOf(index) + 1, newProteinMap);
//		}
//	}

	public HashMap<Integer, String> getRelationWordsMap() {
		return relationWordsMap;
	}

	public void setRelationWordsMap(HashMap<Integer, String> relationWordsMap) {
		this.relationWordsMap = relationWordsMap;
	}

	public HashMap<Integer, String> getNegativeWordsMap() {
		return negativeWordsMap;
	}

	public void setNegativeWordsMap(HashMap<Integer, String> negativeWordsMap) {
		this.negativeWordsMap = negativeWordsMap;
	}

	public void setProteinMap(HashMap<Integer, String> proteinMap) {
		this.proteinMap = proteinMap;
	}
	
	public String getOriSentenceText() {
		return oriSentenceText;
	}

	public void setOriSentenceText(String oriSentenceText) {
		this.oriSentenceText = oriSentenceText;
	}
	
	public String getNewSentenceText() {
		return newSentenceText;
	}

	public void setNewSentenceText(String newSentenceText) {
		this.newSentenceText = newSentenceText;
	}

	/**
	 * Get the index of specified words in a Hashmap with <int, string>
	 */
	public int getLocationOfSpecifiedWords(
			HashMap<Integer, String> IntStrWordsMap, String words) {
		Set<Integer> keySet = IntStrWordsMap.keySet();
		List<Integer> keyList = new ArrayList<Integer>(keySet);
		Collections.sort(keyList);

		for (Integer key : keyList) {
			String value = IntStrWordsMap.get(key);
			if (words.compareTo(value) == 0) {
				return key; // the location in the array
			}
		}
		return -1;
	}

	/******************************************************************************************************/
	/**
	 * Get the negative words number in current line, and Print all negative
	 * words in current line if needed.
	 */
	public int getNumberOfNegativeWords() {
		return negativeWordsMap.size();
	}

	public String getStringOfNegativeWords() {

		String relationStr = "";

		Set<Integer> keySet = negativeWordsMap.keySet();
		List<Integer> keyList = new ArrayList<Integer>(keySet);
		Collections.sort(keyList);

		int firstFlag = 1;
		for (Integer key : keyList) {
			String value = negativeWordsMap.get(key);
			if (firstFlag == 1) {
				relationStr = key + ": ";
				firstFlag = 0;
			} else {
				relationStr += " || " + key + ": ";
			}
			relationStr += value;
		}
		return relationStr.trim();
	}

	/******************************************************************************************************/

	/******************************************************************************************************/
	/**
	 * Get the location of Given HasWord or String.
	 */
	public int getLocationOfAHasWord(HasWord word) {
		int index = 0;
		for (HasWord aWord : newSimpleSentenceList) {
			if (word.equals(aWord)) {
				break;
			}
			index += 1;
		}
		return index;
	}

	public int getLocationOfAString(String str) {
		int index = 0;
		Word wordSample = new Word(str);
		for (HasWord aWord : newSimpleSentenceList) {
			if (wordSample.equals(aWord)) {
				break;
			}
			index += 1;
		}
		return index;
	}

	public String getStringFromLocation(int index) {
		if (index >= 0) {
			return newSimpleSentenceList.get(index).word();
		}
		return null;
	}

	/******************************************************************************************************/

	/************************* 关系词 *********************************************************************/
	/**
	 * Get the number of relation words in current line.
	 */
	public int getNumberOfRelationWords() {
		return relationWordsMap.size();
	}

	/**
	 * Print all the recognition words and the corresponding location in current
	 * line.
	 */
	public String getStringOfRelationWords() {
		String relationStr = "";

		Set<Integer> keySet = relationWordsMap.keySet();
		List<Integer> keyList = new ArrayList<Integer>(keySet);
		Collections.sort(keyList);

		int firstFlag = 1;
		for (int key : keyList) {
			String value = relationWordsMap.get(key);
			if (firstFlag == 1) {
				relationStr = key + ": ";
				firstFlag = 0;
			} else {
				relationStr += " || " + key + ": ";
			}
			relationStr += value;
		}
		return relationStr.trim();
	}

	/******************************************************************************************************/

	/******************************************************************************************************/
	/**
	 * Get the recognition Gene in current line.
	 */
	public int getNumberOfRecognitionProteins() {
		return proteinMap.size();
	}
	
	public int getNumberOfRecognitionGenePair() {
		return respStr2GeneMap.size();
	}

	/**
	 * 给定基因名字，输出其对应的位置
	 * 
	 * @param protein
	 * @return
	 */
	public int getLocationOfRecognitionProtein(String protein) {
		Set<Integer> keySet = proteinMap.keySet();
		List<Integer> keyList = new ArrayList<Integer>(keySet);
		Collections.sort(keyList);

		for (int key : keyList) {
			String value = proteinMap.get(key);
			if (protein.compareTo(value) == 0) {
				return key;
			}
		}
		return -1;
	}
	
	/**
     * 输出识别出的基因，官方基因
     * 格式如下：
     * 		位置1: 识别出的基因1 -> 标准化的基因1 || 位置2: 识别出的基因2 -> 标准化的基因2
     */
	public String getStringOfRecognitionProtein() {
		String recognitionStr = "";
		Set<Integer> keySet = proteinMap.keySet();
		List<Integer> keyList = new ArrayList<>(keySet);
		Collections.sort(keyList);
		
		int firstFlag = 1;
		for (Integer key : keyList) {
			String GeneValue = proteinMap.get(key);
			if (1 == firstFlag) {
				recognitionStr = key + ": ";
				firstFlag = 0;
			}
			else {
				recognitionStr += " || " + key + ": ";
			}
			recognitionStr += GeneValue;
		}
		return recognitionStr;
	}
	
	/******************************************************************************************************/

	/*
	 * 句子边界的启发式检测算法： （1）在.?!(和可能的;:-)出现位置之后加一个假设的句子边界。
	 * （2）如果假设边界后面有引号，那么把假设边界移到引号后面。 （3）除去以下情况中句点的边界资格：
	 * -如果在句点之前是一个不总出现在句子末尾的众所周知的缩写形式，而且通常后面会跟一 个大写的名字，例如Prof.或者vs.。
	 * -如果句点前面是一个众所周知的缩写形式，但是句点后面没有大写词。这样即可正确地处
	 * 理像etc.或者Jr.这样的大多数缩写用法，这些缩写一般出现在句子的中间或者末尾。 （4）如果下面的条件成立，则除去?或者!的边界资格：
	 * -这些符号后面跟着一个小写字母（或者一个已知名字）。 （5）认为其他假设边界就是句子的边界。
	 * 
	 * 检测句子边界可以看出是一个分类问题。
	 */
}
