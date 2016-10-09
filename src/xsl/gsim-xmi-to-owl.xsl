<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
	xmlns:owl="http://www.w3.org/2002/07/owl#"
	xmlns:gsim-xmi="http://unece.org/gsim/0.8"
	xmlns:gsim="http://stamina-project.org/models/gsim/"
	xmlns:xslGSIM="localhost.localdomain"
	exclude-result-prefixes="xslGSIM gsim-xmi">

	<!-- gsim-xmi: is input XML ; $short-base-uri is XML output ; xslGSIM: is xslt code -->

	<xsl:output method="xml" encoding="UTF-8" indent="yes" />

	<xsl:variable name="short-base-uri">gsim</xsl:variable>

	<xsl:variable name="classesName">
		<!-- Clean class names: suppress spaces or other characters -->
		<xsl:for-each select="/gsim-xmi:Model/gsim-xmi:Package/gsim-xmi:Classes/gsim-xmi:Class/gsim-xmi:Name">
			<xslGSIM:item>
				<xsl:value-of select="concat($short-base-uri,':',xslGSIM:cleanClassesName(string(.)))" />
			</xslGSIM:item>
		</xsl:for-each>
	</xsl:variable>

	<!-- Strings to be matched for doc tokenization -->
	<xsl:variable name="gsim-Doc-strings-to-be-matched">
		<xslGSIM:item>
			<xslGSIM:match-string>Definition:</xslGSIM:match-string>
			<xslGSIM:XML-tag><xsl:value-of select="$short-base-uri"/>:classDefinition</xslGSIM:XML-tag>
		</xslGSIM:item>
		<xslGSIM:item>
			<xslGSIM:match-string>Explanatory Text:</xslGSIM:match-string>
			<xslGSIM:XML-tag><xsl:value-of select="$short-base-uri"/>:classExplanatoryText</xslGSIM:XML-tag>
		</xslGSIM:item>
		<xslGSIM:item>
			<xslGSIM:match-string>Synonyms:</xslGSIM:match-string>
			<xslGSIM:XML-tag><xsl:value-of select="$short-base-uri"/>:classSynonyms</xslGSIM:XML-tag>
		</xslGSIM:item>
	</xsl:variable>

	<!-- Type mappings -->
	<xsl:variable name="UML-classes-to-DataTypes">
		<xslGSIM:range-class value='xs:string'>
			<xslGSIM:range-class-item>xs:string</xslGSIM:range-class-item>
			<xslGSIM:range-class-item>string</xslGSIM:range-class-item>
			<xslGSIM:range-class-item/>
		</xslGSIM:range-class>
		<xslGSIM:range-class value='xs:date'>
			<xslGSIM:range-class-item>date</xslGSIM:range-class-item>
		</xslGSIM:range-class>
		<xslGSIM:range-class value='xs:boolean'>
			<xslGSIM:range-class-item>boolean</xslGSIM:range-class-item>
			<xslGSIM:range-class-item>binary</xslGSIM:range-class-item>
		</xslGSIM:range-class>
		<xslGSIM:range-class value='xs:integer'>
			<xslGSIM:range-class-item>int</xslGSIM:range-class-item>
		</xslGSIM:range-class>
		<xslGSIM:range-class value='xs:decimal'>
			<xslGSIM:range-class-item>numeric</xslGSIM:range-class-item>
			<xslGSIM:range-class-item>number</xslGSIM:range-class-item>
		</xslGSIM:range-class>
		<xslGSIM:range-class value='xs:string'>
			<xslGSIM:range-class-item>controlledVocabulary</xslGSIM:range-class-item>
			<xslGSIM:range-class-item>extensibleRedefinedlist</xslGSIM:range-class-item>
			<xslGSIM:range-class-item>Id</xslGSIM:range-class-item>
			<xslGSIM:range-class-item>identifier</xslGSIM:range-class-item>
			<xslGSIM:range-class-item>Link</xslGSIM:range-class-item>
			<xslGSIM:range-class-item>dateRange</xslGSIM:range-class-item>
			<xslGSIM:range-class-item>met/unmet</xslGSIM:range-class-item>
			<xslGSIM:range-class-item>entityDesignator</xslGSIM:range-class-item>
			<xslGSIM:range-class-item>versionDesignator</xslGSIM:range-class-item>
		</xslGSIM:range-class>
	</xsl:variable>

	<!-- Name of the GSIM object class -->
	<xsl:variable name="gsimObjectClass">GSIMObject</xsl:variable> 

	<xsl:template match="/">
		<rdf:RDF>
			<xsl:apply-templates select="gsim-xmi:Model"/>
		</rdf:RDF>
	</xsl:template>

	<xsl:template match="gsim-xmi:Model">
		<xsl:element name="{$short-base-uri}:{$gsimObjectClass}">
			<rdf:type> <owl:Class/> </rdf:type>
			<rdfs:label xml:lang="en">GSIM Object</rdfs:label>
		</xsl:element>
		<xsl:apply-templates select="gsim-xmi:Package">
			<xsl:sort select="gsim-xmi:Name" />
		</xsl:apply-templates>
		<xsl:call-template name="properties" />
	</xsl:template>

	<xsl:template name="properties">
		<xsl:variable name="attributes"
			select="gsim-xmi:Package/gsim-xmi:Classes/gsim-xmi:Class/gsim-xmi:Attribute" />
		<xsl:variable name="associations"
			select="gsim-xmi:Package/gsim-xmi:Associations/gsim-xmi:Association" />
		<xsl:variable name="proto-properties" select="$associations, $attributes" />
		<xsl:for-each-group select="$proto-properties"
			group-by="xslGSIM:PropertyNameSearch(.)">
			<!-- xsl:message>Search key: <xsl:value-of select="current-grouping-key()"/>, 
				corresponding values: <xsl:value-of select="current-group()/gsim-xmi:Name" separator=", 
				"/></xsl:message -->
			<xsl:variable name="cardinality1" select="count(current-group())" />
			<xsl:for-each-group select="current-group()"
				group-by="xslGSIM:PropertyNameSearchWithSourceDestination(.)">
				<!-- Source and Destination Name -->
				<xsl:variable name="cardinality2" select="count(current-group())" />
				<xsl:for-each-group select="current-group()"
					group-by="xslGSIM:PropertyRangeName(.)">
					<!-- xsl:message>Range key: <xsl:value-of select="current-grouping-key()"/> 
						</xsl:message -->
					
					<xsl:variable name="cardinality3" select="count(current-group())" />
					<xsl:for-each-group select="current-group()"
						group-by="concat(xslGSIM:PropertyRangeMinCardinality(.),'..',xslGSIM:PropertyRangeMaxCardinality(.))">
						<!-- It works because we check that a couple property-domain appear only once. -->
						<xsl:variable name="cardinality4" select="count(current-group())" />
						<xsl:if test="$cardinality4!=1 and $cardinality4 != $cardinality3  and $cardinality4 != $cardinality2 and $cardinality4 != $cardinality1">
							<xsl:message select="concat($cardinality1,'-',$cardinality2,'-',$cardinality3,'-',$cardinality4)">Problem of programming</xsl:message>
						</xsl:if>
						
						<xsl:element
						name="{$short-base-uri}:{xslGSIM:PropertyFinalName(.,$cardinality1, $cardinality2, $cardinality3, $cardinality4)}">
							<xsl:call-template name="xslGSIM:propertyType" />
							<rdfs:label xml:lang="en">
								<xsl:value-of select="xslGSIM:PropertyName(.)" />
							</rdfs:label>
							
							<xsl:call-template name="property-Domain">
								<xsl:with-param name="Domains">
								<xsl:for-each select="current-group()">
							<xslGSIM:item name="{xslGSIM:PropertyDomainName(.)}" min="{xslGSIM:PropertyDomainMinCardinality(.)}" max="{xslGSIM:PropertyDomainMinCardinality(.)}"/>
							</xsl:for-each>
								</xsl:with-param>
							</xsl:call-template>
							<xsl:call-template name="property-range"/>
						</xsl:element>
						
					</xsl:for-each-group>
				</xsl:for-each-group>
			</xsl:for-each-group>
		</xsl:for-each-group>
	</xsl:template>
	
	<xsl:template name="property-Domain">
		<xsl:param name="Domains" />
		<rdfs:domain>
				<xsl:choose>
				<xsl:when test="count($Domains/xslGSIM:item)=1">
					<xsl:copy-of select="xslGSIM:propertyDomains($Domains)"/>
				</xsl:when>
				<xsl:when test="count($Domains/xslGSIM:item)&gt;1">
					<owl:Class>
						<owl:unionOf rdf:parseType="Collection">
							<xsl:copy-of select="xslGSIM:propertyDomains($Domains)"/>
						</owl:unionOf>
					</owl:Class>
				</xsl:when>
				<xsl:otherwise>
				<xsl:message>Problem: no domain</xsl:message>
				</xsl:otherwise>
				</xsl:choose>
			</rdfs:domain>
		</xsl:template>
		
	<xsl:function name="xslGSIM:propertyDomains">
		<xsl:param name="Domains" />
		<xsl:for-each select="$Domains/xslGSIM:item">
			<xsl:call-template name="xslGSIM:cardinalityRestriction">
					<xsl:with-param name="restricted-class" select="./@name" />
					<xsl:with-param name="minCardinality" select="./@min" />
					<xsl:with-param name="maxCardinality" select="./@max" />
			</xsl:call-template>
		</xsl:for-each>
	</xsl:function>

	<xsl:template name="property-range">
		<rdfs:range>
			<xsl:call-template name="xslGSIM:cardinalityRestriction">
				<xsl:with-param name="restricted-class" select="xslGSIM:PropertyRangeName(.)" />
				<xsl:with-param name="minCardinality"
					select="xslGSIM:PropertyRangeMinCardinality(.)" />
				<xsl:with-param name="maxCardinality"
					select="xslGSIM:PropertyRangeMaxCardinality(.)" />
			</xsl:call-template>
		</rdfs:range>
	</xsl:template>

	<xsl:template match="gsim-xmi:Package">
		<xsl:variable name="package-name" select="xslGSIM:cleanClassesName(gsim-xmi:Name)" />
				
		<xsl:comment>
			Classes from package
			<xsl:value-of select="$package-name" />
		</xsl:comment>
		<xsl:element name="{$short-base-uri}:{$package-name}">
			<rdf:type> <owl:Class/> </rdf:type>
			<rdfs:label xml:lang="en">
				<xsl:value-of select="gsim-xmi:Name" />
			</rdfs:label>
			<rdfs:subClassOf>
				<xsl:element name="{$short-base-uri}:{$gsimObjectClass}">
				</xsl:element>
			</rdfs:subClassOf>
		</xsl:element>
		<xsl:apply-templates select="gsim-xmi:Classes/gsim-xmi:Class">
			<xsl:sort select="gsim-xmi:Name" />
			<xsl:with-param name="base-class" select="$package-name" />
		</xsl:apply-templates>
	</xsl:template>

	<xsl:template match="gsim-xmi:Class">
		<xsl:param name="base-class" />
		<xsl:element name="{$short-base-uri}:{xslGSIM:cleanClassesName(gsim-xmi:Name)}">
			<rdf:type> <owl:Class/> </rdf:type>
			<rdfs:label xml:lang="en">
				<xsl:value-of select="gsim-xmi:Name" />
			</rdfs:label>
			
			<rdfs:subClassOf> <xsl:element name="{$short-base-uri}:{$base-class}"/> </rdfs:subClassOf>
			<xsl:for-each select="gsim-xmi:Specializes">
			<!-- TODO: check subclasses -->
			<xsl:if test="count(xslGSIM:equal-classesName(@class)) &gt; 0">
				<rdfs:subClassOf> <xsl:element name="{xslGSIM:equal-classesName(@class)}"/>  </rdfs:subClassOf>
			</xsl:if>
			<!-- /TODO: check subclasses -->
			</xsl:for-each>
			<xsl:apply-templates select="gsim-xmi:Doc" />
			<!-- Other unused informations: 
			@id: entreprise architect id
			@abstract (true/false) indicates if the class is abstract
			xslGSIM:Generalizes/@class inverse of subClass Of 
			-->
		</xsl:element>
	</xsl:template>

	<xsl:function name="xslGSIM:cleanClassesName">
	<!-- First Character: [A-Za-z]  -->
	<!-- Other characters: [A-Za-z0-9\\-]* -->
	<xsl:param name="s" as="xs:string"/>
	<xsl:value-of select="translate($s,' ','-')"/>
	</xsl:function>

	<xsl:function name="xslGSIM:PropertyName">
		<xsl:param name="property" as="element()" />
		<xsl:value-of select="$property/gsim-xmi:Name" />
	</xsl:function>

	<xsl:function name="xslGSIM:ObjectPropertySourceName">
		<xsl:param name="property" as="element()" />
		<xsl:value-of
			select="if(string-length(string($property/gsim-xmi:Source))&gt;0) then xslGSIM:simplify-String-SourceDestination($property/gsim-xmi:Source) else ''" />
	</xsl:function>

	<xsl:function name="xslGSIM:ObjectPropertyDestinationName">
		<xsl:param name="property" as="element()" />
		<xsl:value-of
			select="if(string-length(string($property/gsim-xmi:Destination))&gt;0) then xslGSIM:simplify-String-SourceDestination($property/gsim-xmi:Destination) else ''" />
	</xsl:function>

	<xsl:function name="xslGSIM:PropertyNameSearchWithSourceDestination">
		<xsl:param name="property" as="element()" />
		<xsl:value-of
			select="concat(
	xslGSIM:PropertyNameSearch($property),
	xslGSIM:simplify-String(xslGSIM:ObjectPropertyDestinationName($property)),
	xslGSIM:simplify-String(xslGSIM:ObjectPropertySourceName($property))
	)" />
	</xsl:function>
