package fr.insee.stamina.national;

import fr.insee.stamina.utils.Names;
import fr.insee.stamina.utils.XKOS;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DC;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * The <code>NAICSModelMaker</code> class creates and saves the Jena model corresponding to the NAICS 2012 classification.
 * 
 * @see <a href="http://www.census.gov/eos/www/naics/">http://www.census.gov/eos/www/naics/</a>
 * @author Franck Cotton
 */
public class NAICSModelMaker {

	/** Base local folder for reading and writing files */
	public static String LOCAL_FOLDER = "src/main/resources/data/";

	/** File name of the spreadsheet containing the NAICS structure */
	public static String NAICS_FILE = "2-digit_2012_Codes.xls";

	/** File name of the spreadsheet containing the NAICS to ISIC correspondence */
	public static String NAICS_ISIC_FILE = "2012 NAICS_to_ISIC_4.xlsx";

	/** Base URI for the RDF resources belonging to NAICS */
	public final static String BASE_URI = "http://stamina-project.org/codes/naics2012/";

	/** Base URI for the RDF resources belonging to the ISIC-NAICS correspondence */
	public final static String BASE_CORRESPONDENCE_URI = "http://stamina-project.org/codes/isicr4-naics2012/";

	/** Cases where the ISIC-NAICS correspondence is at ISIC group level */
	static Map<String, List<String>> GROUP_LINK_CASES = new HashMap<>();
	static {
		// 012X (0121 to 0129), 014X (0141 to 0146, 0149), 331X (3311 to 3315, 3319)
		GROUP_LINK_CASES.put("012X", Arrays.asList("0121", "0122", "0123", "0124", "0125", "0126", "0127", "0128", "0129"));
		GROUP_LINK_CASES.put("014X", Arrays.asList("0141", "0142", "0143", "0144", "0145", "0146", "0149"));
		GROUP_LINK_CASES.put("331X", Arrays.asList("3311", "3312", "3313", "3314", "3315", "3319"));
	}

	/** Log4J2 logger */ // This must be before the configuration initialization
	private static final Logger logger = LogManager.getLogger(NAICSModelMaker.class);

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

