# Content, vocabularies and naming

## Content

The repository contains different types of data:
* models (GSIM, GSBPM, GAMSO, etc.) and information about CSPA services
* glossaries, codes, classifications and correspondence tables
* metadata about the main content: provenance, cataloging, provenance and publication information

## Vocabularies

The RDF vocabularies used depend on the type of data as described above:
* models will mostly be expressed in OWL
* glossaries and classifications will be represented in SKOS/XKOS
* metadata will use the standard vocabularies: ADMS/DCAT, VoID, PROV

## Naming policy

### Resources to identify

We deduce form the previous sections the different types of resources that we have to identify:
* Vocabulary or ontology elements: ontologies, classes and properties, datatypes, individuals. This is essentially for the "model" part.
* For the classification part: concept schemes, classification levels, concepts, notes, correspondence tables, concept associations, etc.
* Regarding the metadata: ADMS catalogs, assets and asset distributions, VoID datasets, PROV entities, activities and agents, named graphs, etc.

### Principles

The data sources for the project come from different producers: Eurostat, the UNECE, the UNSC, national offices, the SDMX sponsors group, etc. Ideally, each resource should be identified by a URI based on a domain controlled by its publisher: for example the GSIM should use URIs in the `http://www.unece.org` domain, the CPC should use URIs in the `http://www.unsd.org` and the NACE should use `http://ec.europa.eu/eurostat`. However, it is clearly impossible within the timeframe of the project to design a naming policy for each of these actors, specific to their data, and have it validated by them.

This is why it is suggested to define a naming policy based on a "neutral" domain name, for example `stamina-project.org`. We suggest to divide the root domain name according to the main type of data: `/models`, `/concepts` (for glossaries and classifications) and `/metadata`.

Under `concepts`, we subdivide by an identifier of the major version of a classification (`isicr31`, `nacer2`, etc.) or, for the correspondences, by the combination of the major versions of the classifications that the table compares: `isicr4-cpcv21` for example. The source classification scheme should be first.

Similarly, the `/metadata` path root will be further refined according to the vocabulary, for example `/adms' or `/prov`.

Under `/models`, it is useful to distinguish between the identification of the individuals (a given GSBP phase or GSIM object) and a the identification of the vocabulary terms defined to represent the models themselves (the OWL class corresponding to a GSBPM phase or to a GSIM object):
* for individuals, we subdivide further according to the name of model: `/gsim`, `/gsbmp`, `/gamso`, etc.
* OWL vocabularies usually use hash-namespaces, so our OWL objects will be in the `http://stamina-project.org/models/def#` namespace.

Inside the given context, the default pattern for identifying a given resource will be: `/{resource-type}/{resource-identifier}`, except for OWL artifacts whose URI will be `http://stamina-project.org/models/def#{name-of-artifacts}`. The `{resource-identifier}` can be any local identifier for the resource, for example the item code for a classification item, a version number or a publication date for a concept description, etc. When there is only one resource of a given type within the context, the `/{resource-identifier}` path element can be omitted.

When a resource strongly depends on another, this can be represented in the path hierarchy, for example:
* An explanatory note for a classification item exists only in the context of this item, so (if there is a necessity to represent it as a resource and not simply as a RDF literal) it will have an URI like `http://stamina-project/concepts/nacer2/class/51.22/inclusion-note`.
* Likewise, an ADMS asset distribution will be identified by extending the asset URI. On the contrary, the asset URI does not extend the catalog URI since an asset may be included in several catalogs.

### Examples

For identifying section B of the ISIC Rev.3.1 we have the following URI components:

| Element | Value |
|----|----|
| Authority   | `http://stamina-project.org` |
| Path element for classifications   | `/concepts` |
| Path for ISIC Rev.3.1   | `/isicr31` |
| Resource type   | `/section` |
| Resource identifier   | `/B` |

The URI is thus `http://stamina-project/concepts/isicr31/section/B`.

For sub-process 3.1 of the GSBPM, we have:

| Element | Value |
|----|----|
| Authority   | `http://stamina-project.org` |
| Path element for models   | `/models` |
| Path for GSBPM   | `/gsbpm` |
| Resource type   | `/sub-process` |
| Resource identifier   | `/3.1` |

The URI is thus `http://stamina-project/models/gsbpm/sub-process/3.1`.

Additional examples:

| Resource | URI |
|----|----|
| ADMS asset for CPC Ver.2.1 | `http://stamina-project.org/meta/adms/asset/cpcv21` |
| VoID dataset of the GSBPM individuals | `http://stamina-project.org/meta/void/dataset/gsbpm` |
| VoID dataset of the GSBPM ontology | `http://stamina-project.org/meta/void/dataset/gsbpm-def` |
