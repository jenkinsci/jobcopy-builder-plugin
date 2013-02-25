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

import hudson.util.FormValidation;

import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Tests for AdditionalFileset concerned with Jenkins
 */
public class AdditionalFilesetJenkinsTest extends HudsonTestCase
{
    private AdditionalFileset.DescriptorImpl getDescriptor()
    {
        return (AdditionalFileset.DescriptorImpl)(new AdditionalFileset(null, null, false, null)).getDescriptor();
    }
    
    public void testDescriptorDoCheckIncludeFileOk()
    {
        AdditionalFileset.DescriptorImpl descriptor = getDescriptor();
        
        // Simple value
        {
            String value = "**/config.xml";
            assertEquals("Simple value",
                    FormValidation.Kind.OK,
                    descriptor.doCheckIncludeFile(value).kind
                    );
        }
        
        // Multiple values
        {
            String value = "**/config.xml, */additional.xml";
            assertEquals("Multiple values",
                    FormValidation.Kind.OK,
                    descriptor.doCheckIncludeFile(value).kind
                    );
        }
        
        // Surrounded with spaces
        {
            String value = "  **/config.xml, */additional.xml  ";
            assertEquals("Surrounded with spaces",
                    FormValidation.Kind.OK,
                    descriptor.doCheckIncludeFile(value).kind
                    );
        }
    }
    
    public void testDescriptorDoCheckIncludeFileError()
    {
        AdditionalFileset.DescriptorImpl descriptor = getDescriptor();
        
        // empty
        {
            String value = "  ";
            assertEquals("empty",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckIncludeFile(value).kind
                    );
        }
        
        // null
        {
            String value = null;
            assertEquals(null,
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckIncludeFile(value).kind
                    );
        }
    }
}
