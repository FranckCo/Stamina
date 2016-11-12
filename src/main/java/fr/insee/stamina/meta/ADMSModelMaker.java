package fr.insee.stamina.meta;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.insee.stamina.utils.ADMS;
import fr.insee.stamina.utils.DCAT;
import fr.insee.stamina.utils.Names;

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
		logger.debug("New ADMSModelMaker instance created");
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

		// Produce the asset and distribution descriptions for CPC 1.1, CPC 2, CPC 2.1, ISIC 3.1 and 4
		this.addISICAssetModel("CPC", "1.1");
		this.addISICAssetModel("CPC", "2");
		this.addISICAssetModel("CPC", "2.1");
		this.addISICAssetModel("ISIC", "3.1");
		this.addISICAssetModel("ISIC", "4");

		// Write the Turtle file and clear the model
		admsModel.write(new FileOutputStream(ADMS_TURTLE_FILE), "TTL");
		admsModel.close();
	}

	/**
	 * Adds the asset and asset distribution information for a classification in the model.
	 * 
	 * @param classification Short name of the classification, e.g. "NACE", "ISIC", etc.
	 * @param version Version of the classification ("4", "2.1", "2008", etc.).
	 */
	private void addISICAssetModel(String classification, String version) {

		Resource csAssetResource = admsModel.createResource(ADMS_BASE_URI + "asset/" + Names.getCSContext(classification, version), ADMS.Asset);
		csAssetResource.addProperty(DCTerms.type, ADMS.TaxonomyAssetType); // Or CodeList
		csAssetResource.addProperty(ADMS.identifier, Names.getCSShortName(classification, version));
		csAssetResource.addProperty(DCTerms.title, Names.getCSLabel(classification, version));
		csAssetResource.addProperty(ADMS.status, ADMS.CompletedStatus); // TODO The asset itself is completed, the distribution is under development

		// TODO Do we directly take the SKOS ConceptScheme resource as ADMS distribution, or do we define a specific resource?
		Resource csDistributionResource = admsModel.createResource(csAssetResource.getURI() + "/skos", ADMS.AssetDistribution);
		csDistributionResource.addProperty(ADMS.status, ADMS.UnderDevelopmentStatus);
		csDistributionResource.addProperty(ADMS.representationTechnique, ADMS.SKOSRepresentationTechnique);
		csDistributionResource.addProperty(ADMS.representationTechnique, ADMS.SPARQLRepresentationTechnique);
		csAssetResource.addProperty(DCAT.distribution, csDistributionResource);

		repositoryResource.addProperty(ADMS.includedAsset, csAssetResource);
	}

}
