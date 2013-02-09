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

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import jenkins.model.Jenkins;

import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.Cause;
import hudson.model.FreeStyleProject;
import hudson.model.ParametersAction;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;
import hudson.model.StringParameterValue;
import hudson.model.Result;
import hudson.util.ComboBoxModel;
import hudson.util.FormValidation;

import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

/**
 * Tests for JobcopyBuilder, corresponded to Jenkins.
 *
 */
public class JobcopyBuilderJenkinsTest extends HudsonTestCase
{
    private JobcopyBuilder.DescriptorImpl getDescriptor()
    {
        return (JobcopyBuilder.DescriptorImpl)(new JobcopyBuilder(null, null, false, null)).getDescriptor();
    }
    
    public void testDescriptorDoFillFromJobNameItems() throws IOException
    {
        JobcopyBuilder.DescriptorImpl descriptor = getDescriptor();
        
        // Job will be added after new job created.
        ComboBoxModel beforeList = descriptor.doFillFromJobNameItems();
        
        FreeStyleProject project = createFreeStyleProject("testDescriptorDoFillFromJobNameItems1");
        String newJobname = project.getName();
        
        ComboBoxModel afterList = descriptor.doFillFromJobNameItems();
        
        assertEquals("new job created", beforeList.size() + 1, afterList.size());
        assertTrue("new job created", afterList.contains(newJobname));
    }
    
    public void testDescriptorDoCheckFromJobName() throws IOException
    {
        JobcopyBuilder.DescriptorImpl descriptor = getDescriptor();
        FreeStyleProject project = createFreeStyleProject("testDescriptorDoCheckFromJobName1");
        String existJobname = project.getName();
        
        // exist job
        {
            assertEquals(
                    "exist job",
                    FormValidation.Kind.OK,
                    descriptor.doCheckFromJobName(existJobname).kind
            );
        }
        
        // exist job surrounded with blank
        {
            assertEquals(
                    "exist job surrounded with blank",
                    FormValidation.Kind.OK,
                    descriptor.doCheckFromJobName("  " + existJobname + " ").kind
            );
        }
        
        // non-exist job
        {
            assertEquals(
                    "non-exist job",
                    FormValidation.Kind.WARNING,
                    descriptor.doCheckFromJobName("nosuchjob").kind
            );
        }
        
        // uses variable
        {
            assertEquals(
                    "uses variable",
                    FormValidation.Kind.OK,
                    descriptor.doCheckFromJobName("nosuchjob${name}").kind
            );
        }
        
        // escaped dollar
        {
            // Not warned even if no such job exists.
            assertEquals(
                    "escaped dollar",
                    FormValidation.Kind.OK,
                    descriptor.doCheckFromJobName("$$nosuchjob").kind
            );
        }
        
        // null
        {
            assertEquals(
                    "null",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckFromJobName(null).kind
            );
        }
        
        // empty
        {
            assertEquals(
                    "empty",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckFromJobName("").kind
            );
        }
        
        // blank
        {
            assertEquals(
                    "blank",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckFromJobName("  ").kind
            );
        }
    }
    
    public void testDescriptorDoCheckToJobName() throws IOException
    {
        JobcopyBuilder.DescriptorImpl descriptor = getDescriptor();
        FreeStyleProject project = createFreeStyleProject("testDescriptorDoCheckToJobName1");
        String existJobname = project.getName();
        
        // exist job, overwrite
        {
            assertEquals(
                    "exist job, overwrite",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToJobName(existJobname, true).kind
            );
        }
        
        // exist job, not overwrite
        {
            assertEquals(
                    "exist job, not overwrite",
                    FormValidation.Kind.WARNING,
                    descriptor.doCheckToJobName(existJobname, false).kind
            );
        }
        
        // exist job surrounded with blank, not overwrite
        {
            assertEquals(
                    "exist job surrounded with blank, not overwrite",
                    FormValidation.Kind.WARNING,
                    descriptor.doCheckToJobName("  " + existJobname + "  ", false).kind
            );
        }
        
        // non-exist job, overwrite
        {
            assertEquals(
                    "non-exist job",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToJobName("nosuchjob", true).kind
            );
        }
        
        // non-exist job, not overwrite
        {
            assertEquals(
                    "non-exist job",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToJobName("nosuchjob", false).kind
            );
        }
        
        // uses variable
        {
            assertEquals(
                    "uses variable",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToJobName("nosuchjob${name}", false).kind
            );
        }
        
        // null
        {
            assertEquals(
                    "null",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckToJobName(null, false).kind
            );
        }
        
        // empty
        {
            assertEquals(
                    "empty",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckToJobName("", false).kind
            );
        }
        
        // blank
        {
            assertEquals(
                    "blank",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckToJobName("  ", false).kind
            );
        }
    }
    
