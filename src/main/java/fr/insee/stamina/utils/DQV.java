package fr.insee.stamina.utils;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the <a href="https://www.w3.org/TR/vocab-dqv/">Data Quality vocabulary</a>.
 * 
 * @see <a href="https://github.com/w3c/dwbp/blob/gh-pages/dqv.ttl">Turtle specification</a>
 */
public class DQV {
	/**
	 * The RDF model that holds the DQV entities
	 */
	public static Model model = ModelFactory.createDefaultModel();
	/**
	 * The namespace of the DQV vocabulary as a string
	 */
	public static final String uri = "http://www.w3.org/ns/dqv#";
	/**
	 * Returns the namespace of the DQV vocabulary as a string
	 * @return the namespace of the DQV vocabulary
	 */
	public static String getURI() {
		return uri;
	}
	/**
	 * The namespace of the DQV vocabulary
	 */
	public static final Resource NAMESPACE = model.createResource(uri);
	/* ##########################################################
	 * Defines DQV Classes
	   ########################################################## */

	public static final Resource Category = model.createResource(uri + "Category");
	public static final Resource Dimension = model.createResource(uri + "Dimension");
	public static final Resource Metric = model.createResource(uri + "Metric");
	public static final Resource QualityAnnotation = model.createResource(uri + "QualityAnnotation");
	public static final Resource QualityCertificate = model.createResource(uri + "QualityCertificate");
	public static final Resource QualityMeasurement = model.createResource(uri + "QualityMeasurement");
	public static final Resource QualityMeasurementDataset = model.createResource(uri + "QualityMeasurementDataset");
	public static final Resource QualityMetadata = model.createResource(uri + "QualityMetadata");
	public static final Resource QualityPolicy = model.createResource(uri + "QualityPolicy");
	public static final Resource UserQualityFeedback = model.createResource(uri + "UserQualityFeedback");
	/* ##########################################################
	 * Defines DQV Properties
	   ########################################################## */
	// DQV object properties
	public static final Property computedOn = model.createProperty(uri + "computedOn");
	public static final Property expectedDataType = model.createProperty(uri + "expectedDataType");
	public static final Property inCategory = model.createProperty(uri + "inCategory");
	public static final Property inDimension = model.createProperty(uri + "inDimension");
	public static final Property isMeasurementOf = model.createProperty(uri + "isMeasurementOf");
	public static final Property hasQualityAnnotation = model.createProperty(uri + "hasQualityAnnotation");
	public static final Property hasQualityMeasurement = model.createProperty(uri + "hasQualityMeasurement");
	public static final Property hasQualityMetadata = model.createProperty(uri + "hasQualityMetadata");
	public static final Property value = model.createProperty(uri + "value");
}