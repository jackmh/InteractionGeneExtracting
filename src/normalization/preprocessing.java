package normalization;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import normalization.processing.geneToken;

public class preprocessing {

	public static void main(String[] args) throws IOException {
		TreeMap<String, geneToken> geneDict = loadGeneINFO("/home/jack/Workspaces/HomoSapiens/gene_summary.result");
		TreeMap<String, String> synonymDict = new TreeMap<String, String>();
		Iterator<String> iter = geneDict.keySet().iterator();
		while (iter.hasNext())
		{
			Object officialGeneKey = iter.next();
			geneToken officialGeneValues = geneDict.get(officialGeneKey);
			List<String> synonyms = officialGeneValues.getGeneSynonym();
			for (String synKey : synonyms) {
				synKey = synKey.trim();
				if (synKey.compareTo("") == 0) {
					continue;
				}
				String synValue = (String) officialGeneKey;
				if (!synonymDict.containsKey(synKey))
				{
					synonymDict.put(synKey, synValue);
				}
				else {
					String preValueS = synonymDict.get(synKey);
					boolean flag = isStringInArrayList(synValue, preValueS, ";");
					if (!flag) {
						preValueS = preValueS + "; " + synValue;
					}
					synonymDict.remove(synKey);
					synonymDict.put(synKey, preValueS);
				}
			}
		}
	}
	
	private static boolean isStringInArrayList(String key, String geneSet, String delimeter)
	{
		String[] geneList = geneSet.split(delimeter);
		for (String geneString : geneList) {
			geneString = geneString.trim();
			if (geneString.compareTo("") == 0)
				continue;
			if (key.compareTo(geneString) == 0) {
				return true;
			}
		}
		return false;
	}
	
	public static TreeMap<String, geneToken> loadGeneINFO(String inputFilename)
			throws IOException {
		// Get the input text
		TreeMap<String, geneToken> geneDict = new TreeMap<String, geneToken>();
		BufferedReader inputReader = new BufferedReader(new FileReader(
				inputFilename));
		String line = inputReader.readLine();
		while (line != null) {
			String text = line.trim();
			if (text.compareTo("") == 0 || text.substring(0, 1).compareTo("#") == 0) {
				line = inputReader.readLine();
				continue;
			}
			String geneOfficialname = text.split("\t")[0];
			geneToken geneInfo = new geneToken(text, "\t");
			geneDict.put(geneOfficialname, geneInfo);
			line = inputReader.readLine();
		}
		inputReader.close();
		return geneDict;
	}
}
