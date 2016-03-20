<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:skos="http://www.w3.org/2004/02/skos/core#"
    xmlns:xkos="http://rdf-vocabulary.ddialliance.org/xkos#"
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:lf="http://local-functions#"
    exclude-result-prefixes="xs lf"
    version="2.0">

    <xsl:output method="xml" encoding="utf-8" indent="yes"/>
    <xsl:variable name="base-url">http://ec.europa.eu/eurostat/codes/nacer2/</xsl:variable>

    <xsl:template match="Claset">
        <rdf:RDF>
            <xsl:apply-templates select="Classification"/>
        </rdf:RDF>
    </xsl:template>

    <xsl:template match="Classification">
        <rdf:Description rdf:about="{$base-url}nace">
            <!-- TODO Check that a shorthand can be used -->
            <rdf:type rdf:resource="http://www.w3.org/2004/02/skos/core#ConceptScheme"/>
            <skos:prefLabel xml:lang="en"><xsl:value-of select="Label/LabelText"/></skos:prefLabel>
            <skos:altLabel xml:lang="en">NACE Rev. 2</skos:altLabel>
            <dcterms:issued rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime">2008-01-01T00:00:00.000+01:00</dcterms:issued>
            <xkos:hasLevels rdf:resource="{$base-url}levels/list"/>
            <xsl:for-each select="Item[@idLevel='1']">
                <skos:hasTopConcept rdf:resource="{lf:item-uri(@id)}"/>
            </xsl:for-each>
        </rdf:Description>
        <xsl:apply-templates select="Level"/>
        <xsl:apply-templates select="Item"/>
    </xsl:template>

    <xsl:template match="Level">
        <!-- The information on the levels in the source file is useless: recreate from scratch -->
        <xsl:param name="level-names" select="tokenize('Sections,Divisions,Groups,Classes', ',')"/>
        <xsl:param name="level-patterns" select="tokenize('[A-U],[0-9]{2},[0-9]{2}\.[0-9],[0-9]{2}\.[0-9]{2}', ',')"/>
        <xsl:variable name="level-node" select="."/>
        <xsl:variable name="level-count"><xsl:value-of select="max(descendant-or-self::Level/xs:integer(@id))"/></xsl:variable> <!-- Number of levels -->
        <xsl:for-each select="1 to $level-count">
            <xsl:variable name="level-number" select="position()"/>
            <rdf:Description rdf:about="{lf:level-uri($level-number)}">
                <rdf:type rdf:resource="http://rdf-vocabulary.ddialliance.org/xkos#ClassificationLevel"/>
                <skos:prefLabel xml:lang="en"><xsl:value-of select="concat('NACE Rev. 2 - Level ', $level-number, ' - ', $level-names[$level-number])"/></skos:prefLabel>
                <xkos:depth rdf:datatype="http://www.w3.org/2001/XMLSchema#int"><xsl:value-of select="$level-number"/></xkos:depth>
                <xkos:notationPattern><xsl:value-of select="$level-patterns[$level-number]"></xsl:value-of></xkos:notationPattern>
                <xkos:organizedBy rdf:resource="{lf:level-concept-uri($level-number)}"/>
                <xsl:for-each select="$level-node/following-sibling::Item[@idLevel=$level-number]">
                    <skos:member rdf:resource="{lf:item-uri(@id)}"/>
                </xsl:for-each>
            </rdf:Description>
        </xsl:for-each>

        <!-- Creation of the level list -->
        <xsl:variable name="id-prefix">nodenacelevlist</xsl:variable>
        <rdf:Description rdf:resource="{$base-url}levels/list">
            <rdf:first rdf:resource="{lf:level-uri(1)}"/>
            <rdf:rest rdf:nodeID="{concat($id-prefix, '2')}"/>
        </rdf:Description>
        <xsl:for-each select="2 to xs:integer($level-count - 1)">
            <rdf:Description rdf:nodeID="{concat($id-prefix, position() + 1)}">
                <rdf:first rdf:resource="{lf:level-uri(position() + 1)}"/>
                <rdf:rest rdf:nodeID="{concat($id-prefix, position() + 2)}"/>
            </rdf:Description>
        </xsl:for-each>
        <rdf:Description rdf:nodeID="{concat($id-prefix, $level-count)}">
            <rdf:first rdf:resource="{lf:level-uri($level-count)}"/>
            <rdf:rest rdf:resource="http://www.w3.org/1999/02/22-rdf-syntax-ns#nil"/>
        </rdf:Description>
    </xsl:template>

    <xsl:template match="Item">
        <xsl:variable name="item-code" select="@id"/>
        <rdf:Description rdf:about="{lf:item-uri($item-code)}">
            <rdf:type rdf:resource="http://www.w3.org/2004/02/skos/core#Concept"/>
            <skos:inScheme rdf:resource="{$base-url}nace"/>
            <xsl:if test="@idLevel='1'">
                <skos:topConceptOf rdf:resource="{$base-url}nace"/>
            </xsl:if>
            <skos:notation rdf:datatype="http://www.w3.org/2001/XMLSchema#token"><xsl:value-of select="@id"/></skos:notation>
            <xsl:for-each select="Label[@qualifier='Usual']"> <!-- There should be exactly one of those for each item -->
                <skos:prefLabel xml:lang="{lower-case(LabelText/@language)}"><xsl:value-of select="LabelText"/></skos:prefLabel>
            </xsl:for-each>
            <xsl:variable name="this-level" select="@idLevel"/>
            <xsl:variable name="above-level" select="xs:string(xs:decimal(@idLevel) - 1)"/>
            <xsl:variable name="below-level" select="xs:string(xs:decimal(@idLevel) + 1)"/>

            <!-- Hierarchical links -->
            <!-- Broader link for level 2 and more with first predecessor of level above -->
            <xsl:if test="xs:decimal(@idLevel) > 1">
                <skos:broader rdf:resource="{lf:item-uri(preceding-sibling::Item[@idLevel=$above-level][1]/@id)}"/>
            </xsl:if>
            <!-- Narrower links with items of the level below whose first predecessor at this level is the current item -->
            <xsl:for-each select="following-sibling::Item[(@idLevel=$below-level) and (preceding-sibling::Item[@idLevel=$this-level][1] = current())]">
                <skos:narrower rdf:resource="{lf:item-uri(@id)}"/>
            </xsl:for-each>
            <!-- Broader match link to parent ISIC4 item -->
            <!-- Links to the ISIC classfication items -->
            <xsl:for-each select="Property[@name='Generic']/PropertyQualifier"> <!-- Should be one and only one -->
                <skos:broadMatch rdf:resource="{lf:isic-item-uri(PropertyText)}"/>
            </xsl:for-each>

            <!-- URIs of explanatory notes -->
            <xsl:for-each select="Property[@name='ExplanatoryNote']/PropertyQualifier">
                <xsl:element name="xkos:{lf:note-property(@name)}">
                    <xsl:attribute name="rdf:resource" select="lf:note-uri($item-code, @name)"/>
                </xsl:element>
            </xsl:for-each>
        </rdf:Description>

        <!-- Explanatory notes -->
        <xsl:for-each select="Property[@name='ExplanatoryNote']/PropertyQualifier">
            <rdf:Description rdf:about="{lf:note-uri($item-code, @name)}">
                <rdf:type rdf:resource="http://rdf-vocabulary.ddialliance.org/xkos#ExplanatoryNote"/>
                <xkos:plainText xml:lang="en"><xsl:value-of select="PropertyText"/></xkos:plainText>
            </rdf:Description>
        </xsl:for-each>

    </xsl:template>

    <xsl:function name="lf:item-uri" as="xs:string">
        <xsl:param name="item-code" as="xs:string"/>
        <xsl:sequence select="concat($base-url, $item-code)"/>
    </xsl:function>

    <xsl:function name="lf:level-uri" as="xs:string">
        <xsl:param name="level-depth" as="xs:decimal"/>
        <xsl:choose>
            <xsl:when test="$level-depth=1"><xsl:value-of select="concat($base-url, 'sections')"/></xsl:when>
            <xsl:when test="$level-depth=2"><xsl:value-of select="concat($base-url, 'divisions')"/></xsl:when>
            <xsl:when test="$level-depth=3"><xsl:value-of select="concat($base-url, 'groups')"/></xsl:when>
            <xsl:when test="$level-depth=4"><xsl:value-of select="concat($base-url, 'classes')"/></xsl:when>
            <xsl:otherwise>unknown</xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="lf:level-concept-uri" as="xs:string">
        <xsl:param name="level-depth" as="xs:decimal"/>
        <xsl:choose>
            <xsl:when test="$level-depth=1"><xsl:value-of select="concat($base-url, 'concept/section')"/></xsl:when>
            <xsl:when test="$level-depth=2"><xsl:value-of select="concat($base-url, 'concept/division')"/></xsl:when>
            <xsl:when test="$level-depth=3"><xsl:value-of select="concat($base-url, 'concept/group')"/></xsl:when>
            <xsl:when test="$level-depth=4"><xsl:value-of select="concat($base-url, 'concept/class')"/></xsl:when>
            <xsl:otherwise>unknown</xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="lf:isic-item-uri" as="xs:string">
        <xsl:param name="item-code" as="xs:string"/>
        <xsl:sequence select="concat('http://stats.un.org/codes/isic4/', $item-code)"/>
    </xsl:function>

    <xsl:function name="lf:note-uri" as="xs:string">
        <xsl:param name="item-code" as="xs:string"/>
        <xsl:param name="note-type" as="xs:string"/>
        <xsl:sequence select="concat($base-url, $item-code, '/', $note-type)"/>
    </xsl:function>

    <xsl:function name="lf:note-property" as="xs:string">
        <xsl:param name="note-type" as="xs:string"/>
        <xsl:choose>
            <xsl:when test="$note-type='CentralContent'">coreContentNote</xsl:when>
            <xsl:when test="$note-type='LimitContent'">additionalContentNote</xsl:when>
            <xsl:when test="$note-type='Exclusions'">exclusionNote</xsl:when>
            <xsl:when test="$note-type='Rules'">caseLaw</xsl:when>
            <xsl:otherwise>unknown</xsl:otherwise>
        </xsl:choose>
    </xsl:function>

</xsl:stylesheet>