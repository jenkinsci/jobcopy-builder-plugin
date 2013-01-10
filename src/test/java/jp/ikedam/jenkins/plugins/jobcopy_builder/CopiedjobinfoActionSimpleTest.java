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

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.acegisecurity.AccessDeniedException;

import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.TopLevelItem;
import hudson.model.TopLevelItemDescriptor;
import hudson.model.Job;
import hudson.search.SearchIndex;
import hudson.search.Search;
import hudson.security.ACL;
import hudson.security.Permission;
import junit.framework.TestCase;

/**
 * Tests for CopiedjobinfoAction, not concerned with Jenkins
 *
 */
public class CopiedjobinfoActionSimpleTest extends TestCase
{
    public void testCopiedjobinfoAction()
    {
        String fromItemName = "fromItemName";
        String fromItemUrl = "URL for fromItem";
        String toItemName = "toItemName";
        String toItemUrl = "URL for toItem";
        
        TopLevelItem fromItem = new DummyJob(fromItemName, fromItemUrl);
        TopLevelItem toItem = new DummyJob(toItemName, toItemUrl);
        CopiedjobinfoAction target = new CopiedjobinfoAction(
                fromItem,
                toItem
        );
        
        assertEquals(fromItemName, target.getFromJobName());
        assertEquals(fromItemUrl,  target.getFromUrl());
        assertEquals(toItemName,   target.getToJobName());
        assertEquals(toItemUrl,    target.getToUrl());
    }
    
    private static class DummyJob implements TopLevelItem
    {
        private String name;
        private String url;
        
        public DummyJob(String name, String url)
        {
            this.name = name;
            this.url = url;
        }
        
        @Override
        public String getName()
        {
            return name;
        }
        
        @Override
        public String getUrl()
        {
            return url;
        }
        
        @Override
        public ItemGroup<? extends Item> getParent()
        {
            return null;
        }
        
        @SuppressWarnings("rawtypes")
        @Override
        public Collection<? extends Job> getAllJobs()
        {
            return null;
        }
        
        @Override
        public String getFullName()
        {
            return null;
        }
        
        @Override
        public String getDisplayName()
        {
            return null;
        }
        
        @Override
        public String getFullDisplayName()
        {
            return null;
        }
        
        @Override
        public String getRelativeNameFrom(@SuppressWarnings("rawtypes") ItemGroup g)
        {
            return null;
        }
        
        @Override
        public String getRelativeNameFrom(Item item)
        {
            return null;
        }
        
        @Override
        public String getShortUrl()
        {
            return null;
        }
        
        @Override
        public String getAbsoluteUrl()
        {
            return null;
        }
        
        @Override
        public void onLoad(ItemGroup<? extends Item> parent, String name)
                throws IOException
        {
        }
        
        @Override
        public void onCopiedFrom(Item src)
        {
        }
        
        @Override
        public void onCreatedFromScratch()
        {
        }
        
        @Override
        public void save() throws IOException
        {
        }
        
        @Override
        public void delete() throws IOException, InterruptedException
        {
        }
        
        @Override
        public File getRootDir()
        {
            return null;
        }
        
        @Override
        public Search getSearch()
        {
            return null;
        }
        
        @Override
        public String getSearchName()
        {
            return null;
        }
        
        @Override
        public String getSearchUrl()
        {
            return null;
        }
        
        @Override
        public SearchIndex getSearchIndex()
        {
            return null;
        }
        
        @Override
        public ACL getACL()
        {
            return null;
        }
        
        @Override
        public void checkPermission(Permission permission)
                throws AccessDeniedException
        {
        }
        
        @Override
        public boolean hasPermission(Permission permission)
        {
            return false;
        }
        
        @Override
        public TopLevelItemDescriptor getDescriptor()
        {
            return null;
        }
    }
}
