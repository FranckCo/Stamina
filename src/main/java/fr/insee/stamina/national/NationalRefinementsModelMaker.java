package fr.insee.stamina.national;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import fr.insee.stamina.utils.Names;
import fr.insee.stamina.utils.XKOS;

/**
 * The <code>NationalRefinementsModelMaker</code> class creates and saves the Jena model corresponding to correspondences with national refinements of the NACE or CPA.
 * 
 * @author Franck Cotton
 */
public class NationalRefinementsModelMaker {

	/** Base local folder for reading and writing files */
	public static String LOCAL_FOLDER = "D:\\Temp\\NAICS\\";

	/** File name of the spreadsheet containing the last level of Ateco */
	public static String ATECO_EXCEL_FILE = LOCAL_FOLDER + "ateco_struttura_17dicembre_2008.xls";

	/** File name of the file containing the RDF representation of the Ateco 2007 classification */
	public static String ATECO_TTL_FILE = LOCAL_FOLDER + "ateco2007.rdf";

	/** File name of the spreadsheet containing the last level of NAF rév. 2 */
	public static String NAF_EXCEL_FILE = LOCAL_FOLDER + "naf2008_liste_n5.xls";

	/** Name of the file containing the RDF representation of the NAF rév. 2 classification */
	public static String NAF_RDF_FILE = LOCAL_FOLDER + "naf08.rdf";

	/** File name of the spreadsheet containing the last level of CPF rév. 2.1 */
	public static String CPF_EXCEL_FILE = LOCAL_FOLDER + "cpf2015_liste_n6.xls";

	/** Name of the file containing the RDF representation of the NAF rév. 2 classification */
	public static String CPF_RDF_FILE = LOCAL_FOLDER + "cpf15.rdf";

	/** Name of the file containing the RDF representation of the correspondence between NAF rév. 2 and CPF rév 2.1 */
	public static String NAF_CPF_RDF_FILE = LOCAL_FOLDER + "correspondancesNafCpf.rdf";

	/** Base URI for all resources in the Ateco 2007 classification model */
	public static String ATECO_BASE_URI = "http://www.ims/concepts/ateco2007/Ateco2007/";

	/** Base URI for all resources in the NAF rév. 2 classification model */
	public static String NAF_BASE_URI = "http://stamina-project.org/codes/nafr2/";

	/** Base URI for all resources in the CPF rév 2.1 classification model */
	public static String CPF_BASE_URI = "http://stamina-project.org/codes/cpfr21/";

	/** Base URI for the RDF resources belonging to the NACE-Ateco correspondence */
	public final static String NACE_ATECO_BASE_URI = "http://stamina-project.org/codes/nacer2-ateco2007/";

	/** Base URI for the RDF resources belonging to the NACE-NAF correspondence */
	public final static String NACE_NAF_BASE_URI = "http://stamina-project.org/codes/nacer2-nafr2/";

	/** Base URI for the RDF resources belonging to the NACE-CPF correspondence */
	public final static String NACE_CPF_BASE_URI = "http://stamina-project.org/codes/nacer2-cpfr21/";

	/** Log4J2 logger */ // This must be before the configuration initialization
	private static final Logger logger = LogManager.getLogger(NationalRefinementsModelMaker.class);

	/** Current Jena model */
	private Model model = null;

	/**
	 * Main method: reads the spreadsheet and creates the triplets in the model.
	 */
	public static void main(String[] args) throws Exception {

		NationalRefinementsModelMaker modelMaker = new NationalRefinementsModelMaker();
		// Check the Ateco data
//		modelMaker.checkAteco();
		// Check the NAF data
		modelMaker.checkNAFCPF();
		// Creation of the NACE-Ateco hierarchy
//		modelMaker.initializeModel();
//		modelMaker.createNACEAtecoHierarchy();
//		modelMaker.writeModel(LOCAL_FOLDER + "nacer2-ateco2007.ttl");
		// Creation of the NACE-NAF hierarchy
//		modelMaker.initializeModel();
//		modelMaker.createNACENAFHierarchy();
//		modelMaker.writeModel(LOCAL_FOLDER + "nacer2-nafr2.ttl");
		// Creation of the CPA-CPF correspondence
//		modelMaker.initializeModel();
//		modelMaker.createCPACPFCorrespondence();
//		modelMaker.writeModel(LOCAL_FOLDER + "cpav2-cpfr21.ttl");
	}

