package normalization.processing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import config.config;

/**
 * Deal with gene which is recognized by BANNER, but isn't existed in gene and
 * corresponding gene synonyms database. In this case, we find the similar
 * geneSynonyms and the similarity bettween them. so, we can set a threshold.
 * when greater than similarity rate, keeping save the geneSynonyms and
 * corresponding rate.
 * 
 * @author jackmhdong
 */

public class SimGeneSynonyms {
	private String geneTag; 		// 识别出、需要标准化的基因	
	private Map<String, Double> simGeneSynonymsMap; 	// 与geneTag相似的基因、以及对应的相似率
	private double Threshold;		// 设定阈值,查找相似基因

	/**
	 *  字符串处理之后的同义词字典(无空格、特殊字符、停顿词、括号等等)
	 *  
	 *  // 对原来GeneSynonym-GeneSynonyms字典中的所有keySets进行处理
	 *  // key: 处理之前的 GeneSymbolName; Value: 处理之后的NewGeneSybolName
	 */
	
	public SimGeneSynonyms(String geneTag) {
		if (geneTag == null)
			throw new IllegalArgumentException("Text cannot be null");
		this.geneTag = geneTag;
		this.simGeneSynonymsMap = new HashMap<String, Double>();
		this.Threshold = 0.80;
	}

	public SimGeneSynonyms(String geneTag, double threshold) {
		if (geneTag == null)
			throw new IllegalArgumentException("Text cannot be null");
		this.geneTag = geneTag;
		this.simGeneSynonymsMap = new HashMap<String, Double>();
		this.Threshold = threshold;
	}

	/**
	 * 从字典synonymDict(key: 基因别名/基因官方名/基因全称; value: 基因官方名)中找出相似的官方基因
	 * processingSynonymKeyDict: 
	 * @param synonymDict
	 */
	public void geneSynonymsDictHashMap() {
		
		// 字典processingSynonymKeyDict (key:预处理之后的基因别名/全称; value:基因官方名)
		HashMap<String, String> processingSynonymKeyDict = config.genePostProcessingSynonym2OfficialDict;
		if (processingSynonymKeyDict.size() == 0)
			throw new IllegalArgumentException("Gene Dict is null.");
		
		// 预处理识别出来的基因, 这里不需要区分特殊字符 /, 则tagGeneSynonym.getPostGeneValueList()仅含一个元素
		GeneSynonym tagGeneSynonym = new GeneSynonym(geneTag);
		tagGeneSynonym.postProcessingForOrigene(false);
		
		// 此时tagGeneSynonym.getPostGeneValueList()这里只有一个元素
		for (String newTag : tagGeneSynonym.getPostGeneValueList()) {
			newTag = newTag.trim();
			if (newTag.compareTo("") == 0 || newTag.length() < 2)
				continue;
			
			String keyValue = "";
			double similarity = 1.0;
			// 处理完之后的字符串, 当处理完之后的字符串中存在于字典中, 则直接添加
			if (processingSynonymKeyDict.containsKey(newTag))
			{
				keyValue = processingSynonymKeyDict.get(newTag);
				// 若蛋白质别名与蛋白质对应的官方基因名称为1---N的关系，则需要做如下处理
				// keyvalue可能为多个官方基因名称的集合
				String[] keyValueList = keyValue.split(";");
				for (String key : keyValueList) {
					simGeneSynonymsMap.put(key, similarity);
				}
			}
			else {
				// 否则, 遍历字典中所有key, 计算其相似性
				Iterator<String> iterator = processingSynonymKeyDict.keySet().iterator();
				while (iterator.hasNext())
				{
					String key = iterator.next();
					// 比较相似性: 若大于阈值则保存起来, 相似度保留2位小数
					similarity = similarityOfTwoString(newTag, key);
					if (Double.compare(similarity, Threshold) > 0) {
						keyValue = processingSynonymKeyDict.get(key);
						// 若蛋白质别名与蛋白质对应的官方基因名称为1---N的关系，则需要做如下处理
						// keyvalue可能为多个官方基因名称的集合
						String[] keyValueList = keyValue.split(";");
						for (String keytemp : keyValueList) {
							simGeneSynonymsMap.put(keytemp, similarity);
						}					
					}
				}
			}
		}
		//sortBySimilarityForGene();
	}
	
