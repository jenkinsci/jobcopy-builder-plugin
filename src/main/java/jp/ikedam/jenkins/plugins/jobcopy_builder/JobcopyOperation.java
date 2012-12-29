package jp.ikedam.jenkins.plugins.jobcopy_builder;

import java.io.PrintStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.transform.TransformerException;

import hudson.EnvVars;
import hudson.ExtensionPoint;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.AbstractDescribableImpl;
import jenkins.model.Jenkins;

/**
 * ジョブをコピーするときの追加処理を定義する基底クラス。
 */
public abstract class JobcopyOperation extends AbstractDescribableImpl<JobcopyOperation> implements ExtensionPoint
{
    /**
     * 定義されているJobcopyOperationリストを返す。
     */
    static public DescriptorExtensionList<JobcopyOperation,Descriptor<JobcopyOperation>> all()
    {
        return Jenkins.getInstance().<JobcopyOperation,Descriptor<JobcopyOperation>>getDescriptorList(JobcopyOperation.class);
    }
    
    /**
     * 変換したXMLを返す。
     */
    public abstract String perform(String xmlString, String encoding, EnvVars env, PrintStream logger);
    
    
    
    /* XML関係のユーティリティメソッド群 */
    /**
     * 文字列からXML Documentオブジェクトを取得する
     */
    protected Document getXmlDocumentFromString(String xmlString, String encoding)
        throws ParserConfigurationException,UnsupportedEncodingException,SAXException,IOException
    {
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        //domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        InputStream is = new ByteArrayInputStream(xmlString.getBytes(encoding)); 
        
        return builder.parse(is);
    }
    
    /**
     * XML Documentオブジェクトからxpathで特定のノード群を取得する
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
     * XML Documentオブジェクトからxpathで特定のノードを取得する
     */
    protected Node getNode(Document doc, String xpath)
        throws XPathExpressionException
    {
        NodeList nodeList = getNodeList(doc, xpath);
        
        if(nodeList.getLength() != 1){
            return null;
        }
        
        return nodeList.item(0);
    }
    
    /**
     * ノードからXpathを取得する
     */
    protected String getXpath(Node targetNode)
    {
        StringBuilder pathBuilder = new StringBuilder();
        for(Node node = targetNode; node != null; node = node.getParentNode()){
            pathBuilder.insert(0, node.getNodeName());
            pathBuilder.insert(0, '/');
        }
        return pathBuilder.toString();
    }
    
    /**
     * XML DocumentからXMLファイルの中身を取得する
     */
    protected String getXmlString(Document doc)
        throws TransformerException
    {
        TransformerFactory tfactory = TransformerFactory.newInstance(); 
        Transformer transformer = tfactory.newTransformer(); 
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(sw)); 
        
        return sw.toString();
    }
}

