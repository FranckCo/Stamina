package fr.insee.stamina.isic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.healthmarketscience.jackcess.Cursor;
import com.healthmarketscience.jackcess.CursorBuilder;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

import fr.insee.stamina.cpc.CPCModelMaker;
import fr.insee.stamina.utils.XKOS;

/**
 * The <code>ISICModelMaker</code> creates and saves Jena models corresponding to the ISIC classification.
 * 
 * Only ISIC revisions 3.1 and 4 are considered here, but the program can easily be extended to other versions.
 * The data is extracted from MS Access files available on the UNSD web site (http://unstats.un.org/unsd/cr/registry/regdnld.asp).
 * 
 * @author Franck Cotton
 * @version 0.9, 20 Apr 2016
 */
public class ISICModelMaker {

	/** Files containing the Access databases */
	public static Map<String, String> ISIC_ACCESS_FILE = new HashMap<String, String>();
	/** Name of the Access tables containing the data */
	public static Map<String, String> ISIC_ACCESS_TABLE = new HashMap<String, String>();
	/** Name of the Access tables containing the names of the levels */
	public static Map<String, String> ISIC_STRUCTURE_ACCESS_TABLE = new HashMap<String, String>();
	/** Files where the ISIC models will be saved as Turtle */
	public static Map<String, String> ISIC_TURTLE_FILE = new HashMap<String, String>();
	/** Base URIs for the resources */
	public static Map<String, String> ISIC_BASE_URI = new HashMap<String, String>();
	/** Labels for the concept schemes representing the classification versions */
	public static Map<String, String> ISIC_SCHEME_LABEL = new HashMap<String, String>();
	/** Notations for the concept schemes representing the classification versions */
	public static Map<String, String> ISIC_SCHEME_NOTATION = new HashMap<String, String>();
	/** CSV files containing the additional French labels */
	public static Map<String, String> ISIC_FRENCH_LABELS_FILE = new HashMap<String, String>();
	/** CSV files containing the additional Spanish labels */
	public static Map<String, String> ISIC_SPANISH_LABELS_FILE = new HashMap<String, String>();
	/** CSV format of the files containing additional labels */
	public static Map<String, CSVFormat> ISIC_LABELS_FILE_FORMAT = new HashMap<String, CSVFormat>();
	// Initialization of the static properties
	static {
		ISIC_ACCESS_FILE.put("3.1", "D:\\Temp\\unsd\\ISIC31_english.mdb");
		ISIC_ACCESS_FILE.put("4", "D:\\Temp\\unsd\\ISIC4_english.mdb");
		ISIC_ACCESS_TABLE.put("3.1", "tblTitles_English_ISICRev31");
		ISIC_ACCESS_TABLE.put("4", "tblTitles_English_ISICRev4");
		ISIC_STRUCTURE_ACCESS_TABLE.put("3.1", "tblStructure_ISICRev31");
		ISIC_STRUCTURE_ACCESS_TABLE.put("4", "tblStructure_ISICRev4");
		ISIC_TURTLE_FILE.put("3.1", "src/main/resources/data/isic31.ttl");
		ISIC_TURTLE_FILE.put("4", "src/main/resources/data/isic4.ttl");
		ISIC_BASE_URI.put("3.1", "http://stamina-project.org/codes/isicr31/");
		ISIC_BASE_URI.put("4", "http://stamina-project.org/codes/isicr4/");
		ISIC_SCHEME_LABEL.put("3.1", "International Standard Industrial Classification - Rev.3.1");
		ISIC_SCHEME_LABEL.put("4", "International Standard Industrial Classification - Rev.4");
		ISIC_SCHEME_NOTATION.put("3.1", "ISIC Rev.3.1");
		ISIC_SCHEME_NOTATION.put("4", "ISIC Rev.4");
		ISIC_FRENCH_LABELS_FILE.put("3.1", null);
		ISIC_FRENCH_LABELS_FILE.put("4", "D:\\Temp\\unsd\\ISIC_Rev_4_french_structure.txt");
		ISIC_SPANISH_LABELS_FILE.put("3.1", "D:\\Temp\\unsd\\ISIC_Rev_3_1_spanish_structure.txt");
		ISIC_SPANISH_LABELS_FILE.put("4", "D:\\Temp\\unsd\\ISIC_Rev_4_spanish_structure.txt");
		ISIC_LABELS_FILE_FORMAT.put("3.1", CSVFormat.TDF.withQuote(null).withIgnoreEmptyLines());
		ISIC_LABELS_FILE_FORMAT.put("4", CSVFormat.DEFAULT.withHeader());
	}

