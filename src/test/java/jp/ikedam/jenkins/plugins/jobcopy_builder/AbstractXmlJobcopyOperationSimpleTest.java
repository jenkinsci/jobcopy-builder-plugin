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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import junit.framework.TestCase;

/**
 * Tests for AbstractXmlJobcopyOperation, not concerning with Jenkins.
 *
 */
public class AbstractXmlJobcopyOperationSimpleTest extends TestCase
{
    private class XmlJobcopyOperationImpl extends AbstractXmlJobcopyOperation
    {
        public Document passedDocument;
        @SuppressWarnings("unused")
        public EnvVars passedEnv;
        @SuppressWarnings("unused")
        public PrintStream passedLogger;
        public Document documentToReturn;
        
        @Override
        public Document perform(Document doc, EnvVars env, PrintStream logger)
        {
            passedDocument = doc;
            passedEnv = env;
            passedLogger = logger;
            
            return documentToReturn;
        }
    }
    
    private XmlJobcopyOperationImpl target;
    private DocumentBuilder builder;
    private DOMImplementation domImpl;
    private Document emptyDoc;
    
    @Override
    public void setUp() throws ParserConfigurationException
    {
         target = new XmlJobcopyOperationImpl();
         builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         domImpl = builder.getDOMImplementation();
         emptyDoc = domImpl.createDocument("", "doc", null);
    }
    
    public void testPerform() throws IOException
    {
        EnvVars env = new EnvVars();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintStream logger = new PrintStream(stream);
        
        // Test for correct XML will be performed in tests for subclasses.
        
        // null XML String
        {
            stream.flush();
            target.passedDocument = null;
            target.documentToReturn = emptyDoc;
            assertNull("null XML", target.perform(null, "UTF-8", env, logger));
            assertNull("null XML", target.passedDocument);
        }
        // null XML encoding
        {
            stream.flush();
            target.passedDocument = null;
            target.documentToReturn = emptyDoc;
            assertNotNull("null XML encoding", target.perform("<?xml version=\"1.0\" ?><doc>test</doc>", null, env, logger));
            assertNotNull("null XML encoding", target.passedDocument);
        }
        // Unknown Encoding
        {
            stream.flush();
            target.passedDocument = null;
            target.documentToReturn = emptyDoc;
            assertNull("Unknown Encoding", target.perform("<?xml version=\"1.0\" encoding=\"UTF-8\" ?><doc>テスト</doc>", "hogehogehoge", env, logger));
            assertNull("Unknown Encoding", target.passedDocument);
        }
        // InvalidXMLString
        {
            stream.flush();
            target.passedDocument = null;
            target.documentToReturn = emptyDoc;
            assertNull("Invalid XML", target.perform("hogehoge", "UTF-8", env, logger));
            assertNull("Invalid XML", target.passedDocument);
        }
        // Encoding in XML Mismatch
        {
            stream.flush();
            target.passedDocument = null;
            target.documentToReturn = emptyDoc;
            assertNull("Encoding in XML Mismatch", target.perform("<?xml version=\"1.0\" encoding=\"EUC-JP\" ?><doc>テスト</doc>", "UTF-8", env, logger));
            assertNull("Encoding in XML Mismatch", target.passedDocument);
        }
        
        // Failed to convert the XML document to a string.
        // There's no way to trigger this!!! 
    }
    
