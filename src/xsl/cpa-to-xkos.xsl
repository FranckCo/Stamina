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
    <xsl:param name="base-url">http://ec.europa.eu/eurostat/codes/cpav21/</xsl:param>
    <xsl:param name="version" as="xs:string">2.1</xsl:param>
    <xsl:param name="with-notes" as="xs:boolean">true</xsl:param>

    <xsl:template match="Claset">
        <rdf:RDF>
            <xsl:apply-templates select="Classification"/>
        </rdf:RDF>
    </xsl:template>

    <xsl:template match="Classification">
        <rdf:Description rdf:about="{$base-url}cpa">
            <!-- TODO Check that a shorthand can be used -->
            <rdf:type rdf:resource="http://www.w3.org/2004/02/skos/core#ConceptScheme"/>
            <skos:prefLabel xml:lang="en"><xsl:value-of select="Label/LabelText"/></skos:prefLabel>
            <skos:notation xml:lang="en"><xsl:value-of select="concat('CPA Ver. ', $version)"/></skos:notation>
            <!-- Compute the date of publication -->
            <xsl:choose>
                <xsl:when test="$version = '2.1'">
                    <dcterms:issued rdf:datatype="http://www.w3.org/2001/XMLSchema#date">2015-01-01</dcterms:issued>
                </xsl:when>
            </xsl:choose>
            <xsl:choose>
                <xsl:when test="$version = '2008'">
                    <dcterms:issued rdf:datatype="http://www.w3.org/2001/XMLSchema#date">2008-01-01</dcterms:issued>
                </xsl:when>
            </xsl:choose> <!-- No otherwise -->
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
        <xsl:variable name="level-names" select="tokenize('Sections,Divisions,Groups,Classes,Categories,Sub-categories', ',')"/>
        <xsl:variable name="level-patterns" select="tokenize('[A-U],[0-9]{2},[0-9]{2}\.[0-9],[0-9]{2}\.[0-9]{2},[0-9]{2}\.[0-9]{2}\.[0-9],[0-9]{2}\.[0-9]{2}\.[0-9]{2}', ',')"/>
        <xsl:variable name="level-node" select="."/>
        <xsl:variable name="level-count"><xsl:value-of select="max(descendant-or-self::Level/xs:integer(@id))"/></xsl:variable> <!-- Number of levels -->
        <xsl:for-each select="1 to $level-count">
            <xsl:variable name="level-number" select="position()"/>
            <rdf:Description rdf:about="{lf:level-uri($level-number)}">
                <rdf:type rdf:resource="http://rdf-vocabulary.ddialliance.org/xkos#ClassificationLevel"/>
                <skos:prefLabel xml:lang="en"><xsl:value-of select="concat('CPA Ver. ', $version,' - Level ', $level-number, ' - ', $level-names[$level-number])"/></skos:prefLabel>
                <xkos:depth rdf:datatype="http://www.w3.org/2001/XMLSchema#int"><xsl:value-of select="$level-number"/></xkos:depth>
                <xkos:notationPattern><xsl:value-of select="$level-patterns[$level-number]"></xsl:value-of></xkos:notationPattern>
                <xkos:organizedBy rdf:resource="{lf:level-concept-uri($level-number)}"/>
                <xsl:for-each select="$level-node/following-sibling::Item[@idLevel=$level-number]">
                    <skos:member rdf:resource="{lf:item-uri(@id)}"/>
                </xsl:for-each>
            </rdf:Description>
        </xsl:for-each>

        <!-- Creation of the level list -->
        <xsl:variable name="id-prefix">nodecpalevlist</xsl:variable>
        <rdf:Description rdf:about="{$base-url}levels/list">
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
            <skos:inScheme rdf:resource="{$base-url}cpa"/>
            <xsl:if test="@idLevel='1'">
                <skos:topConceptOf rdf:resource="{$base-url}cpa"/>
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

            <!-- Only two explanatory notes (case laws) for the CPA 2.1 -->
            <xsl:if test="$with-notes">
                <xsl:for-each select="Property[@name='ExplanatoryNote']/PropertyQualifier">
                    <xsl:element name="xkos:{lf:note-property(@name)}">
                        <xsl:attribute name="rdf:resource" select="lf:note-uri($item-code, @name)"/>
                    </xsl:element>
                </xsl:for-each>
            </xsl:if>
        </rdf:Description>

        <!-- Explanatory notes -->
        <xsl:if test="$with-notes">
            <xsl:for-each select="Property[@name='ExplanatoryNote']/PropertyQualifier">
                <rdf:Description rdf:about="{lf:note-uri($item-code, @name)}">
                    <rdf:type rdf:resource="http://rdf-vocabulary.ddialliance.org/xkos#ExplanatoryNote"/>
                    <xkos:plainText xml:lang="en"><xsl:value-of select="PropertyText"/></xkos:plainText>
                </rdf:Description>
            </xsl:for-each>
        </xsl:if>

    </xsl:template>

    <!-- Naming policy and other useful functions -->
    <!-- For CPA, the structure is the same for 2008 and 2.1 so we can have more explicit and straitghforward functions -->
    <!-- (that would be different for CPA 2002 which is aligned with the structure of NACE Rev. 1.1, with subsections) -->

    <xsl:function name="lf:item-uri" as="xs:string">
        <xsl:param name="item-code" as="xs:string"/>
        <xsl:variable name="code-length" select="string-length($item-code)"/>
        <xsl:choose>
            <xsl:when test="$code-length=1"><xsl:value-of select="concat($base-url, 'section/', $item-code)"/></xsl:when>
            <xsl:when test="$code-length=2"><xsl:value-of select="concat($base-url, 'division/', $item-code)"/></xsl:when>
            <xsl:when test="$code-length=4"><xsl:value-of select="concat($base-url, 'group/', $item-code)"/></xsl:when>
            <xsl:when test="$code-length=5"><xsl:value-of select="concat($base-url, 'class/', $item-code)"/></xsl:when>
            <xsl:when test="$code-length=7"><xsl:value-of select="concat($base-url, 'category/', $item-code)"/></xsl:when>
            <xsl:when test="$code-length=8"><xsl:value-of select="concat($base-url, 'subcategory/', $item-code)"/></xsl:when>
            <xsl:otherwise>unknown</xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="lf:level-uri" as="xs:string">
        <xsl:param name="level-depth" as="xs:decimal"/>
        <xsl:choose>
            <xsl:when test="$level-depth=1"><xsl:value-of select="concat($base-url, 'sections')"/></xsl:when>
            <xsl:when test="$level-depth=2"><xsl:value-of select="concat($base-url, 'divisions')"/></xsl:when>
            <xsl:when test="$level-depth=3"><xsl:value-of select="concat($base-url, 'groups')"/></xsl:when>
            <xsl:when test="$level-depth=4"><xsl:value-of select="concat($base-url, 'classes')"/></xsl:when>
            <xsl:when test="$level-depth=5"><xsl:value-of select="concat($base-url, 'categories')"/></xsl:when>
            <xsl:when test="$level-depth=6"><xsl:value-of select="concat($base-url, 'subcategories')"/></xsl:when>
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
            <xsl:when test="$level-depth=5"><xsl:value-of select="concat($base-url, 'concept/category')"/></xsl:when>
            <xsl:when test="$level-depth=6"><xsl:value-of select="concat($base-url, 'concept/subcategory')"/></xsl:when>
            <xsl:otherwise>unknown</xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="lf:note-uri" as="xs:string">
        <xsl:param name="item-code" as="xs:string"/>
        <xsl:param name="note-type" as="xs:string"/>
        <xsl:sequence select="concat(lf:item-uri($item-code), '/', $note-type)"/>
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