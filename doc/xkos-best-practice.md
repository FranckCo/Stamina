# XKOS Best Practice

## Introduction

This document describes some best practices for representing statistical classifications as XKOS.

## Classifications and classifications schemes

* All classification schemes should have a `skos:notation` property which value is the short name of the classification scheme with no language tag.

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