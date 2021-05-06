package fr.insee.stamina.codes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.insee.stamina.utils.Names;
import fr.insee.stamina.utils.XKOS;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.*;

/**
 * The <code>HSModelMaker</code> creates and saves Jena models corresponding to the Harmonized System classification.
 * 
 * HS 2017 is considered here.
 * The original data used is:
 *  - JSON file giving the codes and English labels: https://comtrade.un.org/data/cache/classificationH5.json
 * 
 * @author Franck Cotton
 * @version 0.1.0, 1 May 2021
 */
public class HSModelMaker {

	/** Directory for input files */
	private static final String INPUT_FOLDER = "src/main/resources/data/in/";
	/** Directory for output files */
	private static final String OUTPUT_FOLDER = "src/main/resources/data/out/";
	
	/** JSON file containing the codes and English labels */
	private static final String HS_2017_BASE_JSON = INPUT_FOLDER + "classificationH5.json";

	/** Turtle file containing the model */
	private static final String HS_2017_TURTLE = OUTPUT_FOLDER + "hs2017.ttl";

	/** Log4J2 logger */
	private static final Logger logger = LogManager.getLogger(HSModelMaker.class);

	/**
	 * Main method: basic launcher that produces all the models.
	 * 
	 * @param args Not used.
	 * @throws Exception In case of problem
	 */
	public static void main(String... args) throws Exception {

		HSModelMaker modelMaker = new HSModelMaker();
		logger.debug("New HSModelMaker instance created");
		Model hsModel = modelMaker.createHSModel("2017", true);
		if (hsModel != null) {
			RDFDataMgr.write(new FileOutputStream(HS_2017_TURTLE), hsModel, RDFFormat.TURTLE);
			hsModel.close();
		}
		logger.debug("Program terminated");
	}