    /**
     * Test cases that builds succeed
     * @throws IOException 
     * @throws InterruptedException 
     * @throws ExecutionException 
     */
    public void testPerform() throws IOException, InterruptedException, ExecutionException
    {
        FreeStyleProject fromJob = createFreeStyleProject("testPerform1");
        
        String toJobName = "JobCopiedTo";
        FreeStyleProject toJob = (FreeStyleProject)Jenkins.getInstance().getItem(toJobName);
        if(toJob != null)
        {
            toJob.delete();
        }
        
        // Set up the job copied from.
        // Define parameters replaced in ReplaceOperation
        fromJob.addProperty(new ParametersDefinitionProperty(
                new StringParameterDefinition(
                        "PARAM1",
                        "DEFAULTVALUE",
                        "Description"
                )
        ));
        // disable the job.
        fromJob.disable();
        fromJob.save();
        
        ParametersAction paramAction = new ParametersAction(
                new StringParameterValue("fromJobName", fromJob.getName()),
                new StringParameterValue("toJobName", toJobName)
        );
        
        // No variable use.
        {
            List<JobcopyOperation> lst = new ArrayList<JobcopyOperation>();
            lst.add(new EnableOperation());
            lst.add(new ReplaceOperation(
                    "PARAM1", false,
                    "PARAM2", false
            ));
            JobcopyBuilder target = new JobcopyBuilder(fromJob.getName(), toJobName, false, lst);
            
            FreeStyleProject project = createFreeStyleProject("testPerform2");
            project.getBuildersList().add(target);
            
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("No variable use", Result.SUCCESS, b.getResult());
            
            toJob = (FreeStyleProject)Jenkins.getInstance().getItem(toJobName);
            assertNotNull("No variable use", toJob);
            
            assertFalse("No variable use", toJob.isDisabled());
            
            ParametersDefinitionProperty prop = toJob.getAction(ParametersDefinitionProperty.class);
            assertFalse("No variable use", prop.getParameterDefinitionNames().contains("PARAM1"));
            assertTrue("No variable use", prop.getParameterDefinitionNames().contains("PARAM2"));
            
            toJob.delete();
        }
        
        // Using variables.
        {
            List<JobcopyOperation> lst = new ArrayList<JobcopyOperation>();
            lst.add(new EnableOperation());
            lst.add(new ReplaceOperation(
                    "PARAM1", false,
                    "PARAM2", false
            ));
            JobcopyBuilder target = new JobcopyBuilder("${fromJobName}", "${toJobName}", false, lst);
            
            FreeStyleProject project = createFreeStyleProject("testPerform3");
            project.getBuildersList().add(target);
            
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("Using variables", Result.SUCCESS, b.getResult());
            
            toJob = (FreeStyleProject)Jenkins.getInstance().getItem(toJobName);
            assertNotNull("Using variables", toJob);
            
            assertFalse("Using variables", toJob.isDisabled());
            
            ParametersDefinitionProperty prop = toJob.getAction(ParametersDefinitionProperty.class);
            assertFalse("Using variables", prop.getParameterDefinitionNames().contains("PARAM1"));
            assertTrue("Using variables", prop.getParameterDefinitionNames().contains("PARAM2"));
            
            toJob.delete();
        }
        
        // overwrite
        {
            // create new job
            {
                List<JobcopyOperation> lst = new ArrayList<JobcopyOperation>();
                lst.add(new EnableOperation());
                JobcopyBuilder target = new JobcopyBuilder(fromJob.getName(), toJobName, false, lst);
                
                FreeStyleProject project = createFreeStyleProject("testPerform4");
                project.getBuildersList().add(target);
                
                FreeStyleBuild b = project.scheduleBuild2(
                        project.getQuietPeriod(),
                        new Cause.UserIdCause(),
                        paramAction
                ).get();
                while(b.isBuilding())
                {
                    Thread.sleep(100);
                }
                assertEquals("overwrite(create a new job)", Result.SUCCESS, b.getResult());
                
                toJob = (FreeStyleProject)Jenkins.getInstance().getItem(toJobName);
                assertNotNull("overwrite(create a new job)", toJob);
                
                assertFalse("overwrite(create a new job)", toJob.isDisabled());
                
                ParametersDefinitionProperty prop = toJob.getAction(ParametersDefinitionProperty.class);
                assertTrue("overwrite(create a new job)", prop.getParameterDefinitionNames().contains("PARAM1"));
                assertFalse("overwrite(create a new job)", prop.getParameterDefinitionNames().contains("PARAM2"));
                
                toJob.save();
                // Execute it.
                assertEquals("overwrite(create a new job)", 0, toJob.getBuilds().size());
                b = toJob.scheduleBuild2(toJob.getQuietPeriod()).get();
                while(b.isBuilding())
                {
                    Thread.sleep(100);
                }
                assertEquals("overwrite(create a new job)", 1, toJob.getBuilds().size());
            }
            
            // overwrite
            {
                List<JobcopyOperation> lst = new ArrayList<JobcopyOperation>();
                lst.add(new ReplaceOperation(
                        "PARAM1", false,
                        "PARAM2", false
                ));
                JobcopyBuilder target = new JobcopyBuilder(fromJob.getName(), toJobName, true, lst);
                
                FreeStyleProject project = createFreeStyleProject("testPerform5");
                project.getBuildersList().add(target);
                
                FreeStyleBuild b = project.scheduleBuild2(
                        project.getQuietPeriod(),
                        new Cause.UserIdCause(),
                        paramAction
                ).get();
                while(b.isBuilding())
                {
                    Thread.sleep(100);
                }
                assertEquals("overwrite(overwrite)", Result.SUCCESS, b.getResult());
                
                toJob = (FreeStyleProject)Jenkins.getInstance().getItem(toJobName);
                assertNotNull("overwrite(overwrite)", toJob);
                
                assertTrue("overwrite(overwrite)", toJob.isDisabled());
                
                ParametersDefinitionProperty prop = toJob.getAction(ParametersDefinitionProperty.class);
                assertFalse("overwrite(overwrite)", prop.getParameterDefinitionNames().contains("PARAM1"));
                assertTrue("overwrite(overwrite)", prop.getParameterDefinitionNames().contains("PARAM2"));
                
                assertEquals("overwrite(overwrite)", 1, toJob.getBuilds().size());
            }
            
            toJob.delete();
        }
        
        // null for jobcopyOperationList
        {
            JobcopyBuilder target = new JobcopyBuilder(fromJob.getName(), toJobName, false, null);
            
            FreeStyleProject project = createFreeStyleProject("testPerform6");
            project.getBuildersList().add(target);
            
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("null for jobcopyOperationList", Result.SUCCESS, b.getResult());
            
            toJob = (FreeStyleProject)Jenkins.getInstance().getItem(toJobName);
            assertNotNull("null for jobcopyOperationList", toJob);
            
            toJob.delete();
        }
        
        // empty for jobcopyOperationList
        {
            JobcopyBuilder target = new JobcopyBuilder(fromJob.getName(), toJobName, false, new ArrayList<JobcopyOperation>(0));
            
            FreeStyleProject project = createFreeStyleProject("testPerform7");
            project.getBuildersList().add(target);
            
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("empty for jobcopyOperationList", Result.SUCCESS, b.getResult());
            
            toJob = (FreeStyleProject)Jenkins.getInstance().getItem(toJobName);
            assertNotNull("empty for jobcopyOperationList", toJob);
            
            toJob.delete();
        }
    }
    
