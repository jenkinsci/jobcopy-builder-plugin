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

import java.io.PrintStream;



import hudson.EnvVars;
import hudson.ExtensionPoint;
import hudson.DescriptorExtensionList;
import hudson.model.Descriptor;
import hudson.model.AbstractDescribableImpl;
import jenkins.model.Jenkins;

/**
 * Additional operations performed when the jobcopy build step copies a job.
 * 
 * A new additional operation can be defined in following steps:
 * <ol>
 *    <li>Define a new class derived from JobcopyOperation. AbstractXmlJobcopyOperation is also available.</li>
 *    <li>Override {@link JobcopyOperation#perform(String, String, EnvVars, PrintStream)} or {@link AbstractXmlJobcopyOperation#perform(org.w3c.dom.Document, EnvVars, PrintStream)}</li>
 *    <li>Define the internal public static class named DescriptorImpl, derived from Descriptor&lt;JobcopyOperation&gt;</li>
 *    <li>annotate the DescriptorImpl with Extension</li>
 * </ol>
 */
public abstract class JobcopyOperation extends AbstractDescribableImpl<JobcopyOperation> implements ExtensionPoint
{
    /**
     * Return all the available JobcopyOperation whose DescriptorImpl annotated with Extension.
     * 
     * @return
     */
    static public DescriptorExtensionList<JobcopyOperation,Descriptor<JobcopyOperation>> all()
    {
        return Jenkins.getInstance().<JobcopyOperation,Descriptor<JobcopyOperation>>getDescriptorList(JobcopyOperation.class);
    }
    
    /**
     * Return modified XML string of the job configuration.
     * 
     * @param xmlString the XML string  of the job to be copied (job/NAME/config.xml)
     * @param encoding  the encoding of the XML.
     * @param env       Variables defined in the build.
     * @param logger    The output stream to log.
     * @return          modified XML string. Return null if an error occurs.
     */
    public abstract String perform(String xmlString, String encoding, EnvVars env, PrintStream logger);
}