	/** CSV files containing the correspondences */
	public static String ISIC31_TO_ISIC4_FILE = "D:\\Temp\\unsd\\ISIC31_ISIC4.txt";
	public static String ISIC4_TO_CPC21_FILE = "D:\\Temp\\unsd\\isic4-cpc21.txt";

	/** Base URIs for RDF resources in correspondences. */
	public final static String ISIC31_TO_ISIC4_BASE_URI = "http://stamina-project.org/codes/isicr31-isicr4/";
	public final static String ISIC4_TO_CPC21_BASE_URI = "http://stamina-project.org/codes/isicr4-cpc21/";

	/** File where the ISIC31 to ISIC4 correspondence information is saved as Turtle */
	private static String ISIC31_TO_ISIC4_TTL = "src/main/resources/data/isic31-isic4.ttl";
	private static String ISIC4_TO_CPC21_TTL = "src/main/resources/data/isic4-cpc21.ttl";

	/** Log4J2 logger */
	private static final Logger logger = LogManager.getLogger(ISICModelMaker.class);

	/** Mapping of the division codes to the section codes. */
	private static Map<String, String> divisionsToSections = new HashMap<String, String>();

	/** Names of the items for each level, singular and plural */
	private static String[] levelNames = new String[]{"section", "division", "group", "class", "sections", "divisions", "groups", "classes"};

	/** Current Jena model */
	private Model isicModel = null;
	/** Resource corresponding to the current concept scheme */
	private Resource scheme = null;
	/** List of resources corresponding to the current classification levels */
	private List<Resource> levels = null;

	/**
	 * Main method: basic launcher that produces all the models.
	 * 
	 * @param args Not used.
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		ISICModelMaker modelMaker = new ISICModelMaker();
		logger.debug("New ISICModelMaker instance created");
		modelMaker.createISICModel("4", false);
		modelMaker.createISICModel("3.1", false);
		modelMaker.createCorrespondenceModels();
		logger.debug("Program terminated");
	}

	/**
	 * Creates an Jena model corresponding to a version of ISIC and saves it to a Turtle file.
	 * 
	 * @throws Exception In case of problem getting the data or creating the file.
	 */
	private void createISICModel(String version, boolean withNotes) throws Exception {

		logger.debug("Construction of the Jena model for ISIC version " + version);
		logger.debug("Preparing to read the divisions to sections mapping from table " + ISIC_STRUCTURE_ACCESS_TABLE.get(version) + " in database " + ISIC_ACCESS_FILE.get(version));
		try {
			Table table = DatabaseBuilder.open(new File(ISIC_ACCESS_FILE.get(version))).getTable(ISIC_STRUCTURE_ACCESS_TABLE.get(version));
			Cursor cursor = CursorBuilder.createCursor(table);
			for (Row row : cursor.newIterable().addMatchPattern("CodeLevel", 2))
				divisionsToSections.put(row.getString("Level2"), row.getString("Level1"));
		} catch (IOException e) {
			logger.fatal("Error reading the database", e);
			return;
		}

		// Init the Jena model for the given version of the classification
		initModel(version);

		// Open a cursor on the main table and iterate through all the records
		Table table = DatabaseBuilder.open(new File(ISIC_ACCESS_FILE.get(version))).getTable(ISIC_ACCESS_TABLE.get(version));
		Cursor cursor = CursorBuilder.createCursor(table);
		logger.debug("Cursor defined on table " + ISIC_ACCESS_TABLE.get(version));
		for (Row row : cursor.newIterable()) {
			String itemCode = row.getString("Code");
			Resource itemResource = isicModel.createResource(getItemURI(itemCode, ISIC_BASE_URI.get(version)), SKOS.Concept);
			itemResource.addProperty(SKOS.notation, isicModel.createLiteral(itemCode));
			itemResource.addProperty(SKOS.prefLabel, isicModel.createLiteral(row.getString("Description"), "en"));
			// Add explanatory notes if requested
			if (withNotes) {
				String note = row.getString("ExplanatoryNoteInclusion");
				if ((note != null) && (note.length() > 0)) itemResource.addProperty(XKOS.inclusionNote, isicModel.createLiteral(note, "en"));
				note = row.getString("ExplanatoryNoteExclusion");
				if ((note != null) && (note.length() > 0)) itemResource.addProperty(XKOS.exclusionNote, isicModel.createLiteral(note, "en"));
			}
			// Create the SKOS hierarchical properties for the item
			itemResource.addProperty(SKOS.inScheme, scheme);
			String parentCode = getParentCode(itemCode);
			if (parentCode == null) {
				scheme.addProperty(SKOS.hasTopConcept, itemResource);
				itemResource.addProperty(SKOS.topConceptOf, scheme);
			} else {
				Resource parentResource = isicModel.createResource(getItemURI(parentCode, ISIC_BASE_URI.get(version)), SKOS.Concept);
				parentResource.addProperty(SKOS.narrower, itemResource);
				itemResource.addProperty(SKOS.broader, parentResource);
			}
			// Add the item as a member of its level
			Resource level = levels.get(itemCode.length() - 1);
			level.addProperty(SKOS.member, itemResource);
		}
		// TODO No table/cursor to close?
		logger.debug("Finished reading table " + ISIC_ACCESS_TABLE.get(version));
		// Addition of French and Spanish labels
		this.addLabels(ISIC_FRENCH_LABELS_FILE.get(version), version, "fr");
		this.addLabels(ISIC_SPANISH_LABELS_FILE.get(version), version, "es");

		// Write the Turtle file and clear the model
		isicModel.write(new FileOutputStream(ISIC_TURTLE_FILE.get(version)), "TTL");
		logger.debug("The Jena model for ISIC Rev." + version + " has been written to " + ISIC_TURTLE_FILE.get(version));
		isicModel.close();
	}

