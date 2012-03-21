<?xml version="1.0" encoding="UTF-8"?>

<xsl:stylesheet version="2.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:qti="http://www.imsglobal.org/xsd/imsqti_v2p1"
  xmlns:jqti="http://jqti.qtitools.org"
  xmlns="http://www.w3.org/1999/xhtml"
  exclude-result-prefixes="qti jqti xs">

  <xsl:template match="qti:matchInteraction">
    <input name="jqtipresented_{@responseIdentifier}" type="hidden" value="1"/>
    <xsl:variable name="orderedSet1" select="jqti:get-visible-ordered-choices(., qti:simpleMatchSet[1]/qti:simpleAssociableChoice)" as="element(qti:simpleAssociableChoice)*"/>
    <xsl:variable name="orderedSet2" select="jqti:get-visible-ordered-choices(., qti:simpleMatchSet[2]/qti:simpleAssociableChoice)" as="element(qti:simpleAssociableChoice)*"/>
    <div class="{local-name()}">
      <xsl:variable name="responseIdentifier" select="@responseIdentifier" as="xs:string"/>
      <xsl:if test="jqti:is-invalid-response(@responseIdentifier)">
        <xsl:call-template name="jqti:generic-bad-response-message"/>
      </xsl:if>

      <table>
        <tr>
          <th class="prompt">
            <xsl:apply-templates select="qti:prompt"/>
          </th>
          <xsl:for-each select="$orderedSet1">
            <th>
              <xsl:apply-templates/>
            </th>
          </xsl:for-each>
        </tr>
        <xsl:for-each select="$orderedSet2">
          <xsl:variable name="choiceIdentifier" select="@identifier" as="xs:string"/>
          <tr>
            <th>
              <xsl:apply-templates/>
            </th>
            <xsl:for-each select="$orderedSet1">
              <td align="center">
                <xsl:variable name="responseValue" select="concat(@identifier, ' ', $choiceIdentifier)" as="xs:string"/>
                <input type="checkbox" name="jqtiresponse_{$responseIdentifier}" value="{$responseValue}">
                  <xsl:if test="jqti:value-contains(jqti:get-response-value($responseIdentifier), $responseValue)">
                    <xsl:attribute name="checked" select="'checked'"/>
                  </xsl:if>
                </input>
              </td>
            </xsl:for-each>
          </tr>
        </xsl:for-each>
      </table>
      <script type='text/javascript'>
        JQTIItemRendering.registerMatchInteraction('<xsl:value-of select="@responseIdentifier"/>',
          <xsl:value-of select="@maxAssociations"/>,
          {<xsl:for-each select="$orderedSet1">
            <xsl:if test="position() > 1">,</xsl:if>
            <xsl:value-of select="@identifier"/>:<xsl:value-of select="@matchMax"/>
          </xsl:for-each>},
          {<xsl:for-each select="$orderedSet2">
            <xsl:if test="position() > 1">,</xsl:if>
            <xsl:value-of select="@identifier"/>:<xsl:value-of select="@matchMax"/>
          </xsl:for-each>});
      </script>
    </div>
  </xsl:template>
</xsl:stylesheet>