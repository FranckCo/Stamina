package fr.insee.stamina.sims;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.SKOS;

/**
 * The <code>SIMSSimpleModelMaker</code> creates and saves a Jena model corresponding to a concept scheme based on the SIMS.
 * 
 * The source used here is the version with only the SIMS structure (see ref. below).
 * A more complete version is produced by the SIMSModelMaker class.
 * 
 * @see http://ec.europa.eu/eurostat/documents/64157/4373903/SIMS-2-0-Revised-standards-November-2015-ESSC-final.pdf/47c0b80d-0e19-4777-8f9e-28f89f82ce18
 * @author Franck Cotton
 * @version 0.10, 12 May 2016
 */
public class SIMSSimpleModelMaker {

	static String SIMS_PDF = "src/main/resources/data/SIMS-2-0-Revised-standards-November-2015-ESSC-final.pdf";
	static String SIMS_TTL = "src/main/resources/data/sims.ttl";
	static String SIMS_BASE_URI = "http://id.unece.org/codes/sims/";
	static int SIMS_PAGE = 6;
	static int SIMS_SKIP = 7;

	/** The list of concept codes and labels in SIMS */
	private SortedMap<String, String> entryMap = null;

	/** The RDF model containing the SKOS concept scheme */
	Model simsModel = null;

	private static Logger logger = LogManager.getLogger(SIMSSimpleModelMaker.class);

	public SIMSSimpleModelMaker() {
		entryMap = new TreeMap<String, String>();
		simsModel = ModelFactory.createDefaultModel();
	}

	public static void main(String[] args) {

		SIMSSimpleModelMaker reader = new SIMSSimpleModelMaker();

		try {
			reader.extractFromPDF();
			reader.buildModel();
			reader.simsModel.write(new FileOutputStream(SIMS_TTL), "TTL");
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
		logger.debug("Starting PDF extraction from " + SIMS_PDF);
		PDDocument document = PDDocument.load(new File(SIMS_PDF));
		PDFTextStripper stripper = new PDFTextStripper();
		stripper.setStartPage(SIMS_PAGE);
		stripper.setEndPage(SIMS_PAGE);
		String text = stripper.getText(document);
		String[] lines = text.split("\r\n");
		int lineIndex = 0;
		StringBuilder currentLine = new StringBuilder();
		for (String line : lines) {
			String trimmedLine = line.trim();
			if ((lineIndex++ < SIMS_SKIP) || (trimmedLine.length() == 0)) continue; // First lines are titles
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
		logger.debug("End of PDF extraction");

		document.close();
	}

	/**
	 * Builds the SKOS concept scheme as a Jena model.
	 */
	public void buildModel() {

		logger.debug("Starting model construction");

		simsModel.setNsPrefix("skos", SKOS.getURI());
		simsModel.setNsPrefix("sims", SIMS_BASE_URI);

		// Create the ConceptScheme
		Resource simsCS = simsModel.createResource(SIMS_BASE_URI + "sims", SKOS.ConceptScheme);
		simsCS.addProperty(SKOS.notation, simsModel.createLiteral("SIMS v2.0"));
		simsCS.addProperty(SKOS.prefLabel, simsModel.createLiteral("Single Integrated Metadata Structure v 2.0", "en"));

		// Iterate on the entry map to create the concepts
		for (Entry<String, String> entry : entryMap.entrySet()) {
			String conceptCode = entry.getKey();
			String parentCode = getParentCode(conceptCode);
			Resource simsConcept = simsModel.createResource(SIMS_BASE_URI + conceptCode, SKOS.Concept);
			simsConcept.addProperty(SKOS.notation, conceptCode);
			simsConcept.addProperty(SKOS.prefLabel, simsModel.createLiteral(entry.getValue(), "en"));
			simsConcept.addProperty(SKOS.inScheme, simsCS);
			if (parentCode == null) {
				simsCS.addProperty(SKOS.hasTopConcept, simsConcept);
				simsConcept.addProperty(SKOS.topConceptOf, simsCS);
			} else {
				Resource parentConcept = simsModel.getResource(SIMS_BASE_URI + parentCode);
				parentConcept.addProperty(SKOS.narrower, simsConcept);
				simsConcept.addProperty(SKOS.broader, parentConcept);
			}
		}

	}

	/**
	 * Returns the parent code for a SIMS concept code.
	 * 
	 * @param code The SIMS child code.
	 * @return The parent code, or <code>null</code> if code is <code>null</code>, has no parent or is not formed like a SIMS code.
	 */
	public static String getParentCode(String code) {

		String parentCode;
		try {
			parentCode = (code == null) ? "S" : code.substring(0, code.lastIndexOf("."));
		} catch (Exception e) {
			// StringIndexOutOfBoundsException: no '.' in code
			parentCode = "S";
		}

		return (parentCode.equals("S")) ? null : parentCode;
	}

}
