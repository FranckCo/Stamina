package fr.insee.stamina.utils;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the <a href="https://www.w3.org/TR/vocab-dcat">DCAT vocabulary</a>.
 */
public class DCAT {
	/**
	 * The RDF model that holds the DCAT entities
	 */
	public static Model model = ModelFactory.createDefaultModel();
	/**
	 * The namespace of the DCAT vocabulary as a string
	 */
	public static final String uri = "http://www.w3.org/ns/dcat#";
	/**
	 * Returns the namespace of the DCAT schema as a string
	 * @return the namespace of the DCAT schema
	 */
	public static String getURI() {
		return uri;
	}
	/**
	 * The namespace of the DCAT vocabulary
	 */
	public static final Resource NAMESPACE = model.createResource(uri);
	/* ##########################################################
	 * Defines DCAT Classes
	   ########################################################## */
	public static final Resource Catalog = model.createResource(uri + "Catalog");
	public static final Resource CatalogRecord = model.createResource(uri + "CatalogRecord");
	public static final Resource Dataset = model.createResource(uri + "Dataset");
	public static final Resource Distribution = model.createResource(uri + "Distribution");
	/* ##########################################################
	 * Defines DCAT Properties
	   ########################################################## */
	// DCAT datatype properties
	public static final Property keyword = model.createProperty(uri + "keyword");
	// DCAT object properties
	public static final Property themeTaxonomy = model.createProperty(uri + "themeTaxonomy");
	public static final Property dataset = model.createProperty(uri + "dataset");
	public static final Property record = model.createProperty(uri + "record");
	public static final Property theme = model.createProperty(uri + "theme");
	public static final Property contactPoint = model.createProperty(uri + "contactPoint");
	public static final Property distribution = model.createProperty(uri + "distribution");
	public static final Property landingPage = model.createProperty(uri + "landingPage");
	public static final Property accessURL = model.createProperty(uri + "accessURL");
	public static final Property downloadURL = model.createProperty(uri + "downloadURL");
	public static final Property byteSize = model.createProperty(uri + "byteSize");
	public static final Property mediaType = model.createProperty(uri + "mediaType");
}