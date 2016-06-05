package fr.insee.stamina.national;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

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
	public static String ATECO_FILE = "ateco_struttura_17dicembre_2008.xls";

	/** Base URI for all resources in the Ateco 2007 classification model */
	public static String ATECO_BASE_URI = "http://stamina-project.org/codes/ateco2007/";

	/** Base URI for the RDF resources belonging to the NACE-Ateco correspondence */
	public final static String NACE_ATECO_BASE_URI = "http://stamina-project.org/codes/nacer2-ateco2007/";

	/** Log4J2 logger */ // This must be before the configuration initialization
	private static final Logger logger = LogManager.getLogger(NationalRefinementsModelMaker.class);

	/** Current Jena model */
	private Model model = null;

	/**
	 * Main method: reads the spreadsheet and creates the triplets in the model.
	 */
	public static void main(String[] args) throws Exception {

		NationalRefinementsModelMaker modelMaker = new NationalRefinementsModelMaker();
		// Creation of the NACE-Ateco hierarchy
		modelMaker.initializeModel();
		modelMaker.createNACEAtecoHierarchy();
		modelMaker.writeModel(LOCAL_FOLDER + "nacer2-ateco2007.ttl");
	}

	/**
	 * Creates a model containing the resources representing the correspondence between NACE and Ateco.
	 */
	private void createNACEAtecoHierarchy() throws Exception {

		// Read the Excel file and create the classification items
		InputStream sourceFile = new FileInputStream(new File(LOCAL_FOLDER + ATECO_FILE));
		Sheet items = WorkbookFactory.create(sourceFile).getSheetAt(0);
		if (sourceFile != null) try {sourceFile.close();} catch(Exception ignored) {}

		// Creation of the correspondence table resource
		Resource table = model.createResource(NACE_ATECO_BASE_URI + "correspondence", XKOS.Correspondence);
		table.addProperty(SKOS.definition, "Correspondence table between NACE Rev. 2 and Ateco 2007");
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI("NACE", "2")));
		table.addProperty(XKOS.compares, model.createResource(ATECO_BASE_URI + "Ateco"));

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
			association.addProperty(XKOS.sourceConcept, model.createResource(Names.getItemURI(naceCode, "NACE", "2")));	
			association.addProperty(XKOS.targetConcept, model.createResource(getAtecoItemURI(atecoCode)));
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
		return ATECO_BASE_URI + code;
	}

}
