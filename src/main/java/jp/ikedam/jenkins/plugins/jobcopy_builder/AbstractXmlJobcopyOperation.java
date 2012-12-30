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
 * ジョブをコピーするときの処理をXML DOMで行うための抽象クラス。
 */
public abstract class AbstractXmlJobcopyOperation extends JobcopyOperation
{
    /**
     * XML Document に対して変換処理を行い、変換後のXML Documentを返す
     * @param doc 変換対象のXML Document
     * @param env ビルドで定義されている変数
     * @param logger ログ出力
     * @return 変換後のXML Document
     */
    public abstract Document perform(Document doc, EnvVars env, PrintStream logger);
    
    /**
     * 変換したXMLを返す。
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
     * XML DocumentからXMLファイルの中身を取得する
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
     * 文字列からXML Documentオブジェクトを取得する
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
    
    
    /* XML関係のユーティリティメソッド群 */
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
    
}