<xsl:function name="xslGSIM:stripURI">
<xsl:param name="s" as="xs:string" />
<xsl:value-of select="substring($s,string-length($short-base-uri)+2)"/>
</xsl:function>
	<xsl:function name="xslGSIM:PropertyFinalName">
		<xsl:param name="property" as="element()" />
		<xsl:param name="cardinality-1" as="xs:integer" />
		<xsl:param name="cardinality-2" as="xs:integer" />
		<xsl:param name="cardinality-3" as="xs:integer" />
		<xsl:param name="cardinality-4" as="xs:integer" />
		
		<xsl:variable name="propertyName" select="xslGSIM:PropertyName($property)"/>
		<!-- Name of the property Domain: -->
		<xsl:if
			test="$cardinality-4 = 1 and $cardinality-3 != $cardinality-4">
			<xsl:value-of select="concat(xslGSIM:stripURI(xslGSIM:PropertyDomainName($property)),'-')" />
		</xsl:if>
		<!-- Initial Name of the property: -->
		<xsl:value-of select="translate(if(matches($propertyName,'^[/\\(].*')) then substring($propertyName,2) else $propertyName,' (/)','---')" />
		<xsl:if
			test="$cardinality-1 != $cardinality-4  and string-length(xslGSIM:ObjectPropertyDestinationName($property)) &gt; 0 ">
			<xsl:value-of
				select="concat('--',xslGSIM:ObjectPropertyDestinationName($property))" />
		</xsl:if>
		<xsl:if
			test="$cardinality-1 != $cardinality-4 and string-length(xslGSIM:ObjectPropertySourceName($property)) &gt; 0 ">
			<xsl:value-of select="concat('--',xslGSIM:ObjectPropertySourceName($property))" />
		</xsl:if>
		<!-- Name of the property Range: -->
		<xsl:if
			test="$cardinality-2 != $cardinality-4">
			<xsl:value-of select="concat('-',xslGSIM:stripURI(xslGSIM:PropertyRangeName($property)))" />
		</xsl:if>
	</xsl:function>


	<xsl:function name="xslGSIM:simplify-String">
		<xsl:param name="property" as="xs:string" />
		<xsl:value-of select="translate(lower-case($property),'(/)- ','')" />
	</xsl:function>


	<xsl:function name="xslGSIM:simplify-String-SourceDestination">
		<xsl:param name="property" as="element()" />
		<xsl:value-of
			select="
	if(string-length(string($property/gsim-xmi:Name))&gt;0 and not( contains(xslGSIM:PropertyNameSearch($property/..),xslGSIM:simplify-String($property/gsim-xmi:Name)) ) )
		then $property/gsim-xmi:Name 
		else ''
