package normalization.processing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import config.config;

/*
 * @author mhdong
 * 		Dictionary (add with corresponding protein to gene) Dictionary
 *         and Entity Rules: Exact String Matching: 
		1> Normalization of case. 
		2> Removal of parenthesized materials. --- Interleukin(IL)-1 beta ---> Interleukin-1 beta
		3> Replacement of hyphens, punctuation(semicolon, colon, comma) with spaces.
			--- protease, cyseine 1 ---> protease cyseine 1 --- PLK-1 or PLK_1 ---> PLK 1
		4> Replacement of Arabic numbers to Roman numerals. --- MKK 2 ---> MKK II
		5> Removal of extral words. --- human c-myb gene ---> c-myb
		6> Removal of stop words. ---Homolog of zyg-11 ---> Homolog zyg-11
		7> Removal of all spaces.
		
		Deal with string with punctuation of "/": 
			1> serine/threonine-protein kinase SBK2 ---> serine kinase SBK2 ---> threonine-protein kinase SBK2
			2> alkaline phosphatase, liver/bone/kidney
			---> alkaline phosphatase, liver ---> alkaline phosphatase, bone ---> alkaline phosphatase, kidney
			3> alpha-1-microglobulin/bikunin precursor ---> alpha-1-microglobulin precursor ---> bikunin precursor
			4> cell adhesion molecule-related/down-regulated by oncogenes
			---> cell adhesion molecule-related by oncogenes ---> cell adhesion down-regulated by oncogenes
			5> heart/skeletal muscle ATP/ADP translocator
			---> heart muscle ATP translocator ---> heart muscle ADP translocator
			---> skeletal muscle ATP translocator ---> skeletal muscle ADP translocator
			6> phosphotyrosine-binding/-interacting domain (PTB)-bearing protein
			---> phosphotyrosine-binding domain (PTB)-bearing protein
			---> phosphotyrosine-interacting domain (PTB)-bearing protein
			7> U4/U6.U5 tri-snRNP-associated protein 3
			---> U4 tri-snRNP-associated protein 3 ---> U6.U5 tri-snRNP-associated protein 3
			8> CDKN2A/B ---> CDKN2A | CDKN2B
			9> IKK1/2 ---> IKK1 | IKK2
			10> IkappaBalpha/beta ---> IkappaBalpha | IkappaBbeta
			11> affixin/beta-parvin ---> affixin | beta-parvin
 */

public class GeneSynonym {
	private String oriGeneSynonym;
	private List<String> postGeneSynonymValueList;

	public GeneSynonym(String oriGeneSynonym) {
		if (oriGeneSynonym == null)
            throw new IllegalArgumentException();
		this.oriGeneSynonym = oriGeneSynonym;
		postGeneSynonymValueList = new ArrayList<>();
	}

