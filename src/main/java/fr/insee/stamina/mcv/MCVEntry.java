package fr.insee.stamina.mcv;

import java.net.URI;
import java.util.List;

public class MCVEntry {

	int number;
	String label;
	String definition;
	String context;
	String source;
	URI sourceURI;
	List<Integer> related;

	public MCVEntry(int number, String label) {
		super();
		this.number = number;
		this.label = label;
	}

	public void populate(List<String> chunk) {

		// System.out.println("Populating entry " + this.number + " with lines " + chunk);
		
	}
}
