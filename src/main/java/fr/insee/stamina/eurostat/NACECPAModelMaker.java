package fr.insee.stamina.eurostat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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

import fr.insee.stamina.utils.Names;
import fr.insee.stamina.utils.XKOS;

/**
 * The <code>NACECPAModelMaker</code> creates and saves Jena models corresponding to the European classifications (NACE and CPA).
 * 
 * The source data is downloaded from RAMON
 * 
 * @author Franck Cotton
 * @version 0.10, 2 Jun 2016
 */
public class NACECPAModelMaker {

	/** Base local folder for input files */
	public static String LOCAL_FOLDER = "D:\\Temp\\NACE\\";
	/** Expression for filtering files corresponding to a given classification scheme or correspondence */
	private static Map<String, String> FILE_FILTER = new HashMap<String, String>();
	static {
		FILE_FILTER.put("NACE1.1", "NACE_1_1_*.xml");
		FILE_FILTER.put("NACE2", "NACE_REV2_*.xml");
		FILE_FILTER.put("CPA2008", "CPA_2008_*.xml");
		FILE_FILTER.put("CPA2.1", "CPA_2_1_*.xml");
		// For correspondences, we use the concatenation of keys
		FILE_FILTER.put("NACE1.1NACE2", "NACE REV. 1.1 - NACE REV. 2_*.csv"); // NACE Rev. 1.1 to NACE Rev. 2 correspondence
		FILE_FILTER.put("CPA2008CPA2.1", "CPA 2008 - CPA 2.1_*.csv"); // CPA Ver. 2008 to CPA Ver. 2.1 correspondence
		FILE_FILTER.put("ISIC4NACE2", "ISIC4_NACE2.txt"); // ISIC Rev.4 to NACE Rev. 2 correspondence
	}

	/** XSL transformation file for NACE */
	public static String NACE_XSL_FILE = LOCAL_FOLDER + "nace-to-xkos.xsl";
	/** XSL transformation file for CPA */
	public static String CPA_XSL_FILE = LOCAL_FOLDER + "cpa-to-xkos.xsl";

	/** Log4J2 logger */ // This must be before the configuration initialization
	private static final Logger logger = LogManager.getLogger(NACECPAModelMaker.class);

	/** Table organizing the parameters for easier access */
	public static Map<String, Map<String, TransformationSpecification>> BASE_CONFIGURATION = initializeBaseConfiguration();

	/** Lines to remove from an XML file produced by RAMON (1 is for validity, 3 for efficiency) */
	static List<Integer> ramonLines = Arrays.asList(1, 3);

	/** Current Jena model */
	private Model model = null;

