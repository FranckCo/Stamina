<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:skos="http://www.w3.org/2004/02/skos/core#"
    xmlns:g="ddi:group:3_1"
    xmlns:l="ddi:logicalproduct:3_1"
    xmlns:r="ddi:reusable:3_1"
    exclude-result-prefixes="rdf skos xs"
    version="2.0">

    <xsl:output method="xml" encoding="utf-8" indent="yes"/>

    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()" />
        </xsl:copy>
    </xsl:template>

    <xsl:template match="/">
        <rdf:DDIInstance agency="ca.statcan" id="XXX">
            <g:ResourcePackage agency="ca.statcan" id="XXX">
                <g:Purpose urn="urn:ddi:ca:statcan.CodeScheme.XXX" id="XXX">
                    <l:Content/>
                </g:Purpose>
                <l:CategoryScheme urn="urn:ddi:ca.statcan:CategoryScheme.XXX" id="XXX" xml:lang="en" version="1.0.0" isPublished="true">
                    <l:CategorySchemeName>NAICS 2012 - Level 1 - Sectors</l:CategorySchemeName>
                    <r:Description/>
                    <xsl:apply-templates select="/rdf:RDF/rdf:Description[rdf:type/@rdf:resource='http://www.w3.org/2004/02/skos/core#Concept']" mode="categories">
                        <xsl:sort select="skos:notation"/>
                    </xsl:apply-templates>
                </l:CategoryScheme>
                <l:CodeScheme urn="urn:ddi:ca:statcan.CodeScheme.XXX" id="XXX" xml:lang="en" version="1.0.0" isPublished="true">
                    <l:CodeSchemeName>North American Industry Classification System (NAICS) 2012</l:CodeSchemeName>
                    <l:CategorySchemeReference>
                        <r:URN>urn:ddi:ca.statcan:CategoryScheme.XXX</r:URN>
                    </l:CategorySchemeReference>
                    <xsl:apply-templates select="/rdf:RDF/rdf:Description[skos:inScheme and not(skos:broader)]" mode="codes">
                        <xsl:sort select="skos:notation"/>
                    </xsl:apply-templates>
                </l:CodeScheme>
            </g:ResourcePackage>
        </rdf:DDIInstance>
    </xsl:template>

    <xsl:template match="rdf:Description" mode="categories">
        <l:Category urn="urn:ddi:ca:statcan.Category.XXX" id="XXX">
            <r:Label><xsl:value-of select="normalize-space(skos:prefLabel[@xml:lang='en'])"/></r:Label>
        </l:Category>
    </xsl:template>

    <xsl:template match="rdf:Description" mode="codes">
        <xsl:variable name="code" select="skos:notation"/>
        <l:Code>
            <l:CategoryReference>
                <r:URN>urn:ddi:ca:statcan.Category.XXX</r:URN>
            </l:CategoryReference>
            <l:Value><xsl:value-of select="$code"/></l:Value>
            <xsl:for-each select="skos:narrower">
                <xsl:sort select="@rdf:resource"/>
                <xsl:variable name="uri" select="@rdf:resource"/>
                <xsl:apply-templates select="/rdf:RDF/rdf:Description[@rdf:about=$uri]" mode="codes"/>
            </xsl:for-each>
        </l:Code>
    </xsl:template>

</xsl:stylesheet>