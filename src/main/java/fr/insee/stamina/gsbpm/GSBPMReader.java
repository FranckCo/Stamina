package fr.insee.stamina.gsbpm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class GSBPMReader {

	private static final Logger logger = LogManager.getLogger(GSBPMReader.class);

	public GSBPMReader() {}

	public List<GSBPMEntry> read(File file) throws IOException {

		BufferedReader gsbpmInput  = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		String gsbpmLine;
		StringBuilder currentNote = new StringBuilder();
		GSBPMEntry currentEntry = null;

		List<GSBPMEntry> GSBPMEntries = new ArrayList<>();

		while ((gsbpmLine = gsbpmInput.readLine()) != null) {

			String trimmedLine = gsbpmLine.trim();
			if ((trimmedLine.length() == 0)) continue;

			if (Character.isDigit(trimmedLine.charAt(0))) {
				// First entry line contains the code and label
				String[] codeLabel = trimmedLine.split("\t");
				if (codeLabel.length != 2) {
					logger.error("Invalid code/label line: " + trimmedLine);
					break;
				}
				// Save current entry to the list
				if (currentEntry != null) {
					logger.debug("Adding new entry with code " + currentEntry.getCode() + " to the list");
					currentEntry.setDescription(currentNote.toString());
					currentNote = new StringBuilder();
					GSBPMEntries.add(currentEntry);
				}
				// Start new entry
				currentEntry = new GSBPMEntry(codeLabel[0], codeLabel[1]);
				logger.debug("Starting new entry with code " + currentEntry.getCode());
			} else {
				// Current line is added to the entry description
				currentNote.append(trimmedLine).append("\n");
			}
		}
		gsbpmInput.close();

		// Save last entry to the list
		if (currentEntry != null) {
			logger.debug("Adding last entry with code " + currentEntry.getCode() + " to the list");
			currentEntry.setDescription(currentNote.toString());
			GSBPMEntries.add(currentEntry);
		}

		return GSBPMEntries;
	}
}
