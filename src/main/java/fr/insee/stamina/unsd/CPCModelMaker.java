package fr.insee.stamina.unsd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
 * The <code>CPCModelMaker</code> creates and saves Jena models corresponding to the CPC classification.
 * 
 * Only CPC revisions 1.1, 2 and 2.1 are considered here, but the program can easily be extended to other versions.
 * The data is extracted from MS Access files available on the UNSD web site (http://unstats.un.org/unsd/cr/registry/regdnld.asp).
 * 
 * @author Franck Cotton
 * @version 0.10, 12 May 2016
 */
public class CPCModelMaker {

	/** Directory for input files */
	private static String INPUT_FOLDER = "D:\\Temp\\unsd\\";
	/** Directory for output files */
	private static String OUTPUT_FOLDER = "src/main/resources/data/";
	
	/** Files containing the Access databases */
	private static Map<String, String> CPC_ACCESS_FILE = new HashMap<String, String>();
	/** Name of the Access tables containing the data */
	private static Map<String, String> CPC_ACCESS_TABLE = new HashMap<String, String>();
	// There are no French labels for the CPC on the UNSD web site
	/** CSV files containing the additional Spanish labels */
	private static Map<String, String> CPC_SPANISH_LABELS_FILE = new HashMap<String, String>();
	// Initialization of the static properties
	static {
		CPC_ACCESS_FILE.put("1.1", "cpc_v11_english.mdb");
		CPC_ACCESS_FILE.put("2", "CPCv2_english.mdb");
		CPC_ACCESS_FILE.put("2.1", "CPC21_english.mdb");
		CPC_ACCESS_TABLE.put("1.1", "tblTitles_English_CPCV11");
		CPC_ACCESS_TABLE.put("2", "CPC2-structure");
		CPC_ACCESS_TABLE.put("2.1", "CPC21-structure");
		CPC_SPANISH_LABELS_FILE.put("1.1", null); // No Spanish labels for CPC Ver.1.1
		CPC_SPANISH_LABELS_FILE.put("2", "CPCv2_Spanish_structure.txt");
		CPC_SPANISH_LABELS_FILE.put("2.1", null); // No Spanish labels for CPC Ver.2.1
	}

	/** CSV file containing the correspondences between CPC Ver.2 and CPC Ver.2.1 */
	private static String CPC2_TO_CPC21_FILE = "cpc2-cpc21.txt";
	/** CSV file containing the correspondences between CPC Ver.1.1 and CPC Ver.2 */
	private static String CPC11_TO_CPC2_FILE = "CPCv11_CPCv2.txt";

	/** Log4J2 logger */
	private static final Logger logger = LogManager.getLogger(CPCModelMaker.class);

	/** Current Jena model */
	private Model cpcModel = null;
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

		CPCModelMaker modelMaker = new CPCModelMaker();
		logger.debug("New CPCModelMaker instance created");
		modelMaker.createCPCModel("1.1", true);
		modelMaker.createCPCModel("2", false);
		modelMaker.createCPCModel("2.1", false);
		modelMaker.createCorrespondenceModels();
		logger.debug("Program terminated");
	}

	/**
	 * Creates an Jena model corresponding to a version of CPC and saves it to a Turtle file.
	 * 
	 * @param version Version of the classification ("1.1", "2", "2.1").
	 * @param withNotes Boolean indicating if the explanatory notes must be produced in the model.
	 * @throws Exception In case of problem getting the data or creating the file.
	 */
	private void createCPCModel(String version, boolean withNotes) throws Exception {

		logger.debug("Construction of the Jena model for CPC version " + version);

		// Init the Jena model for the given version of the classification
		initModel(version);

		// For CPC, the naming of the columns in the tables between different versions is not coherent
		String codeColumnName = ("2.1".equals(version)) ? "CPC21code" : "Code";
		String labelColumnName = ("2.1".equals(version)) ? "CPC21title" : "Description";
		String noteColumnName = ("2.1".equals(version)) ? "CPC21ExplanatoryNote" : "ExplanatoryNote";

		// Open a cursor on the main table and iterate through all the records
		Table table = DatabaseBuilder.open(new File(INPUT_FOLDER + CPC_ACCESS_FILE.get(version))).getTable(CPC_ACCESS_TABLE.get(version));
		Cursor cursor = CursorBuilder.createCursor(table);
		logger.debug("Cursor defined on table " + CPC_ACCESS_TABLE.get(version));
		for (Row row : cursor.newIterable()) {
			String itemCode = row.getString(codeColumnName);
			Resource itemResource = cpcModel.createResource(Names.getItemURI(itemCode, "CPC", version), SKOS.Concept);
			itemResource.addProperty(SKOS.notation, cpcModel.createLiteral(itemCode));
			itemResource.addProperty(SKOS.prefLabel, cpcModel.createLiteral(row.getString(labelColumnName), "en"));
			// Add explanatory notes if requested
			// TODO For CPC Ver.2 and CPC Ver.2.1, all notes together in one column. For now all is recorded as a skos:skosNote
			if (withNotes) {
				String note = row.getString(noteColumnName + "ExplanatoryNote");
				if ((note != null) && (note.length() > 0)) itemResource.addProperty(SKOS.scopeNote, cpcModel.createLiteral(note, "en"));
			}
			// Create the SKOS hierarchical properties for the item
			itemResource.addProperty(SKOS.inScheme, scheme);
			String parentCode = getParentCode(itemCode);
			if (parentCode == null) {
				scheme.addProperty(SKOS.hasTopConcept, itemResource);
				itemResource.addProperty(SKOS.topConceptOf, scheme);
			} else {
				Resource parentResource = cpcModel.createResource(Names.getItemURI(parentCode, "CPC", version), SKOS.Concept);
				parentResource.addProperty(SKOS.narrower, itemResource);
				itemResource.addProperty(SKOS.broader, parentResource);
			}
			// Add the item as a member of its level
			Resource level = levels.get(itemCode.length() - 1);
			level.addProperty(SKOS.member, itemResource);
		}
		logger.debug("Finished reading table " + CPC_ACCESS_TABLE.get(version));
		// Create additional labels if they exist
		if (CPC_SPANISH_LABELS_FILE.get(version) != null)
			this.addLabels(INPUT_FOLDER + CPC_SPANISH_LABELS_FILE.get(version), version, "es");

		// Write the Turtle file and clear the model
		String turtleFileName = OUTPUT_FOLDER + Names.getCSContext("CPC", version) + ".ttl";
		cpcModel.write(new FileOutputStream(turtleFileName), "TTL");
		logger.info("The Jena model for CPC Ver." + version + " has been written to " + turtleFileName);
		cpcModel.close();
	}

	/**
	 * Initializes the Jena model with the namespaces and the top resources (concept scheme, levels, ...).
	 * 
	 * @param version The CPC version corresponding to the model.
	 */
	private void initModel(String version) {

		logger.debug("Initializing Jena model for CPC version " + version);
		cpcModel = ModelFactory.createDefaultModel();
		cpcModel.setNsPrefix("skos", SKOS.getURI());
		cpcModel.setNsPrefix("xkos", XKOS.getURI());

		// Create the classification, classification levels and their properties
		scheme = cpcModel.createResource(Names.getCSURI("CPC", version), SKOS.ConceptScheme);
		scheme.addProperty(SKOS.prefLabel, cpcModel.createLiteral(Names.getCSLabel("CPC", version), "en"));
		scheme.addProperty(SKOS.notation, Names.getCSShortName("CPC", version));
		int numberOfLevels = Names.LEVEL_NAMES.get("CPC").size();
		scheme.addProperty(XKOS.numberOfLevels, cpcModel.createTypedLiteral(numberOfLevels));

		levels = new ArrayList<Resource>();
		for (int levelIndex = 1; levelIndex <= numberOfLevels; levelIndex++) {
			Resource level = cpcModel.createResource(Names.getClassificationLevelURI("CPC", version, levelIndex), XKOS.ClassificationLevel);
			level.addProperty(SKOS.prefLabel, cpcModel.createLiteral(Names.getClassificationLevelLabel("CPC", version, levelIndex), "en"));
			level.addProperty(XKOS.depth, cpcModel.createTypedLiteral(levelIndex));
			levels.add(level);
		}
		scheme.addProperty(XKOS.levels, cpcModel.createList(levels.toArray(new Resource[levels.size()])));
	}

	/**
	 * Adds labels read in a CSV file to the Jena model.
	 * 
	 * @param filePath The path of the CSV file.
	 * @param version The version of the CPC classification.
	 * @param language The tag representing the language of the labels ("fr", "es", etc.). 
	 */
	private void addLabels(String filePath, String version, String language) {

		if (filePath == null) return;

		logger.debug("Preparing to create additional labels for version " + version + ", language is " + language);
		try {
			Reader reader = new InputStreamReader(new FileInputStream(filePath), "Cp1252");
			CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
			for (CSVRecord record : parser) {
				String itemCode = record.get(0);
				Resource itemResource = cpcModel.createResource(Names.getItemURI(itemCode, "CPC", version));
				itemResource.addProperty(SKOS.prefLabel, cpcModel.createLiteral(record.get(1), language));
			}
			parser.close();
			reader.close();
		} catch (Exception e) {
			logger.error("Error adding labels from " + filePath, e);
		}
	}

	/**
	 * Creates the models for correspondences between CPC Ver.1.1, CPC Ver.2 and CPC Ver.2.1.
	 */
	private void createCorrespondenceModels() {

		logger.debug("Preparing to create model for the correspondences between CPC Ver.1.1 and CPC Ver.2");
		cpcModel = ModelFactory.createDefaultModel();
		cpcModel.setNsPrefix("rdfs", RDFS.getURI());
		cpcModel.setNsPrefix("skos", SKOS.getURI());
		cpcModel.setNsPrefix("xkos", XKOS.getURI());
		cpcModel.setNsPrefix("asso", Names.getCorrespondenceBaseURI("CPC", "1.1", "CPC", "2") + "association/");
		// TODO Not sure it is a good idea to create a prefix associated with different namespaces: see what happens when you load data

		// Creation of the correspondence table resource
		Resource table = cpcModel.createResource(Names.getCorrespondenceURI("CPC",  "1.1",  "CPC",  "2"), XKOS.Correspondence);
		table.addProperty(SKOS.definition, "Correspondence table between CPC Ver.1.1 - CPC Ver.2");
		table.addProperty(XKOS.compares, cpcModel.createResource(Names.getCSURI("CPC", "1.1")));
		table.addProperty(XKOS.compares, cpcModel.createResource(Names.getCSURI("CPC", "2")));
		try {
			logger.debug("Preparing to read correspondence data from " + CPC11_TO_CPC2_FILE);
			Reader reader = new FileReader(INPUT_FOLDER + CPC11_TO_CPC2_FILE);
			CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
			for (CSVRecord record : parser) {
				String cpc11Code = record.get("CPC11Code");
				String cpc2Code = record.get("CPC2Code");
				Resource association = cpcModel.createResource(Names.getAssociationURI(cpc11Code, "CPC",  "1.1", cpc2Code, "CPC", "2"), XKOS.ConceptAssociation);
				association.addProperty(RDFS.label, "CPC Ver.1.1 " + cpc11Code + " - CPC Ver.2 " + cpc2Code);
				association.addProperty(XKOS.sourceConcept, cpcModel.createResource(Names.getItemURI(cpc11Code, "CPC", "1.1")));
				association.addProperty(XKOS.targetConcept, cpcModel.createResource(Names.getItemURI(cpc2Code, "CPC", "2")));
				// There are no descriptions of the correspondences for CPC11-CPC2
				table.addProperty(XKOS.madeOf, association);
				// TODO Add 'partial' information
			}
			parser.close();
			reader.close();
		} catch (Exception e) {
			logger.error("Error reading correspondences from " + CPC11_TO_CPC2_FILE, e);
		}
		// Write the Turtle file and clear the model
		String turtleFileName = Names.getCorrespondenceContext("CPC", "1.1", "CPC", "2") + ".ttl";
		try {
			cpcModel.write(new FileOutputStream(OUTPUT_FOLDER + turtleFileName), "TTL");
			logger.info("The Jena model for the correspondence between CPC Ver.1.1 and CPC Ver.2 has been written to " + OUTPUT_FOLDER + turtleFileName);
		} catch (FileNotFoundException e) {
			logger.error("Error saving the CPC11-CPC2 correspondences to " + turtleFileName, e);
		}
		cpcModel.close();

		logger.debug("Preparing to create model for the correspondences between CPC Ver.2 and CPC Ver.2.1");
		cpcModel = ModelFactory.createDefaultModel();
		cpcModel.setNsPrefix("rdfs", RDFS.getURI());
		cpcModel.setNsPrefix("skos", SKOS.getURI());
		cpcModel.setNsPrefix("xkos", XKOS.getURI());
		cpcModel.setNsPrefix("asso", Names.getCorrespondenceBaseURI("CPC", "2", "CPC", "2.1") + "association/");

		// Creation of the correspondence table resource
		table = cpcModel.createResource(Names.getCorrespondenceURI("CPC",  "2",  "CPC",  "2.1"), XKOS.Correspondence);
		table.addProperty(SKOS.definition, "CPC Ver.2 - CPC Ver.2.1 correspondence table");
		table.addProperty(XKOS.compares, cpcModel.createResource(Names.getCSURI("CPC", "2")));
		table.addProperty(XKOS.compares, cpcModel.createResource(Names.getCSURI("CPC", "2.1")));
		// Comment extracted from the 'readme.txt' file (could be better in a skos:historyNote)
		table.addProperty(RDFS.comment, cpcModel.createLiteral("The correspondence does not yet include divisions 61 and 62 of the CPC", "en"));
		try {
			logger.debug("Preparing to read correspondence data from " + CPC2_TO_CPC21_FILE);
			Reader reader = new FileReader(INPUT_FOLDER + CPC2_TO_CPC21_FILE);
			CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
			for (CSVRecord record : parser) {
				String cpc2Code = record.get("CPC2code");
				String cpc21Code = record.get("CPC21code");
				Resource association = cpcModel.createResource(Names.getAssociationURI(cpc2Code, "CPC",  "2", cpc21Code, "CPC", "2.1"), XKOS.ConceptAssociation);
				association.addProperty(RDFS.label, "CPC Ver.2 " + cpc2Code + " - CPC Ver.2.1 " + cpc21Code);
				association.addProperty(XKOS.sourceConcept, cpcModel.createResource(Names.getItemURI(cpc2Code, "CPC", "2")));
				association.addProperty(XKOS.targetConcept, cpcModel.createResource(Names.getItemURI(cpc21Code, "CPC", "2.1")));
				// There are no descriptions of the correspondences for CPC2-CPC2.1
				table.addProperty(XKOS.madeOf, association);
				// TODO Add 'partial' information
			}
			parser.close();
			reader.close();
		} catch (Exception e) {
			logger.error("Error reading correspondences from " + CPC2_TO_CPC21_FILE, e);
		}
		// Write the Turtle file and clear the model
		turtleFileName = Names.getCorrespondenceContext("CPC", "2", "CPC", "2.1") + ".ttl";
		try {
			cpcModel.write(new FileOutputStream(OUTPUT_FOLDER + turtleFileName), "TTL");
			logger.info("The Jena model for the correspondence between CPC Ver.2 and CPC Ver.2.1 has been written to " + OUTPUT_FOLDER + turtleFileName);
		} catch (FileNotFoundException e) {
			logger.error("Error saving the CPC2-CPC21 correspondence to " + turtleFileName, e);
		}
		cpcModel.close();
	}

	/**
	 * Returns the code of the parent of the item whose code is provided.
	 * 
	 * @param code The code of the item.
	 * @return The code of the parent of the item.
	 */
	public static String getParentCode(String code) {

		if ((code == null) || (code.length() <= 1)) return null;

		return code.substring(0, code.length() - 1);
	}
}
