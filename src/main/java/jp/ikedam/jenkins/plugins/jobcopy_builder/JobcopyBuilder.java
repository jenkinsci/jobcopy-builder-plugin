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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import hudson.Extension;
import hudson.XmlFile;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.DescriptorExtensionList;
import hudson.matrix.MatrixProject;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.security.ACL;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractItem;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import jenkins.model.ModifiableTopLevelItemGroup;
import jenkins.model.Jenkins;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 * A build step to copy a job.
 * 
 * You can specify additional operations that is performed when copying,
 * and the operations can be extended with plugins using Extension Points.
 */
public class JobcopyBuilder extends Builder
{
    private String fromJobName;
    
    /**
     * Returns the name of job to be copied from.
     * 
     * Variable expressions will be expanded.
     * 
     * @return the name of job to be copied from
     */
    public String getFromJobName()
    {
        return fromJobName;
    }
    
    private String toJobName;
    
    /**
     * Returns the name of job to be copied to.
     * 
     * Variable expressions will be expanded.
     * 
     * @return the name of job to be copied to
     */
    public String getToJobName()
    {
        return toJobName;
    }
    
    private boolean overwrite = false;
    
    /**
     * Returns whether to overwrite an existing job.
     * 
     * If the copied-to job is already exists,
     * jobcopy build step works as following depending on this value.
     * <table>
     *     <caption>How jobcopy build step works with {@code isOverwrite}</caption>
     *     <tr>
     *         <th>isOverwrite</th>
     *         <th>behavior</th>
     *     </tr>
     *     <tr>
     *         <td>true</td>
     *         <td>Overwrite the configuration of the existing job.</td>
     *     </tr>
     *     <tr>
     *         <td>false</td>
     *         <td>Build fails.</td>
     *     </tr>
     * </table>
     * 
     * @return whether to overwrite an existing job.
     */
    public boolean isOverwrite()
    {
        return overwrite;
    }
    
    private List<JobcopyOperation> jobcopyOperationList;
    
    /**
     * Returns the list of operations.
     * 
     * @return the list of operations
     */
    public List<JobcopyOperation> getJobcopyOperationList()
    {
        return jobcopyOperationList;
    }
    
    private List<AdditionalFileset> additionalFilesetList;
    
    /**
     * Retuns a list of sets of files to copy additional to JOBNAME/config.xml.
     * 
     * @return the additionalFilesetList
     */
    public List<AdditionalFileset> getAdditionalFilesetList()
    {
        return additionalFilesetList;
    }

