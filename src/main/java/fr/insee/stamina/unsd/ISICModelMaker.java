package fr.insee.stamina.unsd;

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

import fr.insee.stamina.utils.Names;
import fr.insee.stamina.utils.XKOS;

/**
 * The <code>ISICModelMaker</code> creates and saves Jena models corresponding to the ISIC classification.
 * 
 * Only ISIC revisions 3.1 and 4 are considered here, but the program can easily be extended to other versions.
 * The data is extracted from MS Access files available on the UNSD web site (http://unstats.un.org/unsd/cr/registry/regdnld.asp).
 * 
 * @author Franck Cotton
 * @version 0.10, 18 May 2016
 */
public class ISICModelMaker {

	/** Directory for input files */
	private static String INPUT_FOLDER = "D:\\Temp\\unsd\\";
	/** Directory for output files */
	private static String OUTPUT_FOLDER = "src/main/resources/data/";

	/** Files containing the Access databases */
	private static Map<String, String> ISIC_ACCESS_FILE = new HashMap<String, String>();
	/** Name of the Access tables containing the data */
	private static Map<String, String> ISIC_ACCESS_TABLE = new HashMap<String, String>();
	/** Name of the Access tables containing the names of the levels */
	private static Map<String, String> ISIC_STRUCTURE_ACCESS_TABLE = new HashMap<String, String>();
	/** CSV files containing the additional French labels */
	private static Map<String, String> ISIC_FRENCH_LABELS_FILE = new HashMap<String, String>();
	/** CSV files containing the additional Spanish labels */
	private static Map<String, String> ISIC_SPANISH_LABELS_FILE = new HashMap<String, String>();
	/** CSV format of the files containing additional labels */
	private static Map<String, CSVFormat> ISIC_LABELS_FILE_FORMAT = new HashMap<String, CSVFormat>();
	// Initialization of the static properties
	static {
		ISIC_ACCESS_FILE.put("3.1", "ISIC31_english.mdb");
		ISIC_ACCESS_FILE.put("4", "ISIC4_english.mdb");
		ISIC_ACCESS_TABLE.put("3.1", "tblTitles_English_ISICRev31");
		ISIC_ACCESS_TABLE.put("4", "tblTitles_English_ISICRev4");
		ISIC_STRUCTURE_ACCESS_TABLE.put("3.1", "tblStructure_ISICRev31");
		ISIC_STRUCTURE_ACCESS_TABLE.put("4", "tblStructure_ISICRev4");
		ISIC_FRENCH_LABELS_FILE.put("3.1", null);
		ISIC_FRENCH_LABELS_FILE.put("4", "ISIC_Rev_4_french_structure.txt");
		ISIC_SPANISH_LABELS_FILE.put("3.1", "ISIC_Rev_3_1_spanish_structure.txt");
		ISIC_SPANISH_LABELS_FILE.put("4", "ISIC_Rev_4_spanish_structure.txt");
		ISIC_LABELS_FILE_FORMAT.put("3.1", CSVFormat.TDF.withQuote(null).withIgnoreEmptyLines());
		ISIC_LABELS_FILE_FORMAT.put("4", CSVFormat.DEFAULT.withHeader());
	}

	/** CSV file containing the correspondence between ISIC Rev.3.1 and ISIC Rev.4 */
	private static String ISIC31_TO_ISIC4_FILE = INPUT_FOLDER + "ISIC31_ISIC4.txt";
	/** CSV file containing the correspondences between ISIC Rev.4 and CPC Ver.2.1 */
	private static String ISIC4_TO_CPC21_FILE = INPUT_FOLDER + "isic4-cpc21.txt";

	/** Log4J2 logger */
	private static final Logger logger = LogManager.getLogger(ISICModelMaker.class);