	/**
	 * Main method: basic launcher that produces all the models.
	 * 
	 * @param args Not used.
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		if (BASE_CONFIGURATION == null) throw new Exception("Configuration error - aborting program");
		logger.debug("Creating new instance of model maker");
		NACECPAModelMaker modelMaker = new NACECPAModelMaker();

		// Create the models for the classifications and versions listed in the base configuration
		for (String classification : BASE_CONFIGURATION.keySet()) {
			for (String version : BASE_CONFIGURATION.get(classification).keySet()) {
				if (BASE_CONFIGURATION.get(classification).get(version) == null)
					logger.warn("No input file found for " + Names.getCSShortName(classification, version));
				else try {
					modelMaker.createClassificationModel(classification, version);
				} catch (Exception e) {
					logger.error("Error during the creation of the classification model", e);
				}
				logger.debug("Creation of the Turtle file corresponding to " + classification + version);
			}
		}
		logger.debug("All classification models created, starting to process correspondence models");
		String nace11NACE2CorrespondenceFile = getMatchingFileName(FILE_FILTER.get("NACE1.1NACE2"));
		if (nace11NACE2CorrespondenceFile != null)
			modelMaker.createHistoricalCorrespondenceModel(nace11NACE2CorrespondenceFile, "NACE", "1.1", "2");
		String cpa2008CPA21CorrespondenceFile = getMatchingFileName(FILE_FILTER.get("CPA2008CPA2.1"));
		if (cpa2008CPA21CorrespondenceFile != null) {
			modelMaker.createHistoricalCorrespondenceModel(cpa2008CPA21CorrespondenceFile, "CPA", "2008", "2.1");
			modelMaker.createNACECPACorrespondence(cpa2008CPA21CorrespondenceFile, "2", "2008", 0, false);
			modelMaker.createNACECPACorrespondence(cpa2008CPA21CorrespondenceFile, "2", "2.1", 1, false);
		}
		modelMaker.createISICNACECorrespondence(FILE_FILTER.get("ISIC4NACE2"), "4", "2", false, true);

		logger.debug("End of programm");
	}

	/**
	 * Creates the Jena model corresponding to a given specification and saves it as Turtle.
	 * 
	 * @param classification The classification ("NACE" or "CPA").
	 * @param version The version of the source classification ("1.1" or "2" for NACE, "2008" or "2.1" for CPA).
	 * @throws Exception In case of error during the creation of the model.
	 */
	public void createClassificationModel(String classification, String version) throws Exception {

		TransformationSpecification specification = BASE_CONFIGURATION.get(classification).get(version); // Not null at this stage
		// Strip extraneous or useless lines in Ramon files
		String inputFileName = removeLines(specification.getInputFile(), ramonLines);
		logger.info("Creation of XKOS data for classification " + Names.getCSShortName(classification, version) + " from file " + inputFileName);

		// Execute the transformation and store the result in a RDF/XML file
		String rdfFileName = specification.getOutputFile().replace(".ttl", ".rdf");
		// We need Saxon to process XSLT v2
		TransformerFactory transformerFactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
		Transformer transformer = transformerFactory.newTransformer(new StreamSource(specification.getTransformationFile()));
		transformer.setParameter("base-url", Names.getCSBaseURI(classification, version));
		transformer.setParameter("version", version);
		logger.debug("XSL Transformer created, starting transformation " + specification.getTransformationFile());
		transformer.transform(new StreamSource(inputFileName), new StreamResult(new File(rdfFileName)));
		logger.debug("Transformation ended, result stored in RDF/XML file " + rdfFileName);
		// Convert RDF/XML format to Turtle with Jena
		this.initializeModel();
		model.read(rdfFileName, "RDF/XML");
		model.write(new FileOutputStream(specification.getOutputFile()), "TTL");
		model.close();
		logger.info("The XKOS data for " + Names.getCSShortName(classification, version) + " has been written to Turtle file " + specification.getOutputFile());
		// Delete intermediary input file
		try {
			Files.delete(Paths.get(inputFileName));
			logger.debug("File " + specification.getInputFile() + " deleted");
		} catch (Exception e) {
			logger.debug("Error deleting intermediary file " + inputFileName, e);
		}
	}

