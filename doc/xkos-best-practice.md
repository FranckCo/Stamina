# XKOS Best Practice

## Introduction

This document describes some best practices for representing statistical classifications as XKOS. For each proposed rule, a SPARQL query is provided: for a conformant RDF store, the query should return no result.

## Classifications and classifications schemes

* All classification schemes must have a `skos:notation` property which value is the short name of the classification scheme with no language tag.

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

* All classification schemes should have a `dcterms:issued` property which value is the publication date of the of the classification scheme with datatype xsd:date.

* All classification schemes should have a `dcterms:modified` property which value is the last modification date of the of the classification scheme with datatype xsd:date.

Associated queries:

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

* All classification schemes must have a `skos:prefLabel` property which value is the complete name of the classification scheme in English. Names in other languages may be provided with the same property. All names must have a language tag.

Associated query:

```
TBD
```

* All classification schemes should have a `dc:description` property which value is the short descriptive text about the classification scheme in English. Description in other languages may be provided with the same property. All descriptive texts should have a language tag.

Associated query:

```
TBD
```
* All classification schemes should have a `skos:scopeNote` property which value is a resource of type `xkos:ExplanatoryNote`. The explanatory note must have a `xkos:plainText` property which value is a long descriptive text about the classification scheme in English, with a language tag set at '@en'. Long descriptives in other languages may be provided: for each language a dedicated `xkos:ExplanatoryNote` resource will be created, with a `xkos:plainText` string bearing the corresponding language tag.

Associated query:

```
TBD
```

