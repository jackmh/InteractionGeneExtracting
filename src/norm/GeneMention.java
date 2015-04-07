package norm;

import java.util.Map;

public class GeneMention {
	private MentionType type;
	private int location;
	
	public String getText() {
		return type.getText();
	}
	
	public String getPreText() {
		return type.getPreText();
	}
	
	public String getType() {
		return type.getType();
	}
	
	public Map<String, Double> getSimGeneSynonymsMap() {
		return type.getSimGeneSynonymsMap();
	}
	
	
	public int getLocation() {
		return location;
	}
	
	public void setLocation(int location) {
		this.location = location;
	}
}
