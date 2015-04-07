package normalization;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import normalization.processing.GeneSynonym;

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
	private Map<String, Double> simGeneSynonymsMap; 	// 相似基因、对应的相似率
	HashMap<String, String> processingSynonymDict; 		// 处理之后的同义词字典
	private double Threshold;

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

	public void geneSynonymsDictHashMap(HashMap<String, String> synonymDict) {
		if (synonymDict.size() == 0)
			throw new IllegalArgumentException("Gene Dict is null.");
		processingSynonymDict = geneSynonymPreprocessing(synonymDict);
		GeneSynonym tagGeneSynonym = new GeneSynonym(geneTag);
		tagGeneSynonym.postProcessingForOrigene(false);
		
		// 此时tagGeneSynonym.getPostGeneValueList()这里只有一个元素
		for (String newTag : tagGeneSynonym.getPostGeneValueList()) {
			newTag = newTag.trim();
			if (newTag.compareTo("") == 0)
				continue;
			// 处理完之后的字符串, 当处理完之后的字符串中存在于字典中, 则直接添加
			if (processingSynonymDict.containsKey(newTag))
			{
				String key = processingSynonymDict.get(newTag);
				simGeneSynonymsMap.put(key, 1.0);
			}
			else {
				// 否则, 遍历字典中所有字符串, 计算其相似性
				Iterator<String> iterator = processingSynonymDict.keySet().iterator();
				while (iterator.hasNext())
				{
					String key = iterator.next();
					// 比较相似性，若大于阈值则保存起来
					double similarity = similarityOfTwoString(newTag, key);
					if (similarity > Threshold) {
						simGeneSynonymsMap.put(key, similarity);
					}
				}
			}
		}
	}
	
	//
	//先做好预处理
	// 
	private HashMap<String, String> geneSynonymPreprocessing(
			HashMap<String, String> synonymDict) {
		if (synonymDict.size() == 0)
			throw new IllegalArgumentException("Gene Dict is null.");
		HashMap<String, String> processingSynonymDict = new HashMap<String, String>();
		
		Iterator<String> iterator = synonymDict.keySet().iterator();
		while (iterator.hasNext()) {
			String geneSynonym = iterator.next();
//			String geneValueOfSynonym = synonymDict.get(geneSynonym);
			GeneSynonym newGeneSynonym = new GeneSynonym(geneSynonym);
			newGeneSynonym.postProcessingForOrigene(true);
			for (String newGene : newGeneSynonym.getPostGeneValueList()) {
				newGene = newGene.trim();
				if (newGene.compareTo("") == 0 || newGene == null)
					continue;
				
				if (!processingSynonymDict.containsKey(newGene)) {
					processingSynonymDict.put(newGene, geneSynonym);
				}
				else {
					String newValueSyn = processingSynonymDict.get(newGene);
					if (newValueSyn == null)
						continue;
//---------------------------------// 需要改进
					boolean flag = isStringInArrayList(geneSynonym, newValueSyn, ";");
					if (!flag) {
						newValueSyn = newValueSyn + "; " + geneSynonym;
					}
//---------------------------------
					processingSynonymDict.remove(newGene);
					processingSynonymDict.put(newGene, newValueSyn);
				}
			}
		}
		return processingSynonymDict;
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
			System.out.println(synonymDict.get(key));
			resultStr += key + " (" + processingSynonymDict.get(key) + "), Sim: " + simGeneSynonymsMap.get(key) + "\n";
		}
		return resultStr.trim();
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
			return similarity;
		}
		return 0.0;
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
	
	private static boolean isStringInArrayList(String key, String geneSet, String delimeter)
	{
		String[] keyList = key.split(delimeter);
		String[] geneList = geneSet.split(delimeter);
		for (String geneString : geneList) {
			geneString = geneString.trim();
			if (geneString.compareTo("") == 0)
				continue;
			for(String keyGene : keyList) {
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

	public HashMap<String, String> getProcessingSynonymDict() {
		return processingSynonymDict;
	}

	public void setProcessingSynonymDict(
			HashMap<String, String> processingSynonymDict) {
		this.processingSynonymDict = processingSynonymDict;
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

	public Map<String, Double> getSimGeneSynonymsMap() {
		return simGeneSynonymsMap;
	}

	public void setSimGeneSynonymsMap(Map<String, Double> simGeneSynonymsMap) {
		this.simGeneSynonymsMap = simGeneSynonymsMap;
	}
}
