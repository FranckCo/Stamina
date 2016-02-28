package fr.insee.stamina.utils;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the <a href="http://www.ddialliance.org/Specification/RDF/XKOS">XKOS vocabulary</a>.
 */
public class XKOS {
	/**
	 * The RDF model that holds the XKOS entities
	 */
	public static Model model = ModelFactory.createDefaultModel();
	/**
	 * The namespace of the XKOS vocabulary as a string
	 */
	public static final String uri = "http://rdf-vocabulary.ddialliance.org/xkos#";
	/**
	 * Returns the namespace of the XKOS schema as a string
	 * @return the namespace of the XKOS schema
	 */
	public static String getURI() {
		return uri;
	}
	/**
	 * The namespace of the XKOS vocabulary
	 */
	public static final Resource NAMESPACE = model.createResource( uri );
	/* ##########################################################
	 * Defines XKOS Classes
	   ########################################################## */
	public static final Resource ClassificationLevel = model.createResource( uri + "ClassificationLevel");
	public static final Resource ConceptAssociation = model.createResource( uri + "ConceptAssociation");
	public static final Resource Correspondence = model.createResource(uri + "Correspondence");
	public static final Resource ExplanatoryNote = model.createResource( uri + "ExplanatoryNote"); // Not yet in the official draft
	/* ##########################################################
	 * Defines XKOS Properties
	   ########################################################## */
	// XKOS annotation properties (sub-properties of skos:note)
	public static final Property inclusionNote = model.createProperty( uri + "inclusionNote");
	public static final Property coreContentNote = model.createProperty( uri + "coreContentNote");
	public static final Property additionalContentNote = model.createProperty( uri + "additionalContentNote");
	public static final Property exclusionNote = model.createProperty( uri + "exclusionNote");
	public static final Property caseLaw = model.createProperty( uri + "caseLaw"); // Not yet in the official draft
	// XKOS data properties
	public static final Property numberOfLevels = model.createProperty( uri + "numberOfLevels");
	public static final Property depth = model.createProperty( uri + "depth");
	public static final Property maxLength = model.createProperty( uri + "maxLength");
	// XKOS object properties
	public static final Property levels = model.createProperty( uri + "levels");
	public static final Property madeOf = model.createProperty( uri + "madeOf");
	public static final Property sourceConcept = model.createProperty( uri + "sourceConcept");
	public static final Property targetConcept = model.createProperty( uri + "targetConcept");
	public static final Property follows = model.createProperty( uri + "follows");
	public static final Property supersedes = model.createProperty( uri + "supersedes");
	public static final Property variant = model.createProperty( uri + "variant");
	public static final Property belongsTo = model.createProperty( uri + "belongsTo");
	public static final Property organizedBy = model.createProperty( uri + "organizedBy");
	public static final Property classifiedUnder = model.createProperty( uri + "classifiedUnder");
	// XKOS coverage properties (object properties)
	public static final Property covers = model.createProperty( uri + "covers");
	public static final Property coversExhaustively = model.createProperty( uri + "coversExhaustively");
	public static final Property coversMutuallyExclusively = model.createProperty( uri + "coversMutuallyExclusively");
	// XKOS semantic relations properties
	public static final Property causal = model.createProperty( uri + "causal");
	public static final Property causes = model.createProperty( uri + "causes");
	public static final Property causedBy = model.createProperty( uri + "causedBy");
	public static final Property sequential = model.createProperty( uri + "sequential");
	public static final Property precedes = model.createProperty( uri + "precedes");
	public static final Property previous = model.createProperty( uri + "previous");
	public static final Property succeeds = model.createProperty( uri + "succeeds");
	public static final Property next = model.createProperty( uri + "next");
	public static final Property temporal = model.createProperty( uri + "temporal");
	public static final Property before = model.createProperty( uri + "before");
	public static final Property after = model.createProperty( uri + "after");
	public static final Property isPartOf = model.createProperty( uri + "isPartOf");
	public static final Property hasPart = model.createProperty( uri + "hasPart");
	public static final Property specializes = model.createProperty( uri + "specializes");
	public static final Property generalizes = model.createProperty( uri + "generalizes");
	public static final Property disjoint = model.createProperty( uri + "disjoint");
}