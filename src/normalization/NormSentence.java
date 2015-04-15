package normalization;

import java.util.HashMap;
import java.util.Map;

import config.config;
import normalization.processing.SimGeneSynonyms;
import banner.Sentence;
import banner.tagging.Mention;

public class NormSentence {
	
	/****
	 * 标准化过程中遇到问题：
	 * 		1> 当单词顺序打乱时，该如何处理。如 HSPB1： heat shock 27 kDa protein 与 pubmedID: 17342744中的 27 kDa heat shock protein
	 * 		2> 当识别出的蛋白质与官方基因关系为1--N关系时，该如何取舍
	 */
	private String PreSentText;
	// SentELem保存句子中识别出的Gene、Relation Words、Negative Words对应的位置信息
	private ElemOfSentence SentElem;
	// 保存句中对应的蛋白质-GeneOfficialGene对，去掉重复的Pairs
	private Map<String, String> ProteinStr2OfficialGene;
	
	private String NormSentText;
	private String NormTagSentText;

	public NormSentence(String PreSentText) {
		this.PreSentText = this.NormSentText = this.NormTagSentText = PreSentText;
		SentElem = new ElemOfSentence();
		ProteinStr2OfficialGene = new HashMap<String, String>();

		ProteinStr2OfficialGene.clear();
	}

	public void HPNormlization(Sentence HPNERSentence,
			HashMap<String, String> synonymDict) {
		this.PreSentText = this.NormSentText = this.NormTagSentText = HPNERSentence.getText();
		
		String newNormText = new String(this.PreSentText);
		String newTagNormText = new String(this.PreSentText);
		
		String preSubStr = "";
		String substituteSubstr = "";
		boolean flag = false;
		for (Mention geneMention : HPNERSentence.getMentions()) {
			flag = false;
			preSubStr = substituteSubstr = geneMention.getText();
			if (synonymDict.containsKey(preSubStr.toLowerCase())) {
				/**
				 * Attention: 这里可能出现多个值. (protein)1-N(Gene)
				 */
				substituteSubstr = synonymDict.get(preSubStr.toLowerCase());
                flag = true;
			} else {
				/**
				 * 如果当前识别出来的基因不在建立的字典中
				 * (key: 基因别名/基因官方名/基因全称; value: 基因官方名)
				 * 找出其相似的基因(去特殊字符、符号等)
				 */
				SimGeneSynonyms simGene = new SimGeneSynonyms(preSubStr);
				simGene.geneSynonymsDictHashMap();
				if (simGene.getSizeOfGeneSynonyms() > 0) {
					// 得到需要替换的官方基因名称， 注意这里可能有多个
					substituteSubstr = simGene.getMaxSimilarityGene();
					flag = true;
				}
			}
			if (true == flag)
			{
				// 若此蛋白质别名含有多个官方基因名称, 这里暂时取第一个
				String[] substitutestrList = substituteSubstr.split(";");
				if (substitutestrList.length > 1)
				{
					substituteSubstr = substitutestrList[1].trim();
					substituteSubstr = substituteSubstr.trim();
				}
				newNormText = newNormText.replace(preSubStr, substituteSubstr);
				newTagNormText = newTagNormText.replace(preSubStr, "<GENE> " + substituteSubstr + " </GENE>");
				
				if (!ProteinStr2OfficialGene.containsKey(preSubStr)) {
					ProteinStr2OfficialGene.put(preSubStr, substituteSubstr);
				}
			}
		}
		this.NormSentText = newNormText;
		this.NormTagSentText = newTagNormText;
	}
	
	public String GetGeneIDFromGeneOfficialName(String GeneOfficialName)
	{
		if (GeneOfficialName == null) {
			return null;
		}
		String GeneID = null;
		if (config.GeneOfficialName2GeneIDDict.containsKey(GeneOfficialName))
		{
			GeneID = config.GeneOfficialName2GeneIDDict.get(GeneOfficialName);
		}
		return GeneID;
	}

	public int GetNumberOfProtein2OfficialGene() {
		return this.ProteinStr2OfficialGene.size();
	}

	public String getPreSentText() {
		return PreSentText;
	}

	public void setPreSentText(String preSentText) {
		PreSentText = preSentText;
	}

	public String getNormSentText() {
		return NormSentText;
	}

	public void setNormSentText(String normSentText) {
		NormSentText = normSentText;
	}

	public String getNERFormatText() {
		return NormTagSentText;
	}

	public void setNERFormatText(String nERFormatText) {
		NormTagSentText = nERFormatText;
	}

	public Map<String, String> getProteinStr2OfficialGene() {
		return ProteinStr2OfficialGene;
	}

	public void setProteinStr2OfficialGene(Map<String, String> proteinStr2GeneID) {
		ProteinStr2OfficialGene = proteinStr2GeneID;
	}

	public ElemOfSentence getSentElem() {
		return SentElem;
	}

	public void setSentElem(ElemOfSentence sentElem) {
		SentElem = sentElem;
	}

}
