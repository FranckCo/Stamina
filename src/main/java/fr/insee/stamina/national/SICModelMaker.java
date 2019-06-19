package fr.insee.stamina.national;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import fr.insee.stamina.utils.ExplanatoryNote;
import fr.insee.stamina.utils.Names;
import fr.insee.stamina.utils.NoteType;
import fr.insee.stamina.utils.XKOS;
import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * The <code>SICModelMaker</code> class creates and saves the Jena model corresponding to the UK SIC 2007 classification.
 * 
 * @see https://www.ons.gov.uk/methodology/classificationsandstandards/ukstandardindustrialclassificationofeconomicactivities/uksic2007
 * @author Franck Cotton
 */
public class SICModelMaker {

	/** Base local folder for reading and writing files */
	public static String LOCAL_FOLDER = "src/main/resources/data/";

	/** File name of the spreadsheet containing the SIC structure */
	public static String SIC_STRUCTURE_FILE = "sic2007summaryofstructur_tcm77-223506.xls";

	/** File name of the PDF file containing the explanatory notes */
	public static String SIC_NOTES_FILE = "sic2007explanatorynote_tcm77-223502.pdf";

	/** The explanatory notes are also in a JavaScript file on the Neighbourhood Statistics site */
	public static String SIC_DB_URL = "http://www.neighbourhood.statistics.gov.uk/HTMLDocs/SIC/data/sicDB.js";

	/** Base URI for the RDF resources belonging to NAICS */
	public final static String SIC_BASE_URI = "http://stamina-project.org/codes/sic2007/";

	/** Base URI for the RDF resources belonging to the NACE-SIC correspondence */
	public final static String NACE_SIC_BASE_URI = "http://stamina-project.org/codes/nacer2-sic2007/";

	/** Log4J2 logger */ // This must be before the configuration initialization
	private static final Logger logger = LogManager.getLogger(SICModelMaker.class);

	/** Current Jena model */
	private Model model = null;

	/** RDF resource corresponding to the classification scheme */
	Resource scheme = null;

	/** List of RDF resources corresponding to the classification levels */
	RDFList levelList = null;

	/**
	 * Main method: reads the spreadsheet and creates the triplets in the model.
	 */
	public static void main(String[] args) throws Exception {

		SICModelMaker modelMaker = new SICModelMaker();
		// Creation of the classification with its levels and items
		modelMaker.initializeModel();
		modelMaker.createClassificationAndLevels();
		modelMaker.populateScheme();
//		modelMaker.getNotesPDF();
		modelMaker.writeModel(LOCAL_FOLDER + "sic2007.ttl");
		// Creation of the NACE-SIC hierarchy
		modelMaker.initializeModel();
		modelMaker.createNACESICHierarchy();
		modelMaker.writeModel(LOCAL_FOLDER + "nacer2-sic2007.ttl");
	}

	/**
	 * Creates the statements corresponding to the classification items.
	 * 
	 * @throws Exception In case of problems reading the source file.
	 */
	private void populateScheme() throws Exception {

		// Read the Excel file
		InputStream sourceFile = new FileInputStream(new File(LOCAL_FOLDER + SIC_STRUCTURE_FILE));
		Sheet items = WorkbookFactory.create(sourceFile).getSheetAt(0);
		if (sourceFile != null) try {sourceFile.close();} catch(Exception ignored) {}

		// Iterate over the rows and create the classification items
		Iterator<Row> rows = items.rowIterator ();
		while (rows.hasNext() && rows.next().getRowNum() < 2); // Skip the two header lines
		while (rows.hasNext()) {
			Row row = rows.next();

			// The lines start at different columns depending on the level
			short codeIndex = getFirstNonEmptyCellIndex(row);
			if (codeIndex < 0) continue;
			if (codeIndex == 3) continue; // HACK: unwanted comment in line 572 of the spreadsheet

			String itemCode = getCodeInCell(row.getCell(codeIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK));
			String itemLabel = row.getCell(codeIndex + 1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).toString();
			int level = getItemLevelDepth(itemCode);
			logger.debug("About to create resources for SIC item " + itemCode + " at level " + level);

			// Create the resource representing the classification item (skos:Concept), with its code and label
			Resource itemResource = model.createResource(getItemURI(itemCode), SKOS.Concept);
			itemResource.addProperty(SKOS.notation, itemCode);
			itemResource.addProperty(SKOS.prefLabel, itemLabel, "en");

			// Attach the item to its level
			Resource levelResource = (Resource) levelList.get(level - 1);
			levelResource.addProperty(SKOS.member, itemResource);
			
			// Attach the item to its classification, as top concept for level 1
			itemResource.addProperty(SKOS.inScheme, scheme);
			if (level == 1) {
				scheme.addProperty(SKOS.hasTopConcept, itemResource);
				itemResource.addProperty(SKOS.topConceptOf, scheme);				
			}

			// Attach the item to its parent item (for level > 1)
			if (level > 1) {
				Resource parentResource = model.createResource(getItemURI(getParentCode(itemCode)));
				parentResource.addProperty(SKOS.narrower, itemResource);
				itemResource.addProperty(SKOS.broader, parentResource);
			}
		}
	}