	/**
	 * Returns a Jena model corresponding to the Harmonized System classification.
	 * 
	 * @param version The version of the classification (for now "2017").
	 */
	private Model createHSModel(String version, boolean withNotes) {

		logger.debug("Construction of the Jena model for HS version " + version);

		// Create and init the Jena model for the given version of the classification
		Model hsModel = ModelFactory.createDefaultModel();
		hsModel.setNsPrefix("skos", SKOS.getURI());
		hsModel.setNsPrefix("xkos", XKOS.getURI());
		// Create the resources corresponding to the concept scheme and the levels
		Resource scheme = createScheme(hsModel, version);
		Map<Integer, Resource> levels = createLevels(hsModel, version);
		scheme.addProperty(XKOS.levels, hsModel.createList(levels.values().toArray(new Resource[0])));

		try {
			// Create object mapper instance
			ObjectMapper mapper = new ObjectMapper();
			final JsonNode jsonNode = mapper.readTree(new File(HS_2017_BASE_JSON));
			// The 'results' node is an array that contains the interesting stuff
			final JsonNode resultsNode = jsonNode.get("results");
			if ((resultsNode == null) || !resultsNode.isArray()) {
				logger.fatal("Invalid structure for JSON data: 'results' node absent or not an array");
				return null;
			}
			// Loop through the results, skipping the nodes that are not items
			Resource itemResource, parentResource;
			for (final JsonNode resultNode : resultsNode) {
				final String id = resultNode.get("id").textValue();
				final String parentId = resultNode.get("parent").textValue();
				if (Character.isDigit(id.charAt(0))) {
					// That's a classification item: create corresponding resource and add it to the scheme
					itemResource = hsModel.createResource(Naming.getClassificationItemURI(id, version));
					itemResource.addProperty(SKOS.notation, id);
					itemResource.addProperty(SKOS.inScheme, scheme);
					// To get the label, remove the '{code} - ' part from the 'text' node
					final String itemLabel = resultNode.get("text").textValue().substring(id.length() + 3);
					itemResource.addProperty(SKOS.prefLabel, hsModel.createLiteral(itemLabel, "en"));
					// Attach the item to its level (will throw a NPE if code length not in map keys
					levels.get(id.length()).addProperty(SKOS.member, itemResource);
					// The parent resource should be in the model already: add broader/narrower links
					if (Character.isDigit(parentId.charAt(0))) {
						parentResource = hsModel.createResource(Naming.getClassificationItemURI(parentId, version));
						itemResource.addProperty(SKOS.broader, parentResource);
						parentResource.addProperty(SKOS.narrower, itemResource);
					} else {
						// No parent: item is a top concept
						itemResource.addProperty(SKOS.topConceptOf, scheme);
						scheme.addProperty(SKOS.hasTopConcept, itemResource);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception raised while constructing the model", e);
			return null;
		}
		return hsModel;
	}

	/**
	 * Creates the resource corresponding to the concept scheme and its different properties.
	 *
	 * @param hsModel The model where the resource will be created
	 * @param version The version of the classification (for now "2017").
	 * @return The resource corresponding to the concept scheme.
	 */
	private Resource createScheme(Model hsModel, String version) {

		Resource scheme = hsModel.createResource(Naming.getClassificationURI(version), SKOS.ConceptScheme);
		scheme.addProperty(SKOS.prefLabel, hsModel.createLiteral(Naming.getClassificationLabel(version, false), "en"));
		scheme.addProperty(SKOS.altLabel, hsModel.createLiteral(Naming.getClassificationLabel(version, true), "en"));
		scheme.addProperty(SKOS.notation, Naming.getClassificationNotation(version));

		return scheme;
	}

	/**
	 * Creates the resources corresponding to the classification levels and their properties.
	 *
	 * @param hsModel The model where the resource will be created.
	 * @param version The version of the classification.
	 * @return A map between the level depths and the resources corresponding to the levels.
	 */
	private Map<Integer, Resource> createLevels(Model hsModel, String version) {

		Map<Integer, Resource> levels = new HashMap<>();
		int numberOfLevels = Naming.LEVEL_NAMES.size();
		for (int depth = 1; depth <= numberOfLevels; depth++) {
			Resource level = hsModel.createResource(Naming.getClassificationLevelURI(depth, version), XKOS.ClassificationLevel);
			level.addProperty(SKOS.prefLabel, hsModel.createLiteral(Naming.LEVEL_NAMES.get(depth - 1), "en"));
			level.addProperty(SKOS.notation, hsModel.createLiteral(Naming.LEVEL_NOTATIONS.get(depth - 1)));
			level.addProperty(XKOS.depth, hsModel.createTypedLiteral(depth));
			levels.put(getItemLength(depth), level);
		}
		return levels;
	}

	/**
	 * Returns the code length of the items of a given level specified by its depth.
	 *
	 * @param levelDepth The level depth (1 is highest).
	 * @return The length of the codes for the items of the specified level.
	 */
	private static int getItemLength(int levelDepth) {
		return 2 * levelDepth;
	}

	/**
	 * Constants and methods for the naming of HS resources.
	 */
	private static class Naming {

		final static List<String> LEVEL_NOTATIONS = Arrays.asList("AG2", "AG4", "AG6");
		final static List<String> LEVEL_NAMES = Arrays.asList("Chapters", "Headings", "Subheadings");

		static String getNamingContext(String version) {
			return Names.CLASSIFICATION_BASE_URI + "hs" + version + "/";
		}

		static String getClassificationURI(String version) {
			return getNamingContext(version) + "hs";
		}

		static String getClassificationLabel(String version, boolean alternative) {
			String label = alternative ? "Harmonized System" : "Harmonized Commodity Description and Coding System";
			return label + ", version " + version;
		}

		static String getClassificationNotation(String version) {
			return "HS" + version;
		}

		static String getClassificationLevelURI(int depth, String version) {
			return getNamingContext(version) + "/level/" + LEVEL_NOTATIONS.get(depth - 1);
		}

		static String getClassificationItemURI(String itemId, String version) {
			return getNamingContext(version) + "/item/" + itemId;
		}
	}
}
