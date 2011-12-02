<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	version="2.0">

	<xsl:output method="html" indent="yes" encoding="UTF-8" />
	<xsl:strip-space elements="*" />
	<xsl:param name="track" />

	<xsl:template match="/">
		<xsl:variable name="title">
			<xsl:text>JavaDoc documentation for </xsl:text>
			<xsl:value-of select="$track" />
		</xsl:variable>
		<xsl:element name="html">
			<xsl:element name="head">
				<xsl:element name="title">
					<xsl:value-of select="$title" />
				</xsl:element>
				<meta http-equiv="pragma" content="no-cache" />
				<meta http-equiv="cache-control" content="no-cache" />
				<meta http-equiv="expires" content="0" />
				<meta http-equiv="description" content="$title" />
				<meta http-equiv="Content-Type" content="text/html;charset=UTF-8" />
				<xsl:element name="link">
					<xsl:attribute name="rel">stylesheet</xsl:attribute>
					<xsl:attribute name="type">text/css</xsl:attribute>
					<xsl:attribute name="href">style.css</xsl:attribute>
				</xsl:element>
			</xsl:element>
			<xsl:element name="body">
				<h1>
					<xsl:value-of select="$title" />
				</h1>
				<ul>
					<xsl:for-each select="compartments/compartment">
						<xsl:element name="li">
							<xsl:element name="a">
								<xsl:attribute name="href"><xsl:value-of
									select="concat('#', @name)" /></xsl:attribute>
								<xsl:value-of select="@name" />
							</xsl:element>
						</xsl:element>
					</xsl:for-each>
				</ul>
				<dl>
					<xsl:apply-templates />
				</dl>
			</xsl:element>
		</xsl:element>
	</xsl:template>

	<xsl:template match="compartment">
		<h2>
			<xsl:value-of select="@name" />
		</h2>
		<xsl:element name="a">
			<xsl:attribute name="name"><xsl:value-of select="@name" /></xsl:attribute>
		</xsl:element>
		<xsl:element name="table">
			<xsl:apply-templates />
		</xsl:element>
	</xsl:template>
	<xsl:template match="dc">
		<xsl:element name="tr">
			<xsl:element name="td">
				<xsl:element name="a">
					<xsl:attribute name="href"><xsl:value-of
						select="concat(@folder, '/index.html')" /></xsl:attribute>
					<xsl:value-of select="concat(@vendor, '~', @name)" />
				</xsl:element>
			</xsl:element>
			<xsl:element name="td">
				<xsl:value-of select="text()" />
			</xsl:element>
		</xsl:element>
	</xsl:template>
</xsl:stylesheet>