package jp.ikedam.jenkins.plugins.jobcopy_builder;

import java.util.List;

import hudson.Extension;
import hudson.XmlFile;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.DescriptorExtensionList;
import hudson.model.TopLevelItem;
import hudson.model.BuildListener;
import hudson.model.Job;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.ComboBoxModel;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.Jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;

/**
 * ジョブをコピーするビルドステップ。
 * コピーするときの追加処理をプラグインで追加できる。
 */
public class JobcopyBuilder extends Builder implements Serializable
{
    private static final long serialVersionUID = 1L;
    
    /**
     * コピー元のジョブ名。変数展開する。
     */
    private String fromJobName;
    
    public String getFromJobName()
    {
        return fromJobName;
    }
    
    /**
     * コピー先のジョブ名。変数展開する。
     */
    private String toJobName;
    
    public String getToJobName()
    {
        return toJobName;
    }
    
    /**
     * ジョブを上書きするか？
     */
    private boolean overwrite = false;
    
    public boolean isOverwrite()
    {
        return overwrite;
    }
    
    /**
     * 実行する処理のリスト
     */
    private List<JobcopyOperation> jobcopyOperationList;
    
    public List<JobcopyOperation> getJobcopyOperationList()
    {
        return jobcopyOperationList;
    }
    
    /**
     * 設定画面の入力からオブジェクトを作成するときに使用するコンストラクタ
     */
    @DataBoundConstructor
    public JobcopyBuilder(String fromJobName, String toJobName, boolean overwrite, List<JobcopyOperation> jobcopyOperationList)
    {
        this.fromJobName = fromJobName;
        this.toJobName = toJobName;
        this.overwrite = overwrite;
        this.jobcopyOperationList = jobcopyOperationList;
    }
    
    /**
     * ビルドジョブの実行
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
        throws IOException, InterruptedException
    {
        EnvVars env = build.getEnvironment(listener);
        
        // ジョブ名の解決
        String fromJobNameExpanded = env.expand(fromJobName);
        String toJobNameExpanded = env.expand(toJobName);
        
        listener.getLogger().println("Copying " + fromJobNameExpanded + " to " + toJobNameExpanded);
        
        // コピー元ジョブの同定
        TopLevelItem fromJob = Jenkins.getInstance().getItem(fromJobNameExpanded);
        
        if(fromJob == null)
        {
            listener.getLogger().println("Error: Item was not found.");
            return false;
        }else if(!(fromJob instanceof Job<?,?>)){
            listener.getLogger().println("Error: Item was found, but is not a job.");
            return false;
        }
        
        // コピー先ジョブの同定
        TopLevelItem toJob = Jenkins.getInstance().getItem(toJobNameExpanded);
        if(toJob != null){
            listener.getLogger().println("Already exists: " + toJobNameExpanded);
            if(!isOverwrite()){
                return false;
            }
        }
        
        // コピー元ジョブのXMLの取得
        listener.getLogger().println("Fetching configuration of " + fromJobNameExpanded + "...");
        
        XmlFile file = ((Job<?,?>)fromJob).getConfigFile();
        String jobConfigXmlString = file.asString();
        String encoding = file.sniffEncoding();
        listener.getLogger().println("Original xml:");
        listener.getLogger().print(jobConfigXmlString);
        listener.getLogger().println("");
        
        // XMLの変換処理
        for(JobcopyOperation operation: getJobcopyOperationList()){
            jobConfigXmlString = operation.perform(jobConfigXmlString, encoding, env, listener.getLogger());
            if(jobConfigXmlString == null){
                return false;
            }
        }
        listener.getLogger().println("Copied xml:");
        listener.getLogger().print(jobConfigXmlString);
        listener.getLogger().println("");
        
        // コピー先のジョブの削除(存在する場合)
        if(toJob != null){
            toJob.delete();
            listener.getLogger().println("Deleted " + toJobNameExpanded);
        }
        
        // コピー先のジョブの作成
        listener.getLogger().println("Creating " + toJobNameExpanded);
        InputStream is = new ByteArrayInputStream(jobConfigXmlString.getBytes(encoding)); 
        toJob = Jenkins.getInstance().createProjectFromXML(toJobNameExpanded, is);
        if(toJob == null){
            listener.getLogger().println("Failed to create " + toJobNameExpanded);
            return false;
        }
        build.addAction(new CopiedjobinfoAction(fromJob, toJob));
        
        return true;
    }
    
    /**
     * ビューとの対応付けをするための定義
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
    {
        /**
         * 表示名
         */
        @Override
        public String getDisplayName()
        {
            return Messages.JobCopyBuilder_DisplayName();
        }
        
        /**
         * 適用可能なジョブのフィルタ
         */
        @SuppressWarnings("rawtypes")
        @Override
		public boolean isApplicable(Class<? extends AbstractProject> jobType)
        {
            return true;
        }
        
        /**
         * 利用可能なJobcopyOperationの一覧を返す
         */
        public DescriptorExtensionList<JobcopyOperation,Descriptor<JobcopyOperation>> getJobcopyOperationDescriptors()
        {
            return JobcopyOperation.all();
        }
        
        /**
         * コピー元のジョブとして選択可能なジョブの一覧を返す。
         */
        public ComboBoxModel doFillFromJobNameItems()
        {
            return new ComboBoxModel(Jenkins.getInstance().getTopLevelItemNames());
        }
        
    }
}

