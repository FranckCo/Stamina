<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xhtml="http://www.w3.org/1999/xhtml"
	xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:gsim="http://unece.org/gsim/0.8"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
	xmlns:owl="http://www.w3.org/2002/07/owl" xmlns="http://www.w3.org/1999/xhtml"
	exclude-result-prefixes="xs">

	<!-- TODO change version in GSIM namespace -->
	<!-- TODO Check OWL1 or OWL2 -->
	<xsl:output method="xml" encoding="utf-8" indent="yes" />
	<xsl:variable name="base-uri">http://stamina-project.org/models/gsim/</xsl:variable>
	<xsl:variable name="short-base-uri">gsim</xsl:variable>

	<xsl:variable name="classesName">
	<!-- TODO : clean classes Names : suppress spaces or other characters-->
		<xsl:for-each
			select="/gsim:Model/gsim:Package/gsim:Classes/gsim:Class/gsim:Name">
			<gsim:item>
				<xsl:value-of select="concat($short-base-uri,':',string(.))" />
			</gsim:item>
		</xsl:for-each>
	</xsl:variable>
	<!-- Strings to be matched for Doc tokenization -->
	<xsl:variable name="gsim-Doc-strings-to-be-matched">
		<gsim:item>
			<gsim:match-string>Definition:</gsim:match-string>
			<gsim:XML-tag>gsim:classDefinition</gsim:XML-tag>
		</gsim:item>
		<gsim:item>
			<gsim:match-string>Explanatory Text:</gsim:match-string>
			<gsim:XML-tag>gsim:classExplanatoryText</gsim:XML-tag>
		</gsim:item>
		<gsim:item>
			<gsim:match-string>Synonyms:</gsim:match-string>
			<gsim:XML-tag>gsim:classSynonyms</gsim:XML-tag>
		</gsim:item>
	</xsl:variable>

	<xsl:variable name="UML-classes-to-DataTypes">
		<gsim:range-class value='xs:string'>
			<gsim:range-class-item>xs:string</gsim:range-class-item>
			<gsim:range-class-item>string</gsim:range-class-item>
			<gsim:range-class-item />
		</gsim:range-class>
		<gsim:range-class value='xs:date'>
			<gsim:range-class-item>date</gsim:range-class-item>
		</gsim:range-class>
		<gsim:range-class value='xs:boolean'>
			<gsim:range-class-item>boolean</gsim:range-class-item>
			<gsim:range-class-item>binary</gsim:range-class-item>
		</gsim:range-class>
		<gsim:range-class value='xs:integer'>
			<gsim:range-class-item>int</gsim:range-class-item>
		</gsim:range-class>
		<gsim:range-class value='xs:decimal'>
			<gsim:range-class-item>numeric</gsim:range-class-item>
			<gsim:range-class-item>number</gsim:range-class-item>
		</gsim:range-class>
		<gsim:range-class value='xs:string'>
			<gsim:range-class-item>controlledVocabulary</gsim:range-class-item>
			<gsim:range-class-item>extensibleRedefinedlist</gsim:range-class-item>
			<gsim:range-class-item>Id</gsim:range-class-item>
			<gsim:range-class-item>identifier</gsim:range-class-item>
			<gsim:range-class-item>Link</gsim:range-class-item>
			<gsim:range-class-item>dateRange</gsim:range-class-item>
			<gsim:range-class-item>met/unmet</gsim:range-class-item>
			<gsim:range-class-item>entityDesignator</gsim:range-class-item>
			<gsim:range-class-item>versionDesignator</gsim:range-class-item>
		</gsim:range-class>
	</xsl:variable>

	<xsl:template match="/">
		<rdf:RDF>
			<xsl:apply-templates select="gsim:Model" />
		</rdf:RDF>
	</xsl:template>

	<xsl:template match="gsim:Model">
		<xsl:apply-templates select="gsim:Package">
			<xsl:sort select="gsim:Name" />
		</xsl:apply-templates>
		<xsl:call-template name="properties" />
	</xsl:template>

	<xsl:template name="properties">
		<xsl:variable name="attributes"
			select="gsim:Package/gsim:Classes/gsim:Class/gsim:Attribute" />
		<xsl:variable name="associations"
			select="gsim:Package/gsim:Associations/gsim:Association" />
		<xsl:variable name="proto-properties" select="$associations, $attributes" />
		<xsl:for-each-group select="$proto-properties"
			group-by="gsim:PropertyNameSearch(.)">
			<!-- xsl:message>Search key: <xsl:value-of select="current-grouping-key()"/>, 
				corresponding values: <xsl:value-of select="current-group()/gsim:Name" separator=", 
				"/></xsl:message -->
			<xsl:variable name="cardinality1" select="count(current-group())" />
			<xsl:for-each-group select="current-group()"
				group-by="gsim:PropertyNameSearchWithSourceDestination(.)">
				<!-- Source and Destination Name -->
				<xsl:variable name="cardinality2" select="count(current-group())" />
				<xsl:for-each-group select="current-group()"
					group-by="gsim:PropertyRangeName(.)">
					<!-- xsl:message>Range key: <xsl:value-of select="current-grouping-key()"/> 
						</xsl:message -->
					
					<xsl:variable name="cardinality3" select="count(current-group())" />
					<xsl:for-each-group select="current-group()"
						group-by="concat(gsim:PropertyRangeMinCardinality(.),'..',gsim:PropertyRangeMaxCardinality(.))">
						<!-- It works because we check that a couple property-domain appear only once. -->
						<xsl:variable name="cardinality4" select="count(current-group())" />
						<xsl:if test="$cardinality4!=1 and $cardinality4 != $cardinality3  and $cardinality4 != $cardinality2 and $cardinality4 != $cardinality1">
							<xsl:message select="concat($cardinality1,'-',$cardinality2,'-',$cardinality3,'-',$cardinality4)">Problem of programming</xsl:message>
						</xsl:if>
						
						<rdf:Description rdf:about="{$short-base-uri}:{gsim:PropertyFinalName(.,$cardinality1, $cardinality2, $cardinality3, $cardinality4)}">
							<xsl:call-template name="gsim:propertyType" />
							<rdfs:label xml:lang="en">
								<xsl:value-of select="gsim:PropertyName(.)" />
							</rdfs:label>
							
							<xsl:call-template name="property-Domain">
								<xsl:with-param name="Domains">
								<xsl:for-each select="current-group()">
							<gsim:item name="{gsim:PropertyDomainName(.)}" min="{gsim:PropertyDomainMinCardinality(.)}" max="{gsim:PropertyDomainMinCardinality(.)}"/>
							</xsl:for-each>
								</xsl:with-param>
							</xsl:call-template>
							<xsl:call-template name="property-range"/>
						</rdf:Description>
						
					</xsl:for-each-group>
				</xsl:for-each-group>
			</xsl:for-each-group>
		</xsl:for-each-group>
	</xsl:template>
	
	<xsl:template name="property-Domain">
		<xsl:param name="Domains" />
		<rdfs:domain>
				<xsl:choose>
				<xsl:when test="count($Domains/gsim:item)=1">
					<xsl:copy-of select="gsim:propertyDomains($Domains)"/>
				</xsl:when>
				<xsl:when test="count($Domains/gsim:item)&gt;1">
					<owl:Class>
						<owl:unionOf rdf:parseType="Collection">
							<xsl:copy-of select="gsim:propertyDomains($Domains)"/>
						</owl:unionOf>
					</owl:Class>
				</xsl:when>
				<xsl:otherwise>
				<xsl:message>Problem: no domain</xsl:message>
				</xsl:otherwise>
				</xsl:choose>
			</rdfs:domain>
		</xsl:template>
		
