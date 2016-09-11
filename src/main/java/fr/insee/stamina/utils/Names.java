package fr.insee.stamina.utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the URI and naming policy for the project.
 * 
 * @author Franck Cotton
 * @version 0.2, 2 Jun 2016
 */
public class Names {

	/** Base URI for all resources in the classification models */
	public static String CLASSIFICATION_BASE_URI = "http://stamina-project.org/codes/";

	/** Classifications for which major versions are named 'revisions' instead of 'versions' */
	public static Set<String> REVISION_CLASSIFICATIONS = new HashSet<String>(Arrays.asList("ISIC", "NACE", "NAF", "CPF"));

	public static Map<String, List<String>> LEVEL_NAMES = new HashMap<String, List<String>>();
	static {
		LEVEL_NAMES.put("ISIC", Arrays.asList("section", "division", "group", "class"));
		LEVEL_NAMES.put("CPC", Arrays.asList("section", "division", "group", "class", "subclass"));
		LEVEL_NAMES.put("NACE", Arrays.asList("section", "division", "group", "class"));
		// NACE revisions 1 and 1.1 had subsections
		LEVEL_NAMES.put("NACEX", Arrays.asList("section", "subsection", "division", "group", "class"));
		LEVEL_NAMES.put("CPA", Arrays.asList("section", "division", "group", "class", "category", "subcategory"));
		// CPA 2002 and before had subsections
		LEVEL_NAMES.put("CPAX", Arrays.asList("section", "subsection", "division", "group", "class", "category", "subcategory"));
		// TODO Add other cases
	}

	/**
	 * Returns the base URI corresponding to a classification version (or classification scheme, CS).
	 * <i>Note<i>: the base URI is not the URI of the classification itself (see getClassificationURI).
	 * 
	 * @param classification Short name of the classification, e.g. "NACE", "ISIC", etc.
	 * @param version Version of the classification ("4", "2.1", "2008", etc.).
	 * @return The base URI for all resources of the classification version.
	 */
	public static String getCSBaseURI(String classification, String version) {

		return CLASSIFICATION_BASE_URI + getCSContext(classification, version) + "/";
	}

	/**
	 * Returns the URI corresponding to a classification version.
	 * 
	 * @param classification Short name of the classification, e.g. "NACE", "ISIC", etc.
	 * @param version Version of the classification ("4", "2.1", "2008", etc.).
	 * @return The URI for the resource corresponding to the classification version.
	 */
	public static String getCSURI(String classification, String version) {

		return getCSBaseURI(classification, version) + classification.toLowerCase();
	}

	/**
	 * Returns the naming context corresponding to a classification version.
	 * The naming context is the path element dedicated to a given classification scheme below the
	 * classification base URI, for example "nacer2" for NACE Rev. 2, "cpcv21" for CPC Ver.2.1, etc.
	 * 
	 * @param classification Short name of the classification, e.g. "NACE", "ISIC", etc.
	 * @param version Version of the classification ("4", "2.1", "2008", etc.).
	 * @return The naming context for the classification version.
	 */
	public static String getCSContext(String classification, String version) {

		String versionQualifier = null;
		if (REVISION_CLASSIFICATIONS.contains(classification.toUpperCase())) versionQualifier = "r";
		else versionQualifier = "v";

		return classification.toLowerCase() + versionQualifier + version.replaceAll("\\.", "");
	}

	/**
	 * Returns the long name of a classification version.
	 * 
	 * @param classification Short name of the classification, e.g. "NACE", "ISIC", etc.
	 * @param version Version of the classification ("4", "2.1", "2008", etc.).
	 * @return The long name of the classification version.
	 */
	public static String getCSLabel(String classification, String version) {

		String shortName = classification.toUpperCase();
		if ("ISIC".equals(shortName)) return String.format("International Standard Industrial Classification of All Economic Activities, Rev.%s", version);
		if ("CPC".equals(shortName)) return String.format("Central Product Classification, Ver.%s", version);
		if ("NACE".equals(shortName)) return String.format("Statistical Classification of Economic Activities in the European Community, Rev. %s", version);
		if ("CPA".equals(shortName)) return String.format("Statistical Classification of Products by Activity, Version %s", version);
		if ("NAF".equals(shortName)) return String.format("Nomenclature d'activités française - NAF rév. %s", version);
		if ("CPF".equals(shortName)) return String.format("Classification des produits française - CPF rév. %s", version);

		return null;
	}

