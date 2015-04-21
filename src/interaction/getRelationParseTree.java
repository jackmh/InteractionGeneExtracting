package interaction;

import config.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;

public class getRelationParseTree {
	
	private String sentence = null;
	private HashSet<String> interactionPairSet = new HashSet<String>();
	
	// Three relation extraction rules.
	
	/*
	 * PIP: 这里会出现分词系统错误, 常因词性分析不正确而出错. 这里需要添加常出现的词库
	 **/
	
	private String[] relationPPIRule = new String[]
			{
				"/^NN.*|^CD.*/=GeneA .. (/^NN.*|^CD.*/=GeneB .. /^NN.*/=Relation)",
				"/^NN.*|^CD.*/=GeneA .. (/^VB.*/=Relation .. /^NN.*|^CD.*/=GeneB)",
				"/^NN.*/=Relation .. (/^NN.*|^CD.*/=GeneA .. /^NN.*|^CD.*/=GeneB)",
				"/^NN.*|^CD.*/=GeneA .. (/^VB.*/=Verb .. (/^NN.*/=Relation .. /^NN.*|^CD.*/=GeneB))"
			};
	
	public getRelationParseTree() {
		super();
		this.sentence = "";
		interactionPairSet.clear();
	}
	
	public getRelationParseTree(String sentence) {
		super();
		this.sentence = sentence;
		interactionPairSet.clear();
	}
	
	// 区别模式匹配中加括号的区别以及括号加在某个位置的区别
	public String getRelateion(LexicalizedParser lParser,
			HashSet<String> geneSet,
			HashSet<String> relationKeySet,
			ElemOfSentence geneSent
			)
	{
		String newSentenceText = "";
		// 解析成语法树比较耗时
		Tree parseTree = lParser.apply(geneSent.getNewSentenceList());
		
		if (config.__DEBUG__ == true) {
			parseTree.pennPrint();
		}
		
		newSentenceText += "=============================================\n";
		newSentenceText += parseTree.taggedYield() + "\n";
		if (config.__DEBUG__ == true) {
			System.out.println(parseTree.taggedYield());
			System.out.println(parseTree.taggedLabeledYield());
		}
		newSentenceText += "---------------------------------------------\n";
		newSentenceText +=  geneSent.getStringOfRecognitionProtein() + " ===>> "
				+ geneSent.getStringOfRelationWords() + "\n";
		int k = 0;
		for (String RuleStr: relationPPIRule) {
			String textStr = relationExtract(parseTree, RuleStr, geneSet, relationKeySet, k, geneSent);
			newSentenceText += textStr;
			k += 1;
		}
		newSentenceText += "=============================================";
		return newSentenceText;
	}
	
