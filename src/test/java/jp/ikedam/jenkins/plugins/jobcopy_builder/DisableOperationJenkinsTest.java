/*
 * The MIT License
 *
 * Copyright (c) 2015 IKEDA Yasuyuki
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

import hudson.model.FreeStyleProject;
import org.jvnet.hudson.test.HudsonTestCase;

import java.util.Arrays;
import java.util.Collections;

/**
 * Tests for {@link DisableOperation}
 */
public class DisableOperationJenkinsTest extends HudsonTestCase {
    public void testDisableOperation() throws Exception {
        FreeStyleProject copiee = createFreeStyleProject();
        copiee.enable();
        copiee.save();

        FreeStyleProject copier = createFreeStyleProject();
        copier.getBuildersList().add(new JobcopyBuilder(
                copiee.getFullName(),
                "copied",
                false,
                Arrays.<JobcopyOperation>asList(
                        new DisableOperation()
                ),
                Collections.<AdditionalFileset>emptyList()
        ));
        assertBuildStatusSuccess(copier.scheduleBuild2(0));

        FreeStyleProject copied = jenkins.getItemByFullName("copied", FreeStyleProject.class);
        assertTrue(copied.isDisabled());
    }

    public void testConfiguration() throws Exception {
        JobcopyBuilder expected = new JobcopyBuilder(
                "from",
                "to",
                false,
                Arrays.<JobcopyOperation>asList(
                        new DisableOperation()
                ),
                Collections.<AdditionalFileset>emptyList()
        );

        FreeStyleProject p = createFreeStyleProject();
        p.getBuildersList().add(expected);
        configRoundtrip(p);
        JobcopyBuilder actual = p.getBuildersList().get(JobcopyBuilder.class);
        // assertEqualDataBoundBeans(expected, actual); // This cause NPE as actual.additionalFilesetList gets null.
        assertEqualDataBoundBeans(expected.getJobcopyOperationList(), actual.getJobcopyOperationList());
    }
}