	/**
	 * Returns the short name of a classification version.
	 * Examples of short names are: ISIC Rev.3.1, CPC Ver.2.1, etc.
	 * 
	 * @param classification Short name of the classification, e.g. "NACE", "ISIC", etc.
	 * @param version Version of the classification ("4", "2.1", "2008", etc.).
	 * @return The short name of the classification version.
	 */
	public static String getCSShortName(String classification, String version) {

		// A rather dumb implementation, not sure a smarter one is possible
		String shortName = classification.toUpperCase();
		if ("ISIC".equals(shortName)) return String.format("ISIC Rev.%s", version);
		if ("CPC".equals(shortName)) return String.format("CPC Ver.%s", version);
		if ("NACE".equals(shortName)) return String.format("NACE Rev. %s", version);
		if ("CPA".equals(shortName)) return String.format("CPA %s", version);
		if ("NAF".equals(shortName)) return String.format("NAF rév. %s", version);
		if ("CPF".equals(shortName)) return String.format("CPF rév. %s", version);

		return null;
	}

	/**
	 * Return the URI of a level in a classification version.
	 * 
	 * @param classification Short name of the classification, e.g. "NACE", "ISIC", etc.
	 * @param version Version of the classification ("4", "2.1", "2008", etc.).
	 * @param depth The depth of the level which URI is requested (the most aggregated level has depth 1).
	 * @return The URI for the resource corresponding to the classification version.
	 */
	public static String getClassificationLevelURI(String classification, String version, int depth) {

		String levelName = LEVEL_NAMES.get(classification).get(depth - 1);
		if (levelName.endsWith("ss")) levelName += "es"; // Case of class and subclass
		else if (levelName.endsWith("y")) levelName = levelName.substring(0, levelName.length() - 1) + "ies"; // Case of category and subcategory
		else levelName += "s";

		return getCSBaseURI(classification, version) + levelName;
	}

	/**
	 * Return the label of a level in a classification version.
	 * 
	 * @param classification Short name of the classification, e.g. "NACE", "ISIC", etc.
	 * @param version Version of the classification ("4", "2.1", "2008", etc.).
	 * @param depth The depth of the level which URI is requested (the most aggregated level has depth 1).
	 * @return The label of a level in a classification version.
	 */
	public static String getClassificationLevelLabel(String classification, String version, int depth) {

		String levelName = LEVEL_NAMES.get(classification).get(depth - 1);

		return getCSLabel(classification, version) + " - " + levelName.substring(0, 1).toUpperCase() + levelName.substring(1) + " level";
	}

	/**
	 * Returns the parent section code for a given division code of NACE, CPF and associated classifications.
	 * 
	 * @param division The code of the division.
	 * @return The code of the parent section, or <code>null</code> if the division code is invalid.
	 */
	public static String getNACESectionForDivision(String division) {

		// For verification      "000000000111111111122222222223333333333444444444455555555556666666666777777777788888888889999999999";
		// For verification      "123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
		final String CONVERTER = "AAAXBBBBBCCCCCCCCCCCCCCCCCCCCCCCCXDEEEEXFFFXGGGXHHHHHXIIXJJJJJJKKKXLMMMMMMMXNNNNNNXOPQQQXRRRRSSSTTU";

		try {
			int index = Integer.parseInt(division);
			String section = CONVERTER.substring(index - 1, index);
			return (section.equals("X") ? null : section);
		} catch (Exception e) { return null; }
	}

	/**
	 * Returns the base URI corresponding to a correspondence table between two classification versions.
	 * <i>Note<i>: the base URI is not the URI of the correspondence itself (see getCorrespondenceURI).
	 * 
	 * @param sourceClassification Short name of the source classification, e.g. "NACE", "ISIC", etc.
	 * @param sourceVersion Version of the source classification ("4", "2.1", "2008", etc.).
	 * @param targetClassification Short name of the target classification.
	 * @param targetVersion Version of the target classification.
	 * @return The URI for the resource corresponding to the correspondence table.
	 */
	public static String getCorrespondenceBaseURI(String sourceClassification, String sourceVersion, String targetClassification, String targetVersion) {

		return CLASSIFICATION_BASE_URI + getCorrespondenceContext(sourceClassification, sourceVersion, targetClassification, targetVersion) + "/";
	}

	/**
	 * Returns the URI corresponding to a correspondence table between two classification versions.
	 * 
	 * @param sourceClassification Short name of the source classification, e.g. "NACE", "ISIC", etc.
	 * @param sourceVersion Version of the source classification ("4", "2.1", "2008", etc.).
	 * @param targetClassification Short name of the target classification.
	 * @param targetVersion Version of the target classification.
	 * @return The base URI for all resources of the correspondence table.
	 */
	public static String getCorrespondenceURI(String sourceClassification, String sourceVersion, String targetClassification, String targetVersion) {

		return getCorrespondenceBaseURI(sourceClassification, sourceVersion, targetClassification, targetVersion) + "correspondence";
	}

