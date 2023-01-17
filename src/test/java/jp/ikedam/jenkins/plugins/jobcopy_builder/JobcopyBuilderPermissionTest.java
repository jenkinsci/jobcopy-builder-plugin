/*
 * The MIT License
 * 
 * Copyright (c) 2016 IKEDA Yasuyuki
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

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.acegisecurity.Authentication;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.MockAuthorizationStrategy;
import org.jvnet.hudson.test.MockQueueItemAuthenticator;

import hudson.model.Computer;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.model.Label;
import hudson.model.Result;
import hudson.model.User;
import jenkins.model.Jenkins;
import jenkins.security.QueueItemAuthenticatorConfiguration;

/**
 * Tests for permission handling.
 */
public class JobcopyBuilderPermissionTest
{
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    @Test
    public void testSystemSucceedCreate() throws Exception
    {
        // src: can read by anonymous
        // dest: can create by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            "dest",
            false,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to(Jenkins.ANONYMOUS.getName())
                .grant(Item.CREATE).onRoot().to(Jenkins.ANONYMOUS.getName())
                .grant(Item.READ).onItems(src).to(Jenkins.ANONYMOUS.getName())
                .grant(Item.EXTENDED_READ).onItems(src).to(Jenkins.ANONYMOUS.getName())
        );
        
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        
        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNotNull(dest);
        assertEquals("test", dest.getAssignedLabelString());
    }
    
    @Test
    public void testSystemSucceedOverwrite() throws Exception
    {
        // src: can read by anonymous
        // dest: can configure by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject dest = j.createFreeStyleProject();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            dest.getFullName(),
            true,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to(Jenkins.ANONYMOUS.getName())
                // .grant(Item.CREATE).onRoot().to(Jenkins.ANONYMOUS.getName())
                .grant(Item.READ).onItems(src).to(Jenkins.ANONYMOUS.getName())
                .grant(Item.EXTENDED_READ).onItems(src).to(Jenkins.ANONYMOUS.getName())
                .grant(Item.READ).onItems(dest).to(Jenkins.ANONYMOUS.getName())
                .grant(Item.CONFIGURE).onItems(dest).to(Jenkins.ANONYMOUS.getName())
        );
        
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        
        dest = j.jenkins.getItemByFullName(dest.getFullName(), FreeStyleProject.class);
        assertEquals("test", dest.getAssignedLabelString());
    }
    
    /**
     * If a user has CREATE permission but READ permission,
     * succeeds to overwrite as creating.
     * This happens also when you call createItem REST api.
     */
    @Test
    public void testSystemSucceedOverwriteAsCreate() throws Exception
    {
        // src: can read by anonymous
        // dest: cannot read by anonymous, bu can create by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject dest = j.createFreeStyleProject();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            dest.getFullName(),
            true,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to(Jenkins.ANONYMOUS.getName())
                .grant(Item.CREATE).onRoot().to(Jenkins.ANONYMOUS.getName())
                .grant(Item.READ).onItems(src).to(Jenkins.ANONYMOUS.getName())
                .grant(Item.EXTENDED_READ).onItems(src).to(Jenkins.ANONYMOUS.getName())
                .grant(Item.READ).onItems(dest).to("user1") // not by anonymous
        );
        
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        
        dest = j.jenkins.getItemByFullName(dest.getFullName(), FreeStyleProject.class);
        assertEquals("test", dest.getAssignedLabelString());
    }
    
    @Test
    public void testSystemFailToReadForReadPermission() throws Exception
    {
        // src: cannot read by anonymous
        // dest: can create by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            "dest",
            false,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to(Jenkins.ANONYMOUS.getName())
                .grant(Item.CREATE).onRoot().to(Jenkins.ANONYMOUS.getName())
                .grant(Item.READ).onItems(src).to("user1")  // not by anonymous!
                .grant(Item.EXTENDED_READ).onItems(src).to(Jenkins.ANONYMOUS.getName())
        );
        
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        
        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNull(dest);
    }
    
    @Test
    public void testSystemFailToReadForExtendedReadPermission() throws Exception
    {
        // src: cannot read by anonymous
        // dest: can create by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            "dest",
            false,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to(Jenkins.ANONYMOUS.getName())
                .grant(Item.CREATE).onRoot().to(Jenkins.ANONYMOUS.getName())
                .grant(Item.READ).onItems(src).to(Jenkins.ANONYMOUS.getName())
                .grant(Item.EXTENDED_READ).onItems(src).to("user1") // not by anonymous!
        );
        
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        
        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNull(dest);
    }
    
    @Test
    public void testSystemFailToCreate() throws Exception
    {
        // src: can read by anonymous
        // dest: cannot create by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            "dest",
            false,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to(Jenkins.ANONYMOUS.getName())
                // .grant(Item.CREATE).onRoot().to(Jenkins.ANONYMOUS.getName())
                .grant(Item.READ).onItems(src).to(Jenkins.ANONYMOUS.getName())
                .grant(Item.EXTENDED_READ).onItems(src).to(Jenkins.ANONYMOUS.getName())
        );
        
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        
        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNull(dest);
    }
    
    @Test
    public void testSystemFailToOverwrite() throws Exception
    {
        // src: can read by anonymous
        // dest: cannot configure by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject dest = j.createFreeStyleProject();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            dest.getFullName(),
            true,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to(Jenkins.ANONYMOUS.getName())
                // .grant(Item.CREATE).onRoot().to(Jenkins.ANONYMOUS.getName())
                .grant(Item.READ).onItems(src).to(Jenkins.ANONYMOUS.getName())
                .grant(Item.EXTENDED_READ).onItems(src).to(Jenkins.ANONYMOUS.getName())
                .grant(Item.READ).onItems(dest).to(Jenkins.ANONYMOUS.getName())
                .grant(Item.CONFIGURE).onItems(dest).to("user1")    // not by anonymous!
        );
        
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        
        dest = j.jenkins.getItemByFullName(dest.getFullName(), FreeStyleProject.class);
        assertNotEquals("test", dest.getAssignedLabelString());
    }
    
    @Test
    public void testUserSucceedCreate() throws Exception
    {
        // src: can read by user1
        // dest: can create by user1
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            "dest",
            false,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to("user1")
                .grant(Item.CREATE).onRoot().to("user1")
                .grant(Computer.BUILD).onRoot().to("user1")
                .grant(Item.READ).onItems(src).to("user1")
                .grant(Item.EXTENDED_READ).onItems(src).to("user1")
        );
        
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        Map<String, Authentication> jobsToUsers = new HashMap<>();
        jobsToUsers.put(p.getFullName(), User.get("user1").impersonate());
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
            new MockQueueItemAuthenticator(jobsToUsers)
        );
        
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        
        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNotNull(dest);
        assertEquals("test", dest.getAssignedLabelString());
    }
    
    @Test
    public void testUserSucceedOverwrite() throws Exception
    {
        // src: can read by user1
        // dest: can configure by user1
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject dest = j.createFreeStyleProject();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            dest.getFullName(),
            true,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to("user1")
                // .grant(Item.CREATE).onRoot().to("user1")
                .grant(Computer.BUILD).onRoot().to("user1")
                .grant(Item.READ).onItems(src).to("user1")
                .grant(Item.EXTENDED_READ).onItems(src).to("user1")
                .grant(Item.READ).onItems(dest).to("user1")
                .grant(Item.CONFIGURE).onItems(dest).to("user1")
        );
        
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        Map<String, Authentication> jobsToUsers = new HashMap<>();
        jobsToUsers.put(p.getFullName(), User.get("user1").impersonate());
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
            new MockQueueItemAuthenticator(jobsToUsers)
        );
        
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        
        dest = j.jenkins.getItemByFullName(dest.getFullName(), FreeStyleProject.class);
        assertEquals("test", dest.getAssignedLabelString());
    }
    
    /**
     * If a user has CREATE permission but READ permission,
     * succeeds to overwrite as creating.
     * This happens also when you call createItem REST api.
     */
    @Test
    public void testUserSucceedOverwriteAsCreate() throws Exception
    {
        // src: can read by user1
        // dest: can configure by user1
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject dest = j.createFreeStyleProject();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            dest.getFullName(),
            true,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to("user1")
                .grant(Item.CREATE).onRoot().to("user1")
                .grant(Computer.BUILD).onRoot().to("user1")
                .grant(Item.READ).onItems(src).to("user1")
                .grant(Item.EXTENDED_READ).onItems(src).to("user1")
                .grant(Item.READ).onItems(dest).to("user2") // not by user1
        );
        
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        Map<String, Authentication> jobsToUsers = new HashMap<>();
        jobsToUsers.put(p.getFullName(), User.get("user1").impersonate());
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
            new MockQueueItemAuthenticator(jobsToUsers)
        );
        
        j.assertBuildStatusSuccess(p.scheduleBuild2(0));
        
        dest = j.jenkins.getItemByFullName(dest.getFullName(), FreeStyleProject.class);
        assertEquals("test", dest.getAssignedLabelString());
    }
    
    @Test
    public void testUserFailToReadForReadPermission() throws Exception
    {
        // src: cannot read by user1
        // dest: can create by user1
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            "dest",
            false,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to("user1")
                .grant(Item.CREATE).onRoot().to("user1")
                .grant(Computer.BUILD).onRoot().to("user1")
                .grant(Item.READ).onItems(src).to("user2")  // not by user1!
                .grant(Item.EXTENDED_READ).onItems(src).to("user1")
        );
        
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        Map<String, Authentication> jobsToUsers = new HashMap<>();
        jobsToUsers.put(p.getFullName(), User.get("user1").impersonate());
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
            new MockQueueItemAuthenticator(jobsToUsers)
        );
        
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        
        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNull(dest);
    }
    
    @Test
    public void testUserFailToReadForExtendedReadPermission() throws Exception
    {
        // src: cannot read by user1
        // dest: can create by user1
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            "dest",
            false,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to("user1")
                .grant(Item.CREATE).onRoot().to("user1")
                .grant(Computer.BUILD).onRoot().to("user1")
                .grant(Item.READ).onItems(src).to("user1")
                .grant(Item.EXTENDED_READ).onItems(src).to("user2") // not by user1!
        );
        
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        Map<String, Authentication> jobsToUsers = new HashMap<>();
        jobsToUsers.put(p.getFullName(), User.get("user1").impersonate());
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
            new MockQueueItemAuthenticator(jobsToUsers)
        );
        
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        
        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNull(dest);
    }
    
    @Test
    public void testUserFailToCreate() throws Exception
    {
        // src: can read by user1
        // dest: cannot create by user1
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            "dest",
            false,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to("user1")
                // .grant(Item.CREATE).onRoot().to("user1")
                .grant(Computer.BUILD).onRoot().to("user1")
                .grant(Item.READ).onItems(src).to("user1")
                .grant(Item.EXTENDED_READ).onItems(src).to("user1")
        );
        
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        Map<String, Authentication> jobsToUsers = new HashMap<>();
        jobsToUsers.put(p.getFullName(), User.get("user1").impersonate());
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
            new MockQueueItemAuthenticator(jobsToUsers)
        );
        
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        
        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNull(dest);
    }
    
    @Test
    public void testUserFailToOverwrite() throws Exception
    {
        // src: can read by user1
        // dest: cannot configure by user1
        FreeStyleProject src = j.createFreeStyleProject();
        src.setAssignedLabel(Label.get("test"));
        src.save();
        
        FreeStyleProject dest = j.createFreeStyleProject();
        
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
            src.getFullName(),
            dest.getFullName(),
            true,
            Collections.<JobcopyOperation>emptyList(),
            Collections.<AdditionalFileset>emptyList()
        ));
        
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(
            new MockAuthorizationStrategy()
                .grant(Jenkins.READ).onRoot().to("user1")
                // .grant(Item.CREATE).onRoot().to("user1")
                .grant(Computer.BUILD).onRoot().to("user1")
                .grant(Item.READ).onItems(src).to("user1")
                .grant(Item.EXTENDED_READ).onItems(src).to("user1")
                .grant(Item.READ).onItems(dest).to("user1")
                .grant(Item.CONFIGURE).onItems(dest).to("user2")    // not by user1!
        );
        
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        Map<String, Authentication> jobsToUsers = new HashMap<>();
        jobsToUsers.put(p.getFullName(), User.get("user1").impersonate());
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
            new MockQueueItemAuthenticator(jobsToUsers)
        );
        
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
        
        dest = j.jenkins.getItemByFullName(dest.getFullName(), FreeStyleProject.class);
        assertNotEquals("test", dest.getAssignedLabelString());
    }
}
