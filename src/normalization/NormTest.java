package normalization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;

import config.config;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import banner.BannerProperties;
import banner.Sentence;
import banner.processing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tokenization.Tokenizer;

public class NormTest {

	public static void main(String[] args) throws IOException {

		config.loadGeneAndRelationWordsData();
		LexicalizedParser lParser = LexicalizedParser
				.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz");

		List<Sentence> geneTagSentenceList = geneTagger("banner.properties",
				"/home/jack/Workspaces/tarDIR/model_BC2GM.bin",
//				"/home/jack/Workspaces/expPPI/20MillionPubmedTextData/17342744");
		 "/home/jack/Workspaces/tarDIR/test.1734274444");
		String newPubmedText = "";
		String geneMention = "";
		for (Sentence sent : geneTagSentenceList) {
			System.out.println("-----------------------");
			newPubmedText += sent.getSGML();
			System.out.println(sent.getText());
			System.out.println(sent.getSGML());
			NormSentence normSent = new NormSentence(sent.getText());
			normSent.HPNormlization(sent, config.geneSynonym2OfficialDict);
			System.out.println(normSent.getNormSentText());
			System.out.println(normSent.getNERFormatText());
						
			System.out.println("-----------------------");
//			
//			Sent normSentence = new Sent(sent.getText());
//			geneOfSentence recGene = new geneOfSentence(sent);
//			normSentence.normalizationRecognizedProteins(sent, config.geneSynonym2OfficialDict, recGene);
//			System.out.println(normSentence.getNormText());
		}
		System.out.println("-----------------------");
		System.out.println(newPubmedText);
		System.out.println("-----------------------");
		System.out.println(geneMention);
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
}
