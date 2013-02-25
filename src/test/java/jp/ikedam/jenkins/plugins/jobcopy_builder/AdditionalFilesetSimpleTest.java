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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import junit.framework.TestCase;

/**
 * Tests for AdditionalFileset not concerned with Jenkins.
 */
public class AdditionalFilesetSimpleTest extends TestCase
{
    public void testAdditionalFileset()
    {
        String includeFile = "**/config.xml";
        String excludeFile = "test/foobar/config.xml";
        List<JobcopyOperation> emptyList = new ArrayList<JobcopyOperation>();
        List<JobcopyOperation> lst = new ArrayList<JobcopyOperation>();
        lst.add(new ReplaceOperation("from", false, "to", false));
        lst.add(new EnableOperation());
        
        // simple values
        {
            AdditionalFileset target = new AdditionalFileset(
                    includeFile,
                    excludeFile,
                    false,
                    lst
                    );
            
            assertEquals("simple values", includeFile, target.getIncludeFile());
            assertEquals("simple values", excludeFile, target.getExcludeFile());
            assertEquals("simple values", false, target.isOverwrite());
            assertEquals("simple values", lst, target.getJobcopyOperationList());
        }
        // values surrounded with spaces
        {
            AdditionalFileset target = new AdditionalFileset(
                    "  " + includeFile + "  ",
                    "\t" + excludeFile + "\t",
                    true,
                    emptyList
                    );
            
            assertEquals("values surrounded with spaces", includeFile, target.getIncludeFile());
            assertEquals("values surrounded with spaces", excludeFile, target.getExcludeFile());
            assertEquals("values surrounded with spaces", true, target.isOverwrite());
            assertEquals("values surrounded with spaces", emptyList, target.getJobcopyOperationList());
        }
        
        // null
        {
            AdditionalFileset target = new AdditionalFileset(
                    null,
                    null,
                    false,
                    null
                    );
            
            assertNull("null", target.getIncludeFile());
            assertNull("null", target.getExcludeFile());
            assertNull("null", target.getJobcopyOperationList());
        }
    }
    
    public static class PublicAdditionalFileset extends AdditionalFileset
    {
        private static final long serialVersionUID = -8474841816751367916L;
        
        public PublicAdditionalFileset(
                String includeFile,
                String excludeFile,
                boolean overwrite,
                List<JobcopyOperation> jobcopyOperationList
        )
        {
            super(includeFile, excludeFile, overwrite, jobcopyOperationList);
        }

        public List<String> publicGetFilesToCopy(File dir)
        {
            return getFilesToCopy(dir);
        }
    }
    
    // mainly test that unexpected exception not thrown...
    public void testGetFilesToCopy() throws IOException
    {
        File tempFile = null;
        
        try
        {
            tempFile = File.createTempFile("test", null);
            tempFile.delete();
            tempFile.mkdir();
            
            FileUtils.writeStringToFile(new File(tempFile, "test1/test.txt"), "hogehoge");
            FileUtils.writeStringToFile(new File(tempFile, "test2/test.txt"), "hogehoge");
            FileUtils.writeStringToFile(new File(tempFile, "test3/test/test.txt"), "hogehoge");
            
            // only includes 1
            {
                List<String> files = new PublicAdditionalFileset("**/test.txt", null, false, null).publicGetFilesToCopy(tempFile);
                assertNotNull("only includes 1", files);
                assertEquals("only includes 1", 3, files.size());
            }
            
            // only includes 2
            {
                List<String> files = new PublicAdditionalFileset("**/test.txt", "", false, null).publicGetFilesToCopy(tempFile);
                assertNotNull("only includes 2", files);
                assertEquals("only includes 2", 3, files.size());
            }
            
            // no affect excludes
            {
                List<String> files = new PublicAdditionalFileset("**/test.txt", "**/nosuchfile.txt", false, null).publicGetFilesToCopy(tempFile);
                assertNotNull("no affect excludes", files);
                assertEquals("no affect excludes", 3, files.size());
            }
            
            // no files 1
            {
                List<String> files = new PublicAdditionalFileset("**/test.txt", "**/test.txt", false, null).publicGetFilesToCopy(tempFile);
                assertNotNull("no files 1", files);
                assertEquals("no files 1", 0, files.size());
            }
            
            // no files 2
            {
                List<String> files = new PublicAdditionalFileset("**/nosuchfile.txt", null, false, null).publicGetFilesToCopy(tempFile);
                assertNotNull("no files 2", files);
                assertEquals("no files 2", 0, files.size());
            }
            
            // no includes(null)
            {
                List<String> files = new PublicAdditionalFileset(null, null, false, null).publicGetFilesToCopy(tempFile);
                assertNotNull("no includes 1", files);
                assertEquals("no includes 1", 0, files.size());
            }
            
            // no includes(empty)
            {
                List<String> files = new PublicAdditionalFileset("", null, false, null).publicGetFilesToCopy(tempFile);
                assertNotNull("no includes 2", files);
                assertEquals("no includes 2", 0, files.size());
            }
            
            // no includes(only comma)
            {
                List<String> files = new PublicAdditionalFileset(",", null, false, null).publicGetFilesToCopy(tempFile);
                assertNotNull("no includes(only comma)", files);
                assertEquals("no includes(only comma)", 3, files.size());
            }
            
            // root
            {
                List<String> files = new PublicAdditionalFileset("/nosuchfile.txt", "/foobar.txt", false, null).publicGetFilesToCopy(tempFile);
                assertNotNull("root", files);
                assertEquals("root", 0, files.size());
            }
        }
        finally
        {
            if(tempFile != null && tempFile.exists())
            {
                if(tempFile.isDirectory())
                {
                    FileUtils.deleteDirectory(tempFile);
                }
                else
                {
                    tempFile.delete();
                }
            }
        }
    }
    
    public void testPerform()
    {
        // TODO
    }
    
    public void testPerformFile()
    {
        // TODO
    }
}
