package fr.insee.stamina.mcv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;


public class MCVReader {

	static String MCV_PDF = "src/main/resources/data/04_sdmx_cog_annex_4_mcv_2009.pdf";
	static String MCV_TXT = "src/main/resources/data/mcv.txt";
	static String MCV_TXT_TOC = "src/main/resources/mcv-toc.txt";
	static String PAGE_NUMBER_REGEX = "Page \\d+ of \\d+";
	static String ENTRY_START_REGEX = "^\\d+\\.\\s.+";

	static Map<Integer, String> entryList = null;
	static Map<Integer, MCVEntry> mcvEntries = null;

	private static Logger logger = LogManager.getLogger(MCVReader.class);

	public static void main(String[] args) {
		try {
			//extractFormPDF();
			readEntryList();
			readMCVContent();
			Model model = ModelFactory.createDefaultModel();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads the PDF file and extracts the content and table of contents to text files.
	 */
	public static void extractFormPDF() throws IOException {

		PDFParser parser = new PDFParser(new FileInputStream(MCV_PDF));
		parser.parse();
		
		PDDocument document = parser.getPDDocument();
		PDFTextStripper stripper = new PDFTextStripper();

		// Extract the main contents
		stripper.setStartPage(15);
		String text = stripper.getText(document);
		Files.write(Paths.get(MCV_TXT), text.getBytes());

		// Extract the table of contents
		stripper.setStartPage(3);
		stripper.setEndPage(14);
		text = stripper.getText(document);
		Files.write(Paths.get(MCV_TXT_TOC), text.getBytes());

		document.close();
	}

	/**
	 * Reads the table of contents extracted from the PDF file to create the list of all entries.
	 */
	public static void readEntryList() throws IOException {

		entryList = new HashMap<Integer, String>();

		List<String> tocLines = Files.readAllLines(Paths.get(MCV_TXT_TOC), Charset.forName("UTF-8"));

		for (String line : tocLines) {
			if ((line.length() > 0) && (Character.isDigit(line.charAt(0)))) {
				String entryLine = line.substring(0, line.indexOf("..")).trim();
				String[] entryArray = entryLine.split("\\. ");
				entryList.put(Integer.parseInt(entryArray[0]), entryArray[1]);
			}
		}
		// for (int entryNumber : entryList.keySet()) System.out.println(entryNumber + "\t'" + entryList.get(entryNumber) + "'");
		tocLines = null;
	}

	public static void readMCVContent() throws IOException {

		BufferedReader mcvInput  = new BufferedReader(new InputStreamReader(new FileInputStream(new File(MCV_TXT))));
		String mcvLine = null;
		List<String> currentChunk = new ArrayList<String>();
		MCVEntry currentEntry = null;

		while ((mcvLine = mcvInput.readLine()) != null) {

			String mcvTrimmedLine = mcvLine.trim();
			if ((mcvTrimmedLine.length() == 0) || (mcvTrimmedLine.matches(PAGE_NUMBER_REGEX))) continue;

			int entryNumber = entryStart(mcvLine);
			if (entryNumber > 0) {
				// Capture synonym entries
				int mainEntry = synomymOf(currentChunk);
				if (mainEntry > 0) {
					// Do something
					continue;
				}
				// Treat current entry
				if (currentEntry != null) currentEntry.populate(currentChunk);
				currentEntry = new MCVEntry(entryNumber, entryList.get(entryNumber));
				currentChunk = new ArrayList<String>();
			}
			else currentChunk.add(mcvLine);
		}
		// Treat last entry
		currentEntry.populate(currentChunk);
		mcvInput.close();
	}

	/**
	 * Checks if a sequence corresponds to a synonym; if so returns the number of the item referenced, otherwise 0.
	 */
	private static int synomymOf(List<String> chunk) {

		if (chunk.size() == 1) {
			String line = chunk.get(0).trim();
			if (line.startsWith("Definition: ")) line = line.substring(12); // Entry 149
			if (!line.startsWith("See ") && !line.startsWith("see ")) return 0; // Eliminates general title
			line = line.substring(4).trim();
			if (line.startsWith("\"")) line = line.substring(1, line.length() - 1);
			if (line.equals("Data Validation")) line = "Data validation"; // Deals with case 389
			if (line.equals("Originator Data identifier")) line = "Originator data identifier"; // Deals with case 91
			// Search for the referenced entry
			int found = findEntry(line);
		    if (found == 0) logger.warn("Reference term not found for synonym '" + line + "'");

		    return found;
		}
		return 0;
	}


	/**
	 * Checks if line is the title of an entry and if so returns the number; otherwise returns 0.
	 */
	private static int entryStart(String line) {

		if (!line.matches(ENTRY_START_REGEX)) return 0;
		// Divide at first point (there should be only one)
		int firstPointPosition = line.indexOf(".");
		int entryNumber = Integer.parseInt(line.substring(0, firstPointPosition));
		String entryLabel = line.substring(firstPointPosition + 1).trim();

		// Catch inconsistencies (e.g. items 14, 57 and 129 have internal numbering)
		// It is assumed that entryList has been populated first
		if (!entryList.get(entryNumber).equals(entryLabel)) {
			logger.warn("Label mismatch for entry number " + entryNumber + " (" + entryLabel + ")");
			System.out.println("Label mismatch for entry number " + entryNumber + " (" + entryLabel + ")");
			return 0;
		}

		return entryNumber;
	}

	public static int findEntry(String searched) {
		int found = 0;
		// TODO Very inefficient, find another structure for entryList
	    for (Entry<Integer, String> entry : entryList.entrySet()) {
	        if (searched.equals(entry.getValue())) {
	            found = entry.getKey();
	            break;
	        }
	    }
	    return found;

	}
//	public static void readEntry(List<String> chunk) {
//		
//	}
}