	public void postProcessingForOrigene(boolean separatedPunctuation) {
		/**
		 * 1. 标准化字符串(大写->小写), 去括号(去掉字符串中包含()的子串)
		 * 2. 以/为分界，划分字符串 -> 字符串数组(postGeneSynonymValueList)
		 * 3. 删去停顿词, 冗余词
		 * 4. 替换阿拉伯字符为罗马字符, 将特殊符号替换为空格
		 * 5. 去除字符之间的所有空格
		 */
		String postGeneValue = RemovalOfParenthesizedMaterials(oriGeneSynonym
				.toLowerCase());
		if (separatedPunctuation == false) {
			postGeneSynonymValueList.add(postGeneValue);
		}
		else {
			SeparatedSpeciedPunctuation(postGeneValue);
		}
		List<String> newGeneValueList = new ArrayList<>();
		for (int index = 0; index < postGeneSynonymValueList.size(); index++) {
			String curGeneValue = postGeneSynonymValueList.get(index);
			postGeneValue = RemovalOfStopWords(curGeneValue);
			if (true == isStringDigit(postGeneValue))
				continue;
			postGeneValue = ReplacementHyphensOrPunctuationWithSpace(postGeneValue.toLowerCase());
			postGeneValue = RemovalOfExtralWords(postGeneValue);
			if (true == isStringDigit(postGeneValue))
				continue;
			postGeneValue = ReplaceArabicNumbersToRomanNumber(postGeneValue);
			postGeneValue = RemovalOfAllSpaces(postGeneValue);
			newGeneValueList.add(postGeneValue);
		}
		postGeneSynonymValueList = newGeneValueList;
	}
	/**
	 * 1> Normalization of case.
	 * 2> Removal of parenthesized materials. () or [] or {} ---
	 * Interleukin(IL(test))-1 beta ---> Interleukin-1 beta Interleukin(IL)-1
	 * beta(alpha) test ---> Interleukin-1 beta test
	 * Attention: 若括号不对称，需要进一步处理
	 */
	private String RemovalOfParenthesizedMaterials(String geneMaterials) {
		// String leftParentheses = new String("([{");
		// String rightParentheses = new String(")]}");
		int klen = 0, kFirstIndex = 0;
		int kBegin = 0, kEnd = 1;
		String newGeneMaterials = "";
		// "abc(te (def(ex)))-1 gjkjkl(ex)-2 beta"
		for (int current = 0; current < geneMaterials.length(); current++) {
			char ch = geneMaterials.charAt(current);
			if (ch == '(') {
				klen += 1;
				if (kFirstIndex == 0) {
					kEnd = current;
				}
			} else if (ch == ')') {
				klen -= 1;
				if (klen == 0) {
					kBegin = current + 1;
					kFirstIndex = 0;
				}
			}
			if (kBegin <= kEnd && klen == 1 && kFirstIndex == 0) {
				newGeneMaterials += geneMaterials.substring(kBegin, kEnd);
				kFirstIndex = -1;
			}

			if (kBegin < current && current >= geneMaterials.length() - 1) {
				newGeneMaterials += geneMaterials
						.substring(kBegin, current + 1);
			}
		}
		return newGeneMaterials.trim();
	}

	/**
	 * 3> Replacement of hyphens, punctuation(semicolon, colon, comma) (see as
	 * follow) with spaces. ## protease, cyseine 1, PLK-1 ---> protease cyseine 1
	 * PLK 1
	 */
	private String ReplacementHyphensOrPunctuationWithSpace(String geneMaterials) {
		String newGeneMaterials = "";
		for (int current = 0; current < geneMaterials.length(); current++) {
			if (isPunctuation(geneMaterials.charAt(current))) {
				newGeneMaterials += " ";
				continue;
			}
			newGeneMaterials += geneMaterials.substring(current, current + 1);
		}
		return newGeneMaterials.trim();
	}

	/**
	 * 4> Replacement of Arabic numbers to Roman numerals. --- MKK 2 ---> MKK II
	 */
	private String ReplaceArabicNumbersToRomanNumber(String geneMaterials) {
		String newGeneMaterials = "";
		List<String> romanNumberList = Arrays.asList("0", "Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ",
				"Ⅴ", "Ⅵ", "Ⅶ", "Ⅷ", "Ⅸ", "X", "Ⅺ", "Ⅻ"); // from 1-12
		for (int current = 0; current < geneMaterials.length(); current++) {
			if (Character.isDigit(geneMaterials.charAt(current))) {
				int index = Integer.parseInt(String.valueOf(geneMaterials
						.charAt(current)));
				if ((current + 1 < geneMaterials.length())
						&& Character.isDigit(geneMaterials.charAt(current + 1))) {
					int curIndex = Integer.parseInt(String
							.valueOf(geneMaterials.substring(current,
									current + 2)));
					if (curIndex <= 12) {
						index = curIndex;
						current += 1;
					}
				}
				newGeneMaterials += romanNumberList.get(index);
				continue;
			}
			newGeneMaterials += geneMaterials.substring(current, current + 1);
		}
		return newGeneMaterials.trim();
	}

	/**
	 * 5> Removal of stop words. --- Homolog of zyg-11 ---> Homolog zyg-11
	 */
	public String RemovalOfStopWords(String geneMaterials) {
		HashSet<String> stopWords = config.stopWords;
		String newGeneMaterials = removalOfSpecialWords(geneMaterials,
				stopWords);
		return newGeneMaterials.trim();
	}

	/**
	 * 6> Removal of extral words. --- human c-myb gene ---> c-myb
	 */
	private String RemovalOfExtralWords(String geneMaterials) {
		HashSet<String> extralWords = new HashSet<String>(Arrays.asList(
				"human", "gene", "protein", "like"));
		String newGeneMaterials = removalOfSpecialWords(geneMaterials,
				extralWords);
		return newGeneMaterials.trim();
	}

