package normalization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
		HashMap<String, geneToken> geneDict = loadGeneINFO("/home/jack/Workspaces/HomoSapiens/gene_summary.result");
		HashMap<String, String> synonymDict = geneSynonymsDictHashMap(geneDict);
		
		List<Sentence> geneTagSentenceList = geneTagger("banner.properties",
				"/home/jack/Workspaces/tarDIR/model_BC2GM.bin",
//				"/home/jack/Workspaces/tarDIR/test.17342744");
				"/home/jack/Workspaces/expPPI/20MillionPubmedTextData/18329328");
		for (Sentence sent : geneTagSentenceList) {
			//System.out.print(String.valueOf(k));
			SentenceNM sentNM = geneNormalization(sent, synonymDict);
			System.out.println("--------------");
			System.out.println(sentNM.getText());
			if (sentNM.getMentions().size() > 0) {
				System.out.println("********  " + sentNM.getMentions().size() + "(" + printAllGeneMentions(sentNM.getMentions()) + ")");
				System.out.println(sentNM.getNormSentence());
			}
			System.out.println();
		}
	}
	
	public static SentenceNM geneNormalization(Sentence geneTagSentence, HashMap<String, String> synonymDict) {
		String preText = geneTagSentence.getText();
		SentenceNM sent = new SentenceNM(preText);
		sent.setMentions(geneTagSentence.getMentions());
		
		String newText = new String(preText);
		String substituteSubstr = "";
		for (Mention mention : geneTagSentence.getMentions()) {
			substituteSubstr = mention.getText();
			if (synonymDict.containsKey(mention.getText()))
			{
				// 若有多个, 这里暂时只取第一个
				substituteSubstr = synonymDict.get(mention.getText());
				String[] substitutestrList = substituteSubstr.split(";");
				substituteSubstr = substitutestrList[0];
			}
			else {
				/**
				 *  如果当前识别出来的基因不在建立的字典中(key: 基因别名/基因官方名/基因全称; value: 基因官方名)
				 *  找出其相似的基因(去特殊字符、符号等)
				 */
				SimGeneSynonyms simGene = new SimGeneSynonyms(mention.getText());
				simGene.geneSynonymsDictHashMap(synonymDict);
				Map<String, Double> simGeneSynonymsMap = simGene.getSimGeneSynonymsMap();
				if (!simGeneSynonymsMap.isEmpty()) {
					substituteSubstr = simGene.getMaxSimilarityGene(synonymDict);
				}
//				if (simGene.printAllSimilarityGene(synonymDict) != null) {
//					System.out.println(mention.getText());
//					System.out.println(simGene.printAllSimilarityGene(synonymDict));
//				}
			}
			substituteSubstr = substituteSubstr.trim();
			newText = newText.replace(mention.getText(), substituteSubstr);
		}
		sent.setNormSentence(newText);
		return sent;
	}
	
	// 函数作用: 识别出句子中的基因
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
	 * Build a dictionary with key: gene_synonym_name to value of gene_official_name
	 * key: gene_synonym_name (<String>)
	 * Value: gene_official_name (String), a List<String> save a String split with ";"
	 * @param geneDict (key: 基因别名/基因官方名/基因全称; value: 基因官方名)
	 * @return
	 */
	public static HashMap<String, String> geneSynonymsDictHashMap(HashMap<String, geneToken> geneDict)
	{
		if (geneDict.size() == 0)
			throw new IllegalArgumentException("Gene Dict is null.");
		
		HashMap<String, String> synonymDict = new HashMap<String, String>();
		// 遍历基因字典哈希表((key: 基因官方名称; value: 基因完整信息)), 建立以基因官方名称为value, 基因别名/基因相似名为key的字典
		Iterator<String> iter = geneDict.keySet().iterator();
		while (iter.hasNext())
		{
			String officialGeneKey = iter.next();
			geneToken GeneTokenValues = geneDict.get(officialGeneKey);
			List<String> synonyms = GeneTokenValues.getGeneSynonym();
			
			for (String synKey : synonyms) {
				synKey = synKey.trim();
				if (synKey.compareTo("") == 0 || synKey.length() < 2) {
					continue;
				}
				if (!synonymDict.containsKey(synKey))
				{
					synonymDict.put(synKey, officialGeneKey);
				}
				else {
					// 若字典中已经包含此基因别名为key, 则需要把新增加的基因官方名称加入作为value
					String preValueS = synonymDict.get(synKey);
					boolean flag = isStringInArrayList(officialGeneKey, preValueS, ";");
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
	 * Get Value of Gene Information, Save as a dictionary.
	 * Key: Gene_Official
	 * Value: Gene_Token(Gene_Official<String>, Gene_ID<String>, Gene_Synonyms(List<String>), Gene_Type<String>) 
	 * @param 输入文件名 (/home/jack/Workspaces/HomoSapiens/gene_summary.result)
	 * @return 字典 (key: 基因官方名称; value: 基因完整信息)
	 * @throws IOException
	 */
	public static HashMap<String, geneToken> loadGeneINFO(String inputFilename)
			throws IOException {
		// Get the input text
		BufferedReader inputReader = new BufferedReader(new FileReader(inputFilename));
		
		String line = inputReader.readLine();
		HashMap<String, geneToken> geneDict = new HashMap<String, geneToken>();
		while (line != null) {
			String text = line.trim();
			if (text == null || text.compareTo("") == 0 || text.substring(0, 1).compareTo("#") == 0) {
				line = inputReader.readLine();
				continue;
			}
			// 基因官方名称
			String geneOfficialname = text.split("\t")[0];
			// 基因完整信息
			geneToken geneInfo = new geneToken(text, "\t");
			// 以key：基因官方名称，Value：基因完整信息建立字典
			geneDict.put(geneOfficialname, geneInfo);
			line = inputReader.readLine();
		}
		inputReader.close();
		return geneDict;
	}
	
	// 需要进一步处理, 比较字符串key中是否有重复gene在字符串geneSet中
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
	
	public static String printAllGeneMentions(List<Mention> geneMentionList) {
		String resultString = "";
		for (Mention men : geneMentionList) {
			resultString += men.getText() + "; ";
		}
		return resultString.trim();
		// System.out.print(resultString.trim());
	}
}
