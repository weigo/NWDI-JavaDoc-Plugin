<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

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
        <meta http-equiv="description" content="$title" />
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width,initial-scale=1.0" />
        <xsl:element name="link">
          <xsl:attribute name="rel">stylesheet</xsl:attribute>
          <xsl:attribute name="href">style.css</xsl:attribute>
        </xsl:element>
        <xsl:text disable-output-escaping="yes">
<![CDATA[<!--[if lt IE 9]>]]>
</xsl:text>
        <script src="html5shiv.js"></script>
        <xsl:text disable-output-escaping="yes">
<![CDATA[<![endif] -->]]>
</xsl:text>
      </xsl:element>
      <xsl:element name="body">
        <header>
          <h1>
            <xsl:value-of select="$title" />
          </h1>
        </header>
        <div id="container">
          <aside>
            <nav>
              <ul>
                <xsl:for-each select="compartments/compartment">
                  <xsl:sort select="@vendor" />
                  <xsl:sort select="@name" />
                  <xsl:element name="li">
                    <xsl:element name="a">
                      <xsl:attribute name="href"><xsl:value-of select="concat('#', @vendor, '_', @name)" /></xsl:attribute>
                      <xsl:value-of select="concat(@vendor, '_', @name)" />
                    </xsl:element>
                  </xsl:element>
                </xsl:for-each>
              </ul>
            </nav>
          </aside>
          <div id="content">
            <xsl:apply-templates />
          </div>
        </div>
      </xsl:element>
    </xsl:element>
  </xsl:template>
  <xsl:template match="compartment">
    <article>
      <header>
        <xsl:element name="a">
          <xsl:attribute name="name"><xsl:value-of select="concat(@vendor, '_', @name)" /></xsl:attribute>
        </xsl:element>
        <h2>
          <xsl:value-of select="concat(@vendor, ' ', @name)" />
        </h2>
        <p>
          <xsl:apply-templates select="description" />
        </p>
      </header>
      <section>
        <xsl:element name="dl">
          <xsl:apply-templates select="dc" />
        </xsl:element>
      </section>
    </article>
  </xsl:template>
  <xsl:template match="dc">
    <xsl:element name="dt">
      <xsl:element name="a">
        <xsl:attribute name="href"><xsl:value-of select="concat(@folder, '/index.html')" /></xsl:attribute>
        <xsl:value-of select="concat(@vendor, '~', @name)" />
      </xsl:element>
    </xsl:element>
    <xsl:element name="dd">
      <xsl:value-of select="text()" />
    </xsl:element>
  </xsl:template>
</xsl:stylesheet>