	/** Mapping of the division codes to the section codes. */
	private static Map<String, String> divisionsToSections = new HashMap<String, String>();

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
	 * @param version Version of the classification ("3.1", "4").
	 * @param withNotes Boolean indicating if the explanatory notes must be produced in the model.
	 * @throws Exception In case of problem getting the data or creating the file.
	 */
	private void createISICModel(String version, boolean withNotes) throws Exception {

		logger.debug("Construction of the Jena model for ISIC version " + version);
		logger.debug("Preparing to read the divisions to sections mapping from table " + ISIC_STRUCTURE_ACCESS_TABLE.get(version) + " in database " + INPUT_FOLDER + ISIC_ACCESS_FILE.get(version));
		try {
			Table table = DatabaseBuilder.open(new File(INPUT_FOLDER + ISIC_ACCESS_FILE.get(version))).getTable(ISIC_STRUCTURE_ACCESS_TABLE.get(version));
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
		Table table = DatabaseBuilder.open(new File(INPUT_FOLDER + ISIC_ACCESS_FILE.get(version))).getTable(ISIC_ACCESS_TABLE.get(version));
		Cursor cursor = CursorBuilder.createCursor(table);
		logger.debug("Cursor defined on table " + ISIC_ACCESS_TABLE.get(version));
		for (Row row : cursor.newIterable()) {
			String itemCode = row.getString("Code");
			Resource itemResource = isicModel.createResource(Names.getItemURI(itemCode, "ISIC", version), SKOS.Concept);
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
				Resource parentResource = isicModel.createResource(Names.getItemURI(parentCode, "ISIC", version), SKOS.Concept);
				parentResource.addProperty(SKOS.narrower, itemResource);
				itemResource.addProperty(SKOS.broader, parentResource);
			}
			// Add the item as a member of its level
			Resource level = levels.get(itemCode.length() - 1);
			level.addProperty(SKOS.member, itemResource);
		}
		logger.debug("Finished reading table " + ISIC_ACCESS_TABLE.get(version));
		// Addition of French and Spanish labels
		if (ISIC_FRENCH_LABELS_FILE.get(version) != null)
			this.addLabels(INPUT_FOLDER + ISIC_FRENCH_LABELS_FILE.get(version), version, "fr");
		if (ISIC_SPANISH_LABELS_FILE.get(version) != null)
			this.addLabels(INPUT_FOLDER + ISIC_SPANISH_LABELS_FILE.get(version), version, "es");

		// Write the Turtle file and clear the model
		String turtleFileName = OUTPUT_FOLDER + Names.getCSContext("ISIC", version) + ".ttl";
		isicModel.write(new FileOutputStream(turtleFileName), "TTL");
		logger.info("The Jena model for ISIC Rev." + version + " has been written to " + turtleFileName);
		isicModel.close();
	}