	/**
	 * Delete the specialized words in oriGene String. if the specialWords set
	 * is null, just escape the extral space.
	 * 
	 * @param oriGeneStr
	 * @param specialWordsSet
	 * @return
	 */
	private String removalOfSpecialWords(String oriGeneStr,
			HashSet<String> specialWordsSet) {
		String newGeneMaterials = "";
		String[] wordsList = oriGeneStr.split(" ");
		for (String word : wordsList) {
			word = word.trim();
			if ((specialWordsSet.size() > 0 && specialWordsSet.contains(word))
					|| word.compareTo("") == 0)
				continue;
			newGeneMaterials += word + " ";
		}
		return newGeneMaterials.trim();
	}

	/**
	 * 7> Removal of all spaces.
	 */

	private String RemovalOfAllSpaces(String geneMaterials) {
		String newGeneMaterials = "";
		String[] wordsList = geneMaterials.split(" ");
		for (String word : wordsList) {
			word = word.trim();
			if (word.compareTo("") == 0)
				continue;
			newGeneMaterials += word;
		}
		return newGeneMaterials.trim();
	}

	/*
	 * Deal with string with punctuation of "/": 1> heart/skeletal muscle
	 * ATP/ADP translocator ---> heart muscle ATP translocator ---> heart muscle
	 * ADP translocator ---> skeletal muscle ATP translocator ---> skeletal
	 * muscle ADP translocator 2> phosphotyrosine-binding/-interacting domain
	 * (PTB)-bearing protein ---> phosphotyrosine-binding domain (PTB)-bearing
	 * protein ---> phosphotyrosine-interacting domain (PTB)-bearing protein 3>
	 * CDKN2A/B ---> CDKN2A | CDKN2B 4> IkappaBalpha/beta ---> IkappaBalpha |
	 * IkappaBbeta
	 */
	private void SeparatedSpeciedPunctuation(String geneMaterials) {
		// List<String> newGeneMaterialList = new ArrayList<String>();
		String[] curStrList = geneMaterials.split(" ");
		for (String curWordstr : curStrList) {
			String[] speciedWordList = curWordstr.split("/");
			if (postGeneSynonymValueList.size() == 0) {
				for (int indexK = 0; indexK < speciedWordList.length; indexK ++) {
					String curWord = speciedWordList[indexK];
					if (curWord.length() == 1 && indexK > 0) {
						String preWord = postGeneSynonymValueList
								.get(indexK - 1);
						curWord = preWord.substring(0, preWord.length() - 1)
								+ curWord;
					}
					postGeneSynonymValueList.add(curWord);
				}
			} else {
				String[] preStrList = new String[postGeneSynonymValueList
						.size()];
				for (int index = 0; index < postGeneSynonymValueList.size(); index ++) {
					preStrList[index] = postGeneSynonymValueList.get(index);
				}
				postGeneSynonymValueList.clear();
				for (String baseStr : preStrList) {
					for (int indexK = 0; indexK < speciedWordList.length; indexK++) {
						String curWord = speciedWordList[indexK];
						boolean flag = false;
						if (curWord.length() == 1 && indexK > 0) {
							String preWord = postGeneSynonymValueList
									.get(indexK - 1);
							curWord = preWord
									.substring(0, preWord.length() - 1)
									+ curWord;
							flag = true;
						}
						String newWordstr = baseStr + " " + curWord;
						if (flag == true) {
							newWordstr = curWord;
						}
						postGeneSynonymValueList.add(newWordstr);
					}
				}
			}
		}
	}
	
	private boolean isStringDigit(String str)
	{
		for (int i = 0; i < str.length(); i ++)
		{
			char ch = str.charAt(i);
			if (Character.isDigit(ch))
			{
				continue;
			}
			return false;
		}
		return true;
	}
	private boolean isPunctuation(char ch) {
		return ("`~!@#$%^&*()-=_[]\\{}|;':\",./<>?".indexOf(ch) != -1);
	}

	public String getOriGeneValue() {
		return oriGeneSynonym;
	}

	public void setOriGeneValue(String oriGeneSynonym) {
		this.oriGeneSynonym = oriGeneSynonym;
	}

	public List<String> getPostGeneValueList() {
		return postGeneSynonymValueList;
	}

	public void setPostGeneValueList(List<String> postGeneSynonymValueList) {
		this.postGeneSynonymValueList = postGeneSynonymValueList;
	}
}
