package fr.insee.stamina.sims;

import java.io.File;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

public class SIMSReader {

	static String SIMS_PDF = "src/main/resources/data/SIMS-2-0-Revised-standards-November-2015-ESSC-final.pdf";

	/** The list of concept codes and labels in SIMS */
	private SortedMap<String, String> entryMap = null;

	/** The RDF model containing the SKOS concept scheme */
	Model model = null;

	private static Logger logger = LogManager.getLogger(SIMSReader.class);

	public SIMSReader() {
		entryMap = new TreeMap<String, String>();
		model = ModelFactory.createDefaultModel();
	}

	public static void main(String[] args) {

		SIMSReader reader = new SIMSReader();

		try {
			reader.extractFromPDF();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the PDF file and extracts the SIMS contents to the internal sorted map.
	 */
	public void extractFromPDF() throws IOException {

		// Reinitialize the sorted map
		entryMap = new TreeMap<String, String>();

		// Extract the SIMS contents into a list of strings
		PDDocument document = PDDocument.load(new File(SIMS_PDF));
		PDFTextStripper stripper = new PDFTextStripper();
		stripper.setStartPage(6);
		stripper.setEndPage(6);
		String text = stripper.getText(document);
		String[] lines = text.split("\r\n");
		int lineIndex = 0;
		StringBuilder currentLine = new StringBuilder();
		for (String line : lines) {
			String trimmedLine = line.trim();
			if ((lineIndex++ < 7) || (trimmedLine.length() == 0)) continue; // First 7 lines are titles
			// If line does not start with 'S.', it is a continuation of the previous line
			if (!trimmedLine.startsWith("S.")) {
				currentLine.append(" ").append(trimmedLine);
				continue;
			}
			// If line starts with 'S.', it is a new item. Store previous item if there is one.
			if (currentLine.length() > 0) {
				// The code is everything before the first space (we assume there is one)
				int codeEnd = currentLine.indexOf(" ");
				entryMap.put(currentLine.substring(0, codeEnd), currentLine.substring(codeEnd + 1));
			}
			currentLine = new StringBuilder(trimmedLine);
		}
		document.close();
	}

}
