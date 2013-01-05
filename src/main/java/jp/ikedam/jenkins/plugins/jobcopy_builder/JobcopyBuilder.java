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
 * A build step to copy a job.
 * 
 * You can specify additional operations that is performed when copying,
 * and the operations can be extended with plugins using Extension Points.
 */
public class JobcopyBuilder extends Builder implements Serializable
{
    private static final long serialVersionUID = 1L;
    
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
     *     <tr>
     *         <th>isOverwrite</th>
     *         <th>behavior</th>
     *     </tr>
     *     <tr>
     *         <td>true</td>
     *         <td>Delete the existing job, and create a new job.</td>
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
     * Performs the build step.
     * 
     * @param build
     * @param launcher
     * @param listener
     * @return  whether the process succeeded.
     * @throws IOException
     * @throws InterruptedException
     * @see hudson.tasks.BuildStepCompatibilityLayer#perform(hudson.model.AbstractBuild, hudson.Launcher, hudson.model.BuildListener)
     */
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
        throws IOException, InterruptedException
    {
        EnvVars env = build.getEnvironment(listener);
        
        // Expand the variable expressions in job names.
        String fromJobNameExpanded = env.expand(fromJobName);
        String toJobNameExpanded = env.expand(toJobName);
        
        listener.getLogger().println(String.format("Copying %s to %s", fromJobNameExpanded, toJobNameExpanded));
        
        // Retieve the job to be copied from.
        TopLevelItem fromJob = Jenkins.getInstance().getItem(fromJobNameExpanded);
        
        if(fromJob == null)
        {
            listener.getLogger().println("Error: Item was not found.");
            return false;
        }
        else if(!(fromJob instanceof Job<?,?>))
        {
            listener.getLogger().println("Error: Item was found, but is not a job.");
            return false;
        }
        
        // Check whether the job to be copied to is already exists.
        TopLevelItem toJob = Jenkins.getInstance().getItem(toJobNameExpanded);
        if(toJob != null){
            listener.getLogger().println(String.format("Already exists: %s", toJobNameExpanded));
            if(!isOverwrite()){
                return false;
            }
        }
        
        // Retrieve the config.xml of the job copied from.
        // TODO: what happens if this runs on a slave node?
        listener.getLogger().println(String.format("Fetching configuration of %s...", fromJobNameExpanded));
        
        XmlFile file = ((Job<?,?>)fromJob).getConfigFile();
        String jobConfigXmlString = file.asString();
        String encoding = file.sniffEncoding();
        listener.getLogger().println("Original xml:");
        listener.getLogger().println(jobConfigXmlString);
        
        // Apply additional operations to the retrieved XML.
        for(JobcopyOperation operation: getJobcopyOperationList())
        {
            jobConfigXmlString = operation.perform(jobConfigXmlString, encoding, env, listener.getLogger());
            if(jobConfigXmlString == null)
            {
                return false;
            }
        }
        listener.getLogger().println("Copied xml:");
        listener.getLogger().println(jobConfigXmlString);
        
        // Remove the job copied to (only if it already exists)
        if(toJob != null)
        {
            toJob.delete();
            listener.getLogger().println(String.format("Deleted %s", toJobNameExpanded));
        }
        
        // Create the job copied to.
        listener.getLogger().println(String.format("Creating %s", toJobNameExpanded));
        InputStream is = new ByteArrayInputStream(jobConfigXmlString.getBytes(encoding)); 
        toJob = Jenkins.getInstance().createProjectFromXML(toJobNameExpanded, is);
        if(toJob == null)
        {
            listener.getLogger().println(String.format("Failed to create %s", toJobNameExpanded));
            return false;
        }
        
        // add the information of jobs copied from and to to the build.
        build.addAction(new CopiedjobinfoAction(fromJob, toJob));
        
        return true;
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
        // TODO: Form validation
        
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
         * @return the list of jobs
         */
        public ComboBoxModel doFillFromJobNameItems()
        {
            return new ComboBoxModel(Jenkins.getInstance().getTopLevelItemNames());
        }
    }
}

