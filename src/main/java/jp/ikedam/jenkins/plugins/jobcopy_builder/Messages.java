package jp.ikedam.jenkins.plugins.jobcopy_builder;

import org.jvnet.localizer.Localizable;
import org.jvnet.localizer.ResourceBundleHolder;

public class Messages {
    private static final ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class);

    public Messages() {
    }

    public static String DisableOperation_DisplayName() {
        return holder.format("DisableOperation.DisplayName");
    }

    public static Localizable _DisableOperation_DisplayName() {
        return new Localizable(holder, "DisableOperation.DisplayName");
    }

    public static String ReplaceOperation_fromStr_enclosedWithBlank() {
        return holder.format("ReplaceOperation.fromStr.enclosedWithBlank");
    }

    public static Localizable _ReplaceOperation_fromStr_enclosedWithBlank() {
        return new Localizable(holder, "ReplaceOperation.fromStr.enclosedWithBlank");
    }

    public static String JobCopyBuilder_DisplayName() {
        return holder.format("JobCopyBuilder.DisplayName");
    }

    public static Localizable _JobCopyBuilder_DisplayName() {
        return new Localizable(holder, "JobCopyBuilder.DisplayName");
    }

    public static String JobCopyBuilder_JobName_notExists() {
        return holder.format("JobCopyBuilder.JobName.notExists");
    }

    public static Localizable _JobCopyBuilder_JobName_notExists() {
        return new Localizable(holder, "JobCopyBuilder.JobName.notExists");
    }

    public static String EnableOperation_DisplayName() {
        return holder.format("EnableOperation.DisplayName");
    }

    public static Localizable _EnableOperation_DisplayName() {
        return new Localizable(holder, "EnableOperation.DisplayName");
    }

    public static String JobCopyBuilder_JobName_empty() {
        return holder.format("JobCopyBuilder.JobName.empty");
    }

    public static Localizable _JobCopyBuilder_JobName_empty() {
        return new Localizable(holder, "JobCopyBuilder.JobName.empty");
    }

    public static String AdditionalFileset_DisplayName() {
        return holder.format("AdditionalFileset.DisplayName");
    }

    public static Localizable _AdditionalFileset_DisplayName() {
        return new Localizable(holder, "AdditionalFileset.DisplayName");
    }

    public static String AdditionalFileSet_includeFile_empty() {
        return holder.format("AdditionalFileSet.includeFile.empty");
    }

    public static Localizable _AdditionalFileSet_includeFile_empty() {
        return new Localizable(holder, "AdditionalFileSet.includeFile.empty");
    }

    public static String ReplaceOperation_DisplayName() {
        return holder.format("ReplaceOperation.DisplayName");
    }

    public static Localizable _ReplaceOperation_DisplayName() {
        return new Localizable(holder, "ReplaceOperation.DisplayName");
    }

    public static String JobCopyBuilder_JobName_notAbstractItem() {
        return holder.format("JobCopyBuilder.JobName.notAbstractItem");
    }

    public static Localizable _JobCopyBuilder_JobName_notAbstractItem() {
        return new Localizable(holder, "JobCopyBuilder.JobName.notAbstractItem");
    }

    public static String JobCopyBuilder_JobName_exists() {
        return holder.format("JobCopyBuilder.JobName.exists");
    }

    public static Localizable _JobCopyBuilder_JobName_exists() {
        return new Localizable(holder, "JobCopyBuilder.JobName.exists");
    }

    public static String CopiedjobinfoAction_DisplayName() {
        return holder.format("CopiedjobinfoAction.DisplayName");
    }

    public static Localizable _CopiedjobinfoAction_DisplayName() {
        return new Localizable(holder, "CopiedjobinfoAction.DisplayName");
    }

    public static String ReplaceOperation_fromStr_empty() {
        return holder.format("ReplaceOperation.fromStr.empty");
    }

    public static Localizable _ReplaceOperation_fromStr_empty() {
        return new Localizable(holder, "ReplaceOperation.fromStr.empty");
    }
}