	/**
	 * Initializes the Jena model with the namespaces and the top resources (concept scheme, levels, ...).
	 * 
	 * @param version The ISIC version corresponding to the model (will be 4 if not recognized).
	 */
	private void initModel(String version) {

		isicModel = ModelFactory.createDefaultModel();
		isicModel.setNsPrefix("skos", SKOS.getURI());
		isicModel.setNsPrefix("xkos", XKOS.getURI());

		// Create the classification, classification levels and their properties
		String baseURI = ISIC_BASE_URI.get(version);
		String schemeLabel = ISIC_SCHEME_LABEL.get(version);
		scheme = isicModel.createResource(getSchemeURI(version), SKOS.ConceptScheme);
		scheme.addProperty(SKOS.prefLabel, isicModel.createLiteral(schemeLabel, "en"));
		scheme.addProperty(SKOS.notation, ISIC_SCHEME_NOTATION.get(version));
		int numberOfLevels = levelNames.length / 2;
		scheme.addProperty(XKOS.numberOfLevels, isicModel.createTypedLiteral(numberOfLevels));

		levels = new ArrayList<Resource>();
		for (int levelIndex = 1; levelIndex <= numberOfLevels; levelIndex++) {
			String levelName = levelNames[levelIndex + 3];
			Resource level = isicModel.createResource(baseURI + levelNames[levelIndex + 3], XKOS.ClassificationLevel);
			level.addProperty(SKOS.prefLabel, isicModel.createLiteral(schemeLabel + " - " + levelName.substring(0, 1).toUpperCase() + levelName.substring(1), "en"));
			level.addProperty(XKOS.depth, isicModel.createTypedLiteral(levelIndex));
			levels.add(level);
		}

		scheme.addProperty(XKOS.levels, isicModel.createList(levels.toArray(new Resource[levels.size()])));
	}

	/**
	 * Adds labels read in a CSV file to the Jena model.
	 * 
	 * @param filePath The path of the CSV file.
	 * @param version The version of the ISIC classification.
	 * @param language The tag representing the language of the labels ("fr", "es", etc.). 
	 */
	private void addLabels(String filePath, String version, String language) {

		if (filePath == null) return;

		logger.debug("Preparing to create additional labels, language is " + language + ", source file is " + filePath);

		String baseURI = ISIC_BASE_URI.get(version);
		try {
			Reader reader = new InputStreamReader(new FileInputStream(filePath), "Cp1252");
			CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
			for (CSVRecord record : parser) {
				String itemCode = record.get(0);
				Resource itemResource = isicModel.createResource(getItemURI(itemCode, baseURI));
				itemResource.addProperty(SKOS.prefLabel, isicModel.createLiteral(record.get(1), language));
			}
			parser.close();
			reader.close();
		} catch (Exception e) {
			logger.error("Error adding labels from " + filePath, e);
		}
	}

