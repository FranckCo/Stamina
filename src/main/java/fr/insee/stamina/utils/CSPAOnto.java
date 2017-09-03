package fr.insee.stamina.utils;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the CSPA ontology.
 */
public class CSPAOnto {
	/**
	 * The RDF model that holds the CSPA entities
	 */
	public static Model model = ModelFactory.createDefaultModel();
	/**
	 * The namespace of the CSPA vocabulary as a string
	 */
	public static final String uri = "http://rdf.unece.org/ontologies/hlg#";
	/**
	 * Returns the namespace of the CSPA vocabulary as a string.
	 * 
	 * @return The namespace of the CSPA vocabulary.
	 */
	public static String getURI() {
		return uri;
	}
	/**
	 * The namespace of the CSPA vocabulary
	 */
	public static final Resource NAMESPACE = model.createResource(uri);

	/* #################### *
	 * Defines CSPA Classes *
	 * #################### */

	public static final Resource StatisticalActivity = model.createResource(uri + "StatisticalActivity");
	public static final Resource StatisticalActivityArea = model.createResource(uri + "StatisticalActivityArea");

}