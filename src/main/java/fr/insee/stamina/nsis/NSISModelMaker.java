package fr.insee.stamina.nsis;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.SKOS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.AddressComponent;
import com.google.maps.model.AddressComponentType;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.Geometry;

/**
 * The <code>NSISModelMaker</code> class creates and saves the Jena models corresponding to the list of NSIs.
 * 
 * This program uses the ORG ontology published by the W3C.
 * 
 * @see https://www.w3.org/TR/vocab-org/
 * @author Franck Cotton
 * @version 0.3, 11 Nov 2016
 */
public class NSISModelMaker {

	/** Input TSV file */
	static String NSIS_TXT = "src/main/resources/data/nsis.csv";

	/** File containing the Google Maps API key (must be of type Server key) */
	public static String API_KEY_FILE = "src/main/resources/data/gm-api-key.txt";

	/** URL of the ORG ontology in Turtle format */
	public static String ORG_ONTO_URL = "http://www.w3.org/ns/org.ttl";
	/** Base URI for the ORG ontology */
	public static String ORG_BASE_URI = "http://www.w3.org/ns/org#";

	/** Base URL of the vCard ontology */
	public static String VCARD_BASE_URI = "http://www.w3.org/2006/vcard/ns#";

	/** Output file: Turtle file containing the NSI individuals */
	public static String NSIS_TTL = "src/main/resources/data/nsis.ttl";

	/** Base URI for the individuals (this is a temporary scheme) */
	public static String BASE_URI = "http://id.unece.org/nsi/";

	/** Log4J2 logger */
	private static final Logger logger = LogManager.getLogger(NSISModelMaker.class);

	/** Jena model for vocabulary components */
	static OntModel ontologyModel = null;
	/** Jena model for information on NSIs */
	static Model nsisModel = null;

	// Maximum number of input lines that will be treated (can be used for tests, careful with the header line)
	static int MAX_INPUT_LINES = 1000;

	public static void main(String[] args) {

		// Read the Google Maps API key from the file and initialize the API context
		String apiKey = null;
		try {
			apiKey = new String(Files.readAllBytes(Paths.get(API_KEY_FILE)));
		} catch (IOException e) {
			logger.fatal("An exception occurred while reading the Google Maps API key", e);
			return;
		}
		GeoApiContext context = new GeoApiContext().setApiKey(apiKey);
		logger.info("Google Maps API key read from " + API_KEY_FILE);

		// Initialize the ontology model, populate it with the ORG ontology, extract some useful ORG components, add useful components from other ontologies
		ontologyModel = ModelFactory.createOntologyModel();
		ontologyModel.read(ORG_ONTO_URL, "TTL");
		Resource orgClass = ontologyModel.getOntClass(ORG_BASE_URI + "Organization");
		Resource siteClass = ontologyModel.getOntClass(ORG_BASE_URI + "Site");
		Property siteProperty = ontologyModel.getOntProperty(ORG_BASE_URI + "hasSite");
		Property siteAddressProperty = ontologyModel.getOntProperty(ORG_BASE_URI + "siteAddress");
		Resource workClass = ontologyModel.createClass(VCARD_BASE_URI + "Work");
		Property streetAddressProperty = ontologyModel.createProperty(VCARD_BASE_URI + "street-address");
		Property hasGeoProperty = ontologyModel.createProperty(VCARD_BASE_URI + "hasGeo");
		logger.info("ORG ontology downloaded from " + ORG_ONTO_URL);

		// Select useful prefix mappings in the ontology model to transfer them in the NSI model, add the vCard prefix
		ontologyModel.getNsPrefixURI("org");
		System.out.println(ontologyModel.getNsPrefixMap());
		Map<String, String> namespaces = ontologyModel.getNsPrefixMap();
		Set<String> keepNs = new HashSet<String>();
		keepNs.addAll(Arrays.asList("owl", "org", "skos"));
		namespaces.keySet().retainAll(keepNs);
		namespaces.put("vcard", VCARD_BASE_URI);

		nsisModel = ModelFactory.createDefaultModel();
		nsisModel.setNsPrefixes(namespaces);

		// Read the CSV file and create the associated resources
		logger.info("Preparing to read CSV file " + NSIS_TXT);
		// Counters
		int noResults = 0, severalResults = 0, invalidResult = 0, okResult = 0;
		CSVParser parser = null;
		try {
			parser = new CSVParser(new FileReader(NSIS_TXT), CSVFormat.TDF.withQuote(null).withHeader().withIgnoreEmptyLines());
			for (CSVRecord record : parser) {
				if (parser.getCurrentLineNumber() > MAX_INPUT_LINES) continue;
				Resource nsi = nsisModel.createResource(BASE_URI + record.get("Country"), orgClass);
				String shortName = record.get("Short name");
				if (shortName.length() != 0) nsi.addProperty(SKOS.altLabel, nsisModel.createLiteral(shortName));
				// No language tag for now, most of the names are in English, but not all
				nsi.addProperty(SKOS.prefLabel, nsisModel.createLiteral(record.get("Long name")));
				// Create a resource corresponding to the NSI site (supposed to be the headquarters) and link it to the NSI
				Resource site = nsisModel.createResource(nsi.getURI() + "/hq", siteClass);
				nsi.addProperty(siteProperty, site);
				// Add site address if validated by the Google Maps API
				String addressToCode = record.get("Corrected address"); // Take the corrected address when there is one
				if (addressToCode.length() == 0) addressToCode = record.get("Raw address"); // Otherwise take the original address (always present)
				try {
					logger.debug("Preparing to call the geocoding API for address " + addressToCode);
					GeocodingResult[] results = GeocodingApi.geocode(context, addressToCode).await();
					if ((results == null) || (results.length == 0)) {
						noResults++;
						logger.debug("The geocoding API returned no results");
						continue;
					}
					if (results.length > 1) {
						severalResults++;
						logger.debug("The geocoding API returned several results");
						for (GeocodingResult result : results) logger.debug(dumpGeocodingResult(result));
						continue;
					}
					if (!checkComponents(results[0])) {
						invalidResult++;
						logger.debug("The geocoding API returned a single result that was rejected");
						logger.debug(dumpGeocodingResult(results[0]));
						continue;
					}
					// One unique geocoding result, which is acceptable: add the site address and coordinates
					okResult++;
					logger.debug("The geocoding API returned a single result that was accepted");
					logger.debug(dumpGeocodingResult(results[0]));
					Resource siteAddress = nsisModel.createResource(site.getURI() + "/address", workClass);
					siteAddress.addProperty(streetAddressProperty, results[0].formattedAddress); // For now we put everything in the street address
					site.addProperty(siteAddressProperty, siteAddress);
					// Geocoding information is attached to the address, as suggested in https://www.w3.org/TR/vocab-org/#org:siteAddress, but it could be on the site
					// vCard recommands geo: URI scheme (not widely supported...). Latitude and longitude can be used in Google Maps or OpenStreetMap as follows:
					// http://www.openstreetmap.org/#map=18/41.32480120/19.82369140 (18 is the zoom level)
					// http://maps.google.com/?q=41.32480120,19.82369140
					Resource location = nsisModel.createResource("geo:" + results[0].geometry.location.toUrlValue());
					siteAddress.addProperty(hasGeoProperty, location);
				} catch (Exception e) {
					logger.error("An exception occurred during geocoding", e);
				}
			}
		} catch (IOException e) {
			logger.error("An exception occurred while reading the TSV file", e);
		}
		finally {
			try {parser.close();} catch (Exception ignored) {}
		}

		// Write the whole model in the output file
		try {
			nsisModel.write(new FileOutputStream(NSIS_TTL), "TTL");
		} catch (FileNotFoundException e) {
			logger.error("An exception occurred while writing the Turtle file", e);
		}
		logger.info("Process completed, " + (noResults + severalResults + invalidResult + okResult) + " lines read.");
		logger.info("Number of cases with no geocoding results: " + noResults);
		logger.info("Number of cases with several geocoding results: " + severalResults);
		logger.info("Number of cases with one geocoding result rejected: " + invalidResult);
		logger.info("Number of cases with one geocoding result accepted: " + okResult);
	}

