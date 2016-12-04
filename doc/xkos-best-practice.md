# XKOS Best Practice

## Introduction

This document describes some best practices for representing statistical classifications as XKOS. Where appropriate, a SPARQL query is provided: for a conformant RDF dataset, the query associated to compulsory rules should return no results.

## Classifications and classifications schemes

* All classification schemes MUST have a `skos:notation` property which value is the short name of the classification scheme with no language tag.

Associated query:

```
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
PREFIX xkos:<http://rdf-vocabulary.ddialliance.org/xkos#>

SELECT ?s ?label ?code {
  ?s rdf:type skos:ConceptScheme .
  MINUS {
      SELECT ?s ?label ?code {
        ?s rdf:type skos:ConceptScheme .
        ?s skos:notation ?code .
      }
    }
  }
```

* All classification schemes MUST have a `skos:prefLabel` property which value is the complete name of the classification scheme in English, with a language tag set at '@en'. Names in other languages MAY be provided with the same property. All names MUST have a language tag.

Associated query:

```
TBD
```
* All classification schemes MAY have additional labels represented by values of the `skos:altLabel` property. The SKOS integrity rules MUST be applied. The [XKOS specification](http://rdf-vocabulary.ddialliance.org/xkos.html#add-labels) gives rules regarding the representation of fixed-length labels.

* All classification schemes SHOULD have a `dc:description` property which value is the short descriptive text about the classification scheme in English, with a language tag set at '@en'. Description in other languages MAY be provided with the same property. All descriptive texts MUST have a language tag.

Associated query:

```
TBD
```
* All classification schemes SHOULD have a `skos:scopeNote` property which value is a resource of type `xkos:ExplanatoryNote`. The explanatory note MUST have a `xkos:plainText` property which value is a long descriptive text about the classification scheme in English, with a language tag set at '@en'. Long descriptives in other languages MAY be provided: for each language a dedicated `xkos:ExplanatoryNote` resource will be created, with a `xkos:plainText` string bearing the corresponding language tag.

Associated query:

```
TBD
```

* All classification schemes MUST have a `dcterms:issued` property which value is the publication date of the of the classification scheme with datatype xsd:date.

Associated query:

```
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
PREFIX xkos:<http://rdf-vocabulary.ddialliance.org/xkos#>
PREFIX dcterms:<http://purl.org/dc/terms/>

  SELECT ?s ?label ?issued {
    ?s rdf:type skos:ConceptScheme .
    MINUS {
      SELECT ?s ?label ?issued {
        ?s rdf:type skos:ConceptScheme .
        ?s dcterms:issued ?issued . 
      }
    }
  }
```

* All classification schemes SHOULD have a `dcterms:modified` property which value is the last modification date of the of the classification scheme with datatype xsd:date.

Associated query:

```
PREFIX skos:<http://www.w3.org/2004/02/skos/core#>
PREFIX xkos:<http://rdf-vocabulary.ddialliance.org/xkos#>
PREFIX dcterms:<http://purl.org/dc/terms/>

  SELECT ?s ?label ?modified {
    ?s rdf:type skos:ConceptScheme .
    MINUS {
      SELECT ?s ?label ?modified {
        ?s rdf:type skos:ConceptScheme .
        ?s dcterms:modified ?modified . 
      }
    }
  }
```