    public void testGetNode() throws SAXException, IOException, XPathExpressionException
    {
        String xml = "<?xml version=\"1.0\"?>" +
                "<root>" +
                    "<subnode><node>value0</node></subnode>" +
                    "<node>value1</node>" +
                    "<dupNode>value2</dupNode>" +
                    "<dupNode>value3</dupNode>" +
                "</root>";
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        
        // Existing single node
        {
            Node node = target.getNode(doc, "/root/node");
            assertNotNull("Existing single node", node);
            assertEquals("Existing single node", "value1", node.getTextContent());
        }
        
        // Duplicated node1
        {
            Node node = target.getNode(doc, "/root/dupNode");
            assertNull("Duplicated node1", node);
        }
        
        // Duplicated node2
        {
            Node node = target.getNode(doc, "//node");
            assertNull("Duplicated node2", node);
        }
        
        // Non existing node
        {
            Node node = target.getNode(doc, "//nosuchnode");
            assertNull("Non existing node", node);
        }
        
        // Invalid xpath
        {
            try{
                target.getNode(doc, "hoge()");
                assertTrue("Not reacheble", false);
            }
            catch(XPathExpressionException e)
            {
                assertTrue(true);
            }
        }
    }
    
    public void testGetNodeList() throws SAXException, IOException, XPathExpressionException
    {
        String xml = "<?xml version=\"1.0\"?>" +
                "<root>" +
                    "<subNode><node>value0</node></subNode>" +
                    "<node>value1</node>" +
                    "<dupNode>value2</dupNode>" +
                    "<dupNode>value3</dupNode>" +
                "</root>";
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        
        // Existing single node
        {
            NodeList nodeList = target.getNodeList(doc, "/root/node");
            assertNotNull("Existing single node", nodeList);
            assertEquals("Existing single node", 1, nodeList.getLength());
            assertEquals("Existing single node", "value1", nodeList.item(0).getTextContent());
        }
        
        // Duplicated node1
        {
            NodeList nodeList = target.getNodeList(doc, "/root/dupNode");
            assertNotNull("Duplicated node1", nodeList);
            assertEquals("Duplicated node1", 2, nodeList.getLength());
            assertEquals("Duplicated node1", "value2", nodeList.item(0).getTextContent());
            assertEquals("Duplicated node1", "value3", nodeList.item(1).getTextContent());
        }
        
        // Duplicated node2
        {
            NodeList nodeList = target.getNodeList(doc, "//node");
            assertNotNull("Duplicated node2", nodeList);
            assertEquals("Duplicated node2", 2, nodeList.getLength());
            assertEquals("Duplicated node2", "value0", nodeList.item(0).getTextContent());
            assertEquals("Duplicated node2", "value1", nodeList.item(1).getTextContent());
        }
        
        // Non existing node
        {
            NodeList nodeList = target.getNodeList(doc, "//nosuchnode");
            assertNotNull("Non existing node", nodeList);
            assertEquals("Non existing node", 0, nodeList.getLength());
        }
        
        // Invalid xpath
        {
            try{
                target.getNodeList(doc, "hoge()");
                assertTrue("Not reacheble", false);
            }
            catch(XPathExpressionException e)
            {
                assertTrue(true);
            }
        }
    }
    
    public void testGetXpath() throws SAXException, IOException, XPathExpressionException
    {
        String xml = "<?xml version=\"1.0\"?>" +
                "<root>" +
                    "<subNode><node>value0</node></subNode>" +
                    "<node>value1</node>" +
                    "<dupNode>value2</dupNode>" +
                    "<dupNode>value3</dupNode>" +
                "</root>";
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes()));
        
        // depth 1
        {
            Node rootNode = target.getNode(doc, "/root");
            assertEquals("Xpath to root node", "/root", target.getXpath(rootNode));
        }
        
        // depth 3
        {
            Node node = target.getNode(doc, "/root/subNode/node");
            assertEquals("Xpath to a node", "/root/subNode/node", target.getXpath(node));
        }
        
        // depth3
        {
            NodeList nodeList = target.getNodeList(doc, "/root/dupNode");
            assertEquals("Xpath to a duplicated node", "/root/dupNode", target.getXpath(nodeList.item(1)));
        }
        
        // text
        {
            Node node = target.getNode(doc, "/root/subNode/node/text()");
            assertEquals("Xpath to a text node", "/root/subNode/node/text()", target.getXpath(node));
        }
    }
}
