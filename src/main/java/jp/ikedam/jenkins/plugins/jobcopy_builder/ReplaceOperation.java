package jp.ikedam.jenkins.plugins.jobcopy_builder;

import java.io.PrintStream;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Descriptor;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * 設定ファイル内の文字列の置き換えをする。
 */
public class ReplaceOperation extends JobcopyOperation
{
    private static final long serialVersionUID = 1L;
    
    /**
     * ジョブの設定で使用するビューの情報
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<JobcopyOperation>
    {
        /**
         * ジョブ設定で項目追加時などに表示される項目名
         */
        @Override
        public String getDisplayName()
        {
            return Messages._ReplaceOperation_DisplayName().toString();
        }
    }
    
    /**
     * 変換元の文字列
     */
    private String fromStr;
    
    public String getFromStr(){
        return fromStr;
    }
    
    /**
     * 変換元の変数展開を行うか
     */
    private boolean expandFromStr;
    
    public boolean isExpandFromStr(){
        return expandFromStr;
    }
    
    /**
     * 変換先の文字列
     */
    private String toStr;
    
    public String getToStr(){
        return toStr;
    }
    
    /**
     * 変換先の変数展開を行うか
     */
    private boolean expandToStr;
    
    public boolean isExpandToStr(){
        return expandToStr;
    }
    
    /**
     * 入力された設定で初期化する
     */
    @DataBoundConstructor
    public ReplaceOperation(String fromStr, boolean expandFromStr, String toStr, boolean expandToStr)
    {
        this.fromStr = fromStr;
        this.expandFromStr = expandFromStr;
        this.toStr = toStr;
        this.expandToStr = expandToStr;
    }
    
    
    /**
     * 変換したXMLを返す。
     */
    @Override
    public String perform(String xmlString, String encoding, EnvVars env, PrintStream logger){
        String expandedFromStr = isExpandFromStr()?env.expand(getFromStr()):getFromStr();
        String expandedToStr = isExpandToStr()?env.expand(getToStr()):getToStr();
        
        // String.replaceは正規表現としてあつかわれるため、正規表現のエスケープをしておく
        String pattern = Pattern.quote(expandedFromStr);
        
        //return xmlString.replaceAll(pattern, expandedToStr);
        
        logger.print("Replacing: " + expandedFromStr + " -> " + expandedToStr);
        try{
            Document doc = getXmlDocumentFromString(xmlString, encoding);
            
            // 全てのテキストノードを取得
            NodeList textNodeList = getNodeList(doc, "//text()");
            
            // 全テキストノードに処理を行う。
            // NodeListはCollectionではないのでforeachは使えない。
            for(int i = 0; i < textNodeList.getLength(); ++i){
                Node node = textNodeList.item(i);
                node.setNodeValue(node.getNodeValue().replaceAll(pattern, expandedToStr));
            }
            logger.println("");
            
            return getXmlString(doc);
        }catch(Exception e){
            logger.print("Error occured in XML operation");
            e.printStackTrace(logger);
            return null;
        }
    }
}

