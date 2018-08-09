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

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import hudson.model.Cause;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import org.jvnet.hudson.test.HudsonTestCase;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Tests for CopiedjobinfoAction, corresponded to Jenkins.
 */
public class CopiedjobinfoActionJenkinsTest extends HudsonTestCase {
    /**
     * Test that summary.jelly does not fail.
     *
     * @throws IOException
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws SAXException
     */
    public void testView() throws IOException, InterruptedException, ExecutionException, SAXException {
        // Create jobs to be shown.
        FreeStyleProject fromJob = createFreeStyleProject();
        FreeStyleProject toJob = createFreeStyleProject();
        String fromJobUrl = fromJob.getUrl();
        String toJobUrl = toJob.getUrl();

        // Create job, and create a build.
        FreeStyleProject job = createFreeStyleProject();
        CopiedjobinfoAction action = new CopiedjobinfoAction(fromJob, toJob, false);
        FreeStyleBuild build = job.scheduleBuild2(job.getQuietPeriod(), new Cause.UserIdCause(), action).get();

        // Wait for build is completed.
        while (build.isBuilding()) {
            Thread.sleep(100);
        }


        // Create a failed build.
        CopiedjobinfoAction failedAction = new CopiedjobinfoAction(fromJob, toJob, true);
        FreeStyleBuild failedBuild = job.scheduleBuild2(job.getQuietPeriod(), new Cause.UserIdCause(), failedAction).get();

        // Wait for build is completed.
        while (failedBuild.isBuilding()) {
            Thread.sleep(100);
        }

        // Access to page.
        WebClient wc = new WebClient();

        // access to succeeded build.
        {
            HtmlPage page = wc.getPage(build);

            // contains link to from job.
            {
                List<Object> nodes = page.getByXPath(String.format("//a[%s]", getEndsWithXpath("@href", fromJobUrl)));
                assertNotNull(nodes);
                assertTrue(nodes.size() > 0);
            }
            // contains link to to job.
            {
                List<Object> nodes = page.getByXPath(String.format("//a[%s]", getEndsWithXpath("@href", toJobUrl)));
                assertNotNull(nodes);
                assertTrue(nodes.size() > 0);
            }

            // does not contains warning message
            {
                List<Object> nodes = page.getByXPath("//*[@class='warning']");
                assertNotNull(nodes);
                assertEquals(0, nodes.size());
            }
        }

        // access to failed build.
        {
            HtmlPage page = wc.getPage(failedBuild);

            // contains warning message
            {
                List<Object> nodes = page.getByXPath("//*[@class='warning']");
                assertNotNull(nodes);
                assertTrue(nodes.size() > 0);
            }
        }

        // it works even if the jobs are removed.
        fromJob.delete();
        toJob.delete();
        {
            HtmlPage page = wc.getPage(build);

            // contains link to from job.
            {
                List<Object> nodes = page.getByXPath(String.format("//a[%s]", getEndsWithXpath("@href", fromJobUrl)));
                assertNotNull(nodes);
                assertTrue(nodes.size() > 0);
            }
            // contains link to to job.
            {
                List<Object> nodes = page.getByXPath(String.format("//a[%s]", getEndsWithXpath("@href", toJobUrl)));
                assertNotNull(nodes);
                assertTrue(nodes.size() > 0);
            }
        }
    }

    // Xpath 1.0 does not support ends-with, so do same with other functions.
    private String getEndsWithXpath(String nodeExp, String value) {
        return String.format("substring(%s, string-length(%s) - string-length('%s') + 1) = '%s'", nodeExp, nodeExp, value, value);
    }
}