	/**
	 *  列出所有符合给定阈值相似基因，利用如下三种原则选择一个最适合的基因:
	 *  1. 若相似基因只有一个，则选择此基因;
	 *  2. 若相似基因有多个, 选择概率最大的一个
	 *  2.1. 若相似概率最大且相同的有多个，选择在这篇文献中出现频率最多的一个
	 * @param synonymDict
	 * @return
	 */
	public String getMaxSimilarityGene()
	{
		if (simGeneSynonymsMap.size() <= 0)
		{
			System.out.println("Similarity Gene Synonyms Dictionary is null.");
			return null;
		}
		String text = "";
		Iterator<String> iterator = simGeneSynonymsMap.keySet().iterator();
		Double maxSimi = Double.MIN_VALUE;
		while (iterator.hasNext())
		{
			String key = iterator.next();
			Double value = simGeneSynonymsMap.get(key);
			if (maxSimi.compareTo(value) <= 0) {
				maxSimi = value;
				text = key;
			}
		}
		// 若有多个，选择其中之一
//		String[] simSynonymList = text.split(";");
//		text = simSynonymList[0].trim();
		return text;
	}
	
	/** 可以改为余弦相似性
	 * http://my.oschina.net/BreathL/blog/42477
	 * http://wdhdmx.iteye.com/blog/1343856
	 * 可以使用的地方：DNA分析 　　拼字检查 　　语音辨识 　　抄袭侦测
	 * @param geneOne
	 * @param geneTwo
	 * @return
	 */
	public double similarityOfTwoString(String geneOne, String geneTwo)
	{
		if (geneOne != null && geneOne.trim().length() > 0 && geneTwo != null && geneTwo.trim().length() > 0)
		{
			//计算两个字符串的长度。
			int len1 = geneOne.length();
			int len2 = geneTwo.length();
			//建立上面说的数组，比字符长度大一个空间
			int[][] dif = new int[len1 + 1][len2 + 1];
			//赋初值，步骤B。
			for (int a = 0; a <= len1; a++) {
				dif[a][0] = a;
			}
			for (int a = 0; a <= len2; a++) {
				dif[0][a] = a;
			}
			//计算两个字符是否一样，计算左上的值
			int temp;
			for (int i = 1; i <= len1; i++) {
				for (int j = 1; j <= len2; j++) {
					if (geneOne.charAt(i - 1) == geneTwo.charAt(j - 1)) {
						temp = 0;
					} else {
						temp = 1;
					}
					//取三个值中最小的
					dif[i][j] = min(dif[i - 1][j - 1] + temp, dif[i][j - 1] + 1,
							dif[i - 1][j] + 1);
				}
			}
//			System.out.println("字符串\""+geneOne+"\"与\""+geneTwo+"\"的比较");
			//取数组右下角的值，同样不同位置代表不同字符串的比较
//			System.out.println("差异步骤："+dif[len1][len2]);
			//计算相似度
			double similarity = 1 - (float) dif[len1][len2] / Math.max(geneOne.length(), geneTwo.length());
//			System.out.println("相似度："+similarity);
			BigDecimal bSim = new BigDecimal(similarity);
			double   similarity_f1   =   bSim.setScale(2,   BigDecimal.ROUND_HALF_UP).doubleValue(); 
			return similarity_f1;
		}
		return 0.0;
	}

	public String printAllSimilarityGene(HashMap<String, String> synonymDict)
	{
		if (simGeneSynonymsMap.size() < 1)
			return null;
		String resultStr = "";
		Iterator<String> iterator = simGeneSynonymsMap.keySet().iterator();
		while (iterator.hasNext())
		{
			String key = iterator.next();
			resultStr += key + " | Sim: " + simGeneSynonymsMap.get(key) + "\n";
		}
		return resultStr.trim();
	}
	
	//得到最小值
	private static int min(int... is) {
		int min = Integer.MAX_VALUE;
		for (int i : is) {
			if (min > i) {
				min = i;
			}
		}
		return min;
	}
	
	public void sortBySimilarityForGene() {
		List<Map.Entry<String, Double>> list = new ArrayList<>();
		list.addAll(simGeneSynonymsMap.entrySet());
		SimGeneSynonyms.ValueComarator vComarator = new ValueComarator();
		Collections.sort(list, vComarator);
	}

	public double getThreshold() {
		return Threshold;
	}

	public void setThreshold(double threshold) {
		Threshold = threshold;
	}

	public String getGeneTag() {
		return geneTag;
	}

	public void setGeneTag(String geneTag) {
		this.geneTag = geneTag;
	}
	
	public int getSizeOfGeneSynonyms() {
		return simGeneSynonymsMap.size();
	}

	public Map<String, Double> getSimGeneSynonymsMap() {
		return simGeneSynonymsMap;
	}

	public void setSimGeneSynonymsMap(Map<String, Double> simGeneSynonymsMap) {
		this.simGeneSynonymsMap = simGeneSynonymsMap;
	}
	
	private class ValueComarator implements Comparator<Map.Entry<String, Double>>
	{
		public int compare(Map.Entry<String, Double> m, Map.Entry<String, Double> n)
		{
			double minusValue = n.getValue() - m.getValue();
			return minusValue >= 0 ? 1 : -1;
		}
	}
}
