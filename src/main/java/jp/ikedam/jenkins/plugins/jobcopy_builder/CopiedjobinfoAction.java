package jp.ikedam.jenkins.plugins.jobcopy_builder;

import hudson.model.TopLevelItem;
import hudson.model.Action;

/**
 * コピーされたジョブの情報
 */
public class CopiedjobinfoAction implements Action
{
    private static final long serialVersionUID = 1L;
    
    /**
     * コピー元ジョブの名前
     */
    public String fromJobName;
    
    public String getFromJobName() {
        return this.fromJobName;
    }
    
    /**
     * コピー元ジョブのURL
     */
    public String fromUrl;
    
    public String getFromUrl() {
        return this.fromUrl;
    }
    
    /**
     * コピー先ジョブの名前
     */
    public String toJobName;
    
    public String getToJobName() {
        return this.toJobName;
    }
    
    /**
     * コピー先ジョブのURL
     */
    public String toUrl;
    
    public String getToUrl() {
        return this.toUrl;
    }
    
    public CopiedjobinfoAction(TopLevelItem fromItem, TopLevelItem toItem) {
        this.fromJobName = fromItem.getName();
        this.fromUrl = fromItem.getUrl();
        this.toJobName = toItem.getName();
        this.toUrl = toItem.getUrl();
    }
    
    /**
     * アイコン
     * メニューには表示しないのでnullを返す。
     */
    @Override
    public String getIconFileName() {
       return null;
    }
    
    /**
     * URL
     * メニューには表示しないのでnullを返す。
     */
    @Override
    public String getUrlName() {
        return null;
    }
    /**
     * 表示名。メニューには表示されないので実際には使用されない。
     */
    @Override
    public String getDisplayName() {
        return Messages._CopiedjobinfoAction_DisplayName().toString();
    }
}
