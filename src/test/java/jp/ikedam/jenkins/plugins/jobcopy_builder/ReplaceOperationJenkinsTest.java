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

import hudson.util.FormValidation;

import org.jvnet.hudson.test.HudsonTestCase;

/**
 * Tests for ReplaceOperation corresponded to Jenkins.
 */
public class ReplaceOperationJenkinsTest extends HudsonTestCase
{
    private ReplaceOperation.DescriptorImpl getDescriptor()
    {
        return (ReplaceOperation.DescriptorImpl)(new ReplaceOperation(null, false, null, false)).getDescriptor();
    }
    
    public void testDescriptorDoCheckFromStr()
    {
        ReplaceOperation.DescriptorImpl descriptor = getDescriptor();
        
        // a value without expansion
        {
            assertEquals(
                    "a value without expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckFromStr("test", false).kind
                    );
        }
        
        // a value with expansion
        {
            assertEquals(
                    "a value with expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckFromStr("test", true).kind
                    );
        }
        
        // a value containing space without expansion
        {
            assertEquals(
                    "a value containing space without expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckFromStr("test value", false).kind
                    );
        }
        
        // a value with expansion
        {
            assertEquals(
                    "a value containing space with expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckFromStr("test value", true).kind
                    );
        }
        
        // variable without expansion
        {
            assertEquals(
                    "variable without expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckFromStr("${var1}", false).kind
                    );
        }
        
        // variable with expansion
        {
            assertEquals(
                    "a value containing space with expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckFromStr("${var1}", true).kind
                    );
        }
        
        // starts with blank without expansion
        {
            assertEquals(
                    "starts with blank without expansion",
                    FormValidation.Kind.WARNING,
                    descriptor.doCheckFromStr("  test", false).kind
                    );
        }
        
        // starts with blank with expansion
        {
            assertEquals(
                    "starts with blank with expansion",
                    FormValidation.Kind.WARNING,
                    descriptor.doCheckFromStr("  test", true).kind
                    );
        }
        
        // ends with blank without expansion
        {
            assertEquals(
                    "ends with blank without expansion",
                    FormValidation.Kind.WARNING,
                    descriptor.doCheckFromStr("test  ", false).kind
                    );
        }
        
        // ends with blank with expansion
        {
            assertEquals(
                    "ends with blank with expansion",
                    FormValidation.Kind.WARNING,
                    descriptor.doCheckFromStr("test ", true).kind
                    );
        }
        
        // surrounded with blank without expansion
        {
            assertEquals(
                    "surrounded with blank without expansion",
                    FormValidation.Kind.WARNING,
                    descriptor.doCheckFromStr("  test  ", false).kind
                    );
        }
        
        // surrounded with blank with expansion
        {
            assertEquals(
                    "surrounded with blank with expansion",
                    FormValidation.Kind.WARNING,
                    descriptor.doCheckFromStr("  test  ", true).kind
                    );
        }
        
        // blank without expansion
        {
            assertEquals(
                    "blank without expansion",
                    FormValidation.Kind.WARNING,
                    descriptor.doCheckFromStr("    ", false).kind
                    );
        }
        
        // blank with expansion
        {
            assertEquals(
                    "blank with expansion",
                    FormValidation.Kind.WARNING,
                    descriptor.doCheckFromStr("    ", true).kind
                    );
        }
        
        // null without expansion
        {
            assertEquals(
                    "null without expansion",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckFromStr(null, false).kind
                    );
        }
        
        // null with expansion
        {
            assertEquals(
                    "null with expansion",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckFromStr(null, true).kind
                    );
        }
        
        // empty without expansion
        {
            assertEquals(
                    "empty without expansion",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckFromStr("", false).kind
                    );
        }
        
        // empty with expansion
        {
            assertEquals(
                    "empty with expansion",
                    FormValidation.Kind.ERROR,
                    descriptor.doCheckFromStr("", true).kind
                    );
        }
    }
    
    
    public void testDescriptorDoCheckToStr()
    {
        ReplaceOperation.DescriptorImpl descriptor = getDescriptor();
        
        // a value without expansion
        {
            assertEquals(
                    "a value without expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("test", false).kind
                    );
        }
        
        // a value with expansion
        {
            assertEquals(
                    "a value with expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("test", true).kind
                    );
        }
        
        // a value containing space without expansion
        {
            assertEquals(
                    "a value containing space without expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("test value", false).kind
                    );
        }
        
        // a value with expansion
        {
            assertEquals(
                    "a value containing space with expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("test value", true).kind
                    );
        }
        
        // variable without expansion
        {
            assertEquals(
                    "variable without expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("${var1}", false).kind
                    );
        }
        
        // variable with expansion
        {
            assertEquals(
                    "a value containing space with expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("${var1}", true).kind
                    );
        }
        
        // starts with blank without expansion
        {
            assertEquals(
                    "starts with blank without expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("  test", false).kind
                    );
        }
        
        // starts with blank with expansion
        {
            assertEquals(
                    "starts with blank with expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("  test", true).kind
                    );
        }
        
        // ends with blank without expansion
        {
            assertEquals(
                    "ends with blank without expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("test  ", false).kind
                    );
        }
        
        // ends with blank with expansion
        {
            assertEquals(
                    "ends with blank with expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("test ", true).kind
                    );
        }
        
        // surrounded with blank without expansion
        {
            assertEquals(
                    "surrounded with blank without expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("  test  ", false).kind
                    );
        }
        
        // surrounded with blank with expansion
        {
            assertEquals(
                    "surrounded with blank with expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("  test  ", true).kind
                    );
        }
        
        // blank without expansion
        {
            assertEquals(
                    "blank without expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("    ", false).kind
                    );
        }
        
        // blank with expansion
        {
            assertEquals(
                    "blank with expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("    ", true).kind
                    );
        }
        
        // null without expansion
        {
            assertEquals(
                    "null without expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr(null, false).kind
                    );
        }
        
        // null with expansion
        {
            assertEquals(
                    "null with expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr(null, true).kind
                    );
        }
        
        // empty without expansion
        {
            assertEquals(
                    "empty without expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("", false).kind
                    );
        }
        
        // empty with expansion
        {
            assertEquals(
                    "empty with expansion",
                    FormValidation.Kind.OK,
                    descriptor.doCheckToStr("", true).kind
                    );
        }
    }
}