	/*
	 * 这里需要进一步处理：
	 * 1. 判断A、C是否在Gene库中，B是否是关系词(访问Gene库文件、关系词库文件)
	 * 2. 判断在三种情况下PPI、IPP、PIP三种情况下中I的词性，PP之间词的个数
	 * 	  1> PIP：I的词性常为VP或NN
	 *    2> PPI/IPP情况下I的词性，PP之间词的个数
	 * 3. Negative words recognition
	 *	 two proteins, a relation keyword, a negation keyword.
			For example:
				PIP. A is not interacted with B.
				PPI. A and B is not interaction.
				IPP. not interaction A and B.
	 * */
	private String relationExtract(Tree parseTree,
				String RuleStr,
				HashSet<String> geneSet,
				HashSet<String> relationKeySet,
				int kRule,
				ElemOfSentence proteinSent
				)
	{
		String textStr = "";
		// Attention: Pay attention to the location of bracket
		TregexPattern tregrex = TregexPattern.compile(RuleStr);
		TregexMatcher mat = tregrex.matcher(parseTree);
		String GeneAStr = "", GeneBStr = "", relationStr = "", verbStr = "";
		
		while (mat.find()) {
			GeneAStr = getStrFromTregexMatcher(mat, "GeneA");
			GeneBStr = getStrFromTregexMatcher(mat, "GeneB");
			relationStr = getStrFromTregexMatcher(mat, "Relation");
			
			if (geneSet.contains(GeneAStr) &&
					geneSet.contains(GeneBStr) &&
					GeneAStr.compareTo(GeneBStr) != 0 &&
						relationKeySet.contains(relationStr.toLowerCase())
				)
			{
				boolean flag = isNegativeWordsInSentence(proteinSent, kRule, relationStr, GeneAStr, GeneBStr);
				String proteinsPair = new String(GeneAStr + "|" + GeneBStr);
				if (!interactionPairSet.contains(proteinsPair))
				{
					// False PPI
					if (flag == false)
					{
						interactionPairSet.add(proteinsPair);
						proteinsPair = GeneBStr + "|" + GeneAStr;
						interactionPairSet.add(proteinsPair);
					}
				}
				else {
					continue;
				}
				if (kRule == 3)
				{
					verbStr = getStrFromTregexMatcher(mat, "Verb");
					int verbLoc = proteinSent.getLocationOfAString(verbStr);
					int relationLoc = proteinSent.getLocationOfAString(relationStr);
					if (relationLoc - verbLoc > 3) {
						continue;
					}
				}
				textStr += "---------------------------------------------\n";
				if (flag == true)
				{
					textStr += "False ";
				}
				
				switch (kRule)
				{
					case 0: // PPI
						textStr += "PPI: " + GeneAStr + "  " + GeneBStr + "  " + relationStr;
						break;
					case 1: // PIP
						textStr +=  "PIP: " + GeneAStr + "  " + relationStr + "  " + GeneBStr;
						break;
					case 2: // IPP
						textStr +=  "IPP: " + relationStr + "  " + GeneAStr + "  " + GeneBStr;
						break;
					case 3:
						textStr += "PVIP: " + GeneAStr + " " + verbStr + " " + relationStr + "  " + GeneBStr;
						break;
					default:
						break;
				}
				
				if (flag == true) {
					textStr += " ===>> " + proteinSent.getStringOfNegativeWords();
				}
				textStr += "\n";
			}
		}
		if (config.__DEBUG__ == true && textStr != "") {
			System.out.println(textStr);
		}
		return textStr;		
	}
	
