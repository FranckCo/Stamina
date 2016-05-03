package fr.insee.stamina.utils;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the <a href="https://www.w3.org/TR/vocab-adms">ADMS vocabulary</a>.
 */
public class ADMS {
	/**
	 * The RDF model that holds the ADMS entities
	 */
	public static Model model = ModelFactory.createDefaultModel();
	/**
	 * The namespace of the ADMS vocabulary as a string
	 */
	public static final String uri = "http://www.w3.org/ns/adms#";
	/**
	 * Returns the namespace of the ADMS schema as a string
	 * @return the namespace of the ADMS schema
	 */
	public static String getURI() {
		return uri;
	}
	/**
	 * The namespace of the ADMS vocabulary
	 */
	public static final Resource NAMESPACE = model.createResource(uri);
	/* ##########################################################
	 * Defines ADMS Classes
	   ########################################################## */
	public static final Resource Asset = model.createResource(uri + "Asset");
	public static final Resource AssetDistribution = model.createResource(uri + "AssetDistribution");
	public static final Resource AssetRepository = model.createResource(uri + "AssetRepository");
	public static final Resource Identifier = model.createResource(uri + "Identifier");
	/* ##########################################################
	 * Defines ADMS Properties
	   ########################################################## */
	// ADMS datatype properties
	public static final Property schemeAgency = model.createProperty(uri + "schemeAgency");
	public static final Property versionNotes = model.createProperty(uri + "versionNotes");
	// ADMS object properties schemeAgency
	public static final Property identifier = model.createProperty(uri + "identifier");
	public static final Property includedAsset = model.createProperty(uri + "includedAsset");
	public static final Property interoperabilityLevel = model.createProperty(uri + "interoperabilityLevel");
	public static final Property last = model.createProperty(uri + "last");
	public static final Property next = model.createProperty(uri + "next");
	public static final Property prev = model.createProperty(uri + "prev");
	public static final Property representationTechnique = model.createProperty(uri + "representationTechnique");
	public static final Property supportedSchema = model.createProperty(uri + "supportedSchema");
	public static final Property sample = model.createProperty(uri + "sample");
	public static final Property status = model.createProperty(uri + "status");
	public static final Property translation = model.createProperty(uri + "translation");

	// Define a selection of terms from the controlled vocabularies (see https://joinup.ec.europa.eu/svn/adms/ADMS_v1.00/ADMS_SKOS_v1.00.html)
	public static final Resource CompletedStatus = model.createResource("http://purl.org/adms/status/Completed");
	public static final Resource UnderDevelopmentStatus = model.createResource("http://purl.org/adms/status/UnderDevelopment");
	public static final Resource TaxonomyAssetType = model.createResource("http://purl.org/adms/assettype/Taxonomy");
	public static final Resource SKOSRepresentationTechnique = model.createResource("http://purl.org/adms/representationtechnique/SKOS");
	public static final Resource SPARQLRepresentationTechnique = model.createResource("http://purl.org/adms/representationtechnique/SPARQL");
}