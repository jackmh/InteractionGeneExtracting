package norm;

import interaction.geneOfSentence;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import normalization.processing.SimGeneSynonyms;
import banner.Sentence;
import banner.tagging.Mention;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;

public class Sent {
	private String preProteinText;
	
	private String NormText;
	private List<HasWord> wordTokens;
	private List<GeneMention> mentions;
	
	public Sent(String text) {
		this.preProteinText = this.NormText = text;
	}
	
	/**
	 * 标准化识别出的蛋白质，将其标准化，转换成对应的官方基因名称
	 * geneTagSentence: 识别出蛋白质的Sentence
	 * synonymDict: 同义词词典(key: 基因别名/基因官方名/基因全称; value: 基因官方名)
	 * recGene: 
	 */
	public void normalizationRecognizedProteins(Sentence taggerSentence,
			HashMap<String, String> synonymDict, geneOfSentence recGene)
	{
		String newText = new String(this.preProteinText);
		String preProteinTexts = "", substitute_substr = "";
		for (Mention mention : taggerSentence.getMentions()) {
			preProteinTexts = substitute_substr = mention.getText();
			// 第一步，先在同义词字典中查找
			if (synonymDict.containsKey(preProteinTexts)) {
				/***
				 * 注意：这里需要防止同义词之间包含多个基因的情况，也即一个同义词对应多个基因(这里暂时取第一个)
				 */
				substitute_substr = synonymDict.get(preProteinTexts);
				String[] preProteinsRespGeneList = substitute_substr.split(";");
				substitute_substr = preProteinsRespGeneList[0].trim();
				recGene.addNewRespStr2GeneMap(preProteinTexts, substitute_substr);
			}
			/**
			 * 如果当前识别出来的基因不在建立的字典中(key: 基因别名/基因官方名/基因全称; value: 基因官方名)
			 * 找出其相似的基因(去特殊字符、符号等)
			 */
			else {
				SimGeneSynonyms simGene = new SimGeneSynonyms(preProteinTexts);
				simGene.geneSynonymsDictHashMap(synonymDict);
				Map<String, Double> simGeneSynonymsMap = simGene.getSimGeneSynonymsMap();
				if (!simGeneSynonymsMap.isEmpty()) {
					substitute_substr = simGene.getMaxSimilarityGene(synonymDict);
					// 若此蛋白质别名含有多个官方基因名称
					String[] simSynonymList = substitute_substr.split(";");
					substitute_substr = simSynonymList[0].trim();
                    recGene.addNewRespStr2GeneMap(preProteinTexts, substitute_substr);
				}
			}
			substitute_substr = substitute_substr.trim();
			newText = newText.replaceAll(preProteinTexts, substitute_substr);
		}
		this.NormText = newText;
		setWordTokens();
	}
	
	public void setWordTokens() {
		if (this.NormText == null) {
			throw new IllegalArgumentException();
		}
		Reader paraReader = new StringReader(this.NormText);
		DocumentPreprocessor dPreprocessor = new DocumentPreprocessor(paraReader);
		Iterator<List<HasWord>> it = dPreprocessor.iterator();
		this.wordTokens = new ArrayList<HasWord>();
		if (it.hasNext()) {
			this.wordTokens = it.next();
		} else {
			System.out.println("Change to wordTokens error.");
		}
	}
	
	public int getNumberOfWordTokens() {
		return this.wordTokens.size();
	}

	public List<HasWord> getWordTokens() {
		return wordTokens;
	}

	public void setWordTokens(List<HasWord> wordTokens) {
		this.wordTokens = wordTokens;
	}

	public String getPreProteinText() {
		return preProteinText;
	}

	public void setPreProteinText(String preProteinText) {
		this.preProteinText = preProteinText;
	}

	public String getNormText() {
		return NormText;
	}

	public void setNormText(String normText) {
		NormText = normText;
	}

	public List<GeneMention> getMentions() {
		return mentions;
	}

	public void setMentions(List<GeneMention> mentions) {
		this.mentions = mentions;
	}
	
	public int getNumberOfMentions() {
		return this.mentions.size();
	}
	
}
