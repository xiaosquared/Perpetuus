package canon;

import java.util.HashSet;
import java.util.Iterator;

public class Canon {
	HashSet<Phrase> phrases;
	
	public Canon() {
		phrases = new HashSet<Phrase>();
	}
	
	/**
	 * 
	 * @return how many phrases are in this Canon
	 */
	public int numPhrases() {
		return phrases.size();
	}
	
	public void addPhrase(Phrase p) {
		phrases.add(p);
	}
	
	public void removephrase(Phrase p) {
		phrases.remove(p);
	}
	
	public Iterator<Phrase> getPhrases() {
		return phrases.iterator();
	}
}