	/**
	 * Creates the models for correspondences between ISIC Rev.3.1 and ISIC Rev.4 and between ISIC Rev.4 and CPC Ver.2.1.
	 */
	private void createCorrespondenceModels() {

		// Create model for the correspondences between ISIC Rev.3.1 and ISIC Rev.4
		isicModel = ModelFactory.createDefaultModel();
		isicModel.setNsPrefix("rdfs", RDFS.getURI());
		isicModel.setNsPrefix("skos", SKOS.getURI());
		isicModel.setNsPrefix("xkos", XKOS.getURI());

		// Creation of the correspondence table resource
		Resource table = isicModel.createResource(ISIC31_TO_ISIC4_BASE_URI + "correspondence", XKOS.Correspondence);
		// TODO Add properties properly
		table.addProperty(SKOS.definition, "Correspondence table between ISIC Rev.3.1 and ISIC Rev.4");
		try {
			Reader reader = new FileReader(ISIC31_TO_ISIC4_FILE);
			CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
			for (CSVRecord record : parser) {
				String isic31Code = record.get("ISIC31code");
				String isic4Code = record.get("ISIC4code");
				Resource association = isicModel.createResource(ISIC31_TO_ISIC4_BASE_URI + isic31Code + "-" + isic4Code, XKOS.ConceptAssociation);
				association.addProperty(XKOS.sourceConcept, getItemURI(isic31Code, ISIC_BASE_URI.get("3.1")));
				association.addProperty(XKOS.targetConcept, getItemURI(isic4Code, ISIC_BASE_URI.get("4")));
				if (record.get("Detail").length() > 0) association.addProperty(RDFS.comment, isicModel.createLiteral(record.get("Detail"), "en"));
				table.addProperty(XKOS.madeOf, association);
				// TODO Add 'partial' information
			}
			parser.close();
			reader.close();
		} catch (Exception e) {
			logger.error("Error reading correspondences from " + ISIC31_TO_ISIC4_FILE, e);
		}
		// Write the Turtle file and clear the model
		try {
			isicModel.write(new FileOutputStream(ISIC31_TO_ISIC4_TTL), "TTL");
		} catch (FileNotFoundException e) {
			logger.error("Error saving the ISIC31-ISIC4 correspondences to " + ISIC31_TO_ISIC4_TTL, e);
		}
		isicModel.close();

		// Create model for the correspondences between ISIC Rev.4 and CPC Ver.2.1
		isicModel = ModelFactory.createDefaultModel();
		isicModel.setNsPrefix("rdfs", RDFS.getURI());
		isicModel.setNsPrefix("skos", SKOS.getURI());
		isicModel.setNsPrefix("xkos", XKOS.getURI());

		// Creation of the correspondence table resource
		table = isicModel.createResource(ISIC4_TO_CPC21_BASE_URI + "correspondence", XKOS.Correspondence);
		// TODO Add properties properly
		table.addProperty(SKOS.definition, "Correspondence table between ISIC Rev.4 and CPC Ver.2.1");
		try {
			Reader reader = new FileReader(ISIC4_TO_CPC21_FILE);
			CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
			for (CSVRecord record : parser) {
				String isic4Code = record.get("ISIC4code");
				String cpc21Code = record.get("CPC21code");
				Resource association = isicModel.createResource(ISIC4_TO_CPC21_BASE_URI + isic4Code + "-" + cpc21Code, XKOS.ConceptAssociation);
				association.addProperty(XKOS.sourceConcept, getItemURI(isic4Code, ISIC4_TO_CPC21_BASE_URI));
				association.addProperty(XKOS.targetConcept, CPCModelMaker.getItemURI(cpc21Code, CPCModelMaker.CPC_BASE_URI.get("2.1")));
				// There are no descriptions for the ISIC4-CPC21 correspondences
				table.addProperty(XKOS.madeOf, association);
				// TODO Add 'partial' information
			}
			parser.close();
			reader.close();
		} catch (Exception e) {
			logger.error("Error reading correspondences from " + ISIC4_TO_CPC21_FILE, e);
		}
		// Write the Turtle file and clear the model
		try {
			isicModel.write(new FileOutputStream(ISIC4_TO_CPC21_TTL), "TTL");
		} catch (FileNotFoundException e) {
			logger.error("Error saving the ISIC4-CPC21 correspondences to " + ISIC4_TO_CPC21_TTL, e);
		}
		isicModel.close();
	}

	/**
	 * Returns the URI of the concept scheme corresponding to a major version of ISIC.
	 * 
	 * @param version The version of the ISIC classification.
	 * @return The URI of the concept scheme.
	 */
	public static String getSchemeURI(String version) {

		return ISIC_BASE_URI.get(version) + "isic";
	}

	/**
	 * Computes the URI of an ISIC item.
	 * 
	 * @param code The item code.
	 * @param baseURI The base URI corresponding to the ISIC version.
	 * @return The item URI, or a blank string if the code type was not recognized.
	 */
	public static String getItemURI(String code, String baseURI) {

		String uri = "";
		if ((code == null) || (code.length() == 0)) return uri;
		if (code.length() < 5)  uri = baseURI + levelNames[code.length() - 1] + "/" + code;

		return uri;
	}

	/**
	 * Returns the code of the parent of the item whose code is provided.
	 * 
	 * @param code The code of the item.
	 * @return The code of the parent of the item.
	 */
	public static String getParentCode(String code) {

		if ((code == null) || (code.length() <= 1)) return null;

		if (code.length() > 2) return code.substring(0, code.length() - 1);

		// Here we have a code of length 2 (division)
		return divisionsToSections.get(code); // Will return null if not found
	}
}
