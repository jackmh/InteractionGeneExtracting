package normalization.processing;

import java.util.ArrayList;
import java.util.List;

public class geneToken {
	/*
	 * Official Symbol Gene ID Synonyms Human Protein / Human Protein Kinase
	 * A1BG	1	alpha-1-B glycoprotein;A1BG;A1B; ABG; GAB; HYST2477;alpha-1B-glycoprotein	Human Protein
	 */
	public enum geneCategory {
		HumanProtein, HumanProteinKinase;
	}

	private String officialSymbol;
	private String geneID;
	private List<String> geneSynonym;
	private geneCategory geneType;

	public geneToken(String text, String delimeter) {
		/*
		 * Seperated with delimeter.
		 * A1BG	1	alpha-1-B glycoprotein;A1BG;A1B;ABG; GAB; HYST2477;alpha-1B-glycoprotein	Human Protein
		 */
		if (text == null)
			throw new IllegalArgumentException();
		String geneList[] = text.split(delimeter);
		
		if (geneList.length < 4)
			throw new IllegalArgumentException();
		this.officialSymbol = geneList[0].toString().trim();
		this.geneID = geneList[1].toString().trim();
		String[] geneTmpList = geneList[2].toString().split(";");
		
		geneSynonym = new ArrayList<String>();
		for (int i = 0; i < geneTmpList.length; i ++) {
			String gene = geneTmpList[i].trim();
			// Ignoring String of length shorter than 2.
			if (gene.compareTo("") == 0 || gene.length() < 2 || gene.compareTo("\n") == 0)
			{
				continue;
			}
			this.geneSynonym.add(gene);
		}
		String geneTypeValue = geneList[3].toString().trim();
		this.geneType = geneCategory.HumanProtein;
		if (geneTypeValue.compareTo("Human Protein Kinase") == 0 ) {
			this.geneType = geneCategory.HumanProteinKinase;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final geneToken other = (geneToken) obj;
		if (!this.officialSymbol.equals(other.officialSymbol))
			return false;
		if (!this.geneID.equals(other.geneID))
			return false;
		return true;
	}

	public String getOfficialSymbol() {
		return officialSymbol;
	}

	public void setOfficialSymbol(String officialSymbol) {
		this.officialSymbol = officialSymbol;
	}

	public String getGeneID() {
		return geneID;
	}

	public void setGeneID(String geneID) {
		this.geneID = geneID;
	}

	public List<String> getGeneSynonym() {
		return geneSynonym;
	}

	public void setGeneSynonym(List<String> geneSynonym) {
		this.geneSynonym = geneSynonym;
	}

	public geneCategory getGeneType() {
		return geneType;
	}

	public void setGeneType(geneCategory geneType) {
		this.geneType = geneType;
	}
}