	private void checkAteco() throws IOException {

		// TODO Eliminate empty comments
		model = ModelFactory.createDefaultModel();
		// Other read methods like model.read(ATECO_TTL_FILE, "TURTLE") end in 'content not allowed in prolog' errors
		model.read(new FileInputStream(ATECO_TTL_FILE), null, "TTL");
		// Change to the correct XKOS namespace 
		model.removeNsPrefix("xkos");
		model.setNsPrefix("xkos", XKOS.getURI());
		writeModel(LOCAL_FOLDER + "ateco2007.ttl");
	}

	private void checkNAFCPF() throws IOException {

		// Starting with NAF, first thing is to rewrite the file with the correct base URI
//		String rewrittenFile = replaceInFile(NAF_RDF_FILE, "http://dvrmessnclas01.ad.insee.intra:8080/datalift/", "http://stamina-project.org/");
//		// Then convert the model to Turtle
//		model = ModelFactory.createDefaultModel();
//		model.read(rewrittenFile, "RDF/XML");
//		writeModel(LOCAL_FOLDER + "nafr2.ttl");
//		// Same process for CPF
//		rewrittenFile = replaceInFile(CPF_RDF_FILE, "http://dvrmessnclas01.ad.insee.intra:8080/datalift/", "http://stamina-project.org/");
//		model = ModelFactory.createDefaultModel();
//		model.read(rewrittenFile, "RDF/XML");
//		writeModel(LOCAL_FOLDER + "cpfr21.ttl");
		// Same process for the correspondence
		String rewrittenFile = replaceInFile(NAF_CPF_RDF_FILE, "http://dvrmessnclas01.ad.insee.intra:8080/datalift/", "http://stamina-project.org/");
		model = ModelFactory.createDefaultModel();
		model.read(rewrittenFile, "RDF/XML");
		writeModel(LOCAL_FOLDER + "nafr2-cpfr21.ttl");
		// TODO delete copy files
	}

