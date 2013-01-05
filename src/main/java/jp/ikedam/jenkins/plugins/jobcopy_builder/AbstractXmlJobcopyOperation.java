/*
 * The MIT License
 * 
 * Copyright (c) 2012-2013 IKEDA Yasuyuki
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jp.ikedam.jenkins.plugins.jobcopy_builder;

import hudson.EnvVars;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Abstract class for job copy operation using XML DOM.
 * @see jp.ikedam.jenkins.plugins.jobcopy_builder.JobcopyOperation
 */
public abstract class AbstractXmlJobcopyOperation extends JobcopyOperation
{
    /**
     * Performs modifications to job configure XML Document.
     * 
     * @param doc       XML Document of the job to be copied (job/NAME/config.xml)
     * @param env       Variables defined in the build.
     * @param logger    The output stream to log.
     * @return          modified XML Document. Return null if an error occurs.
     */
    public abstract Document perform(Document doc, EnvVars env, PrintStream logger);
    
    /**
     * Returns modified XML string of the job configuration.
     * 
     * @param xmlString the XML string  of the job to be copied (job/NAME/config.xml)
     * @param encoding  the encoding of the XML.
     * @param env       Variables defined in the build.
     * @param logger    The output stream to log.
     * @return          modified XML string. Returns null if an error occurs.
     * @see jp.ikedam.jenkins.plugins.jobcopy_builder.JobcopyOperation#perform(java.lang.String, java.lang.String, hudson.EnvVars, java.io.PrintStream)
     */
    @Override
    public String perform(String xmlString, String encoding, EnvVars env, PrintStream logger)
    {
        Document doc;
        try
        {
            doc = getXmlDocumentFromString(xmlString, encoding);
        }
        catch (Exception e)
        {
            logger.print("Error occured in XML operation");
            e.printStackTrace(logger);
            return null;
        }
        
        Document newDoc = perform(doc, env, logger);
        if(newDoc == null)
        {
            // It seems that an error occurred in XML processing.
            return null;
        }
        
        try
        {
            return getXmlString(newDoc);
        }
        catch (Exception e)
        {
            logger.print("Error occured in XML operation");
            e.printStackTrace(logger);
            return null;
        }
    }
    
    /**
     * Retrieve the XML string from XML Document object
     * 
     * @param doc   the XML Document object.
     * @return      the XML string
     * @throws TransformerException
     */
    private String getXmlString(Document doc)
        throws TransformerException
    {
        TransformerFactory tfactory = TransformerFactory.newInstance(); 
        Transformer transformer = tfactory.newTransformer(); 
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw)); 
        
        return sw.toString();
    }
    
    /**
     * Construct a XML Document object from a XML string.
     * 
     * @param xmlString     a XML string.
     * @param encoding      encoding of xmlString.
     * @return              Constructed XML Document object.
     * @throws ParserConfigurationException
     * @throws UnsupportedEncodingException
     * @throws SAXException
     * @throws IOException
     */
    private Document getXmlDocumentFromString(String xmlString, String encoding)
        throws ParserConfigurationException,UnsupportedEncodingException,SAXException,IOException
    {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        //domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        InputStream is = new ByteArrayInputStream(xmlString.getBytes(encoding)); 
        
        return builder.parse(is);
    }
    
    /****** Utility methods working with XML. Usable from subclasses. ******/
    
    /**
     * Retrieve a XML node using XPath.
     * 
     * Returns null in following cases:
     * <ul>
     *      <li>No node found.</li>
     *      <li>More than one node found.</li>
     * </ul>
     * 
     * @param doc       the XML Document object.
     * @param xpath     a XPath specifying the retrieving node.
     * @return          the retrieved node.
     * @throws XPathExpressionException
     */
    protected Node getNode(Document doc, String xpath)
        throws XPathExpressionException
    {
        NodeList nodeList = getNodeList(doc, xpath);
        
        if(nodeList.getLength() != 1)
        {
            return null;
        }
        
        return nodeList.item(0);
    }

    /**
     * Retrieve a XML node list using XPath.
     * 
     * @param doc               the XML Document object.
     * @param xpathExpression   a XPath specifying the retrieving nodes.
     * @return                  retrieved nodes in NodeList
     * @throws XPathExpressionException
     */
    protected NodeList getNodeList(Document doc, String xpathExpression)
        throws XPathExpressionException
    {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expr = xpath.compile(xpathExpression);
        
        return (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
    }
    
    /**
     * Retrieve a XPath expression of a node.
     * 
     * Use only for displaying purposes only.
     * For this works not so strict, 
     * the return value supposes not to work proper
     * with XPath processors.
     * 
     * @param targetNode  a node whose XPath expression is retrieved.
     * @return            XPath expression.
     */
    protected String getXpath(Node targetNode)
    {
        StringBuilder pathBuilder = new StringBuilder();
        for(Node node = targetNode; node != null; node = node.getParentNode())
        {
            pathBuilder.insert(0, node.getNodeName());
            pathBuilder.insert(0, '/');
        }
        return pathBuilder.toString();
    }
    
}