	/***************************************************************
		 * Three Rules: PIP, PPI, IPP, when there is a negative between the interation of PP
		 * 1. if PNIP (Protein .. Negative .. relation .. Protein), PINP, NPIP
		 * 		P and P is not interaction. (Negative word is between the first Protein and interaction words)
		 * 		for example:
		 * 			1> proteinA is not interacted with ProteinB.
		 *   			==>> PA I PB;  ===>> not I ===>> PA I PB is false;
		 * 			2> ProteinA and ProteinB is not interacted with ProteinC.
		 *    			==>> PA I PC; PB I PC; ===>> not I ===>> PA I PC is false; PB I PC is false;
		 *  		3> ProteinA is interacted with ProteinB, not with ProteinC.
		 *  			==>> PA I PB; PA I PC; ===>> not PC  ===>> PA I PC is false;
		 *   			===>> PA I PB
		 *  		4> ProteinA and ProteinB is interacted with ProteinC, not with ProteinD and ProteinE.
		 *  			==>> PA I PC; PA I PD; PA I PE; PB I PC; PB I PD; PB I PE;
		 *  			===>> not PD; not PE  ===>> PA I PD is false; PA I PE is false; PB I PD is false; PB I PE is false; 
		 *   			===>> PA I PC; PB I PC;
		 *   
		 * 2. if NPPI (Negative .. Protein .. relation .. Protein), PNPI, PPNI
		 * 		not P and P interaction. (Negative word is ahead of the first protein)
		 * 		for example:
		 * 			1> not ProteinA and ProteinB interaction.
		 * 			2> not ProteinA or ProteinB and ProteinC interaction.
		 * 			3> not ProteinA is interacted with ProteinB.
		 * 			4> not ProteinA is interacted with ProteinB and ProteinC.
		 *
		 * 3. if NIPP (Negative .. relation .. Protein .. Protein)
		 * 		not interaction P and P. (Negative word is ahead of the relation word)
		 * 		for example:
		 * 			1> not interaction ProteinA and ProteinB.
		 * 
		 * 4. the other situations:
		 * 		1> Neither ProteinA nor ProteinB is interacted with ProteinC.
		 * 		2> Neither ProteinA and ProteinB nor ProteinC with ProteinD is interacted with ProteinE.
		 *
	***************************************************************/
	private boolean isNegativeWordsInSentence(
			ElemOfSentence proteinSent,
			int kRule,
			String relationStr,
			String GeneAStr,
			String GeneBStr
			)
	{
		int relationLocation = proteinSent.getLocationOfSpecifiedWords(proteinSent.getRelationWordsLoc(), relationStr);
		boolean relationNegative = false;
		int GeneALocation = proteinSent.getLocationOfRecognitionProtein(GeneAStr);
		boolean geneANegative = false;
		int GeneBLocation = proteinSent.getLocationOfRecognitionProtein(GeneBStr);
		boolean geneBNegative = false;
		
		// not, no, n't
		if (proteinSent.GetNumberOfRecognizedNegativeWords() > 0) {
		
			HashMap<Integer, String> negativeWordsMap = proteinSent.getNegativeWordsLoc();
			
			Set<Integer> keySet = negativeWordsMap.keySet();
			List<Integer> keyList = new ArrayList<Integer>(keySet);
			Collections.sort(keyList);
			
			for (int NegativeLoc: keyList) {
				String value = negativeWordsMap.get(NegativeLoc);
				value = value.toLowerCase();
				if (value.compareTo("not") == 0 || value.compareTo("no") == 0 || value.compareTo("n't") == 0)
				{
					switch (kRule) {
						case 0: // PPI
							if (GeneBLocation < NegativeLoc && NegativeLoc < relationLocation) // PPNI
								relationNegative = true;
							if (GeneALocation < NegativeLoc && NegativeLoc < GeneBLocation) // PNPI
								geneBNegative = true;
							if (NegativeLoc < GeneALocation) // NPPI
								geneANegative = true;
							break;
						case 1: // PIP
						case 3: // PVIP
							if (GeneALocation < NegativeLoc && NegativeLoc < relationLocation) // PNIP or // PNVIP, PVNP
								relationNegative = true;
							if (relationLocation < NegativeLoc && NegativeLoc < GeneBLocation) // PINP or // PVINP
								geneBNegative = true;
							if (NegativeLoc < GeneALocation) // NPIP or // NPVIP
								geneANegative = true;
							break;
						case 2: // IPP
							if (NegativeLoc < relationLocation) // NIPP
								relationNegative = true;
							if (GeneALocation < NegativeLoc && NegativeLoc < GeneBLocation) // IPNP
								geneBNegative = true;
							if (relationLocation < NegativeLoc && NegativeLoc < GeneALocation) // INPP
								geneANegative = true;
							break;
						default:
							break;
					}
					return relationNegative ^ geneANegative ^ geneBNegative;
					
				} // 0:PPI; 1: PIP; 2: IPP
				else if (value.compareTo("neither") == 0)
				{
					; //deal with neither nor sentence.
				}
			}
		}
		return relationNegative ^ geneANegative ^ geneBNegative;
	}
		
	/*
	 * 从TregexMatcher中根据Tree结点名称得到相应的结点值
	 * */
	private String getStrFromTregexMatcher(TregexMatcher matcher, String nodeName) {
		Tree gene = matcher.getNode(nodeName);
		ArrayList<Word> valueList = gene.yieldWords();
		String geneNameStr = valueList.get(0).toString();
		return geneNameStr.trim();
	}

	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	public HashSet<String> getInteractionPairSet() {
		return interactionPairSet;
	}

	public int getNumberOfInteractionPair() {
		return interactionPairSet.size();
	}
}