	/**
	 * Creates the Jena model corresponding to a correspondence table between successive versions of NACE or CPA.

	 * @param filePath Path of the CSV file containing the source and target codes (in that order) for the correspondence.
	 * @param classification The classification ("NACE" or "CPA").
	 * @param sourceVersion The version of the source classification ("1.1" or "2" for NACE, "2008" or "2.1" for CPA).
	 * @param targetVersion The version of the target classification.
	 * @throws IOException In case of error reading the file or creating the model.
	 */
	public void createHistoricalCorrespondenceModel(String filePath, String classification, String sourceVersion, String targetVersion) throws Exception {

		// Get a local copy of useful naming elements to avoid repeated calls to the naming authority
		String sourceBaseURI = Names.getCSBaseURI(classification, sourceVersion);
		String targetBaseURI = Names.getCSBaseURI(classification, targetVersion);
		String tableBaseURI = Names.getCorrespondenceBaseURI(classification, sourceVersion, classification, targetVersion);
		String sourceCSShortName = Names.getCSShortName(classification, sourceVersion);
		String targetCSShortName = Names.getCSShortName(classification, targetVersion);

		logger.info("Creation of XKOS data for correspondence " + sourceCSShortName + " - " + targetCSShortName + " from file " + filePath);

		String definition = "Correspondence table from " + sourceCSShortName + " to " + targetCSShortName;
		logger.debug(definition + " - Preparing to initialise Jena model");

		this.initializeModel();
		model.setNsPrefix("asso", tableBaseURI + "association/"); // This reduces the output file size

		// Creation of the correspondence table resource
		Resource table = model.createResource(Names.getCorrespondenceURI(classification, sourceVersion, classification, targetVersion), XKOS.Correspondence);
		table.addProperty(SKOS.notation, model.createLiteral(Names.getCorrespondenceShortName(classification, sourceVersion, classification, targetVersion)));
		table.addProperty(SKOS.definition, model.createLiteral(definition, "en"));
		definition = "Table de correspondance entre la " + sourceCSShortName + " et la " + targetCSShortName;
		table.addProperty(SKOS.definition, model.createLiteral(definition, "fr"));
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI(classification, sourceVersion)));
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI(classification, targetVersion)));

		// TODO Ramon correspondences define associations for all levels, while UNSD is only at most detailed level: should we filter Ramon files for non-terminal levels?
		// Ramon CSV files have two headers, so we have to strip the first line
		List<Integer> ramonCSVLines = Collections.singletonList(1);
		String inputFilePath = removeLines(filePath, ramonCSVLines);
		Reader reader = new FileReader(inputFilePath);

		logger.debug("Preparing to read CSV file " + inputFilePath);
		int associationCount = 0;
		CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
		for (CSVRecord record : parser) {
			String sourceItemURI = sourceBaseURI + Names.getItemPathInContext(record.get("Source"), classification, sourceVersion);
			String targetItemURI = targetBaseURI + Names.getItemPathInContext(record.get("Target"), classification, targetVersion);
			String associationURI = tableBaseURI + Names.getAssociationPathInContext(record.get("Source"), record.get("Target"));
			Resource association = model.createResource(associationURI, XKOS.ConceptAssociation);
			association.addProperty(XKOS.sourceConcept, model.createResource(sourceItemURI));
			association.addLiteral(XKOS.targetConcept, model.createResource(targetItemURI));
			String associationLabel = sourceCSShortName + " " + record.get("Source") + " - " + targetCSShortName + " " + record.get("Target");
			association.addProperty(RDFS.label, model.createLiteral(associationLabel));
			table.addProperty(XKOS.madeOf, association);
			associationCount++;
		}
		parser.close();

		String turtleFilePath = getTurtleFilePath(classification, sourceVersion, classification, targetVersion);
		logger.info(associationCount + " associations created - writing model to " + turtleFilePath);
		model.write(new FileOutputStream(turtleFilePath), "TTL");
		model.close();

		// Delete copy of input file
		try {
			Files.delete(Paths.get(inputFilePath));
			logger.debug("File " + inputFilePath + " deleted");
		} catch (Exception e) {
			logger.error("Error deleting temporary file " + inputFilePath);
		}
	}

	/**
	 * Creates the Jena model corresponding to the correspondence table between associated versions of the NACE and the CPA.
	 * The method does not verify the coherence of the versions of NACE and CPA: the NACE code is obtained as a substring of the CPA code.
	 * The input file should contain the CPA target version code in either column 1 or 2. What is in the other column is not important.
	 * 
	 * @param filePath Path of the CSV file containing the CPA codes in the column specified by parameter <code>columnIndex</code>.
	 * @param naceVersion Version of the NACE classification.
	 * @param cpaVersion Version of the CPA classification.
	 * @param columnIndex Index of the column in the CSV file that contains the CPA codes (should be 0 or 1, otherwise the method does not do anything).
	 * @param allLevels Indicates if the associations are produced for all classification levels (<code>true</code>) or only for the most detailed level.
	 * @throws IOException In case of error reading the file or creating the model.
	 */
	public void createNACECPACorrespondence(String filePath, String naceVersion, String cpaVersion, int columnIndex, boolean allLevels) throws Exception {

		// Column index should be 0 or 1.
		if ((columnIndex != 0) && (columnIndex != 1)) {
			logger.error("Column index should be 0 or 1, received " + columnIndex);
			return;
		}

		// Get a local copy of useful naming elements to avoid repeated calls to the naming authority
		String naceBaseURI = Names.getCSBaseURI("NACE", naceVersion);
		String cpaBaseURI = Names.getCSBaseURI("CPA", cpaVersion);
		String tableBaseURI = Names.getCorrespondenceBaseURI("NACE", naceVersion, "CPA", cpaVersion);
		String naceShortName = Names.getCSShortName("NACE", naceVersion);
		String cpaShortName = Names.getCSShortName("CPA", cpaVersion);

		logger.info("Creation of XKOS data for correspondence " + naceShortName + " - " + cpaShortName + " from file " + filePath);

		String definition = "Correspondence table from " + naceShortName + " to " + cpaShortName;
		logger.debug(definition + " - Preparing to initialise Jena model");

		this.initializeModel();
		model.setNsPrefix("asso", tableBaseURI + "association/"); // This reduces the output file size

		// Creation of the correspondence table resource
		Resource table = model.createResource(Names.getCorrespondenceURI("NACE", naceVersion, "CPA", cpaVersion), XKOS.Correspondence);
		table.addProperty(SKOS.notation, model.createLiteral(Names.getCorrespondenceShortName("NACE", naceVersion, "CPA", cpaVersion)));
		table.addProperty(SKOS.definition, model.createLiteral(definition, "en"));
		definition = "Table de correspondance entre la " + naceShortName + " et la " + cpaShortName;
		table.addProperty(SKOS.definition, model.createLiteral(definition, "fr"));
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI("NACE", naceVersion)));
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI("CPA", cpaVersion)));

		// Ramon CSV files have two headers, so we have to strip the first line
		List<Integer> ramonCSVLines = Collections.singletonList(1);
		String inputFilePath = removeLines(filePath, ramonCSVLines);
		Reader reader = new FileReader(inputFilePath);

		logger.debug("Preparing to read CSV file " + inputFilePath);
		int associationCount = 0;
		CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
		for (CSVRecord record : parser) {
			String cpaCode = record.get(columnIndex);
			String cpaItemURI = cpaBaseURI + Names.getItemPathInContext(cpaCode, "CPA", cpaVersion);
			// If only the most detailed level is considered, we retain only codes of length 8 (nn.nn.nn)
			if ((!allLevels) && (cpaCode.length() != 8)) continue;
			String naceCode = (cpaCode.length() <= 5) ? cpaCode : cpaCode.substring(0, 5);
			String naceItemURI = naceBaseURI + Names.getItemPathInContext(naceCode, "NACE", naceVersion);
			String associationURI = tableBaseURI + Names.getAssociationPathInContext(naceCode, cpaCode);
			Resource association = model.createResource(associationURI, XKOS.ConceptAssociation);
			association.addProperty(XKOS.sourceConcept, model.createResource(naceItemURI));
			association.addLiteral(XKOS.targetConcept, model.createResource(cpaItemURI));
			String associationLabel = naceShortName + " " + naceCode + " - " + cpaShortName + " " + cpaCode;
			association.addProperty(RDFS.label, model.createLiteral(associationLabel));
			table.addProperty(XKOS.madeOf, association);
			associationCount++;
		}
		parser.close();

		String turtleFilePath = getTurtleFilePath("NACE", naceVersion, "CPA", cpaVersion);
		logger.info(associationCount + " associations created - writing model to " + turtleFilePath);
		model.write(new FileOutputStream(turtleFilePath), "TTL");
		model.close();

		// Delete copy of input file
		try {
			Files.delete(Paths.get(inputFilePath));
			logger.debug("File " + inputFilePath + " deleted");
		} catch (Exception e) {
			logger.error("Could not delete temporary file " + inputFilePath);
		}
	}

	/**
	 * Creates the Jena model corresponding to the correspondence table between associated versions of the ISIC and the NACE.
	 * The method does not verify the coherence of the versions of NACE and CPA.
	 * 
	 * @param inputFilePath Path of the CSV file containing the correspondences.
	 * @param isicVersion Version of the ISIC classification.
	 * @param naceVersion Version of the NACE classification.
	 * @param allLevels Indicates if the associations are produced for all classification levels (<code>true</code>) or only for the most detailed level.
	 * @param skosProperties Indicates if SKOS narrowMatch and exactMatch properties should be created (<code>true</code>) or not.
	 * @throws IOException In case of error reading the file or creating the model.
	 */
	public void createISICNACECorrespondence(String inputFilePath, String isicVersion, String naceVersion, boolean allLevels, boolean skosProperties) throws Exception {

		// Get a local copy of useful naming elements to avoid repeated calls to the naming authority
		String tableBaseURI = Names.getCorrespondenceBaseURI("ISIC", isicVersion, "NACE", naceVersion);
		String isicShortName = Names.getCSShortName("ISIC", isicVersion);
		String naceShortName = Names.getCSShortName("NACE", naceVersion);

		logger.info("Creation of XKOS data for correspondence " + isicShortName + " - " + naceShortName + " from file " + inputFilePath);

		String definition = "Correspondence table from " + isicShortName + " to " + naceShortName;
		logger.debug(definition + " - Preparing to initialise Jena model");

		this.initializeModel();
		model.setNsPrefix("asso", tableBaseURI + "association/"); // This reduces the output file size

		// Creation of the correspondence table resource
		Resource table = model.createResource(Names.getCorrespondenceURI("ISIC", isicVersion, "NACE", naceVersion), XKOS.Correspondence);
		table.addProperty(SKOS.notation, model.createLiteral(Names.getCorrespondenceShortName("ISIC", isicVersion, "NACE", naceVersion)));
		table.addProperty(SKOS.definition, model.createLiteral(definition, "en"));
		definition = "Table de correspondance entre la " + isicShortName + " et la " + naceShortName;
		table.addProperty(SKOS.definition, model.createLiteral(definition, "fr"));
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI("ISIC", isicVersion)));
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI("NACE", naceVersion)));

		logger.debug("Preparing to read CSV file " + inputFilePath);
		Reader reader = new FileReader(inputFilePath);
		int associationCount = 0;
		CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader());
		for (CSVRecord record : parser) {
			String isicCode = record.get("ISIC4code");
			String isicItemURI = Names.getItemURI(isicCode, "ISIC", isicVersion);
			// If only the most detailed level is considered, we retain only ISIC codes of length 4 (nnnn)
			if ((!allLevels) && (isicCode.length() != 4)) continue;
			String naceCode = record.get("NACE2code");
			String naceItemURI = Names.getItemURI(naceCode, "NACE", naceVersion);
			String associationURI = tableBaseURI + Names.getAssociationPathInContext(isicCode, naceCode);
			Resource association = model.createResource(associationURI, XKOS.ConceptAssociation);
			association.addProperty(XKOS.sourceConcept, model.createResource(isicItemURI));
			association.addLiteral(XKOS.targetConcept, model.createResource(naceItemURI));
			String associationLabel = isicShortName + " " + isicCode + " - " + naceShortName + " " + naceCode;
			association.addProperty(RDFS.label, model.createLiteral(associationLabel));
			table.addProperty(XKOS.madeOf, association);
			associationCount++;
			// NACE is a refinement of ISIC, so we can also create skos:exactMatch and skos:narrowMatch properties if requested
			if (skosProperties) {
				Resource isicItemResource = model.createResource(isicItemURI);
				Resource naceItemResource = model.createResource(naceItemURI);
				if (record.get("ISIC4part").equals("0")) isicItemResource.addProperty(SKOS.exactMatch, naceItemResource);
				else isicItemResource.addProperty(SKOS.closeMatch, naceItemResource);
			}
		}
		parser.close();

		String turtleFilePath = getTurtleFilePath("ISIC", isicVersion, "NACE", naceVersion);
		logger.info(associationCount + " associations created - writing model to " + turtleFilePath);
		model.write(new FileOutputStream(turtleFilePath), "TTL");
		model.close();
	}

	/**
	 * Initializes the Jena model and adds standard prefixes.
	 * 
	 * @throws Exception
	 */
	private void initializeModel() throws Exception {

		try {
			if (model != null) model.close(); // Just in case
		} catch (Exception e) {
			logger.error("Error initializing model - " + e.getMessage());
			throw new Exception();
		}
		model = ModelFactory.createDefaultModel();
		model.setNsPrefix("rdfs", RDFS.getURI());
		model.setNsPrefix("skos", SKOS.getURI());
		model.setNsPrefix("xkos", XKOS.getURI());

		logger.debug("Jena model initialized");

		return;
	}

	/**
	 * Copies a file except identified lines.
	 * 
	 * @param fileName The name of the source file.
	 * @param linesToRemove A list of integers giving the indices (base 1) of the lines to be removed.
	 * @return The file name of the copy (name of the original file with a '.copy' extension).
	 * @throws IOException In case of error copying the file.
	 */
	public static String removeLines(String fileName, List<Integer> linesToRemove) throws IOException {

		String outputFileName = fileName + ".copy";

		logger.debug("Copying " + fileName + " to " + outputFileName + " skipping lines " + linesToRemove);
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));

		String line = null;
		int lineNumber = 0;
		while ((line = reader.readLine()) != null) {
			if (linesToRemove.contains(++lineNumber)) {
				logger.debug("Skipped line number " + lineNumber + ": '" + line + "'");
			} else writer.write(line + System.lineSeparator());
		}
		reader.close();
		writer.close();

		return outputFileName;
	}

	/**
	 * Returns the path of the output Turtle file for a given classification version.
	 *  
	 * @param classification The classification ("NACE" or "CPA").
	 * @param version The version of the classification ("1.1" or "2" for NACE, "2008" or "2.1" for CPA).
	 * @return The path of the Turtle file.
	 */
	private static String getTurtleFilePath(String classification, String version) {

		return LOCAL_FOLDER + Names.getCSContext(classification, version) + ".ttl";
	}

	/**
	 * Returns the path of the output Turtle file for a given classification version.
	 *  
	 * @param sourceClassification Short name of the source classification, e.g. "NACE", "ISIC", etc.
	 * @param sourceVersion Version of the source classification ("4", "2.1", "2008", etc.).
	 * @param targetClassification Short name of the target classification.
	 * @param targetVersion Version of the target classification.
	 * @return The path of the Turtle file.
	 */
	private static String getTurtleFilePath(String sourceClassification, String sourceVersion, String targetClassification, String targetVersion) {

		return LOCAL_FOLDER + Names.getCorrespondenceContext(sourceClassification, sourceVersion, targetClassification, targetVersion) + ".ttl";
	}

	/**
	 * Returns the name of a RAMON file matching a given filter.
	 * 
	 * @param filter The file name filter to match.
	 * @return The name of the most recent file matching the filter, or <code>null</code> if no file matches or in case of error.
	 */
	private static String getMatchingFileName(String filter) {

		logger.debug("Matching files in " + LOCAL_FOLDER + " for filter " + filter);
		List<String> fileList = new ArrayList<>();
		// Get all the files whose name matches the filter 
		DirectoryStream<Path> stream;
		try {
			stream = Files.newDirectoryStream(Paths.get(LOCAL_FOLDER), filter);
			for (Path path: stream) {
				fileList.add(path.getFileName().toString());
			}
			stream.close();
		} catch (IOException e) {
			logger.error("Error accesssing the file system", e);
			return null;
		}
		logger.debug("Found " + fileList.size() + " matching file(s)");
		// If no file matches the specification is set to null, else sort the list and take the last element (ie: most recent file)
		if (fileList.isEmpty()) return null;
		else {
			Collections.sort(fileList);
			logger.debug("Matching file selected: " + fileList.get(fileList.size() - 1));
			return LOCAL_FOLDER + fileList.get(fileList.size() - 1);
		}
	}

	/***
	 * Initializes the global configuration.
	 * 
	 * @return A <code>Map<String, Map<String, Specification>></code> containing the configuration by classification and version.
	 */
	private static Map<String, Map<String, TransformationSpecification>> initializeBaseConfiguration() {

		logger.debug("Initializing global configuration");

		Map<String, Map<String, TransformationSpecification>> baseConfiguration = new HashMap<String, Map<String, TransformationSpecification>>();

		// We first initialize the input file to the file filter
		Map<String, TransformationSpecification> cpaSpecification = new HashMap<String, TransformationSpecification>();
		cpaSpecification.put("2008", new TransformationSpecification(FILE_FILTER.get("CPA2008"), CPA_XSL_FILE, getTurtleFilePath("CPA", "2008")));
		cpaSpecification.put("2.1", new TransformationSpecification(FILE_FILTER.get("CPA2.1"), CPA_XSL_FILE, getTurtleFilePath("CPA", "2.1")));
		Map<String, TransformationSpecification> naceSpecification = new HashMap<String, TransformationSpecification>();
		naceSpecification.put("1.1", new TransformationSpecification(FILE_FILTER.get("NACE1.1"), NACE_XSL_FILE, getTurtleFilePath("NACE", "1.1")));
		naceSpecification.put("2", new TransformationSpecification(FILE_FILTER.get("NACE2"), NACE_XSL_FILE, getTurtleFilePath("NACE", "2")));

		baseConfiguration.put("CPA", cpaSpecification);
		baseConfiguration.put("NACE", naceSpecification);

		// Then we resolve the name of the actual input file (if there is no match, the specification will be null)
		for (String classification : baseConfiguration.keySet()) {
			for (String version : baseConfiguration.get(classification).keySet()) {
				baseConfiguration.get(classification).get(version).setInputFile(getMatchingFileName(FILE_FILTER.get(classification + version)));
				logger.info("Specification for " + classification + " " + version + " is: " + baseConfiguration.get(classification).get(version));
			}
		}

		return baseConfiguration;
	}

	/**
	 * The <code>TransformationSpecification</code> utility class specifies an XSL transformation: input, transformation, output.
	 * 
	 * @author Franck Cotton
	 */
	private static class TransformationSpecification {

		String inputFile = null;
		String transformationFile = null;
		String outputFile = null;
		public TransformationSpecification(String inputFile, String transformationFile, String outputFile) {
			super();
			this.inputFile = inputFile;
			this.transformationFile = transformationFile;
			this.outputFile = outputFile;
		}
		@Override
		public String toString() {
			return this.inputFile + " -> " + this.transformationFile + " -> " + this.outputFile;
		}
		public String getInputFile() {
			return inputFile;
		}
		public void setInputFile(String inputFile) {
			this.inputFile = inputFile;
		}
		public String getTransformationFile() {
			return transformationFile;
		}
		@SuppressWarnings("unused")
		public void setTransformationFile(String transformationFile) {
			this.transformationFile = transformationFile;
		}
		public String getOutputFile() {
			return outputFile;
		}
		@SuppressWarnings("unused")
		public void setOutputFile(String outputFile) {
			this.outputFile = outputFile;
		}
	}
}