		NAICSModelMaker modelMaker = new NAICSModelMaker();
		// Creation of the classification and its levels
		modelMaker.initializeModel();
		modelMaker.createClassificationAndLevels();
		modelMaker.populateScheme();
		modelMaker.writeModel(LOCAL_FOLDER + "naicsv2012.ttl", RDFFormat.TURTLE); // Use RDFFormat.RDFXML_PLAIN for flat (and quick) XML
		// Creation of the ISIC-NAICS correspondence
		modelMaker.initializeModel();
		modelMaker.createISICCorrespondence();
		modelMaker.writeModel(LOCAL_FOLDER + "isicr4-naicsv2012.ttl", RDFFormat.TURTLE);
	}

	/**
	 * Creates the statements corresponding to the classification items.
	 * 
	 * @throws Exception In case of problem.
	 */
	private void populateScheme() throws Exception {

		// Read the Excel file and create the classification items
		InputStream sourceFile = new FileInputStream(LOCAL_FOLDER + NAICS_FILE);
		Sheet items = WorkbookFactory.create(sourceFile).getSheetAt(0);
		try {sourceFile.close();} catch(Exception ignored) {}

		Iterator<Row> rows = items.rowIterator ();
		while (rows.hasNext() && rows.next().getRowNum() < 1); // Skip the two header lines
		while (rows.hasNext()) {
			Row row = rows.next();

			// The cell containing the code is generally numeric, except for composite sector codes
			String itemCode;
			if (row.getCell(1).getCellType() == CellType.STRING) {
				itemCode = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).toString();
			}
			else {
				int itemCodeValue = (int)row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getNumericCellValue();
				itemCode = Integer.toString(itemCodeValue);				
			}
			String itemLabel = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).toString();
			int level = getItemLevelDepth(itemCode);
			logger.debug(itemCode);

			// Create the resource representing the classification item (skos:Concept), with its code and label
			Resource itemResource = model.createResource(getItemURI(itemCode), SKOS.Concept);
			itemResource.addProperty(SKOS.notation, itemCode);
			itemResource.addProperty(SKOS.prefLabel, itemLabel, "en");

			// Attach the item to its level
			Resource levelResource = (Resource) levelList.get(level - 1);
			levelResource.addProperty(SKOS.member, itemResource);
			
			// Attach the item to its classification
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
	 * Creates a model containing the resources representing the correspondence between ISIC and NAICS.
	 */
	private void createISICCorrespondence() throws Exception {

		// Read the Excel file and create the classification items
		InputStream sourceFile = new FileInputStream(LOCAL_FOLDER + NAICS_ISIC_FILE);
		Sheet items = WorkbookFactory.create(sourceFile).getSheetAt(0); // The correspondence is on the second sheet
		try {sourceFile.close();} catch(Exception ignored) {}

		// Creation of the correspondence table resource
		Resource table = model.createResource(BASE_CORRESPONDENCE_URI + "correspondence", XKOS.Correspondence);
		table.addProperty(SKOS.definition, "Correspondence table between ISIC Rev.4 and NAICS 2012");
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI("ISIC", "4")));
		table.addProperty(XKOS.compares, scheme);

		Iterator<Row> rows = items.rowIterator ();
		while (rows.hasNext() && rows.next().getRowNum() < 2); // Skip the header line and the two first lines where NAICS code is 0
		while (rows.hasNext()) {
			Row row = rows.next();
			//012X (0121 to 0129), 014X (0141 to 0146, 0149), 331X (3311 to 3315, 3319)
			String naicsCode = getCodeInCell(row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK));
			String isicCode = getCodeInCell(row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK));
			String comment = row.getCell(4).toString().trim();
			// Leading zero is not read correctly
			if (isicCode.length() == 3) isicCode = "0" + isicCode;
			Resource association = model.createResource(BASE_CORRESPONDENCE_URI + "association/" + isicCode + "-" + naicsCode, XKOS.ConceptAssociation);
			association.addProperty(RDFS.label, "ISIC Rev.4 " + isicCode + " - NAICS 2012 " + naicsCode);
			if (comment.length() > 0) association.addProperty(RDFS.comment, model.createLiteral(comment, "en"));
			if (GROUP_LINK_CASES.containsKey(isicCode)) {
				for (String classCode : GROUP_LINK_CASES.get(isicCode)) {
					logger.debug("Class code - " + classCode);
					association.addProperty(XKOS.sourceConcept, model.createResource(Names.getItemURI(classCode, "ISIC", "4")));
				}	
			} else {
				logger.debug("ISIC code - '" + isicCode + "'");
				association.addProperty(XKOS.sourceConcept, model.createResource(Names.getItemURI(isicCode, "ISIC", "4")));	
			}
			association.addProperty(XKOS.targetConcept, model.createResource(getItemURI(naicsCode)));
		}
	}

	/**
	 * Reads a code in a cell, whether the cell type is numeric or string.
	 * 
	 * @param cell The <code>Cell</code> containing the code to read
	 * @return The code value as a string.
	 */
	private String getCodeInCell(Cell cell) {

		String code;
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
		scheme = model.createResource(BASE_URI + "naics", SKOS.ConceptScheme);
		scheme.addProperty(SKOS.prefLabel, model.createLiteral("North American Industry Classification System (NAICS) 2012", "en"));
		scheme.addProperty(SKOS.notation, "NAICS 2012");
		scheme.addProperty(SKOS.definition, model.createLiteral("The North American Industry Classification System (NAICS) is the standard used by Federal statistical agencies in classifying business establishments for the purpose of collecting, analyzing, and publishing statistical data related to the U.S. business economy.", "en"));
		scheme.addProperty(DC.publisher, model.createResource("http://www.census.gov"));
		scheme.addProperty(DC.date, model.createTypedLiteral("2012-01-01", "http://www.w3.org/2001/XMLSchema#date"));
		scheme.addProperty(FOAF.homepage, model.createResource("http://www.census.gov/eos/www/naics/"));
		scheme.addProperty(XKOS.covers, model.createResource("http://eurovoc.europa.eu/5992"));
		scheme.addProperty(XKOS.numberOfLevels, model.createTypedLiteral(5));

		// Create the resources representing the levels (xkos:ClassificationLevel)
		Resource level1 = model.createResource(BASE_URI + "/sectors", XKOS.ClassificationLevel);
		level1.addProperty(SKOS.prefLabel, model.createLiteral("NAICS 2012 - level 1 - Sectors", "en"));
		level1.addProperty(XKOS.depth, model.createTypedLiteral(1));
		level1.addProperty(XKOS.notationPattern, "[1-9]{2}");
		level1.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/naics2012/sector"));

		Resource level2 = model.createResource(BASE_URI + "/subsectors", XKOS.ClassificationLevel);
		level2.addProperty(SKOS.prefLabel, model.createLiteral("NAICS 2012 - level 2 - Subsectors", "en"));
		level2.addProperty(XKOS.depth, model.createTypedLiteral(2));
		level2.addProperty(XKOS.notationPattern, "[1-9]{3}");
		level2.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/naics2012/subsector"));

		Resource level3 = model.createResource(BASE_URI + "/groups", XKOS.ClassificationLevel);
		level3.addProperty(SKOS.prefLabel, model.createLiteral("NAICS 2012 - level 3 - Groups", "en"));
		level3.addProperty(XKOS.depth, model.createTypedLiteral(3));
		level3.addProperty(XKOS.notationPattern, "[1-9]{4}");
		level3.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/naics2012/group"));

		Resource level4 = model.createResource(BASE_URI + "/naics-industries", XKOS.ClassificationLevel);
		level4.addProperty(SKOS.prefLabel, model.createLiteral("NAICS 2012 - level 4 - NAICS Industries", "en"));
		level4.addProperty(XKOS.depth, model.createTypedLiteral(4));
		level4.addProperty(XKOS.notationPattern, "[1-9]{5}");
		level4.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/naics2012/naics-industry"));

		Resource level5 = model.createResource(BASE_URI + "/national-industries", XKOS.ClassificationLevel);
		level5.addProperty(SKOS.prefLabel, model.createLiteral("NAICS 2012 - level 5 - National Industries", "en"));
		level5.addProperty(XKOS.depth, model.createTypedLiteral(5));
		level5.addProperty(XKOS.notationPattern, "[1-9]{5}[0-9]");
		level5.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/naics2012/national-industry"));

		// Attach the level list to the classification
		levelList = model.createList(level1, level2, level3, level4, level5);
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

	}

	/**
	 * Writes the model to the output Turtle file.
	 * 
	 * @throws IOException In case of problem writing the file
	 */
	private void writeModel(String fileName, RDFFormat format) throws IOException {

		RDFDataMgr.write(new FileOutputStream(fileName), model, format) ;
		// Close the model
		model.close();
	}
	
	/**
	 * Computes the parent code for one given code.
	 */
	private static String getParentCode(String code) {

		if ((code.length() <= 2) || (code.contains("-"))) return null;

		String parentCode = code.substring(0, code.length() - 1);
		// There are special cases for "composite" sector codes (31-33, 44-45, 48-49)
		if (code.length() == 3) {
			if (parentCode.startsWith("3")) return "31-33";
			if (parentCode.equals("44") || (parentCode.equals("45"))) return "44-45";
			if (parentCode.equals("48") || (parentCode.equals("49"))) return "48-49";
		}
		return parentCode;
	}

	/**
	 * Computes the URI of a NAICS classification item.
	 * 
	 * @param code The item code.
	 * @return The item URI.
	 */
	private static String getItemURI(String code) {

		if ((code.length() == 2) || (code.contains("-"))) return BASE_URI + "sector/" + code;
		if (code.length() == 6) return BASE_URI + "national-industry/" + code;
		if (code.length() == 5) return BASE_URI + "naics-industry/" + code;
		if (code.length() == 4) return BASE_URI + "group/" + code;
		if (code.length() == 3) return BASE_URI + "subsector/" + code;

		return null;
	}

	/**
	 * Returns the depth of the level to which an item belongs.
	 * 
	 * @param code The item code.
	 * @return The depth of the level.
	 */
	public static int getItemLevelDepth(String code) {

		if ((code.length() == 2) || (code.contains("-"))) return 1;
		return (code.length() - 1);
	}
}