    /**
     * Used for a error case test.
     * 
     * This class must be serializable, so anonymous class cannot be applied.
     */
    static private class NullJobcopyOperation extends JobcopyOperation implements Serializable
    {
        private static final long serialVersionUID = -4314651910414654207L;
        @Override
        public String perform(String xmlString, String encoding,
                EnvVars env, PrintStream logger)
        {
            return null;
        }
    };
    
    /**
     * Test cases that builds fail
     * @throws IOException 
     * @throws ExecutionException 
     * @throws InterruptedException 
     */
    public void testPerformError() throws IOException, InterruptedException, ExecutionException
    {
        FreeStyleProject project = createFreeStyleProject("testPerformError1");
        String toJobName = "JobCopiedTo";
        FreeStyleProject toJob = (FreeStyleProject)Jenkins.getInstance().getItem(toJobName);
        if(toJob != null)
        {
            toJob.delete();
        }
        ParametersAction paramAction = new ParametersAction(
                new StringParameterValue("EMPTY", ""),
                new StringParameterValue("NOSUCHJOB", "nosuchjob")
        );
        
        // From Job Name is null.
        {
            JobcopyBuilder target = new JobcopyBuilder(null, toJobName, true, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("From Job Name is null", Result.FAILURE, b.getResult());
        }
        
        // From Job Name is empty.
        {
            JobcopyBuilder target = new JobcopyBuilder("", toJobName, true, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("From Job Name is empty", Result.FAILURE, b.getResult());
        }
        
        // From Job Name is blank.
        {
            JobcopyBuilder target = new JobcopyBuilder("  ", toJobName, true, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("From Job Name is blank", Result.FAILURE, b.getResult());
        }
        
        // From Job Name gets empty.
        {
            JobcopyBuilder target = new JobcopyBuilder("${EMPTY}", toJobName, true, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("From Job Name gets empty", Result.FAILURE, b.getResult());
        }
        
        // From Job Name gets blank.
        {
            JobcopyBuilder target = new JobcopyBuilder("  ${EMPTY}  ", toJobName, true, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("From Job Name gets blank", Result.FAILURE, b.getResult());
        }
        
        // To Job Name is null.
        {
            JobcopyBuilder target = new JobcopyBuilder(project.getName(), null, true, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("To Job Name is null", Result.FAILURE, b.getResult());
        }
        
        // To Job Name is empty.
        {
            JobcopyBuilder target = new JobcopyBuilder(project.getName(), "", true, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("To Job Name is empty", Result.FAILURE, b.getResult());
        }
        
        // To Job Name is blank.
        {
            JobcopyBuilder target = new JobcopyBuilder(project.getName(), "  ", true, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("To Job Name is blank", Result.FAILURE, b.getResult());
        }
        
        // To Job Name gets empty.
        {
            JobcopyBuilder target = new JobcopyBuilder(project.getName(), "${EMPTY}", true, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("To Job Name gets empty", Result.FAILURE, b.getResult());
        }
        
        // To Job Name gets blank.
        {
            JobcopyBuilder target = new JobcopyBuilder(project.getName(), "  ${EMPTY}  ", true, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("To Job Name gets blank", Result.FAILURE, b.getResult());
        }
        
        // From job does not exist.
        {
            JobcopyBuilder target = new JobcopyBuilder("nosuchjob", toJobName, true, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("From job does not exist", Result.FAILURE, b.getResult());
        }
        
        // From job(expanded) does not exist.
        {
            JobcopyBuilder target = new JobcopyBuilder("${NOSUCHJOB}", toJobName, true, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("From job(expanded) does not exist.", Result.FAILURE, b.getResult());
        }
        
        // To job exists, and not overwrite.
        {
            FreeStyleProject existJob = createFreeStyleProject("testPerformError2");
            
            JobcopyBuilder target = new JobcopyBuilder(project.getName(), existJob.getName(), false, new ArrayList<JobcopyOperation>());
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("To job exists, and not overwrite", Result.FAILURE, b.getResult());
        }
        
        // JobcopyOperation returned error.
        {
            List<JobcopyOperation> lst = new ArrayList<JobcopyOperation>();
            lst.add(new NullJobcopyOperation());
            JobcopyBuilder target = new JobcopyBuilder(project.getName(), toJobName, true, lst);
            project.getBuildersList().add(target);
            FreeStyleBuild b = project.scheduleBuild2(
                    project.getQuietPeriod(),
                    new Cause.UserIdCause(),
                    paramAction
            ).get();
            while(b.isBuilding())
            {
                Thread.sleep(100);
            }
            assertEquals("JobcopyOperation returned error.", Result.FAILURE, b.getResult());
        }
        
        // Failed to create a job
        // I have no idea to achieve this...
    }
    
    public void testView() throws IOException, SAXException
    {
        List<JobcopyOperation> lst = new ArrayList<JobcopyOperation>();
        lst.add(new EnableOperation());
        lst.add(new ReplaceOperation(
                "PARAM1", false,
                "PARAM2", false
        ));
        JobcopyBuilder target = new JobcopyBuilder("fromJob", "toJob", false, lst);
        
        FreeStyleProject project = createFreeStyleProject("testView1");
        project.getBuildersList().add(target);
        
        WebClient wc = new WebClient();
        wc.getPage(project, "configure");
    }
}
