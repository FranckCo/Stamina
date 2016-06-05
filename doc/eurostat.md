# Eurostat classifications: NACE and CPA

## Introduction

The NACE and CPA are the central classifications of economic activities and products in the European Statistical System. They are consequently included in the project perimeter. More precisely, the RDF database will contain the last two versions of both classifications (NACE Rev. 1.1 and Rev. 2, and CPA Ver. 2008 and Ver. 2.1), as well as the historical correspondences between the two revisions of NACE and between the two versions of CPA, and the correspondences between NACE Rev. 2 and the two versions of CPA. Additionally, the correspondence between ISIC Rev.4 and NACE Rev.2 is also included.

The authoritative source for the Eurostat classifications is [RAMON](http://ec.europa.eu/eurostat/ramon/index.cfm). The information is generally available in HTML, CSV and XML. The latter seems to be preferable for the main files giving the structure, labels and notes, whereas CSV can be used for simpler files like correspondence tables.

## Extraction tools

A simple way of processing the XML files is to use XSL transformations to produce the XML representation of the target RDF data. [Apache CSV Commons](https://commons.apache.org/proper/commons-csv/) can be used to process the CSV files.

Unfortunately, XML files produced by RAMON have a little defect: they start with a blank line, which makes them invalid. To avoid manual manipulation, a simple Java program was written which deletes the blank line, executes the XSL transformation and finally converts the RDF/XML result into a Turtle file. This last step allows to produce the same format as for the other sources, and is also useful to validate the outputs of the XSL transformations.

## Details on the sources

RAMON produces the downloadable files on demand and adds a timestamp to the file name (for example `CPA_2_1_20160314_114049.xml`). This is not very handy in a perspective of automation where files names should be deterministic. To circumvent this difficulty, the loading programs must select their inputs based on a file name filter (`CPA_2_1_*.xml` for the previous example). In case several files correspond to an expression, the most recent file will be used.

Using this file naming convention, the following sources were used:

* NACE Rev. 2 in XML can be downloaded from the [NACE Rev. 2 page on RAMON](http://ec.europa.eu/eurostat/ramon/nomenclatures/index.cfm?TargetUrl=LST_CLS_DLD&StrNom=NACE_REV2).
* NACE Rev. 1.1 in XML can be downloaded from the [NACE Rev. 1.1 page on RAMON](http://ec.europa.eu/eurostat/ramon/nomenclatures/index.cfm?TargetUrl=LST_CLS_DLD&StrNom=NACE_1_1).
* CPA Ver. 2.1 in XML can be downloaded from the [CPA Ver. 2.1 page on RAMON](http://ec.europa.eu/eurostat/ramon/nomenclatures/index.cfm?TargetUrl=LST_CLS_DLD&StrNom=CPA_2_1).
* CPA Ver. 2008 in XML can be downloaded from the [CPA Ver. 2008 page on RAMON](http://ec.europa.eu/eurostat/ramon/nomenclatures/index.cfm?TargetUrl=LST_CLS_DLD&StrNom=CPA_2008).
* The correspondence between NACE Rev. 1.1 and NACE Rev 2 is available from [this page](http://ec.europa.eu/eurostat/ramon/relations/index.cfm?TargetUrl=LST_REL_DLD&StrNomRelCode=NACE%20REV.%201.1%20-%20NACE%20REV.%20)
* The correspondence between CPA Ver. 2008 and CPA Ver. 2.1 is available from [this page](http://ec.europa.eu/eurostat/ramon/relations/index.cfm?TargetUrl=LST_REL_DLD&StrNomRelCode=CPA 2008 - CPA 2.1).
* The correspondence between NACE Rev 2 et CPA Ver. 2.1 is not explicitly available from RAMON, but the it can easily be produced since both classifications are completely aligned down to the class level. The same goes for the correspondence between NACE Rev 2 et CPA Ver. 2.1.
* The correspondence between ISIC Rev.4 and NACE Rev. 2 can be downloaded from [this page on RAMON](http://ec.europa.eu/eurostat/ramon/relations/index.cfm?TargetUrl=LST_REL_DLD&StrNomRelCode=NACE%20REV.%202%20-%20ISIC%20REV.%204), and is also accessible on the UNSD web site as file [ISIC4-NACE2.zip](http://unstats.un.org/unsd/cr/registry/regdntransfer.asp?f=133). The UNSD version gives more information, for example indicators of partial coverage for links.
* The correspondence between CPA Ver. 2.1 and CPC Ver.2.1 does not seem to be available from RAMON nor from the UNSD web site. It could be a use case of the project to generate this table.

## Details on the outputs

The following Turtle files are produced by the programs:

* nacer11.ttl, nacer2.ttl, cpav2008.ttl and cpav21.ttl correspond to the classification schemes for NACE Rev. 1.1, NACE Rev. 2, CPA Ver. 2008 and CPA Ver. 2.1
* nacer11-nacer2.ttl, cpav2008-cpav21.ttl, nacer2-cpav2008.ttl and nacer2-cpav21.ttl contain the correspondence tables between NACE Rev. 1.1 and NACE Rev. 2, CPA Ver. 2008 and CPA Ver. 2.1, NACE Rev. 2 and CPA Ver. 2008 and NACE Rev. 2 and CPA Ver. 2.1.
* isicr4-nacer2.ttl contains the correspondence table between ISIC Rev.4 and NACE Rev. 2.
