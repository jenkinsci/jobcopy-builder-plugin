package jp.ikedam.jenkins.plugins.jobcopy_builder;

import java.io.PrintStream;



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
}

