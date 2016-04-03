package fr.insee.stamina.gamso;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import fr.insee.stamina.utils.CSPAOnto;

/**
 * Creates a SKOS concept scheme by reading the Word document describing the GAMSO and saves it in a Turtle file.
 * This program is only to be used with GAMSO v1.0 as published on the UNECE wiki.
 * 
 * @author Franck Cotton
 */
public class GAMSOModelMaker {

	static String GAMSO_DOCX = "src/main/resources/data/GAMSO.docx";
	static String GAMSO_TTL = "src/main/resources/data/gamso.ttl";
	static String GAMSO_BASE_URI = "http://id.unece.org/codes/gamso/";

	/** The numbers of the paragraphs that will build up the concept scheme description. */
	static List<Integer> descriptionIndexes = Arrays.asList(new Integer[]{31, 34});
	static String LEVEL1_STYLING = "Heading2";
	static String LEVEL2_STYLING = "Heading3";

	/** The RDF model containing the SKOS concept scheme. */
	Model gamsoModel = null;

	/** The SKOS ConceptScheme representing the GAMSO. */
	Resource gamsoCS = null;

	/** The description of the concept scheme representing the model. */
	String gamsoDescription = null;

	/** Log4J 2 logger. */
	private static Logger logger = LogManager.getLogger(GAMSOModelMaker.class);

	/**
	 * Constructor: initializes the Jena model.
	 */
	public GAMSOModelMaker() {
		gamsoModel = ModelFactory.createDefaultModel();
		gamsoModel.setNsPrefix("skos", SKOS.getURI());
		gamsoModel.setNsPrefix("gamso", GAMSO_BASE_URI);
		gamsoModel.setNsPrefix("cspa-onto", CSPAOnto.getURI());
	}

	/**
	 * Launcher.
	 * 
	 * @param args Not used.
	 */
	public static void main(String[] args) {

		GAMSOModelMaker reader = new GAMSOModelMaker();

		try {
			reader.readGAMSODocument();
			reader.gamsoModel.write(new FileOutputStream(GAMSO_TTL), "TTL");
		} catch (IOException e) {
			logger.fatal("Error executing the program", e);
		}
	}

	/**
	 * Main method: reads the Word document and extracts the information about entities.
	 * 
	 * @throws IOException In case of error while reading the document.
	 */
	public void readGAMSODocument() throws IOException {

		// Read the document with POI and get the list of paragraphs
		XWPFDocument document = new XWPFDocument(new FileInputStream(GAMSO_DOCX));
		List<XWPFParagraph> paragraphs = document.getParagraphs();

		int paragraphNumber = 0;
		int paragraphStylingNumber = 0;
		int[] currentNumber = {0,0,0};
		List<String> currentDescription = null;
		String currentLabel = null;

		// Creation of the concept scheme resource.
		gamsoCS = gamsoModel.createResource(GAMSO_BASE_URI + "gamso", SKOS.ConceptScheme);

		// Iteration through the document paragraphs
		logger.debug("Document read from " + GAMSO_DOCX + ", starting to iterate through the paragraphs.");
		for (XWPFParagraph paragraph : paragraphs) {

			if (paragraph.getParagraphText() == null) continue; // skipping empty paragraphs
			paragraphNumber++;

			// Styling number will be strictly positive for headings and list elements (eg. bullet points)
			paragraphStylingNumber = (paragraph.getNumID() == null) ? 0 : paragraph.getNumID().intValue();

			// Add the paragraph text to the CS description if its number corresponds
			if (descriptionIndexes.contains(paragraphNumber)) {
				// TODO normalize white spaces
				if (gamsoDescription == null) gamsoDescription = paragraph.getParagraphText();
				else gamsoDescription += " " + paragraph.getParagraphText();
			}

			if (LEVEL1_STYLING.equals(paragraph.getStyle())) {
				// The first headings are in the introduction: we skip those
				if (paragraphStylingNumber == 0) continue;
				// If paragraph has a number styling, we have a new level 1 activity
				currentNumber[2] = 0; // Because third number may have been modified by level 3 operations
				if (currentDescription != null) {
					// Previous description is complete: record in the model
					this.addActivityToModel(currentNumber, currentLabel, currentDescription);
				}
				currentNumber[0]++;
				currentNumber[1] = 0;
				currentDescription = new ArrayList<String>();
				currentLabel = normalizeActivityName(paragraph);
			} else if (LEVEL2_STYLING.equals(paragraph.getStyle())) {
				// Start of a new level 2 activity
				currentNumber[2] = 0;
				// Record previous description (which exists since we are at level 2) in the model
				this.addActivityToModel(currentNumber, currentLabel, currentDescription);
				currentNumber[1]++;
				currentDescription = new ArrayList<String>();
				currentLabel = normalizeActivityName(paragraph); // Strip code for 3.x activities
			} else {
				if (currentNumber[0] == 0) continue; // Skip paragraphs that are before the first activity
				// Not a heading, so part of a description
				String descriptionPart = normalizeDescriptionItem(paragraph, paragraphStylingNumber);
				if (descriptionPart.length() > 0) currentDescription.add(descriptionPart);
				// Transform bullet points of level 2 activities into level 3 activities
				if ((paragraphStylingNumber > 0) && (currentNumber[1] > 0)) {
					currentNumber[2]++;
					this.addActivityToModel(currentNumber, paragraph.getParagraphText().trim(), null);
				}
			}
		}
		// The last activity read has not been added to the model yet: we do it here
		this.addActivityToModel(currentNumber, currentLabel, currentDescription);

		document.close();

		logger.debug("Iteration through the paragraphs finished, completing the Jena model.");

		// Add the properties of the concept scheme (the description is now complete)
		gamsoCS.addProperty(SKOS.notation, gamsoModel.createLiteral("GAMSO v1.0"));
		gamsoCS.addProperty(SKOS.prefLabel, gamsoModel.createLiteral("Generic Activity Model for Statistical Organisations v 1.0", "en"));
		gamsoCS.addProperty(SKOS.scopeNote, gamsoModel.createLiteral(gamsoDescription, "en"));

	}

