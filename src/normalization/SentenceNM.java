package normalization;

import java.util.ArrayList;
import java.util.List;
import banner.tagging.Mention;

public class SentenceNM {
	private String text;
	private String normSentenceText;
	private List<Mention> mentions;
	
	public SentenceNM(String text)
	{
		if (text == null)
			throw new IllegalArgumentException("Text cannot be null");
		text = text.trim();
		if (text.length() == 0)
			throw new IllegalArgumentException(
					"Text must have length greater than 0");
		this.text = text;
		mentions = new ArrayList<Mention>();
	}
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}
	
	public String getNormSentence() {
		return normSentenceText;
	}

	public void setNormSentence(String normSentenceText) {
		this.normSentenceText = normSentenceText;
	}
	
	public void addMentions(Mention newMention) {
		if (newMention == null)
			throw new IllegalArgumentException("Mention cannot be null");
		mentions.add(newMention);
	}
	
	public List<Mention> getMentions() {
		return mentions;
	}

	public void setMentions(List<Mention> mentions) {
		this.mentions = mentions;
	}
}
