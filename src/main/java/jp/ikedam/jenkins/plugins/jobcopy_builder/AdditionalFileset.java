/*
 * The MIT License
 * 
 * Copyright (c) 2013 IKEDA Yasuyuki
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

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.DescriptorExtensionList;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

/**
 * A set of files to copy additional to JOBNAME/config.xml. 
 */
public class AdditionalFileset extends AbstractDescribableImpl<AdditionalFileset> implements Serializable
{
    private static final long serialVersionUID = 2080182353580260319L;
    
    private String includeFile;
    
    /**
     * Returns the pattern of file to copy.
     * 
     * @return the includeFile
     */
    public String getIncludeFile()
    {
        return includeFile;
    }
    
    private String excludeFile;
    
    /**
     * Returns the pattern of file not to copy.
     * 
     * @return the excludeFile
     */
    public String getExcludeFile()
    {
        return excludeFile;
    }
    
    private boolean overwrite;
    
    /**
     * Returns whether to overwrite an existing job.
     * 
     * If the copied-to file is already exists,
     * jobcopy build step works as following depending on this value.
     * <table>
     *     <tr>
     *         <th>isOverwrite</th>
     *         <th>behavior</th>
     *     </tr>
     *     <tr>
     *         <td>true</td>
     *         <td>Overwrite the file.</td>
     *     </tr>
     *     <tr>
     *         <td>false</td>
     *         <td>Not overwrite the file.</td>
     *     </tr>
     * </table>
     * 
     * @return whether to overwrite an existing file.
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
     * @param includeFile   a pattern of files to copy.
     * @param excludeFile   a pattern of files not to copy.
     * @param overwrite     whether to overwrite if the file if it is already existing.
     * @param jobcopyOperationList
     *                      the list of operations to be performed when copying.
     */
    @DataBoundConstructor
    public AdditionalFileset(String includeFile, String excludeFile, boolean overwrite, List<JobcopyOperation> jobcopyOperationList)
    {
        this.includeFile = StringUtils.trim(includeFile);
        this.excludeFile = StringUtils.trim(excludeFile);
        this.overwrite = overwrite;
        this.jobcopyOperationList = jobcopyOperationList;
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
    public static final class DescriptorImpl extends Descriptor<AdditionalFileset>
    {
        /**
         * Returns the display name
         * 
         * This is used nowhere...
         * 
         * @return the display name
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName()
        {
            return Messages.AdditionalFileset_DisplayName();
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
        
    }
}