	/**
	 * Creates a model containing the resources representing the correspondence between NACE and SIC.
	 * 
	 * Since the sub-class level of SIC is not complete, we create exact matches between classes when there
	 * are no SIC sub-classes, and broad/narrow matches between SIC sub-classes and NACE class otherwise.
	 */
	private void createNACESICHierarchy() throws Exception {

		// Due to the design choices made, the process is not purely sequential, so we
		// have to read the whole spreadsheet before starting creating the resources

		List<String> mostDetailedCodes = new ArrayList<String>();

		InputStream sourceFile = new FileInputStream(new File(LOCAL_FOLDER + SIC_STRUCTURE_FILE));
		Sheet items = WorkbookFactory.create(sourceFile).getSheetAt(0);
		if (sourceFile != null) try {sourceFile.close();} catch(Exception ignored) {}

		Iterator<Row> rows = items.rowIterator ();
		while (rows.hasNext() && rows.next().getRowNum() < 2); // Skip the two header lines
		while (rows.hasNext()) {
			Row row = rows.next();

			short codeIndex = getFirstNonEmptyCellIndex(row);
			if (codeIndex < 0) continue;
			if (codeIndex < 6) continue; // We only want classes and sub-classes

			String sicCode = getCodeInCell(row.getCell(codeIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK));
			String naceCode = sicCode.substring(0, 5);
			mostDetailedCodes.add(sicCode);
			if (sicCode.length() == 7) {
				// We create broad/narrow matches, so we have to remove the class code
				mostDetailedCodes.remove(naceCode); // No-op after the first removal
				mostDetailedCodes.add(sicCode);
			}
		}

		// Creation of the correspondence table resource
		Resource table = model.createResource(NACE_SIC_BASE_URI + "correspondence", XKOS.Correspondence);
		table.addProperty(SKOS.definition, "Correspondence table between NACE Rev. 2 and UK SIC 2007");
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI("NACE", "2")));
		table.addProperty(XKOS.compares, model.createResource(SIC_BASE_URI + "sic"));

		for (String sicCode : mostDetailedCodes) {

			String naceCode = sicCode.substring(0, 5);
			Resource association = model.createResource(NACE_SIC_BASE_URI + "association/" + naceCode + "-" + sicCode, XKOS.ConceptAssociation);
			association.addProperty(RDFS.label, "NACE Rev.2 " + naceCode + " - UK SIC 2007 " + sicCode);
			Resource naceItemResource = model.createResource(Names.getItemURI(naceCode, "NACE", "2"));
			Resource sicItemResource = model.createResource(getItemURI(sicCode));
			association.addProperty(XKOS.sourceConcept, naceItemResource);	
			association.addProperty(XKOS.targetConcept, sicItemResource);
			// We create exact matches when SIC stops at class level, broader / narrower match when there are SIC sub-classes
			if (sicCode.length() == 5) {
				naceItemResource.addProperty(SKOS.exactMatch, sicItemResource);
				sicItemResource.addProperty(SKOS.exactMatch, naceItemResource);
				logger.debug("Created exact matches between NACE " + naceCode + " and SIC " + sicCode);
			} else {
				naceItemResource.addProperty(SKOS.narrowMatch, sicItemResource);
				sicItemResource.addProperty(SKOS.broadMatch, naceItemResource);
				logger.debug("Created broad/narrow matches between NACE " + naceCode + " and SIC " + sicCode);
			}
			table.addProperty(XKOS.madeOf, association);
		}
	}

	/**
	 * Extracts the explanatory notes from the official PDF publication of the UK SIC classification.
	 * 
	 * <i>Note:</i> This method is unfinished.
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private void getNotesPDF() throws IOException {

		PDDocument document = PDDocument.load(new File(LOCAL_FOLDER + SIC_NOTES_FILE));
		PDFTextStripper stripper = new PDFTextStripper();

		// Extract the main contents and saves it to a file (this is only for debugging purposes)
		stripper.setStartPage(59);
		String rawText = stripper.getText(document);
		//Files.write(Paths.get(LOCAL_FOLDER + "sic-notes.txt"), rawText.getBytes());

		document.close();

		// Read the string containing the raw text line by line and try to make sense of it
		String noteLine = null;
		List<String> currentNote = null;
		int lineNumber = 0;
		boolean ignore = true; // Lines 1 and 2 must be ignored
		BufferedReader noteReader = new BufferedReader(new StringReader(rawText));
		while ((noteLine = noteReader.readLine()) != null) {
			lineNumber++;
			// Ignore the lines that correspond to the headers of the PDF pages
			// The pattern for the first page is irregular, so we explicitly eliminate the line containing the section letter by its number
			if (lineNumber == 52) ignore = true;
			// For all other pages, the pattern is - line beginning or ending with 'Explanatory Notes', then line with page number, then line with section letter
			if ((noteLine.startsWith("Explanatory Notes")) || (noteLine.endsWith("Explanatory Notes"))) ignore = true; // Checked: we don't eliminate note lines with this test

			if (ignore) {
				//System.out.println("Ignored line " + lineNumber + " - " + noteLine);
				if ((lineNumber == 2) || (lineNumber == 52) || (noteLine.length() == 1)) ignore = false; // Resume reading after current line
				continue;
			}

			// Find the lines that are item titles
			String code = null;
			if (noteLine.startsWith("Section")) {
				System.out.println(noteLine);
				code = noteLine.substring(8, 9); // Checked: first test identifies exactly the section lines
			}
			// A too loose test like 'begins with two digits' (^\\d{2}.+$) misses vicious cases, so we have to be more precise
			// Examples of pathological cases at line numbers 322, 1496, 1827, 3088, 6598, 7098, 8534, 8535, 8622...
			if (noteLine.matches("^\\d{2}.+$")) { // We still use a catch-fall test for optimization
				if ((noteLine.matches("^\\d{2} .+$")) && (lineNumber != 322)) code = noteLine.substring(0, 2); // Checked: the test identifies exactly the division lines
				else if (noteLine.matches("^\\d{2}\\.\\d .+$")) code = noteLine.substring(0, 4); // Checked: the test identifies exactly the group lines
				else if (noteLine.matches("^\\d{2}\\.\\d{2} .+$")) code = noteLine.substring(0, 5); // Checked: the test identifies exactly the classes lines
				else if (noteLine.matches("^\\d{2}\\.\\d{2}/\\d .+$")) code = noteLine.substring(0, 7); // Checked: the test identifies exactly the subclasses lines
			}
			if (code != null) { // Start of a note for item identified by 'code'
				currentNote = new ArrayList<String>();
				System.out.println(lineNumber + " - " + code + " - '" + noteLine.substring(code.length() + 1) + "'");
			} else {
				if (currentNote != null) currentNote.add(noteLine); // We could avoid the null test since we jumped directly to the first title
			}
		}
	}

	/**
	 * Extracts the explanatory notes from the JavaScript file on the Neighbourhood Statistics site and creates associated resources.
	 * 
	 * @throws Exception
	 */
	private void getNotesJS() throws Exception {

		// Create a script engine manager
		ScriptEngineManager factory = new ScriptEngineManager();
		// Create a JavaScript engine
		ScriptEngine engine = factory.getEngineByName("JavaScript");
		// Evaluate the script, which produces a JavaScript object containing an array with the classification and notes
		engine.eval(new FileReader("D:\\Temp\\sicDB.js"));

		JSObject sic = (JSObject)engine.get("SICmeta");
		// The SICmeta object is an array with one entry for each SIC item. The corresponding value is a JavaScript object containing
		// a 'title' key (item label) and some or all of 'detail' (general note), 'includes' (inclusions) and 'excludes' (exclusions).
		// 'includes' and 'excludes' are arrays of string listing the activities included or excluded respectively.

		for (String sicCode : sic.keySet()) {
			JSObject sicEntry = (JSObject)sic.getMember(sicCode);
			logger.debug("Creating explanatory notes for item " + sicCode);

			ExplanatoryNote currentNote = new ExplanatoryNote();

			if (sicEntry.hasMember("detail")) {
				currentNote.setNoteType(NoteType.GENERAL);
				currentNote.addSourceLine(sicEntry.getMember("detail").toString());
				System.out.println(" . detail : " + sicEntry.getMember("detail"));
			}
			
			if (sicEntry.hasMember("includes")) {
				currentNote.setNoteType(NoteType.CENTRAL_CONTENT);
				JSObject includes = (JSObject)sicEntry.getMember("includes");
				for (String includeItem : includes.keySet()) {
					currentNote.addSourceLine(includeItem);
					System.out.println(" . includes " + includes.getMember(includeItem));
				}
			}

			if (sicEntry.hasMember("excludes")) {
				currentNote.setNoteType(NoteType.EXCLUSIONS);
				JSObject excludes = (JSObject)sicEntry.getMember("excludes");
				for (String excludeItem : excludes.keySet()) {
					currentNote.addSourceLine(excludeItem);
					System.out.println(" . excludes " + excludes.getMember(excludeItem));
				}
			}
		}

	}

	/**
	 * Returns the index of the first non-empty cell in a row.
	 * 
	 * @param cell The <code>Row</code> to analyse.
	 * @return The (zero-based) index of the first non-empty cell, or -1 if the row is empty.
	 */
	private short getFirstNonEmptyCellIndex(Row row) {

		if (row == null) return -1;
		short index = row.getFirstCellNum();
		short maxIndex = row.getLastCellNum();
		if (index >= maxIndex) return -1;
		while (index < maxIndex) {
			Cell cell = row.getCell(index);
			index++;
			if (cell == null) continue;
			if (cell.getCellType() == CellType.BLANK) continue;
			if ((cell.getCellType() == CellType.NUMERIC) && (cell.getNumericCellValue() > 0)) return --index;
			if ((cell.getCellType() == CellType.STRING) && (cell.toString().trim().length() > 0)) return --index;
		}
		return -1;
	}

	/**
	 * Reads a code in a cell, whether the cell type is numeric or string.
	 * 
	 * @param cell The <code>Cell</code> containing the code to read.
	 * @return The code value as a string.
	 */
	private String getCodeInCell(Cell cell) {

		if (cell == null) return "";
		String code = null;
		if (cell.getCellType() == CellType.STRING) {
			code = cell.toString();
		}
		else {
			int itemCodeValue = (int)cell.getNumericCellValue();
			code = Integer.toString(itemCodeValue);				
		}
		return code.trim();
	}

	/**
	 * Creates in the model the resources representing the classification and its levels.
	 */
 	public void createClassificationAndLevels() {

		// Create the resource representing the classification (skos:ConceptScheme)
		scheme = model.createResource(SIC_BASE_URI + "sic", SKOS.ConceptScheme);
		scheme.addProperty(SKOS.prefLabel, model.createLiteral("UK Standard Industrial Classification of Economic Activities (SIC) 2007", "en"));
		scheme.addProperty(SKOS.notation, "UK SIC 2007");
		scheme.addProperty(SKOS.definition, model.createLiteral("The current Standard Industrial Classification (SIC) used in classifying business establishments and other statistical units by the type of economic activity in which they are engaged.", "en"));
		scheme.addProperty(DC.publisher, model.createResource("http://www.ons.gov.uk"));
		scheme.addProperty(DCTerms.issued, model.createTypedLiteral("2007-01-01", "http://www.w3.org/2001/XMLSchema#date"));
		scheme.addProperty(DCTerms.modified, model.createTypedLiteral("2007-01-01", "http://www.w3.org/2001/XMLSchema#date"));
		scheme.addProperty(FOAF.homepage, model.createResource("https://www.ons.gov.uk/methodology/classificationsandstandards/ukstandardindustrialclassificationofeconomicactivities/uksic2007"));
		scheme.addProperty(XKOS.covers, model.createResource("http://eurovoc.europa.eu/5992"));
		scheme.addProperty(XKOS.numberOfLevels, model.createTypedLiteral(5));

		// Create the resources representing the levels (xkos:ClassificationLevel)
		Resource level1 = model.createResource(SIC_BASE_URI + "/sections", XKOS.ClassificationLevel);
		level1.addProperty(SKOS.prefLabel, model.createLiteral("UK SIC 2007 - level 1 - Sections", "en"));
		level1.addProperty(XKOS.depth, model.createTypedLiteral(1));
		level1.addProperty(XKOS.notationPattern, "[A-U]");
		level1.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/sic2007/section"));

		Resource level2 = model.createResource(SIC_BASE_URI + "/divisions", XKOS.ClassificationLevel);
		level2.addProperty(SKOS.prefLabel, model.createLiteral("UK SIC 2007 - level 2 - Divisions", "en"));
		level2.addProperty(XKOS.depth, model.createTypedLiteral(2));
		level2.addProperty(XKOS.notationPattern, "[0-9]{2}");
		level2.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/sic2007/division"));

		Resource level3 = model.createResource(SIC_BASE_URI + "/groups", XKOS.ClassificationLevel);
		level3.addProperty(SKOS.prefLabel, model.createLiteral("UK SIC 2007 - level 3 - Groups", "en"));
		level3.addProperty(XKOS.depth, model.createTypedLiteral(3));
		level3.addProperty(XKOS.notationPattern, "[0-9]{2}\\.[0-9]");
		level3.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/sic2007/group"));

		Resource level4 = model.createResource(SIC_BASE_URI + "/classes", XKOS.ClassificationLevel);
		level4.addProperty(SKOS.prefLabel, model.createLiteral("UK SIC 2007 - level 4 - Classes", "en"));
		level4.addProperty(XKOS.depth, model.createTypedLiteral(4));
		level4.addProperty(XKOS.notationPattern, "[0-9]{2}\\.[0-9]{2}");
		level4.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/sic2007/class"));

		Resource level5 = model.createResource(SIC_BASE_URI + "/subclasses", XKOS.ClassificationLevel);
		level5.addProperty(SKOS.prefLabel, model.createLiteral("UK SIC 2007 - level 5 - Subclasses", "en"));
		level5.addProperty(XKOS.depth, model.createTypedLiteral(5));
		level5.addProperty(XKOS.notationPattern, "[0-9]{2}\\.[0-9]{2}\\/[0-9]");
		level5.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/sic2007/subclass"));

		// Attach the level list to the classification
		levelList = model.createList(new RDFNode[] {level1, level2, level3, level4, level5});
		scheme.addProperty(XKOS.levels, levelList);
	}

	/**
	 * Initializes the Jena model and adds standard prefixes.
	 */
	private void initializeModel() {

		try {
			if (model != null) model.close(); // Just in case
		} catch (Exception ignored) {}

		model = ModelFactory.createDefaultModel();
		model.setNsPrefix("rdfs", RDFS.getURI());
		model.setNsPrefix("skos", SKOS.getURI());
		model.setNsPrefix("xkos", XKOS.getURI());

		logger.debug("Jena model initialized");

		return;
	}

	/**
	 * Writes the model to the output Turtle file.
	 * 
	 * @throws Exception In case of problem writing the file.
	 */
	private void writeModel(String fileName) throws IOException {

		model.write(new FileOutputStream(fileName), "TTL");
		// Close the model
		model.close();
	}
	
	/**
	 * Computes the parent code for one given SIC code.
	 * 
	 * @param code The code of the child SIC item.
	 * @return The code of the parent, or <code>null</code> for sections and invalid codes.
	 */
	private static String getParentCode(String code) {

		if (code == null) return null;

		if (code.length() == 7) return code.substring(0, 5);
		if (code.length() == 5) return code.substring(0, 4);
		if (code.length() == 4) return code.substring(0, 2);
		if (code.length() == 2) return Names.getNACESectionForDivision(code);

		return null;
	}

	/**
	 * Computes the URI of a SIC classification item.
	 * 
	 * @param code The item code.
	 * @return The item URI.
	 */
	private static String getItemURI(String code) {

		if (code.length() == 1) return SIC_BASE_URI + "section/" + code;
		if (code.length() == 2) return SIC_BASE_URI + "division/" + code;
		if (code.length() == 4) return SIC_BASE_URI + "group/" + code;
		if (code.length() == 5) return SIC_BASE_URI + "class/" + code;
		if (code.length() == 7) return SIC_BASE_URI + "subclass/" + code; // TODO Keep slashes in subclass codes?

		return null;
	}

	/**
	 * Returns the depth of the level to which an item belongs.
	 * 
	 * @param code The item code.
	 * @return The depth of the level.
	 */
	public static int getItemLevelDepth(String code) {

		if (code.length() == 1) return 1;
		if (code.length() == 2) return 2;
		if (code.length() == 4) return 3;
		if (code.length() == 5) return 4;
		if (code.length() == 7) return 5;
		return 0;
	}
}
