# UNSD classifications: ISIC and CPC

## Introduction

ISIC and CPC are explicitly in the scope of the IMS project. This means that their latest versions (ISIC Rev.4 and CPC Ver2.1 at the time of writing), as well as the correspondences between items of those versions should be included in the RDF database. Also, in order to provide use cases related to the evolution in time of classifications, it was decided to include the previous versions of both classifications, as well as the associated historical correspondences.

The authoritative source for the UNSD classifications is the [UNSD classification registry](http://unstats.un.org/unsd/cr/registry/), and in particular the download page at http://unstats.un.org/unsd/cr/registry/regdnld.asp. The information is available in several formats and languages. Only PDF, MS Access and online HTML have the explanatory notes, and only Access and HTML have the latest corrections. Additional sources provided by the UNSD include, amongst others, French and Spanish labels and correspondence tables (which are only available in English).

## Extraction tools

MS Access is a proprietary format, but there are free and open source tools to read it, so it seems logical to use the Access files as main data sources. [Jackcess] (http://jackcess.sourceforge.net/), which is licensed under the Apache License, Version 2.0, is used to read the databases. The additional labels and correspondence tables are given in CSV files and can be read easily with [Apache CSV Commons](https://commons.apache.org/proper/commons-csv/). Note that the CSV files containing additional languages are encoded using the ANSI (Cp1252) character set; this is expected by the program.

## Details on the sources

For **ISIC Rev.4**, the following sources were used:

* [ISIC_Rev_4_english_database.zip](http://unstats.un.org/unsd/cr/registry/regdntransfer.asp?f=135) for most of the data regarding ISIC Rev.4, including structure, English labels and explanatory notes
* [ISIC_Rev_4_french.zip](http://unstats.un.org/unsd/cr/registry/regdntransfer.asp?f=189) for additional French ISIC Rev.4 labels.
* [ISIC_Rev_4_spanish.zip](http://unstats.un.org/unsd/cr/registry/regdntransfer.asp?f=198) for additional Spanish ISIC Rev.4 labels.

The corresponding files for **ISIC Rev.3.1** are:
* [ISIC_Rev.3.1_database_english.zip](http://unstats.un.org/unsd/cr/registry/regdntransfer.asp?f=172) for the main data
* [ISIC_Rev_3_1_spanish_structure.zip](http://unstats.un.org/unsd/cr/registry/regdntransfer.asp?f=105) for the Spanish labels
* French labels for ISIC Rev.3.1 do not seem to be available.

For **CPC**, the following sources were used:

* [CPCv21_database_english.zip](http://unstats.un.org/unsd/cr/registry/regdntransfer.asp?f=287), which contain the structure, English labels and notes for version 2.1
* [CPCv2_database_english.zip](http://unstats.un.org/unsd/cr/registry/regdntransfer.asp?f=235), which contain the structure, English labels and notes for version 2
* [CPCv2_spanish.zip](http://unstats.un.org/unsd/cr/registry/regdntransfer.asp?f=279), which gives the Spanish labels for version 2
* Labels for other languages or Spanish labels for version 2.1 do not seem to be available

Regarding **correspondence tables** (all files are zipped CSV):

* Between ISIC versions 3.1 and 4: [ISIC31-ISIC4.zip](http://unstats.un.org/unsd/cr/registry/regdntransfer.asp?f=121)
* Between CPC versions 2 and 2.1: [CPCv2_CPCv21.zip](http://unstats.un.org/unsd/cr/registry/regdntransfer.asp?f=291)
* Between ISIC version 4 and CPC version 2.1: [ISIC4_CPCv21.zip](http://unstats.un.org/unsd/cr/registry/regdntransfer.asp?f=289)

The following precisions are copied from the "readme" files available in the archives:

* The correspondence between CPC versions 2 and 2.1 does not yet include divisions 61 and 62 of the CPC.
* The correspondence between ISIC Rev.4 and CPC Rev2.1 does not yet include divisions 45, 46 and 47 of ISIC.

All files should be put in the main/resources/data folder and unzipped before running the programs.

## Details on the outputs

The following Turtle files are produced by the programs:

* isic31.ttl, isic4.ttl, cpc2.ttl and cpc21.ttl correspond to the classification schemes for ISIC Rev.3.1, ISIC Rev.4, CPC Ver.2 and CPC Ver.2.1
* isic31-isic4.ttl, isic4-cpc21.ttl and cpc2-cpc21.ttl contain the correspondence tables between ISIC Rev.3.1 and ISIC Rev.4, ISIC Rev.4 and CPC Ver.2.1, and CPC Ver.2 and CPC Ver.2.1
