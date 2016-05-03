package fr.insee.stamina.meta;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.insee.stamina.isic.ISICModelMaker;
import fr.insee.stamina.utils.ADMS;
import fr.insee.stamina.utils.DCAT;

/**
 * The <code>ADMSModelMaker</code> creates and saves Jena models corresponding to the ADMS descriptions of the repositories.
 * 
 * @author Franck Cotton
 * @version 0.1, 15 Apr 2016
 */
public class ADMSModelMaker {

	public static String ADMS_BASE_URI = "http://stamina-project.org/meta/adms/";
	public static String ADMS_TURTLE_FILE = "src/main/resources/data/adms.ttl";

	/** Log4J2 logger */
	private static final Logger logger = LogManager.getLogger(ADMSModelMaker.class);

	/** Current Jena model */
	private Model admsModel = null;

	/** Resource corresponding to the catalog */
	Resource repositoryResource = null;

	/**
	 * Main method: basic launcher that produces all the models.
	 * 
	 * @param args Not used.
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {

		ADMSModelMaker modelMaker = new ADMSModelMaker();
		logger.debug("New ISICModelMaker instance created");
		modelMaker.createRepositoryModel();
	}

	/**
	 * Creates an Jena model corresponding to the ADMS repository and saves it to a Turtle file.
	 * 
	 * @throws IOException In case of problem saving the file.
	 */
	private void createRepositoryModel() throws IOException {

		logger.debug("Construction of the Jena model for the ADMS repository");
		admsModel = ModelFactory.createDefaultModel();
		admsModel.setNsPrefix("rdfs", RDFS.getURI());
		admsModel.setNsPrefix("xsd", XSD.getURI());
		admsModel.setNsPrefix("dcat", DCAT.getURI());
		admsModel.setNsPrefix("adms", ADMS.getURI());
		admsModel.setNsPrefix("dcterms", DCTerms.getURI());

		repositoryResource = admsModel.createResource(ADMS_BASE_URI + "repository", ADMS.AssetRepository);
		repositoryResource.addProperty(DCTerms.title, admsModel.createLiteral("The Stamina Catalog", "en"));
		repositoryResource.addProperty(DCTerms.description, admsModel.createLiteral("The catalog of all semantic assets created in the Stamina project", "en"));
		// There should be a simpler way of doing that
		Literal dateLiteral = admsModel.createTypedLiteral((new SimpleDateFormat("yyyy-MM-dd")).format(new Date()), new BaseDatatype(XSD.date.getURI()));
		repositoryResource.addProperty(DCTerms.created, dateLiteral);
		repositoryResource.addProperty(DCTerms.publisher, admsModel.createResource("http://www.unece.org"));

		// Produce the asset and distribution descriptions for ISIC 3.1 and 4
		this.addISICAssetModel("3.1");
		this.addISICAssetModel("4");

		// Write the Turtle file and clear the model
		admsModel.write(new FileOutputStream(ADMS_TURTLE_FILE), "TTL");
		admsModel.close();
	}

	/**
	 * Adds the asset and asset distribution information for ISIC in the model.
	 * 
	 * @param version ISIC version as a string, e.g. '4' or '3.1'.
	 */
	private void addISICAssetModel(String version) {

		Resource isicAssetResource = admsModel.createResource(ADMS_BASE_URI + "asset/isicr" +  version.replaceFirst("\\.", ""), ADMS.Asset);
		isicAssetResource.addProperty(DCTerms.type, ADMS.TaxonomyAssetType); // Or CodeList
		isicAssetResource.addProperty(ADMS.identifier, ISICModelMaker.ISIC_SCHEME_NOTATION.get(version));
		isicAssetResource.addProperty(DCTerms.title, ISICModelMaker.ISIC_SCHEME_LABEL.get(version));
		isicAssetResource.addProperty(ADMS.status, ADMS.CompletedStatus); // TODO The asset itself is completed, the distribution is under development

		// TODO Do we directly take the SKOS ConceptScheme resource as ADMS distribution, or do we define a specific resource?
		Resource isicDistributionResource = admsModel.createResource(isicAssetResource.getURI() + "/skos", ADMS.AssetDistribution);
		isicDistributionResource.addProperty(ADMS.status, ADMS.UnderDevelopmentStatus);
		isicDistributionResource.addProperty(ADMS.representationTechnique, ADMS.SKOSRepresentationTechnique);
		isicDistributionResource.addProperty(ADMS.representationTechnique, ADMS.SPARQLRepresentationTechnique);
		isicAssetResource.addProperty(DCAT.distribution, isicDistributionResource);

		repositoryResource.addProperty(ADMS.includedAsset, isicAssetResource);
	}

}
