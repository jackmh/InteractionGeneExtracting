/* 
 Copyright (c) 2007 Arizona State University, Dept. of Computer Science and Dept. of Biomedical Informatics.
 This file is part of the BANNER Named Entity Recognition System, http://banner.sourceforge.net
 This software is provided under the terms of the Common Public License, version 1.0, as published by http://www.opensource.org.  For further information, see the file 'LICENSE.txt' included with this distribution.
 */

package banner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.BreakIterator;

import banner.processing.PostProcessor;
import banner.tagging.CRFTagger;
import banner.tagging.Mention;
import banner.tokenization.Tokenizer;

public class Tag
{
    /**
     * @param args
     */
    public static void main(String[] args) throws IOException
    {
        String propertiesFilename = args[0]; // banner.properties /home/jack/Workspaces/tarDIR/model_BC2GM.bin /home/jack/Workspaces/test/bc2GNtest/bc2GNtestdocs/11102511.txt /home/jack/Workspaces/tarDIR/11102511.result
        String modelFilename = args[1]; // model_BC2GM.bin  /home/jack/Workspaces/tarDIR/model.bin
        String inputFilename = args[2]; // //home/jack/Workspaces/expPPI/20MillionPubmedTextData/18329328
        String outputFilename = args[3]; // /home/jack/Workspaces/tarDIR/11102511.result

        // Get the properties and create the tagger
        BannerProperties properties = BannerProperties.load(propertiesFilename);
        Tokenizer tokenizer = properties.getTokenizer();
        CRFTagger tagger = CRFTagger.load(new File(modelFilename), properties.getLemmatiser(), properties.getPosTagger());
        PostProcessor postProcessor = properties.getPostProcessor();
        // Get the input text
        BufferedReader inputReader = new BufferedReader(new FileReader(inputFilename));
        String text = "";
        String line = inputReader.readLine();
        while (line != null)
        {
            text += line.trim() + " ";
            line = inputReader.readLine();
        }
        inputReader.close();

        // Break the input into sentences, tag and write to the output file
        PrintWriter outputWriter = new PrintWriter(new BufferedWriter(new FileWriter(outputFilename)));
        BreakIterator breaker = BreakIterator.getSentenceInstance();
        breaker.setText(text);
        int start = breaker.first();
        int k = 1;
        for (int end = breaker.next(); end != BreakIterator.DONE; start = end, end = breaker.next())
        {
            String sentenceText = text.substring(start, end).trim();
            System.out.print(k + ": ");
            if (sentenceText.length() > 0)
            {
                Sentence sentence = new Sentence(null, sentenceText);
                tokenizer.tokenize(sentence);
                tagger.tag(sentence);
                if (postProcessor != null)
                    postProcessor.postProcess(sentence);
                outputWriter.println(sentence.getSGML());
                System.out.println(sentence.getText());
                for (Mention mention : sentence.getMentions()) {
                	System.out.println(mention.getStart() + ", " + mention.getEnd() + ": " + mention.getText());
                }
            }
            k += 1;
        }
        outputWriter.close();
    }
}
