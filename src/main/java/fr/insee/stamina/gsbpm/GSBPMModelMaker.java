package fr.insee.stamina.gsbpm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.SKOS;

/**
 * The <code>GSBPMModelMaker</code> class creates and saves the Jena models corresponding to the GSBPM.
 * Individual GSBPM phases and sub-processes are added to the resources already in the ontology.
 * 
 * The GSBPM as raw text and the GSBPM ontology are inputs to this program.
 * 
 * @author Franck Cotton
 * @version 0.2, 21 Aug 2016
 */
public class GSBPMModelMaker {

	/** Input file: GSBPM as text */
	static String GSBPM_TXT = "src/main/resources/data/GSBPM 5.0 - phases.txt"; // File must be encoded in UTF-8 *without BOM*
	/** Input file: GSBPM ontology in Turtle format */
	static String GSBPM_ONTO = "doc/gsbpm/gsbpm-ontology.ttl";
	/** Output file: Turtle file containing the GSBPM individuals */
	static String GSBPM_TTL = "src/main/resources/data/gsbpm.ttl";
	/** Base URI for the GSBPM individuals */
	static String BASE_URI = "http://id.unece.org/models/gsbpm/";
	/** Base URI for the GSBPM ontology */
	static String ONTO_BASE_URI = "http://rdf.unece.org/models/gsbpm#";

	static Model gsbpmModel = null;

	public static void main(String[] args) throws IOException {

		// Initialize the model and populate it with the ontology
		gsbpmModel = ModelFactory.createDefaultModel();
		gsbpmModel.read(new FileInputStream(GSBPM_ONTO), null, "TTL");

		// Read the GSBPM entries in the text file and add them to the model
		GSBPMReader reader = new GSBPMReader();
		List<GSBPMEntry> gsbpmEntries = reader.read(new File(GSBPM_TXT));

		// Create useful resources for the ConceptScheme and the main classes (all are already in the ontology)
		Resource gsbpmCS = gsbpmModel.createResource(BASE_URI + "GSBPM");
		Resource phase = gsbpmModel.createResource(ONTO_BASE_URI + "Phase");
		Resource subProcess = gsbpmModel.createResource(ONTO_BASE_URI + "SubProcess");

		// Iterate through GSBPM entries to create the associated resources
		for (GSBPMEntry entry : gsbpmEntries) {
			Resource activity = null;
			if (entry.isPhase()) activity = gsbpmModel.createResource(BASE_URI + entry.getCode(), phase);
			else activity = gsbpmModel.createResource(BASE_URI + entry.getCode(), subProcess);
			activity.addProperty(SKOS.notation, gsbpmModel.createLiteral(entry.getCode()));
			activity.addProperty(SKOS.prefLabel, gsbpmModel.createLiteral(entry.getLabel(), "en"));
			activity.addProperty(SKOS.inScheme, gsbpmCS);
			if (entry.getCode().length() == 1) {
				activity.addProperty(SKOS.topConceptOf, gsbpmCS);
				gsbpmCS.addProperty(SKOS.hasTopConcept, activity);
			} else {
				Resource parent = gsbpmModel.getResource(BASE_URI + entry.getCode().substring(0, 1));
				activity.addProperty(SKOS.broader, parent);
				parent.addProperty(SKOS.narrower, activity);
			}
			activity.addProperty(SKOS.definition, gsbpmModel.createLiteral(entry.getDescription(), "en"));
		}

		// Write the whole model in the output file
		gsbpmModel.write(new FileOutputStream(GSBPM_TTL), "TTL");
	}
}