<xsl:function name="gsim:propertyDomains">
	<xsl:param name="Domains" />
	<xsl:for-each select="$Domains/gsim:item">
		<xsl:call-template name="gsim:cardinalityRestriction">
				<xsl:with-param name="restricted-class" select="./@name" />
				<xsl:with-param name="minCardinality" select="./@min" />
				<xsl:with-param name="maxCardinality" select="./@max" />
		</xsl:call-template>
	</xsl:for-each>
</xsl:function>
	<xsl:template name="property-range">
		<rdfs:range>
			<xsl:call-template name="gsim:cardinalityRestriction">
				<xsl:with-param name="restricted-class" select="gsim:PropertyRangeName(.)" />
				<xsl:with-param name="minCardinality"
					select="gsim:PropertyRangeMinCardinality(.)" />
				<xsl:with-param name="maxCardinality"
					select="gsim:PropertyRangeMaxCardinality(.)" />
			</xsl:call-template>
		</rdfs:range>
	</xsl:template>

	<xsl:template match="gsim:Package">
		<xsl:variable name="package-name" select="gsim:Name" />
		<xsl:comment>
			Classes from package
			<xsl:value-of select="$package-name" />
		</xsl:comment>
		<rdf:Description rdf:about="{$short-base-uri}:{$package-name}">
			<rdf:type rdf:resource="owl:Class" />
			<rdfs:label xml:lang="en">
				<xsl:value-of select="gsim:Name" />
			</rdfs:label>
		</rdf:Description>
		<xsl:apply-templates select="gsim:Classes/gsim:Class">
			<xsl:sort select="gsim:Name" />
			<xsl:with-param name="base-class" select="$package-name" />
		</xsl:apply-templates>
	</xsl:template>

	<xsl:template match="gsim:Class">
		<xsl:param name="base-class" />
		<rdf:Description rdf:about="{$short-base-uri}:{gsim:Name}">
			<rdf:type rdf:resource="owl:Class" />
			<rdfs:label xml:lang="en">
				<xsl:value-of select="gsim:Name" />
			</rdfs:label>
			<rdfs:subClassOf rdf:resource="{$base-uri}{$base-class}" />
			<xsl:for-each select="gsim:Specializes">
			<!-- TODO : check subclasses -->
				<rdfs:subClassOf rdf:resource="{$short-base-uri}{@class}" />
			<!-- /TODO : check subclasses -->
			</xsl:for-each>
			<xsl:apply-templates select="gsim:Doc" />
			<!-- Other unused informations : 
			@id : entreprise architect id
			@abstract (true/false) indicates if the class is abstract
			gsim:Generalizes/@class inverse of subClass Of 
			-->
		</rdf:Description>
	</xsl:template>

	<xsl:function name="gsim:PropertyName">
		<xsl:param name="property" as="element()" />
		<xsl:value-of select="$property/gsim:Name" />
	</xsl:function>

	<xsl:function name="gsim:ObjectPropertySourceName">
		<xsl:param name="property" as="element()" />
		<xsl:value-of
			select="if(string-length(string($property/gsim:Source))&gt;0) then gsim:simplify-String-SourceDestination($property/gsim:Source) else ''" />
	</xsl:function>

	<xsl:function name="gsim:ObjectPropertyDestinationName">
		<xsl:param name="property" as="element()" />
		<xsl:value-of
			select="if(string-length(string($property/gsim:Destination))&gt;0) then gsim:simplify-String-SourceDestination($property/gsim:Destination) else ''" />
	</xsl:function>

	<xsl:function name="gsim:PropertyNameSearchWithSourceDestination">
		<xsl:param name="property" as="element()" />
		<xsl:value-of
			select="concat(
	gsim:PropertyNameSearch($property),
	gsim:simplify-String(gsim:ObjectPropertyDestinationName($property)),
	gsim:simplify-String(gsim:ObjectPropertySourceName($property))
	)" />
	</xsl:function>