	/**
	 * Creates a model containing the resources representing the correspondence between NACE and Ateco.
	 */
	private void createNACEAtecoHierarchy() throws Exception {

		// Read the Excel file and create the classification items
		InputStream sourceFile = new FileInputStream(new File(ATECO_EXCEL_FILE));
		Sheet items = WorkbookFactory.create(sourceFile).getSheetAt(0);
		if (sourceFile != null) try {sourceFile.close();} catch(Exception ignored) {}

		// Creation of the correspondence table resource
		Resource table = model.createResource(NACE_ATECO_BASE_URI + "correspondence", XKOS.Correspondence);
		table.addProperty(SKOS.definition, "Correspondence table between NACE Rev. 2 and Ateco 2007");
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI("NACE", "2")));
		table.addProperty(XKOS.compares, model.createResource(ATECO_BASE_URI + "ateco"));

		Iterator<Row> rows = items.rowIterator ();
		while (rows.hasNext() && rows.next().getRowNum() < 3); // Skip the header lines
		while (rows.hasNext()) {
			Row row = rows.next();
			String atecoCode = getCodeInCell(row.getCell(0, Row.CREATE_NULL_AS_BLANK));
			// Items eliminated in 2009 have empty labels
			boolean eliminato = (getCodeInCell(row.getCell(1, Row.CREATE_NULL_AS_BLANK)).trim().length() == 0);
			if (eliminato || (atecoCode.length() < 8)) continue;
			String naceCode = atecoCode.substring(0, 5);

			Resource association = model.createResource(NACE_ATECO_BASE_URI + "association/" + naceCode + "-" + atecoCode, XKOS.ConceptAssociation);
			association.addProperty(RDFS.label, "NACE Rev.2 " + naceCode + " - Ateco 2007 " + atecoCode);
			Resource naceItemResource = model.createResource(Names.getItemURI(naceCode, "NACE", "2"));
			Resource atecoItemResource = model.createResource(getAtecoItemURI(atecoCode));
			association.addProperty(XKOS.sourceConcept, naceItemResource);	
			association.addProperty(XKOS.targetConcept, atecoItemResource);
			// We make the hypothesis that we have exact match when Ateco code ends with '00', broader / narrower match otherwise
			if (atecoCode.endsWith(".00")) {
				naceItemResource.addProperty(SKOS.exactMatch, atecoItemResource);
				atecoItemResource.addProperty(SKOS.exactMatch, naceItemResource);
			} else {
				naceItemResource.addProperty(SKOS.narrowMatch, atecoItemResource);
				atecoItemResource.addProperty(SKOS.broadMatch, naceItemResource);
			}
			table.addProperty(XKOS.madeOf, association);
		}
	}

	/**
	 * Creates a model containing the resources representing the correspondence between NACE and NAF.
	 */
	private void createNACENAFHierarchy() throws Exception {

		// Read the Excel file and create the classification items
		InputStream sourceFile = new FileInputStream(new File(NAF_EXCEL_FILE));
		Sheet items = WorkbookFactory.create(sourceFile).getSheetAt(0);
		if (sourceFile != null) try {sourceFile.close();} catch(Exception ignored) {}

		// Creation of the correspondence table resource
		Resource table = model.createResource(NACE_NAF_BASE_URI + "correspondence", XKOS.Correspondence);
		table.addProperty(SKOS.definition, "Correspondence table between NACE Rev. 2 and NAF rév. 2");
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI("NACE", "2")));
		table.addProperty(XKOS.compares, model.createResource(NAF_BASE_URI + "naf"));

		Iterator<Row> rows = items.rowIterator ();
		while (rows.hasNext() && rows.next().getRowNum() < 2); // Skip the header lines
		while (rows.hasNext()) {
			Row row = rows.next();
			String nafCode = getCodeInCell(row.getCell(0, Row.CREATE_NULL_AS_BLANK));
			String naceCode = nafCode.substring(0, 5);

			Resource association = model.createResource(NACE_NAF_BASE_URI + "association/" + naceCode + "-" + nafCode, XKOS.ConceptAssociation);
			association.addProperty(RDFS.label, "NACE Rev.2 " + naceCode + " - NAF rév. 2 " + nafCode);
			Resource naceItemResource = model.createResource(Names.getItemURI(naceCode, "NACE", "2"));
			Resource nafItemResource = model.createResource(getNAFItemURI(nafCode));
			association.addProperty(XKOS.sourceConcept, naceItemResource);	
			association.addProperty(XKOS.targetConcept, nafItemResource);
			// We make the hypothesis that we have exact match when NAF code ends with 'Z', broader / narrower match otherwise
			if (nafCode.endsWith("Z")) {
				naceItemResource.addProperty(SKOS.exactMatch, nafItemResource);
				nafItemResource.addProperty(SKOS.exactMatch, naceItemResource);
			} else {
				naceItemResource.addProperty(SKOS.narrowMatch, nafItemResource);
				nafItemResource.addProperty(SKOS.broadMatch, naceItemResource);
			}
			table.addProperty(XKOS.madeOf, association);
		}
	}

	/**
	 * Creates a model containing the resources representing the correspondence between NACE and NAF.
	 * CPA and CPF are identical in structure, but they may have different explanatory notes.
	 */
	private void createCPACPFCorrespondence() throws Exception {

		// Read the Excel file and create the classification items
		InputStream sourceFile = new FileInputStream(new File(CPF_EXCEL_FILE));
		Sheet items = WorkbookFactory.create(sourceFile).getSheetAt(0);
		if (sourceFile != null) try {sourceFile.close();} catch(Exception ignored) {}

		// Creation of the correspondence table resource
		Resource table = model.createResource(NACE_CPF_BASE_URI + "correspondence", XKOS.Correspondence);
		table.addProperty(SKOS.definition, "Correspondence table between CPA Ver. 2.1 and CPF rév. 2.1");
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI("CPA", "2.1")));
		table.addProperty(XKOS.compares, model.createResource(CPF_BASE_URI + "cpf"));

		Iterator<Row> rows = items.rowIterator ();
		while (rows.hasNext() && rows.next().getRowNum() < 1); // Skip the header lines
		while (rows.hasNext()) {
			Row row = rows.next();
			String cpfCode = getCodeInCell(row.getCell(0, Row.CREATE_NULL_AS_BLANK));
			String cpaCode = cpfCode; // CPA and CPF codes are identical

			Resource association = model.createResource(NACE_NAF_BASE_URI + "association/" + cpaCode + "-" + cpfCode, XKOS.ConceptAssociation);
			association.addProperty(RDFS.label, "CPA Ver. 2.1 " + cpaCode + " - CPF rév. 2.1 " + cpfCode);
			Resource cpaItemResource = model.createResource(Names.getItemURI(cpaCode, "CPA", "2.1"));
			Resource cpfItemResource = model.createResource(getCPFItemURI(cpfCode));
			association.addProperty(XKOS.sourceConcept, cpaItemResource);	
			association.addProperty(XKOS.targetConcept, cpfItemResource);
			// Since explanatory notes may differ, we only create closeMatch relations
			cpaItemResource.addProperty(SKOS.closeMatch, cpfItemResource);
			cpfItemResource.addProperty(SKOS.closeMatch, cpaItemResource);
			table.addProperty(XKOS.madeOf, association);
		}
	}

	/**
	 * Reads a code in a cell, whether the cell type is numeric or string.
	 * 
	 * @param cell The <code>Cell</code> containing the code to read
	 * @return The code value as a string.
	 */
	private String getCodeInCell(Cell cell) {

		String code = null;
		if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
			code = cell.toString();
		}
		else {
			int itemCodeValue = (int)cell.getNumericCellValue();
			code = Integer.toString(itemCodeValue);				
		}
		return code.trim();
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
	 * @throws Exception In case of problem writing the file	 */
	private void writeModel(String fileName) throws IOException {

		model.write(new FileOutputStream(fileName), "TTL");
		// Close the model
		model.close();
	}
	
	/**
	 * Computes the URI of an Ateco classification item.
	 * 
	 * @param code The item code.
	 * @return The item URI.
	 */
	private static String getAtecoItemURI(String code) {

		// TODO Implement the actual naming scheme for Ateco
		return ATECO_BASE_URI + code.replace(".", "");
	}

	/**
	 * Computes the URI of NAF "sous-classe".
	 * 
	 * @param code The item code.
	 * @return The item URI.
	 */
	private static String getNAFItemURI(String code) {

		// To match the naming scheme used in the NAF dataset
		return NAF_BASE_URI + "sousClasse/" + code;
	}

	/**
	 * Computes the URI of CPF "sous-catégorie".
	 * 
	 * @param code The item code.
	 * @return The item URI.
	 */
	private static String getCPFItemURI(String code) {

		// To match the naming scheme used in the NAF dataset
		return CPF_BASE_URI + "sousCategorie/" + code;
	}

	/**
	 * Copies a file while changing a a given string to another.
	 * 
	 * @param fileName The name of the source file.
	 * @param before A string which will be rewritten.
	 * @param after The new value of the rewritten string.
	 * @return The file name of the copy (name of the original file with a '.copy' extension).
	 * @throws IOException In case of error copying the file.
	 */
	public static String replaceInFile(String fileName, String before, String after) throws IOException {

		String outputFileName = fileName + ".copy";

		logger.debug("Copying " + fileName + " to " + outputFileName + " rewriting '" + before + "' into '" + after + "'");
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName));

		String line = null;
		while ((line = reader.readLine()) != null) {
			writer.write(line.replaceAll(before, after) + System.lineSeparator());
		}
		reader.close();
		writer.close();

		return outputFileName;
	}

}