    /**
     * Constructor to instantiate from parameters in the job configuration page.
     * 
     * When instantiating from the saved configuration,
     * the object is directly serialized with XStream,
     * and no constructor is used.
     * 
     * @param fromJobName   a name of a job to be copied from. may contains variable expressions.
     * @param toJobName     a name of a job to be copied to. may contains variable expressions.
     * @param overwrite     whether to overwrite if the job to be copied to is already existing.
     * @param jobcopyOperationList
     *                      the list of operations to be performed when copying.
     * @param additionalFilesetList
     *                      the list of sets of files to copy additional to JOBNAME/config.xml.
     */
    @DataBoundConstructor
    public JobcopyBuilder(String fromJobName, String toJobName, boolean overwrite, List<JobcopyOperation> jobcopyOperationList, List<AdditionalFileset> additionalFilesetList)
    {
        this.fromJobName = StringUtils.trim(fromJobName);
        this.toJobName = StringUtils.trim(toJobName);
        this.overwrite = overwrite;
        this.jobcopyOperationList = jobcopyOperationList;
        this.additionalFilesetList = additionalFilesetList;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
        throws IOException, InterruptedException
    {
        SecurityContext orig = null;
        if(ACL.SYSTEM.equals(Jenkins.getAuthentication()))
        {
            orig = ACL.impersonate(Jenkins.ANONYMOUS);
        }
        
        try
        {
            return performImpl(build, launcher, listener);
        }
        finally
        {
            if(orig != null)
            {
                SecurityContextHolder.setContext(orig);
            }
        }
    }
    
    private boolean performImpl(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
        throws IOException, InterruptedException
    {
        ItemGroup<?> context = build.getProject().getRootProject().getParent();
        EnvVars env = build.getEnvironment(listener);
        
        if(StringUtils.isBlank(getFromJobName()))
        {
            listener.getLogger().println("From Job Name is not specified");
            return false;
        }
        if(StringUtils.isBlank(getToJobName()))
        {
            listener.getLogger().println("To Job Name is not specified");
            return false;
        }
        
        // Expand the variable expressions in job names.
        String fromJobNameExpanded = env.expand(getFromJobName());
        String toJobNameExpanded = env.expand(getToJobName());
        
        if(StringUtils.isBlank(fromJobNameExpanded))
        {
            listener.getLogger().println("From Job Name got to a blank");
            return false;
        }
        if(StringUtils.isBlank(toJobNameExpanded))
        {
            listener.getLogger().println("To Job Name got to a blank");
            return false;
        }
        
        listener.getLogger().println(String.format("Copying %s to %s", fromJobNameExpanded, toJobNameExpanded));
        
        // Reteive the job to be copied from.
        TopLevelItem fromJob = getRelative(fromJobNameExpanded, context, TopLevelItem.class);
        
        if(fromJob == null)
        {
            listener.getLogger().println(String.format("Error: Item '%s' was not found.", fromJobNameExpanded));
            return false;
        }
        else if(!(fromJob instanceof AbstractItem))
        {
            listener.getLogger().println(String.format("Error: Item '%s' was found, but cannot be copied (does not support AbstractItem).", fromJob));
            return false;
        }
        
        // Requires EXTENDED_READ for reading the configuration file.
        if(!fromJob.hasPermission(Item.EXTENDED_READ))
        {
            listener.getLogger().println(String.format("Error: Requires EXTENDED_READ or CONFIGURE permission for '%s'.", fromJobNameExpanded));
            return false;
        }
        
        // Check whether the job to be copied to is already exists.
        TopLevelItem toJob = getRelative(toJobNameExpanded, context, TopLevelItem.class);
        if(toJob != null){
            listener.getLogger().println(String.format("Already exists: %s", toJobNameExpanded));
            if(!isOverwrite()){
                return false;
            }
            if(!(toJob instanceof AbstractItem))
            {
                listener.getLogger().println("Only AbstractItem can be overwritten: please delete manually, and run copy again");
                return false;
            }
        }
        
        // Retrieve the config.xml of the job copied from.
        listener.getLogger().println(String.format("Fetching configuration of %s...", fromJobNameExpanded));
        
        XmlFile file = ((AbstractItem)fromJob).getConfigFile();
        String jobConfigXmlString = file.asString();
        String encoding = file.sniffEncoding();
        listener.getLogger().println("Original xml:");
        listener.getLogger().println(jobConfigXmlString);
        
        // Apply additional operations to the retrieved XML.
        if(getJobcopyOperationList() != null)
        {
            for(JobcopyOperation operation: getJobcopyOperationList())
            {
                jobConfigXmlString = operation.perform(jobConfigXmlString, encoding, env, listener.getLogger());
                if(jobConfigXmlString == null)
                {
                    return false;
                }
            }
        }
        listener.getLogger().println("Copied xml:");
        listener.getLogger().println(jobConfigXmlString);
        
        if(toJob == null)
        {
            // Create the job copied to.
            listener.getLogger().println(String.format("Creating %s", toJobNameExpanded));
            InputStream is = new ByteArrayInputStream(jobConfigXmlString.getBytes(encoding)); 
            ItemGroup<?> toContext = context;
            if(toJobNameExpanded.lastIndexOf('/')  >= 0)
            {
                int pos = toJobNameExpanded.lastIndexOf('/');
                String parentName = toJobNameExpanded.substring(0, pos);
                toJobNameExpanded = toJobNameExpanded.substring(pos + 1);
                if ("".equals(parentName))
                {
                    toContext = Jenkins.getInstance();
                }
                else
                {
                    toContext = getRelative(parentName, context, ItemGroup.class);
                }
                if(toContext == null)
                {
                    listener.getLogger().println(String.format("Error: Target folder '%s' was not found.", parentName));
                    return false;
                }
            }
            
            if(!(toContext instanceof ModifiableTopLevelItemGroup))
            {
                listener.getLogger().println(String.format("Error: Target folder '%s' does not support ModifiableTopLevelItemGroup", toContext.getFullName()));
                return false;
            }
            
            toJob = ((ModifiableTopLevelItemGroup)toContext).createProjectFromXML(toJobNameExpanded, is);
            if(toJob == null)
            {
                listener.getLogger().println(String.format("Failed to create %s", toJobNameExpanded));
                return false;
            }
        }
        else
        {
            listener.getLogger().println(String.format("Updating %s", toJobNameExpanded));
            AbstractItem target = (AbstractItem)toJob;
            InputStream is = new ByteArrayInputStream(jobConfigXmlString.getBytes(encoding));
            
            String combinationFilter = null;
            if(target instanceof MatrixProject)
            {
                MatrixProject matrix = (MatrixProject)target;
                // Workaround for the case combinationFilter is removed.
                // In that case, updateByXml does not update combinationFilter,
                // for combinationFilter is not written in XML.
                // So reset it here in advance. 
                // It will be overwritten if defined.
                combinationFilter = matrix.getCombinationFilter();
                matrix.setCombinationFilter(null);
            }
            
            try
            {
                target.updateByXml((Source)new StreamSource(is));
            }
            catch(IOException e)
            {
                if(combinationFilter != null)
                {
                    // recover combinationFilter.
                    MatrixProject matrix = (MatrixProject)target;
                    matrix.setCombinationFilter(combinationFilter);
                }
                throw e;
            }
        }
        
        boolean failed = false;
        
        if(getAdditionalFilesetList() != null && !getAdditionalFilesetList().isEmpty())
        {
            listener.getLogger().println("Copying Additional Files...");
            for(AdditionalFileset fileset: getAdditionalFilesetList())
            {
                if(!fileset.perform(toJob, fromJob, env, listener.getLogger()))
                {
                    failed = true;
                }
            }
            
            // Do null update to reload the configuration.
            AbstractItem target = (AbstractItem)toJob;
            target.updateByXml((Source)new StreamSource(target.getConfigFile().readRaw()));
        }
        
        // add the information of jobs copied from and to to the build.
        build.addAction(new CopiedjobinfoAction(fromJob, toJob, failed));
        
        return true;
    }
    
    /**
     * Reimplementation of {@link Jenkins#getItem(String, ItemGroup, Class)}
     * 
     * Existing implementation has following problems:
     * * Falls back to {@link Jenkins#getItemByFullName(String)}
     * * Cannot get {@link ItemGroup}
     * 
     * @param pathName relative path to an item to retrieve
     * @param context context to calculate {@code pathName} from.
     * @param klass class of the item to retrieve
     * @param <T> class of the item to retrieve
     * @return an item
     */
    public static <T> T getRelative(String pathName, ItemGroup<?> context, Class<T> klass)
    {
        if(context==null)
        {
            context = Jenkins.getInstance().getItemGroup();
        }
        if (pathName==null)
        {
            return null;
        }
        
        if (pathName.startsWith("/"))
        {
            // absolute
            Item item = Jenkins.getInstance().getItemByFullName(pathName);
            return klass.isInstance(item)?klass.cast(item):null;
        }
        
        Object/*Item|ItemGroup*/ ctx = context;
        
        StringTokenizer tokens = new StringTokenizer(pathName,"/");
        while(tokens.hasMoreTokens())
        {
            String s = tokens.nextToken();
            if(s.equals(".."))
            {
                if(!(ctx instanceof Item))
                {
                    // can't go up further
                    return null;
                }
                ctx = ((Item)ctx).getParent();
                continue;
            }
            if(s.equals("."))
            {
                continue;
            }
            
            if(!(ctx instanceof ItemGroup))
            {
                return null;
            }
            ItemGroup<?> g = (ItemGroup<?>)ctx;
            Item i = g.getItem(s);
            if (i == null || !i.hasPermission(Item.READ))
            {
                return null;
            }
            ctx=i;
        }
        
        return klass.isInstance(ctx)?klass.cast(ctx):null;
    }
    
    /**
     * The internal class to work with views.
     * 
     * The following files are used (put in main/resource directory in the source tree).
     * <dl>
     *     <dt>config.jelly</dt>
     *         <dd>shown as a part of a job configuration page.</dd>
     * </dl>
     */
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder>
    {
        /**
         * Returns the display name
         * 
         * Displayed in the "Add build step" dropdown in a job configuration page. 
         * 
         * @return the display name
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName()
        {
            return Messages.JobCopyBuilder_DisplayName();
        }
        
        /**
         * Test whether this build step can be applied to the specified job type.
         * 
         * This build step works for any type of jobs, for always returns true.
         * 
         * @param jobType the type of the job to be tested.
         * @return true
         * @see hudson.tasks.BuildStepDescriptor#isApplicable(java.lang.Class)
         */
        @SuppressWarnings("rawtypes")
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType)
        {
            return true;
        }
        
        /**
         * Returns all the available JobcopyOperation.
         * 
         * Used for the contents of "Add Copy Operation" dropdown.
         * 
         * @return the list of JobcopyOperation
         */
        public DescriptorExtensionList<JobcopyOperation,Descriptor<JobcopyOperation>> getJobcopyOperationDescriptors()
        {
            return JobcopyOperation.all();
        }
        
        /**
         * Returns the list of jobs.
         * 
         * Used for the autocomplete of From Job Name.
         * 
         * @param project the current job
         * @return the list of names of jobs
         */
        public ComboBoxModel doFillFromJobNameItems(@AncestorInPath AbstractProject<?,?> project)
        {
            final ItemGroup<?> context = (project != null)?project.getParent():Jenkins.getInstance().getItemGroup();
            List<String> itemList = new ArrayList<String>(Lists.transform(
                    Jenkins.getInstance().getAllItems(AbstractItem.class),
                    new Function<Item, String>()
                    {
                        public String apply(Item input)
                        {
                            return input.getRelativeNameFrom(context);
                        }
                    }
            ));
            Collections.sort(itemList);
            return new ComboBoxModel(itemList);
        }
        
        /**
         * Returns whether the value contains variable.
         * 
         * @param value value to be tested.
         * @return whether the value contains variable
         */
        private boolean containsVariable(String value)
        {
            if(StringUtils.isBlank(value) || !value.contains("$")){
                // apparently contains no variable.
                return false;
            }
            
            return true;
        }
        
        /**
         * Validate "From Job Name" or "To Job Name" field.
         * 
         * Returns as following:
         * <table>
         *     <caption>Return values for parameter combinations</caption>
         *     <tr>
         *         <th>jobName</th>
         *         <th>warnIfExists</th>
         *         <th>warnIfNotExists</th>
         *         <th>Returns</th>
         *     </tr>
         *     <tr>
         *         <td>Blank</td>
         *         <td>any</td>
         *         <td>any</td>
         *         <td>error</td>
         *     </tr>
         *     <tr>
         *         <td>value containing variables</td>
         *         <td>any</td>
         *         <td>any</td>
         *         <td>ok</td>
         *     </tr>
         *     <tr>
         *         <td>existing job</td>
         *         <td>false</td>
         *         <td>any</td>
         *         <td>ok</td>
         *     </tr>
         *     <tr>
         *         <td>existing job</td>
         *         <td>true</td>
         *         <td>any</td>
         *         <td>warning</td>
         *     </tr>
         *     <tr>
         *         <td>non existing job</td>
         *         <td>any</td>
         *         <td>false</td>
         *         <td>ok</td>
         *     </tr>
         *     <tr>
         *         <td>non existing job</td>
         *         <td>any</td>
         *         <td>true</td>
         *         <td>warning</td>
         *     </tr>
         * </table>
         * 
         * @param project the current job
         * @param jobName the name of the job to check
         * @param warnIfExists returns warning if the job exists
         * @param warnIfNotExists returns warning if the job not exists
         * @return validation result
         */
        public FormValidation doCheckJobName(AbstractProject<?,?> project, String jobName, boolean warnIfExists, boolean warnIfNotExists)
        {
            ItemGroup<?> context = (project != null)?project.getParent():Jenkins.getInstance().getItemGroup();
            jobName = StringUtils.trim(jobName);
            
            if(StringUtils.isBlank(jobName))
            {
                return FormValidation.error(Messages.JobCopyBuilder_JobName_empty());
            }
            if(containsVariable(jobName))
            {
                return FormValidation.ok();
            }
            
            TopLevelItem job = getRelative(jobName, context, TopLevelItem.class);
            if(job != null)
            {
                // job exists
                if(warnIfExists)
                {
                    return FormValidation.warning(Messages.JobCopyBuilder_JobName_exists());
                }
                if(!(job instanceof AbstractItem))
                {
                    return FormValidation.warning(Messages.JobCopyBuilder_JobName_notAbstractItem());
                }
            }
            else
            {
                // job does not exist
                if(warnIfNotExists)
                {
                    return FormValidation.warning(Messages.JobCopyBuilder_JobName_notExists());
                }
            }
            
            return FormValidation.ok();
        }
        
        /**
         * Validate "From Job Name" field.
         * 
         * @param project the current job
         * @param fromJobName the input to "From Job Name"
         * @return validation result
         */
        public FormValidation doCheckFromJobName(@AncestorInPath AbstractProject<?,?> project, @QueryParameter String fromJobName)
        {
            return doCheckJobName(project, fromJobName, false, true);
        }
        
        /**
         * Validate "To Job Name" field.
         * 
         * @param project the current job
         * @param toJobName the input to "To Job Name"
         * @param overwrite the input to "Overwrite"
         * @return validation result
         */
        public FormValidation doCheckToJobName(@AncestorInPath AbstractProject<?,?> project, @QueryParameter String toJobName, @QueryParameter boolean overwrite)
        {
            return doCheckJobName(project, toJobName, !overwrite, false);
        }
    }
}