<xsl:function name="gsim:stripURI">
<xsl:param name="s" as="xs:string" />
<xsl:value-of select="substring($s,string-length($short-base-uri)+2)"/>
</xsl:function>
	<xsl:function name="gsim:PropertyFinalName">
		<xsl:param name="property" as="element()" />
		<xsl:param name="cardinality-1" as="xs:integer" />
		<xsl:param name="cardinality-2" as="xs:integer" />
		<xsl:param name="cardinality-3" as="xs:integer" />
		<xsl:param name="cardinality-4" as="xs:integer" />
		
		<xsl:variable name="propertyName" select="gsim:PropertyName($property)"/>
		<!-- Name of the property Domain: -->
		<xsl:if
			test="$cardinality-4 = 1 and $cardinality-3 != $cardinality-4">
			<xsl:value-of select="concat(gsim:stripURI(gsim:PropertyDomainName($property)),'-')" />
		</xsl:if>
		<!-- Initial Name of the property : -->
		<xsl:value-of select="translate(if(matches($propertyName,'^[/\\(].*')) then substring($propertyName,2) else $propertyName,' (/)','---')" />
		<xsl:if
			test="$cardinality-1 != $cardinality-4  and string-length(gsim:ObjectPropertyDestinationName($property)) &gt; 0 ">
			<xsl:value-of
				select="concat('+',gsim:ObjectPropertyDestinationName($property))" />
		</xsl:if>
		<xsl:if
			test="$cardinality-1 != $cardinality-4 and string-length(gsim:ObjectPropertySourceName($property)) &gt; 0 ">
			<xsl:value-of select="concat('+',gsim:ObjectPropertySourceName($property))" />
		</xsl:if>
		<!-- Name of the property Range : -->
		<xsl:if
			test="$cardinality-2 != $cardinality-4">
			<xsl:value-of select="concat('-',gsim:stripURI(gsim:PropertyRangeName($property)))" />
		</xsl:if>
	</xsl:function>


	<xsl:function name="gsim:simplify-String">
		<xsl:param name="property" as="xs:string" />
		<xsl:value-of select="translate(lower-case($property),'(/)- ','')" />
	</xsl:function>


	<xsl:function name="gsim:simplify-String-SourceDestination">
		<xsl:param name="property" as="element()" />
		<xsl:value-of
			select="
	if(string-length(string($property/gsim:Name))&gt;0 and not( contains(gsim:PropertyNameSearch($property/..),gsim:simplify-String($property/gsim:Name)) ) )
		then $property/gsim:Name 
		else ''
