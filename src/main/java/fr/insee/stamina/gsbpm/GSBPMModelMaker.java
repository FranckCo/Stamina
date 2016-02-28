package fr.insee.stamina.gsbpm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import org.apache.jena.vocabulary.SKOS;

public class GSBPMModelMaker {

	// File must be encoded in UTF-8 *without BOM*
	static String GSBPM_TXT = "src/main/resources/data/GSBPM 5.0 - phases.txt";
	static String GSBPM_TTL = "src/main/resources/data/gsbpm.ttl";
	static String GSBPM_ONTO = "src/main/resources/data/gsbpm-onto.ttl";
	static String BASE_URI = "http://id.unece.org/codes/gsbpm/";
	static String ONTO_BASE_URI = "http://rdf.unece.org/models/gsbpm#";

	//static Model gsbpmModel = null;
	static OntModel gsbpmModel = null;

	public static void main(String[] args) throws IOException {

		readOntology();
		readModel();
		//gsbpmModel.add(gsbpmModel);

		gsbpmModel.write(new FileOutputStream(GSBPM_TTL), "TTL");
		//gsbpmModel.write(System.out, "TTL");
	}

	public static void readOntology() throws IOException {

		gsbpmModel = ModelFactory.createOntologyModel();
		gsbpmModel.read(new FileInputStream(GSBPM_ONTO), null, "TTL");
	}

	public static void readModel() throws IOException {

		GSBPMReader reader = new GSBPMReader();
		List<GSBPMEntry> gsbpmEntries = reader.read(new File(GSBPM_TXT));
//		gsbpmModel = ModelFactory.createOntologyModel();
		gsbpmModel.setNsPrefix("skos", SKOS.getURI());
		gsbpmModel.setNsPrefix("igsbpm", BASE_URI);

		// Create the ConceptScheme
		Resource gsbpmCS = gsbpmModel.createResource(BASE_URI + "gsbpm", SKOS.ConceptScheme);
		gsbpmCS.addProperty(SKOS.notation, gsbpmModel.createLiteral("GSBPM"));
		gsbpmCS.addProperty(SKOS.prefLabel, gsbpmModel.createLiteral("Generic Statistical Business Process Model", "en"));

		// Create resources for the classes (already in the model)
		Resource Phase = gsbpmModel.createResource(ONTO_BASE_URI + "Phase");
		Resource SubProcess = gsbpmModel.createResource(ONTO_BASE_URI + "SubProcess");

		for (GSBPMEntry entry : gsbpmEntries) {
			Resource activity = null;
			if (entry.isPhase()) activity = gsbpmModel.createResource(BASE_URI + entry.getCode(), Phase);
			else activity = gsbpmModel.createResource(BASE_URI + entry.getCode(), SubProcess);
			activity.addProperty(SKOS.notation, gsbpmModel.createLiteral(entry.getCode()));
			activity.addProperty(SKOS.prefLabel, gsbpmModel.createLiteral(entry.getLabel(), "en"));
			if (entry.getCode().length() == 1) {
				activity.addProperty(SKOS.topConceptOf, gsbpmCS);
				gsbpmCS.addProperty(SKOS.hasTopConcept, activity);
			} else {
				Resource parent = gsbpmModel.getResource(BASE_URI + entry.getCode().substring(0, 1));
				activity.addProperty(SKOS.inScheme, gsbpmCS);
				activity.addProperty(SKOS.broader, parent);
				parent.addProperty(SKOS.narrower, activity);
			}
			activity.addProperty(SKOS.definition, gsbpmModel.createLiteral(entry.getDescription(), "en"));
		}

	}
}
