package jp.ikedam.jenkins.plugins.jobcopy_builder;

import java.io.PrintStream;
import java.io.Serializable;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Descriptor;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * ジョブを有効にする。
 */
public class EnableOperation extends JobcopyOperation implements Serializable
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
            return Messages._EnableOperation_DisplayName().toString();
        }
    }
    
    /**
     * 入力された設定で初期化する
     */
    @DataBoundConstructor
    public EnableOperation()
    {
    }
    
    /**
     * 変換したXMLを返す。
     */
    @Override
    public String perform(String xmlString, String encoding, EnvVars env, PrintStream logger)
    {
        logger.print("Enabling Job...");
        try{
            Document doc = getXmlDocumentFromString(xmlString, encoding);
            
            // 有効/無効の設定のノードを取得
            Node disabledNode = getNode(doc, "/*/disabled/text()");
            if(disabledNode == null){
                logger.println("Failed to fetch disabled node.");
                return null;
            }
            
            // ログへの表示用にノードの場所を取得
            String path = getXpath(disabledNode);
            
            logger.println(path + ": " + disabledNode.getNodeValue() + " -> false");
            disabledNode.setNodeValue("false");
            
            return getXmlString(doc);
        }catch(Exception e){
            logger.print("Error occured in XML operation");
            e.printStackTrace(logger);
            return null;
        }
    }
}