	/**
	 * Returns the naming context corresponding to a correspondence table between two classification versions.
	 * 
	 * @param sourceClassification Short name of the source classification, e.g. "NACE", "ISIC", etc.
	 * @param sourceVersion Version of the source classification ("4", "2.1", "2008", etc.).
	 * @param targetClassification Short name of the target classification.
	 * @param targetVersion Version of the target classification.
	 * @return The naming context for the correspondence table.
	 */
	public static String getCorrespondenceContext(String sourceClassification, String sourceVersion, String targetClassification, String targetVersion) {

		return getCSContext(sourceClassification, sourceVersion) + "-" + getCSContext(targetClassification, targetVersion);
	}

	/**
	 * Returns the short name of a correspondence table between two classification versions.
	 * 
	 * @param sourceClassification Short name of the source classification, e.g. "NACE", "ISIC", etc.
	 * @param sourceVersion Version of the source classification ("4", "2.1", "2008", etc.).
	 * @param targetClassification Short name of the target classification.
	 * @param targetVersion Version of the target classification.
	 * @return The short name of the correspondence table.
	 */
	public static String getCorrespondenceShortName(String sourceClassification, String sourceVersion, String targetClassification, String targetVersion) {

		return getCSShortName(sourceClassification, sourceVersion) + " - " + getCSShortName(targetClassification, targetVersion);
	}

	/**
	 * Computes the URI of a classification item.
	 * 
	 * @param code The item code.
	 * @param classification The classification to which the item belongs, e.g. "NACE", "ISIC", etc.
	 * @param version The version of the classification to which the item belongs ("4", "2.1", "2008", etc.).
	 * @return The item URI.
	 */
	public static String getItemURI(String code, String classification, String version) {

		return getCSBaseURI(classification, version) + getItemPathInContext(code, classification, version);
	}

	/**
	 * Computes the path part of a classification item URI within the context of its classification version.
	 * Examples of item paths relative to a naming context are: section/B, group/12.1, etc.
	 * 
	 * @param code The item code.
	 * @param classification The classification to which the item belongs, e.g. "NACE", "ISIC", etc.
	 * @param version The version of the classification to which the item belongs ("4", "2.1", "2008", etc.).
	 * @return The item path relative to the classification version naming context.
	 */
	public static String getItemPathInContext(String code, String classification, String version) {

		String selector = classification.toUpperCase();
		if (selector.equals("NACE") && (version.startsWith("1"))) selector = "NACEX";
		if (selector.equals("CPA") && (version.length() >= 4) && (Integer.parseInt(version) <= 2002)) selector = "CPAX";

		return LEVEL_NAMES.get(selector).get(getItemLevelDepth(code, classification, version) - 1) + "/" + code;
		
	}

	/**
	 * Returns the depth of the level to which an item belongs.
	 * <i>Note<i>: levels are numbered from the top (base 1): the most aggregated level has depth 1.
	 * 
	 * @param code The item code.
	 * @param classification The classification to which the item belongs, e.g. "NACE", "ISIC", etc.
	 * @param version The version of the classification to which the item belongs ("4", "2.1", "2008", etc.).
	 * @return The depth of the level.
	 */
	public static int getItemLevelDepth(String code, String classification, String version) {

		// Except for old versions of the NACE and CPA, the level is the number of characters (except dots) of the code
		int depth = code.replace(".", "").length();
		// For oldest CPA and NACE versions, the subsections (two letters) are level 2, then the codes are digits
		if (("CPA".equals(classification.toUpperCase())) && (version.length() >= 4) && (Integer.parseInt(version) <= 2002)) {
			if (Character.isDigit(code.charAt(0))) depth++;
		}
		if (("NACE".equals(classification.toUpperCase())) && (version.startsWith("1"))) {
			if (Character.isDigit(code.charAt(0))) depth++;
		}
		// TODO Check that

		return depth;
	}

	/**
	 * Computes the URI of a concept association.
	 * 
	 * @param sourceCode The source item code.
	 * @param sourceClassification Short name of the source classification, e.g. "NACE", "ISIC", etc.
	 * @param sourceVersion Version of the source classification ("4", "2.1", "2008", etc.).
	 * @param targetCode The target item code.
	 * @param targetClassification Short name of the target classification.
	 * @param targetVersion Version of the target classification.
	 * @return The association URI.
	 */
	public static String getAssociationURI(String sourceCode, String sourceClassification, String sourceVersion, String targetCode, String targetClassification, String targetVersion) {

		return getCorrespondenceBaseURI(sourceClassification, sourceVersion, targetClassification, targetVersion) + getAssociationPathInContext(sourceCode, targetCode);
	}

	/**
	 * Computes the path part of a concept association URI within the context of its correspondence table.
	 * 
	 * @param sourceCode The source item code.
	 * @param targetCode The target item code.
	 * @return The association path relative to the correspondence table naming context.
	 */
	public static String getAssociationPathInContext(String sourceCode, String targetCode) {

		return "association/" + sourceCode + "-" + targetCode;
	}

}