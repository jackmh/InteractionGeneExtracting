package norm;

import java.util.Map;

public class MentionType {
	private TagFormat type; 	// 定义为Gene 和 非基因
	private String text;		// 蛋白质标准化后的基因
	
	private String preProteinTexts;		// 原先识别出来的蛋白质
	private Map<String, Double> simGeneSynonymsMap; 	// 相似基因、对应的相似率
	
	
	public enum TagFormat
	{
		G, O; // Gene; not Gene;
	}
	
	public String getType()
	{
		if (type != TagFormat.G || type != TagFormat.O) {
			throw new IllegalArgumentException();
		}
		if(type == TagFormat.G) {
			return "G";
		}
		if (type == TagFormat.O) {
			return "O";
		}
		return null;
	}

	public void setType(String type) {
		if (type.compareTo("G") == 0 || type.compareTo("O") == 0)
		{
			throw new IllegalArgumentException();
		}
		if(0 == type.compareTo("G")) {
			this.type = TagFormat.G;	
		}
		if (0 == type.compareTo("O")) {
			this.type = TagFormat.O;
		}
	}
	
	public void setType(TagFormat type) {
		this.type = type;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getPreText() {
		return preProteinTexts;
	}

	public void setPreText(String preProteinTexts) {
		this.preProteinTexts = preProteinTexts;
	}

	public Map<String, Double> getSimGeneSynonymsMap() {
		return simGeneSynonymsMap;
	}

	public void setSimGeneSynonymsMap(Map<String, Double> simGeneSynonymsMap) {
		this.simGeneSynonymsMap = simGeneSynonymsMap;
	}
	
	
}
