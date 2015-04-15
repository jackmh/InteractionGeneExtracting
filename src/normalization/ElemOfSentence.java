package normalization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import config.config;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;

public class ElemOfSentence {
		
	private HashMap<Integer, String> ProteinLoc = null;
	private HashMap<Integer, String> RelationWordsLoc = null;
	private HashMap<Integer, String> NegativeWordsLoc = null;
	private List<HasWord> newSimpleSentenceList = null;
	
	public ElemOfSentence() {
		ProteinLoc =  new HashMap<Integer, String>();
		RelationWordsLoc = new HashMap<Integer, String>();
		NegativeWordsLoc = new HashMap<Integer, String>();
		newSimpleSentenceList = new ArrayList<HasWord>();
		clear();
	}
	
	private void clear() {
		ProteinLoc.clear();
		RelationWordsLoc.clear();
		NegativeWordsLoc.clear();
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
			this.newSimpleSentenceList.add(wordHastype);
			// 去掉特殊字符, 例如，.-等等
			if (key.length() == 1 && config.specialCharaters.indexOf(key) != -1)
			{
				index += 1;
				continue;
			}
			// 先判断此词是否为否定词，若是则记录位置和否定词信息
			if (negativeWordsSet.contains(key)) {
				this.NegativeWordsLoc.put(index+1, word);
			}
			// 确定句中的关系词位置和关系词
			else if (relationWordsSet.contains(key)) {
				this.RelationWordsLoc.put(index+1, word);
				
			}
			// 找出句中的基因
			else if (allGeneSet.contains(word)) {
				this.ProteinLoc.put(index+1, word);
			}
			index += 1;
		}
	}
	
	public List<HasWord> getNewSentenceList() {
		return newSimpleSentenceList;
	}

	public void setNewSentenceList(List<HasWord> newSentenceList) {
		this.newSimpleSentenceList = newSentenceList;
	}
	
	/**
	 * Print all recognized Protein-OfficialGene
     * Format as followed：
     * 		location1: RecognizedProtein_1 -> Gene_1 || location2: RecognizedProtein_2 -> Gene_2
     */
	public String getStringOfRecognitionProtein() {
		String recognitionStr = "";
		Set<Integer> keySet = this.ProteinLoc.keySet();
		List<Integer> keyList = new ArrayList<>(keySet);
		Collections.sort(keyList);
		
		int firstFlag = 1;
		for (Integer key : keyList) {
			String GeneValue = ProteinLoc.get(key);
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
	
	/**
	 * Print all the recognition words and the corresponding location.
	 */
	public String getStringOfRelationWords() {
		String relationStr = "";

		Set<Integer> keySet = RelationWordsLoc.keySet();
		List<Integer> keyList = new ArrayList<Integer>(keySet);
		Collections.sort(keyList);

		int firstFlag = 1;
		for (int key : keyList) {
			String value = RelationWordsLoc.get(key);
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
	
	/**
	 * Print All Negative words, as the following format:
	 * Location1: NegativeWords1 || Location2: NegativeWords2
	 * @return
	 */
	public String getStringOfNegativeWords() {

		String relationStr = "";

		Set<Integer> keySet = NegativeWordsLoc.keySet();
		List<Integer> keyList = new ArrayList<Integer>(keySet);
		Collections.sort(keyList);

		int firstFlag = 1;
		for (Integer key : keyList) {
			String value = NegativeWordsLoc.get(key);
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
	
	/**
	 * 给定基因名字，输出其对应的位置
	 * 
	 * @param protein
	 * @return
	 */
	public int getLocationOfRecognitionProtein(String protein) {
		Set<Integer> keySet = ProteinLoc.keySet();
		List<Integer> keyList = new ArrayList<Integer>(keySet);
		Collections.sort(keyList);

		for (int key : keyList) {
			String value = ProteinLoc.get(key);
			if (protein.compareTo(value) == 0) {
				return key;
			}
		}
		return -1;
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
	
	/**
	 * False recognition proteins.
	 * Delete the protein string that hasn't corresponding GeneID.
	 * @param location
	 */
	public void DeleteElemOfProteins(int location) {
		if (!ProteinLoc.isEmpty() && ProteinLoc.containsKey(location)) {
			ProteinLoc.remove(location);
		}
	}
	
	public void AddElementOfSentence(int location, String ElemStr, int type) {
		if (type > 3 || type < 0 || location < 0) {
			throw new IllegalArgumentException("Error type in ElemOfSentence.");
		}
		switch (type) {
		case 0:
			if (!ProteinLoc.containsKey(location)) {
				ProteinLoc.put(location, ElemStr);
			}
			break;
		case 1:
			if (!RelationWordsLoc.containsKey(location)) {
				RelationWordsLoc.put(location, ElemStr);
			}
			break;
		case 2:
			if (!NegativeWordsLoc.containsKey(location)) {
				NegativeWordsLoc.put(location, ElemStr);
			}
			break;
		default:
			break;
		}
	}
	
	public int GetNumberOfRecognizedProtein() {
		return this.ProteinLoc.size();
	}
	
	public int GetNumberOfRecognizedRelationWords() {
		return this.RelationWordsLoc.size();
	}
	
	public int GetNumberOfRecognizedNegativeWords() {
		return this.NegativeWordsLoc.size();
	}
		
	public HashMap<Integer, String> getProteinLoc() {
		return ProteinLoc;
	}
	
//	public void setProteinLoc(HashMap<Integer, String> proteinLoc) {
//		this.proteinLoc = proteinLoc;
//	}
	
	public HashMap<Integer, String> getRelationWordsLoc() {
		return RelationWordsLoc;
	}
	
//	public void setRelationWordsLoc(HashMap<Integer, String> relationWordsLoc) {
//		this.relationWordsLoc = relationWordsLoc;
//	}
	
	public HashMap<Integer, String> getNegativeWordsLoc() {
		return NegativeWordsLoc;
	}
	
//	public void setNegativeWordsLoc(HashMap<Integer, String> negativeWordsLoc) {
//		this.negativeWordsLoc = negativeWordsLoc;
//	}
}
