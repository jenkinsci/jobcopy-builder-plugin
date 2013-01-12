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
import java.util.List;

import junit.framework.TestCase;

/**
 * Tests for JobcopyBuilder, not corresponded to Jenkins.
 */
public class JobcopyBuilderSimpleTest extends TestCase
{
    public void testJobcopyBuilder()
    {
        String fromJobName = "fromJob";
        String toJobName = "toJob";
        List<JobcopyOperation> emptyList = new ArrayList<JobcopyOperation>();
        List<JobcopyOperation> lst = new ArrayList<JobcopyOperation>();
        lst.add(new ReplaceOperation("from", false, "to", false));
        lst.add(new EnableOperation());
        
        // pattern1
        {
            JobcopyBuilder target = new JobcopyBuilder(
                    fromJobName,
                    toJobName,
                    true,
                    lst
                    );
            
            assertEquals(fromJobName, target.getFromJobName());
            assertEquals(toJobName, target.getToJobName());
            assertEquals(true, target.isOverwrite());
            assertEquals(lst, target.getJobcopyOperationList());
        }
        
        // pattern2
        {
            JobcopyBuilder target = new JobcopyBuilder(
                    "  " + fromJobName + " ",
                    " " + toJobName + "   ",
                    false,
                    emptyList);
            
            assertEquals(fromJobName, target.getFromJobName());
            assertEquals(toJobName, target.getToJobName());
            assertEquals(false, target.isOverwrite());
            assertEquals(emptyList, target.getJobcopyOperationList());
        }
        
        // pattern3
        {
            JobcopyBuilder target = new JobcopyBuilder(
                    null,
                    null,
                    true,
                    null
                    );
            
            assertNull(target.getFromJobName());
            assertNull(target.getToJobName());
            assertEquals(true, target.isOverwrite());
            assertNull(target.getJobcopyOperationList());
        }
    }
}
