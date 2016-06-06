# National classifications: NAICS, Ateco, NAF and CPF

## Introduction

The international classifications of economic activities and products are in general refined or adapted in each country in order to fit the local needs. In other cases, local classifications may have specific structures, but are linked to UNSD classifications by correspondence tables. We include here different examples of national classifications:
* NAICS 2012
* Ateco 2007
* NAF rév. 2
* CPF rév. 2.1

The authoritative sources for national classifications is generally the country's NSI (national statistical institute). For NAICS, we use the publication made by the [US Census Bureau](http://www.census.gov); Ateco is found on [Istat's web site](http://www.istat.it) and NAF and CPF on [Insee's web site](http://www.insee.fr). Ateco, NAF and CPF are already published by Istat and Insee, so we take those "as-is" even if the modeling can differ from the one used for the rest of the classification. All those the data are available in Excel.

## Extraction tools

[Apache POI](https://poi.apache.org/) is used to read the Excel files and [Apache Jena](https://jena.apache.org/) to produce the RDF datasets.

## Details on the sources

The following sources are used to produced the RDF data:
* The NAICS 2012 Excel spreadsheet can be downloaded at [this URL](http://www.census.gov/eos/www/naics/2012NAICS/2-digit_2012_Codes.xls) and the correspondence between ISIC Rev.4 and NAICS 2012 is available [here](http://www.census.gov/eos/www/naics/concordances/2012_NAICS_to_ISIC_4.xls ).
* Ateco 2007, NAF rév. 2 and CPF rév. 2.1 were directly provided by Istat and Insee (files ateco2007.rdf, naf08.rdf and cpf15.rdf).
* The hierarchical correspondence between NACE Rev. 2 and Ateco 2007 can be calculated from the files contained in [this archive](http://www.istat.it/it/files/2011/03/STRUTTURA.zip?title=Classificazione+Ateco+2007+-+01%2Fott%2F2009+-+Ateco+2007.zip), and more specifically from the `ateco_struttura_17dicembre_2008.xls` Exel file (what is needed is just the list of Ateco codes).
* The hierarchical correspondence between NACE Rev. 2 and NAF rév. 2 can be calculated from the
  list of NAF "sous-classes" available in [this spreadsheet](http://www.insee.fr/fr/methodes/nomenclatures/naf2008/xls/naf2008_liste_n5.xls).
* Likewise, the hierarchical correspondence between CPA Ver. 2 and CPF rév. 2 can be calculated from the list of CPF "sous-catégories" available in [this spreadsheet](http://www.insee.fr/fr/methodes/nomenclatures/cpf2015/xls/cpf2015_liste_n6.xls).

## Details on the outputs

The following Turtle files are produced by the programs:

* naics2012.ttl contains the classification scheme for NAICS 2012.
. ateco2007.rdf, nafr2.ttl and cpfr21.ttl contain the classification schemes for Ateco 2007, , NAF rév. 2 and CPF rév. 2.1. These files result directly from the sources communicated by Istat and Insee through basic operations (change of base URI or of serialization format).
* isicr4-naics2012.ttl, nacer2-nafr2.ttl and cpav21-cpfr21.ttl contain the correspondence tables between ISIC Rev.4 and NAICS 2012, NACE Rev. 2 and NAF rév. 2, and CPA Ver. 2.1 and CPF rév 2.1 respectively.