" />
	</xsl:function>

	<xsl:function name="gsim:PropertyNameSearch">
		<xsl:param name="property" as="element()" />
		<xsl:value-of select="gsim:simplify-String(gsim:PropertyName($property))" />
	</xsl:function>

	<xsl:template name="gsim:propertyType">
		<xsl:choose>
			<xsl:when
				test="count(gsim:equal-range-classes(gsim:PropertyRangeName(.)))&gt;0">
				<rdf:type rdf:resource="owl:DataProperty" />
			</xsl:when>
			<xsl:when
				test="count(gsim:equal-classesName(gsim:PropertyRangeName(.)))&gt;0">
				<rdf:type rdf:resource="owl:ObjectProperty" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:message select="gsim:PropertyRangeName(.)">
					Property Type problem
				</xsl:message>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:function name="gsim:equal-classesName">
		<xsl:param name="initialValue" as="xs:string" />
		<xsl:copy-of select="$classesName/*[text() = $initialValue or text() = concat($short-base-uri,':',$initialValue)]" />
	</xsl:function>

	<xsl:function name="gsim:equal-range-classes">
		<xsl:param name="initialValue" as="xs:string" />
		<xsl:for-each select="$UML-classes-to-DataTypes/gsim:range-class">
			<xsl:for-each select="gsim:range-class-item">
				<xsl:if test="lower-case(./../@value)=lower-case($initialValue) or lower-case(.)=lower-case($initialValue)">
					<gsim:item><xsl:value-of select="./../@value"/></gsim:item>
				</xsl:if>
			</xsl:for-each>
		</xsl:for-each>
	</xsl:function>

	<xsl:function name="gsim:PropertyRangeName">
		<xsl:param name="property" as="element()" />
		<xsl:variable name="initialValue"
			select="concat($property/gsim:Source/gsim:LinkedClass,$property/gsim:AttType)" />
		<xsl:choose>
			<xsl:when test="count(gsim:equal-classesName($initialValue))&gt;0">
				<xsl:value-of select="string(gsim:equal-classesName($initialValue))" />
			</xsl:when>
			<xsl:when test="count(gsim:equal-range-classes($initialValue))=1">
				<xsl:value-of select="string(gsim:equal-range-classes($initialValue))" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:message><xsl:value-of select="concat('!!Unknown range : ',$initialValue)" /></xsl:message>
				
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>

	<xsl:function name="gsim:PropertyDomainName">
		<xsl:param name="property" as="element()" />
		<xsl:variable name="initialValue"
			select="concat($property/gsim:Destination/gsim:LinkedClass,$property/../gsim:Name)" />
		<xsl:choose>
			<xsl:when test="count( gsim:equal-classesName($initialValue)) != 0">
				<xsl:value-of select="string(gsim:equal-classesName($initialValue))" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:message
					select="concat('Unrecognized Domain : ',$initialValue,' for property : ', $property)">
					<xsl:copy-of select="$property"/>
					</xsl:message>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>

	<xsl:function name="gsim:extractCardinality">
		<xsl:param name="property" />
		<xsl:value-of select="if(number($property)&gt;0) then $property else ''" />
	</xsl:function>

	<xsl:function name="gsim:PropertyDomainMinCardinality">
		<xsl:param name="property" as="element()" />
		<xsl:value-of select="gsim:extractCardinality(concat($property/gsim:Min,$property/gsim:Destination/gsim:Min))" />
	</xsl:function>

	<xsl:function name="gsim:PropertyDomainMaxCardinality">
		<xsl:param name="property" as="element()" />
		<xsl:value-of select="gsim:extractCardinality(concat($property/gsim:Max,$property/gsim:Destination/gsim:Max))" />
	</xsl:function>

	<xsl:function name="gsim:PropertyRangeMinCardinality">
		<xsl:param name="property" as="element()" />
		<xsl:value-of select="gsim:extractCardinality($property/gsim:Source/gsim:Min)" />
	</xsl:function>

	<xsl:function name="gsim:PropertyRangeMaxCardinality">
		<xsl:param name="property" as="element()" />
		<xsl:value-of select="gsim:extractCardinality($property/gsim:Source/gsim:Max)" />
	</xsl:function>

	<xsl:template match="gsim:Doc">
		<xsl:variable name="inputString" select="string(.)" />
		<!-- Quality check -->
		<xsl:if
			test="string-length(gsim:tokenizeDoc2($inputString, $gsim-Doc-strings-to-be-matched/gsim:item))&gt;0">
			<xsl:message
				select="concat('tokenization problem for attribute doc element :',$inputString)" />
		</xsl:if>
		<!-- End of quality check -->

		<xsl:for-each select="$gsim-Doc-strings-to-be-matched/gsim:item">
			<xsl:variable name="result-tokenize"
				select="tokenize($inputString,concat('(^|\n)',string(./gsim:match-string)) )" />
			<xsl:if test="count($result-tokenize)&gt;1">
				<!--xsl:element name="{string(./gsim:XML-tag)}" namespace="{string(./gsim:XML-URI)}"-->
				<xsl:element name="{string(./gsim:XML-tag)}">
					<xsl:value-of
						select="replace(gsim:tokenizeDoc2($result-tokenize[2], $gsim-Doc-strings-to-be-matched/gsim:item),'(^\s+|\s+$)','')" />
				</xsl:element>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>

	<xsl:function name="gsim:tokenizeDoc2">
		<xsl:param name="inputString" />
		<xsl:param name="strings-to-be-matched" />
		<xsl:variable name="result-match-string"
			select="concat('(^|\n)',string($strings-to-be-matched[1]/gsim:match-string))" />
		<xsl:variable name="result"
			select="tokenize($inputString,$result-match-string)[1]" />
		<xsl:choose>
			<xsl:when test="count($strings-to-be-matched)=1">
				<xsl:value-of select="$result" />
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of
					select="gsim:tokenizeDoc2($result, $strings-to-be-matched[position()&gt;1])" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:function>


	<xsl:template name="gsim:cardinalityRestriction">
		<xsl:param name="restricted-class" as="xs:string" />
		<xsl:param name="minCardinality" as="xs:string" />
		<xsl:param name="maxCardinality" as="xs:string" />

		<xsl:choose>
			<xsl:when test="$minCardinality = '' and $maxCardinality = '' ">
				<owl:Class rdf:about="{$restricted-class}"/>
			</xsl:when>
			<xsl:otherwise>
				<owl:Restriction>
				<owl:onClass rdf:resource="{$restricted-class}" />
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
							<xsl:message select="'Restriction Error !'" />
						</xsl:otherwise>
					</xsl:choose>
				</owl:Restriction>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>