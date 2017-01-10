package fr.insee.stamina.national;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import fr.insee.stamina.utils.Names;
import fr.insee.stamina.utils.XKOS;

/**
 * The <code>SKDModelMaker</code> class creates and saves the Jena model corresponding to the Slovenian SKD 2008 classification.
 * 
 * @author Franck Cotton
 */
public class SKDModelMaker {

	/** Base local folder for reading and writing files */
	public static String LOCAL_FOLDER = "src/main/resources/data/";

	/** File name of the spreadsheet containing the SKD structure */
	public static String SKD_FILE = "SKD_2008_V2.csv";

	/** Base URI for the RDF resources belonging to NAICS */
	public final static String BASE_URI = "http://stamina-project.org/codes/skd2008/";

	/** Base URI for the RDF resources belonging to the NACE-SKD correspondence */
	public final static String NACE_SKD_BASE_URI = "http://stamina-project.org/codes/nacer2-skd2008/";

	/** Log4J2 logger */ // This must be before the configuration initialization
	private static final Logger logger = LogManager.getLogger(SKDModelMaker.class);

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

		SKDModelMaker modelMaker = new SKDModelMaker();
		// Creation of the classification and its levels
		modelMaker.initializeModel();
		modelMaker.createClassificationAndLevels();
		modelMaker.populateScheme();
		modelMaker.writeModel(LOCAL_FOLDER + "skd2008.ttl");
		modelMaker.initializeModel();
		modelMaker.createNACESKDHierarchy();
		modelMaker.writeModel(LOCAL_FOLDER + "nacer2-skd2008.ttl");
	}

	/**
	 * Creates the statements corresponding to the classification items.
	 * 
	 * @throws Exception
	 */
	private void populateScheme() throws Exception {

		// Read the CSV file and create the classification items
		logger.info("Preparing to read CSV file " + SKD_FILE);
		// Counters
		int noResults = 0, severalResults = 0, invalidResult = 0, okResult = 0;
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(LOCAL_FOLDER + SKD_FILE), CSVFormat.TDF.withQuote(null).withHeader().withIgnoreEmptyLines());
			for (CSVRecord record : parser) {

			// The code and label are in the first cell, separated by the first space
			String line = row.getCell(0, Row.CREATE_NULL_AS_BLANK).toString();

			int firstSpace = line.indexOf(' ');
			String itemCode = line.substring(0, firstSpace);
			String itemLabel = line.substring(firstSpace + 1).trim();
			int level = getItemLevelDepth(itemCode);

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
	 * Creates a model containing the resources representing the correspondence between NACE and SKD.
	 */
	private void createNACESKDHierarchy() throws Exception {

		// Read the Excel file and create the classification items
		InputStream sourceFile = new FileInputStream(new File(SKD_FILE));
		Sheet items = WorkbookFactory.create(sourceFile).getSheetAt(0);
		if (sourceFile != null) try {sourceFile.close();} catch(Exception ignored) {}

		// Creation of the correspondence table resource
		Resource table = model.createResource(NACE_SKD_BASE_URI + "correspondence", XKOS.Correspondence);
		table.addProperty(SKOS.definition, "Correspondence table between NACE Rev. 2 and SKD 2008");
		table.addProperty(XKOS.compares, model.createResource(Names.getCSURI("NACE", "2")));
		table.addProperty(XKOS.compares, model.createResource(BASE_URI + "skd"));

		Iterator<Row> rows = items.rowIterator ();
		while (rows.hasNext() && rows.next().getRowNum() < 2); // Skip the header lines
		while (rows.hasNext()) {
			Row row = rows.next();
			String line = row.getCell(0, Row.CREATE_NULL_AS_BLANK).toString();

			if (line == null) continue;
			int firstSpace = line.indexOf(' ');
			String skdCode = line.substring(0, firstSpace);

			String naceCode = skdToNACECode(skdCode);

			Resource association = model.createResource(NACE_SKD_BASE_URI + "association/" + naceCode + "-" + skdCode, XKOS.ConceptAssociation);
			association.addProperty(RDFS.label, "NACE Rev.2 " + naceCode + " - SKD 2008 " + skdCode);
			Resource naceItemResource = model.createResource(Names.getItemURI(naceCode, "NACE", "2"));
			Resource skdItemResource = model.createResource(getItemURI(skdCode));
			association.addProperty(XKOS.sourceConcept, naceItemResource);	
			association.addProperty(XKOS.targetConcept, skdItemResource);
			// TODO When is it exact match?
			naceItemResource.addProperty(SKOS.narrowMatch, skdItemResource);
			skdItemResource.addProperty(SKOS.broadMatch, naceItemResource);
			table.addProperty(XKOS.madeOf, association);
		}
	}

	/**
	 * Creates in the model the resources representing the classification and its levels.
	 */
 	public void createClassificationAndLevels() {

		// Create the resource representing the classification (skos:ConceptScheme)
		scheme = model.createResource(BASE_URI + "skd", SKOS.ConceptScheme);
		scheme.addProperty(SKOS.prefLabel, model.createLiteral("SKD_2008 - Standardna klasifikacija dejavnosti 2008, V2", "si"));
		scheme.addProperty(SKOS.prefLabel, model.createLiteral("SKD_2008 - Standard classification of activities 2008, V2", "en"));
		scheme.addProperty(SKOS.notation, "SKD 2008");
		scheme.addProperty(SKOS.definition, model.createLiteral("Standardna klasifikacija dejavnosti (SKD) je obvezen nacionalni standard, ki se uporablja za določanje dejavnosti in za razvrščanje poslovnih subjektov in njihovih delov za potrebe uradnih in drugih administrativnih zbirk podatkov (registri, evidence, podatkovne baze ipd.) ter za potrebe statistike in analitike v državi in na mednarodni ravni. Skladno s 6. členom Uredbe o SKD 2008 je za razlago vsebine postavk klasifikacije dejavnosti pristojen Statistični urad Republike Slovenije. Za razvrščanje enot Poslovnega registra Slovenije po dejavnosti je odgovorna Agencija Republike Slovenije za javnopravne evidence in storitve (AJPES).", "si"));
		scheme.addProperty(SKOS.definition, model.createLiteral("The Standard Classification of Activities (SKD) is the obligatory national standard used for defining the main activity and for classifying business entities and their units for the needs of official and other administrative data collections (registers, records, databases, etc.) and for the needs of national and international statistics and analyses. In line with Article 6 of the Decree on the 2008 Standard Classification of Activities, the Statistical Office of the Republic of Slovenia is authorised to explain the content of classification items. Classification of units of the Business Register of Slovenia by activity is the responsibility of the Agency of the Republic of Slovenia for Public Legal Records and Related Services (AJPES).", "en"));
		scheme.addProperty(DC.publisher, model.createResource("http://www.stat.si"));
		 // TODO Confirm creation date and obtain last modification date
		scheme.addProperty(DCTerms.issued, model.createTypedLiteral("2008-02-07", "http://www.w3.org/2001/XMLSchema#date"));
		scheme.addProperty(DCTerms.modified, model.createTypedLiteral("2008-02-07", "http://www.w3.org/2001/XMLSchema#date"));
		scheme.addProperty(FOAF.homepage, model.createResource("http://www.stat.si/klasje/tabela.aspx?cvn=5531"));
		scheme.addProperty(XKOS.covers, model.createResource("http://eurovoc.europa.eu/5992"));
		scheme.addProperty(XKOS.numberOfLevels, model.createTypedLiteral(5));

		// TODO: check the names of the levels
		// Create the resources representing the levels (xkos:ClassificationLevel)
		Resource level1 = model.createResource(BASE_URI + "/sections", XKOS.ClassificationLevel);
		level1.addProperty(SKOS.prefLabel, model.createLiteral("SKD 2008 - level 1 - Sections", "en"));
		level1.addProperty(XKOS.depth, model.createTypedLiteral(1));
		level1.addProperty(XKOS.notationPattern, "[A-U]");
		level1.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/skd2008/section"));

		Resource level2 = model.createResource(BASE_URI + "/divisions", XKOS.ClassificationLevel);
		level2.addProperty(SKOS.prefLabel, model.createLiteral("SKD 2008 - level 2 - Divisions", "en"));
		level2.addProperty(XKOS.depth, model.createTypedLiteral(2));
		level2.addProperty(XKOS.notationPattern, "[0-9]{2}");
		level2.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/skd2008/division"));

		Resource level3 = model.createResource(BASE_URI + "/groups", XKOS.ClassificationLevel);
		level3.addProperty(SKOS.prefLabel, model.createLiteral("SKD 2008 - level 3 - Groups", "en"));
		level3.addProperty(XKOS.depth, model.createTypedLiteral(3));
		level3.addProperty(XKOS.notationPattern, "[0-9]{2}\.[0-9]");
		level3.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/skd2008/group"));

		Resource level4 = model.createResource(BASE_URI + "/classes", XKOS.ClassificationLevel);
		level4.addProperty(SKOS.prefLabel, model.createLiteral("SKD 2008 - level 4 - Classes", "en"));
		level4.addProperty(XKOS.depth, model.createTypedLiteral(4));
		level4.addProperty(XKOS.notationPattern, "[0-9]{2}\.[0-9]{2}");
		level4.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/skd2008/class"));

		Resource level5 = model.createResource(BASE_URI + "/subclasses", XKOS.ClassificationLevel);
		level5.addProperty(SKOS.prefLabel, model.createLiteral("SKD 2008 - level 5 - Subclasses", "en"));
		level5.addProperty(XKOS.depth, model.createTypedLiteral(5));
		level5.addProperty(XKOS.notationPattern, "[0-9]{2}\.[0-9]{3}");
		level5.addProperty(XKOS.organizedBy, model.createResource("http://stamina-project.org/concepts/skd2008/subclass"));

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
	 * @throws Exception In case of problem writing the file
	 */
	private void writeModel(String fileName) throws IOException {

		model.write(new FileOutputStream(fileName), "TTL");
		// Close the model
		model.close();
	}

	
	/**
	 * Computes the parent code for one given SKD code.
	 */
	private static String getParentCode(String code) {

		if ((code.length() == 1) || (code.length() > 5)) return null;

		if (code.length() == 2) return Names.getNACESectionForDivision(code);

		// For codes of length 3 to 5, parent code is the child code truncated on the right
		return code.substring(0, code.length() - 1);
	}

	/**
	 * Computes the NACE code corresponding to a SKD code.
	 * 
	 * @param skdCode A SKD code.
	 * @return The NACE code corresponding to the SKD code.
	 */
	public static String skdToNACECode(String skdCode) {

		// TODO
		return skdCode;
	}

	/**
	 * Computes the URI of a SKD classification item.
	 * 
	 * @param code The item code.
	 * @return The item URI.
	 */
	private static String getItemURI(String code) {

		int level = getItemLevelDepth(code);

		if (level == 1) return BASE_URI + "section/" + code;
		if (level == 2) return BASE_URI + "division/" + code;
		if (level == 3) return BASE_URI + "group/" + code;
		if (level == 4) return BASE_URI + "class/" + code;
		if (level == 5) return BASE_URI + "subclass/" + code; // TODO Check this

		return null;
	}

	/**
	 * Returns the depth of the level to which an item belongs.
	 * 
	 * @param code The item code.
	 * @return The depth of the level.
	 */
	public static int getItemLevelDepth(String code) {

		return code.length();
	}
}