	/**
	 * Initializes the Jena model with the namespaces and the top resources (concept scheme, levels, ...).
	 * 
	 * @param version The ISIC version corresponding to the model (will be 4 if not recognized).
	 */
	private void initModel(String version) {

		logger.debug("Initializing Jena model for ISIC version " + version);
		isicModel = ModelFactory.createDefaultModel();
		isicModel.setNsPrefix("skos", SKOS.getURI());
		isicModel.setNsPrefix("xkos", XKOS.getURI());

		// Create the classification, classification levels and their properties
		scheme = isicModel.createResource(Names.getCSURI("ISIC", version), SKOS.ConceptScheme);
		scheme.addProperty(SKOS.prefLabel, isicModel.createLiteral(Names.getCSLabel("ISIC", version), "en"));
		scheme.addProperty(SKOS.notation, Names.getCSShortName("ISIC", version));
		int numberOfLevels = Names.LEVEL_NAMES.get("ISIC").size();
		scheme.addProperty(XKOS.numberOfLevels, isicModel.createTypedLiteral(numberOfLevels));

		levels = new ArrayList<Resource>();
		for (int levelIndex = 1; levelIndex <= numberOfLevels; levelIndex++) {
			Resource level = isicModel.createResource(Names.getClassificationLevelLabel("ISIC", version, levelIndex), XKOS.ClassificationLevel);
			level.addProperty(SKOS.prefLabel, isicModel.createLiteral(Names.getClassificationLevelLabel("ISIC", version, levelIndex), "en"));
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
		try {
			Reader reader = new InputStreamReader(new FileInputStream(filePath), "Cp1252");
			CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
			for (CSVRecord record : parser) {
				String itemCode = record.get(0);
				Resource itemResource = isicModel.createResource(Names.getItemURI(itemCode, "ISIC", version));
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

		logger.debug("Preparing to create model for the correspondences between ISIC Rev.3.1 and ISIC Rev.4");
		isicModel = ModelFactory.createDefaultModel();
		isicModel.setNsPrefix("rdfs", RDFS.getURI());
		isicModel.setNsPrefix("skos", SKOS.getURI());
		isicModel.setNsPrefix("xkos", XKOS.getURI());
		//isicModel.setNsPrefix("asso", Names.getCorrespondenceBaseURI("ISIC", "3.1", "ISIC", "4") + "association/");
		// TODO Uncomment previous line to create 'asso' prefix 

		// Creation of the correspondence table resource
		Resource table = isicModel.createResource(Names.getCorrespondenceURI("ISIC",  "3.1",  "ISIC",  "4"), XKOS.Correspondence);
		table.addProperty(SKOS.definition, "Correspondence table between ISIC Rev.3.1 and ISIC Rev.4");
		table.addProperty(XKOS.compares, Names.getCSURI("ISIC", "3.1"));
		table.addProperty(XKOS.compares, Names.getCSURI("ISIC", "4"));
		try {
			Reader reader = new FileReader(ISIC31_TO_ISIC4_FILE);
			CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
			for (CSVRecord record : parser) {
				String isic31Code = record.get("ISIC31code");
				String isic4Code = record.get("ISIC4code");
				Resource association = isicModel.createResource(Names.getAssociationURI(isic31Code, "ISIC", "3.1", isic4Code, "ISIC", "4"), XKOS.ConceptAssociation);
				association.addProperty(XKOS.sourceConcept, Names.getItemURI(isic31Code, "ISIC", "3.1"));
				association.addProperty(XKOS.targetConcept, Names.getItemURI(isic4Code, "ISIC", "4"));
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
		String turtleFileName = Names.getCorrespondenceContext("ISIC", "3.1", "ISIC", "4") + ".ttl";
		try {
			isicModel.write(new FileOutputStream(OUTPUT_FOLDER + turtleFileName), "TTL");
			logger.info("The Jena model for the correspondence between ISIC Rev.3.1 and ISIC Rev.4 has been written to " + OUTPUT_FOLDER + turtleFileName);
		} catch (FileNotFoundException e) {
			logger.error("Error saving the ISIC31-ISIC4 correspondence to " + turtleFileName, e);
		}
		isicModel.close();

		logger.debug("Preparing to create model for the correspondences between ISIC Rev.4 and CPC Ver.2.1");
		isicModel = ModelFactory.createDefaultModel();
		isicModel.setNsPrefix("rdfs", RDFS.getURI());
		isicModel.setNsPrefix("skos", SKOS.getURI());
		isicModel.setNsPrefix("xkos", XKOS.getURI());
		//isicModel.setNsPrefix("asso", Names.getCorrespondenceBaseURI("ISIC", "4", "CPC", "2.1") + "association/");
		// TODO Uncomment previous line to create 'asso' prefix 

		// Creation of the correspondence table resource
		table = isicModel.createResource(Names.getCorrespondenceURI("ISIC",  "4",  "CPC",  "2.1"), XKOS.Correspondence);
		table.addProperty(SKOS.definition, "Correspondence table between ISIC Rev.4 and CPC Ver.2.1");
		table.addProperty(XKOS.compares, Names.getCSURI("ISIC", "4"));
		table.addProperty(XKOS.compares, Names.getCSURI("CPC", "2.1"));
		// Comment extracted from the 'readme.txt' file (could be better in a skos:historyNote)
		table.addProperty(RDFS.comment, isicModel.createLiteral("The correspondence does not yet include divisions 45, 46 and 47 of ISIC", "en"));
		try {
			Reader reader = new FileReader(ISIC4_TO_CPC21_FILE);
			CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
			for (CSVRecord record : parser) {
				String isic4Code = record.get("ISIC4code");
				String cpc21Code = record.get("CPC21code");
				Resource association = isicModel.createResource(Names.getAssociationURI(isic4Code, "ISIC", "4", cpc21Code, "CPC", "2.1"), XKOS.ConceptAssociation);
				association.addProperty(XKOS.sourceConcept, Names.getItemURI(isic4Code, "ISIC", "4"));
				association.addProperty(XKOS.targetConcept, Names.getItemURI(cpc21Code, "CPC", "2.1"));
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
		turtleFileName = Names.getCorrespondenceContext("ISIC", "4", "CPC", "2.1") + ".ttl";
		try {
			isicModel.write(new FileOutputStream(OUTPUT_FOLDER + turtleFileName), "TTL");
			logger.info("The Jena model for the correspondence between ISIC Rev.4 and CPC Ver.2.1 has been written to " + OUTPUT_FOLDER + turtleFileName);
		} catch (FileNotFoundException e) {
			logger.error("Error saving the ISIC4-CPC21 correspondence to " + turtleFileName, e);
		}
		isicModel.close();
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