	/**
	 * Returns a URI corresponding to an ISO 3166-A2 country code.
	 * The choice is to use DBPedia resources.
	 * 
	 * @param isoCode ISO 3166-A2 country code.
	 * @return A DBPedia resource URI (e.g. http://dbpedia.org/resource/ISO_3166-2:FR).
	 */
	public static String iso3166A2ToURI(String isoCode) {

		if ((isoCode == null) || (isoCode.length() != 2)) return null;

		String capitalizedCode = isoCode.toUpperCase();

		// Frequent mistake
		if (capitalizedCode.equals("UK")) capitalizedCode = "GB";

		return "http://dbpedia.org/resource/ISO_3166-2:" + capitalizedCode;
	}

	/**
	 * Checks if the address in a geocoding result is acceptable.
	 * 
	 * The address should have at least a PREMISE, ROUTE, STREET_NUMBER or SUBLOCALITY_LEVEL_4 component (these criteria are completely ad hoc)
	 * 
	 * @param result A <code>GeocodingResult</code> returned by the Google Maps API.
	 * @return <code>true</code> if the address is acceptable, <code>false</code> otherwise.
	 */
	public static boolean checkComponents(GeocodingResult result) {

		for (AddressComponent component : result.addressComponents) {
			for (Object componentType : component.types) {
				if (componentType == AddressComponentType.PREMISE) return true;
				if (componentType == AddressComponentType.ROUTE) return true;
				if (componentType == AddressComponentType.STREET_NUMBER) return true;
				if (componentType == AddressComponentType.SUBLOCALITY_LEVEL_4) return true;
			}
		}
		return false;
	}

	/**
	 * Represents a geocoding result as a formatted string.
	 * 
	 * @param result A <code>GeocodingResult</code> returned by the Google Maps API.
	 * @return A string representing the geocoding result.
	 */
	public static String dumpGeocodingResult(GeocodingResult result) {

		String dump = "Geocoding result" + (result.partialMatch ? " (partial match)" : "");
		dump += ":\nFormatted address: " + result.formattedAddress + "\nComponents\n";
		for (AddressComponent component : result.addressComponents) {
			dump += Arrays.toString(component.types) + ": " + component.longName + " (" + component.shortName +")\n";
		}
		Geometry geometry = result.geometry;
		if (geometry != null) {
			dump += "Geometry\nLocation (" + geometry.locationType + "): " + geometry.location;
			if (geometry.bounds != null); // TODO (not used here)
			if (geometry.viewport != null); // TODO (not used here)
		}
		if (result.postcodeLocalities != null) dump += "\nPostcode localities: " + Arrays.toString(result.postcodeLocalities);
		
		return dump;	
	}
}