	/**
	 * Adds a GAMSO activity to the Jena model.
	 * 
	 * @param activityNumber An array of integers with the components of the activity code.
	 * @param activityLabel The label of the activity as read from the Word document.
	 * @param activityDescription The components of the activity description (a <code>List</code> of strings).
	 */
	private void addActivityToModel(int[] activityNumber, String activityLabel, List<String> activityDescription) {

		String code = String.format("%d", activityNumber[0]);
		if (activityNumber[1] > 0) code += String.format(".%d", activityNumber[1]);
		if (activityNumber[2] > 0) code += String.format(".%d", activityNumber[2]);
		String parentCode = getParentCode(code);

		logger.debug("Adding activity " + code + " - " + activityLabel);

		Resource gamsoConcept = gamsoModel.createResource(GAMSO_BASE_URI + code, SKOS.Concept);
		gamsoConcept.addProperty(RDF.type, CSPAOnto.GAMSOActivity);
		gamsoConcept.addProperty(SKOS.notation, code);
		gamsoConcept.addProperty(SKOS.prefLabel, gamsoModel.createLiteral(activityLabel, "en"));
		gamsoConcept.addProperty(SKOS.inScheme, gamsoCS);

		if (parentCode == null) {
			gamsoCS.addProperty(SKOS.hasTopConcept, gamsoConcept);
			gamsoConcept.addProperty(SKOS.topConceptOf, gamsoCS);
		} else {
			Resource parentConcept = gamsoModel.createResource(GAMSO_BASE_URI + parentCode);
			parentConcept.addProperty(SKOS.narrower, gamsoConcept);
			gamsoConcept.addProperty(SKOS.broader, parentConcept);
		}
	}


	/**
	 * Returns the parent code for a GAMSO activity code.
	 * 
	 * @param code The GAMSO child code.
	 * @return The parent code, or <code>null</code> if code is <code>null</code> or has no parent.
	 */
	public static String getParentCode(String code) {

		// TODO This is a very basic implementation, will work for actual GAMSO codes, otherwise impredictible
		if ((code == null) || (code.length() < 3)) return null;
		return code.substring(0, code.lastIndexOf("."));
	}

	/**
	 * Extracts the name of an activity from a paragraph.
	 * This method will trim the text of the paragraph and remove leading numbers 
	 * (in the version 1.0 GAMSO document, the numbers of activities 3.x are written in the labels).
	 * 
	 * @param paragraph The paragraph containing the activity name.
	 * @return The activity name as a string (or an empty string).
	 */
	public static String normalizeActivityName(XWPFParagraph paragraph) {

		// TODO Should also remove [footnoteRef:5] in label of activity 2
		if (paragraph == null) return "";

		String trimmedText = paragraph.getParagraphText().trim();

		if (trimmedText.startsWith("3.")) return trimmedText.substring(trimmedText.indexOf(" ") + 1);

		return trimmedText;
		
	}

	/**
	 * Normalizes a paragraph of an activity description.
	 * This method will trim the text of the paragraph and add a trailing '- ' for bullet points. 
	 * 
	 * @param paragraph The paragraph from the activity description.
	 * @param stylingNumber The styling number of the paragraph.
	 * @return The paragraph text normalized as a string (or an empty string).
	 */
	public static String normalizeDescriptionItem(XWPFParagraph paragraph, int stylingNumber) {

		if (paragraph == null) return "";

		String trimmedText = paragraph.getParagraphText().trim();

		if (stylingNumber > 0) return "- " + trimmedText;

		return trimmedText;
		
	}

}