" />
	</xsl:function>

	<xsl:function name="xslGSIM:PropertyNameSearch">
		<xsl:param name="property" as="element()" />
		<xsl:value-of select="xslGSIM:simplify-String(xslGSIM:PropertyName($property))" />
	</xsl:function>

	<xsl:template name="xslGSIM:propertyType">
		<xsl:choose>
			<xsl:when
				test="count(xslGSIM:equal-range-classes(xslGSIM:PropertyRangeName(.)))&gt;0">
				<rdf:type> <owl:DataProperty/> </rdf:type>
			</xsl:when>
			<xsl:when
				test="count(xslGSIM:equal-classesName(xslGSIM:PropertyRangeName(.)))&gt;0">
				<rdf:type> <owl:ObjectProperty/> </rdf:type>
			</xsl:when>
			<xsl:otherwise>
				<xsl:message select="xslGSIM:PropertyRangeName(.)">
					Property Type problem
				</xsl:message>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:function name="xslGSIM:equal-classesName">
		<xsl:param name="initialValue" as="xs:string" />
		<xsl:copy-of select="$classesName/*[text() = xslGSIM:cleanClassesName($initialValue) or text() = concat($short-base-uri,':',xslGSIM:cleanClassesName($initialValue))]" />
	</xsl:function>

	<xsl:function name="xslGSIM:equal-range-classes">
		<xsl:param name="initialValue" as="xs:string" />
		<xsl:for-each select="$UML-classes-to-DataTypes/xslGSIM:range-class">
			<xsl:for-each select="xslGSIM:range-class-item">
				<xsl:if test="lower-case(./../@value)=lower-case($initialValue) or lower-case(.)=lower-case($initialValue)">
					<xslGSIM:item><xsl:value-of select="./../@value"/></xslGSIM:item>
				</xsl:if>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:function>

	<xsl:function name="xslGSIM:PropertyRangeName">
		<xsl:param name="property" as="element()" />
		<xsl:variable name="initialValue"
			select="concat($property/xslGSIM:Source/xslGSIM:LinkedClass,$property/xslGSIM:AttType)" />
		<xsl:choose>
			<xsl:when test="count(xslGSIM:equal-classesName($initialValue))&gt;0">
				<xsl:value-of select="string(xslGSIM:equal-classesName($initialValue))" />
			</xsl:when>
			<xsl:when test="count(xslGSIM:equal-range-classes($initialValue))=1">
				<xsl:value-of select="string(xslGSIM:equal-range-classes($initialValue))" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:message><xsl:value-of select="concat('!!Unknown range: ',$initialValue)" /></xsl:message>
				
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>

	<xsl:function name="xslGSIM:PropertyDomainName">
		<xsl:param name="property" as="element()" />
		<xsl:variable name="initialValue"
			select="concat($property/gsim-xmi:Destination/gsim-xmi:LinkedClass,$property/../gsim-xmi:Name)" />
		<xsl:choose>
			<xsl:when test="count( xslGSIM:equal-classesName($initialValue)) != 0">
				<xsl:value-of select="string(xslGSIM:equal-classesName($initialValue))" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:message
					select="concat('Unrecognized Domain: ',$initialValue,' for property: ', $property)">
					<xsl:copy-of select="$property"/>
					</xsl:message>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>

	<xsl:function name="xslGSIM:extractCardinality">
		<xsl:param name="property" />
		<xsl:value-of select="if(number($property)&gt;0) then $property else ''" />
	</xsl:function>

	<xsl:function name="xslGSIM:PropertyDomainMinCardinality">
		<xsl:param name="property" as="element()" />
		<xsl:value-of select="xslGSIM:extractCardinality(concat($property/xslGSIM:Min,$property/xslGSIM:Destination/xslGSIM:Min))" />
	</xsl:function>

	<xsl:function name="xslGSIM:PropertyDomainMaxCardinality">
		<xsl:param name="property" as="element()" />
		<xsl:value-of select="xslGSIM:extractCardinality(concat($property/xslGSIM:Max,$property/xslGSIM:Destination/xslGSIM:Max))" />
	</xsl:function>

	<xsl:function name="xslGSIM:PropertyRangeMinCardinality">
		<xsl:param name="property" as="element()" />
		<xsl:value-of select="xslGSIM:extractCardinality($property/xslGSIM:Source/xslGSIM:Min)" />
	</xsl:function>

	<xsl:function name="xslGSIM:PropertyRangeMaxCardinality">
		<xsl:param name="property" as="element()" />
		<xsl:value-of select="xslGSIM:extractCardinality($property/xslGSIM:Source/xslGSIM:Max)" />
	</xsl:function>

	<xsl:template match="gsim-xmi:Doc">
		<xsl:variable name="inputString" select="string(.)" />
		<!-- Quality check -->
		<xsl:if
			test="string-length(xslGSIM:tokenizeDoc2($inputString, $gsim-Doc-strings-to-be-matched/xslGSIM:item))&gt;0">
			<xsl:message
				select="concat('tokenization problem for attribute doc element:',$inputString)" />
		</xsl:if>
		<!-- End of quality check -->

		<xsl:for-each select="$gsim-Doc-strings-to-be-matched/xslGSIM:item">
			<xsl:variable name="result-tokenize"
				select="tokenize($inputString,concat('(^|\n)',string(./xslGSIM:match-string)) )" />
			<xsl:if test="count($result-tokenize)&gt;1">
				<!--xsl:element name="{string(./xslGSIM:XML-tag)}" namespace="{string(./xslGSIM:XML-URI)}"-->
				<xsl:element name="{string(./xslGSIM:XML-tag)}">
					<xsl:value-of
						select="replace(xslGSIM:tokenizeDoc2($result-tokenize[2], $gsim-Doc-strings-to-be-matched/xslGSIM:item),'(^\s+|\s+$)','')" />
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>

	<xsl:function name="xslGSIM:tokenizeDoc2">
		<xsl:param name="inputString" />
		<xsl:param name="strings-to-be-matched" />
		<xsl:variable name="result-match-string"
			select="concat('(^|\n)',string($strings-to-be-matched[1]/xslGSIM:match-string))" />
		<xsl:variable name="result"
			select="tokenize($inputString,$result-match-string)[1]" />
		<xsl:choose>
			<xsl:when test="count($strings-to-be-matched)=1">
				<xsl:value-of select="$result" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of
					select="xslGSIM:tokenizeDoc2($result, $strings-to-be-matched[position()&gt;1])" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>


	<xsl:template name="xslGSIM:cardinalityRestriction">
		<xsl:param name="restricted-class" as="xs:string" />
		<xsl:param name="minCardinality" as="xs:string" />
		<xsl:param name="maxCardinality" as="xs:string" />

		<xsl:choose>
			<xsl:when test="$minCardinality = '' and $maxCardinality = '' ">
				<xsl:element name="{$restricted-class}"> </xsl:element>
			</xsl:when>
			<xsl:otherwise>
				<owl:Restriction>
				<owl:onClass> <xsl:element name="{$restricted-class}"/> </owl:onClass>
				<xsl:choose>
						<xsl:when test="$minCardinality != '' and $maxCardinality = '' ">
							<owl:minQualifiedCardinality
								rdf:datatype="xsd:nonNegativeInteger">
								<xsl:value-of select="$minCardinality" />
							</owl:minQualifiedCardinality>
						</xsl:when>

						<xsl:when test="$minCardinality = '' and $maxCardinality != '' ">
							<owl:maxQualifiedCardinality
								rdf:datatype="xsd:nonNegativeInteger">
								<xsl:value-of select="$maxCardinality" />
							</owl:maxQualifiedCardinality>
						</xsl:when>

						<xsl:when
							test="$minCardinality != '' and $maxCardinality != '' and $minCardinality = $maxCardinality ">
							<owl:qualifiedCardinality rdf:datatype="xsd:nonNegativeInteger">
								<xsl:value-of select="$minCardinality" />
							</owl:qualifiedCardinality>
						</xsl:when>

						<xsl:when
							test="$minCardinality != '' and $maxCardinality != '' and $minCardinality != $maxCardinality ">
							<owl:minQualifiedCardinality
								rdf:datatype="xsd:nonNegativeInteger">
								<xsl:value-of select="$minCardinality" />
							</owl:minQualifiedCardinality>
							<owl:maxQualifiedCardinality
								rdf:datatype="xsd:nonNegativeInteger">
								<xsl:value-of select="$maxCardinality" />
							</owl:maxQualifiedCardinality>
						</xsl:when>
						<xsl:otherwise>
							<xsl:message select="'Restriction Error!'" />
						</xsl:otherwise>
					</xsl:choose>
				</owl:Restriction>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>