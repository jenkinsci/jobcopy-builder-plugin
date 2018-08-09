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

import com.google.common.collect.Sets;
import hudson.model.*;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.Permission;
import hudson.security.ProjectMatrixAuthorizationStrategy;
import jenkins.model.Jenkins;
import jenkins.security.QueueItemAuthenticator;
import jenkins.security.QueueItemAuthenticatorConfiguration;
import org.acegisecurity.Authentication;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Tests for permission handling.
 */
public class JobcopyBuilderPermissionTest {
    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testSystemSucceedCreate() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, Jenkins.ANONYMOUS.getName());
        pmas.add(Item.CREATE, Jenkins.ANONYMOUS.getName());
        j.jenkins.setAuthorizationStrategy(pmas);

        // src: can read by anonymous
        // dest: can create by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            auths.put(Item.EXTENDED_READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
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

        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNotNull(dest);
        assertEquals("test", dest.getAssignedLabelString());
    }

    @Test
    public void testSystemSucceedOverwrite() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, Jenkins.ANONYMOUS.getName());
        j.jenkins.setAuthorizationStrategy(pmas);

        // src: can read by anonymous
        // dest: can configure by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            auths.put(Item.EXTENDED_READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
        src.setAssignedLabel(Label.get("test"));
        src.save();

        FreeStyleProject dest = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            auths.put(Item.CONFIGURE, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            dest.addProperty(new AuthorizationMatrixProperty(auths));
        }

        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
                src.getFullName(),
                dest.getFullName(),
                true,
                Collections.<JobcopyOperation>emptyList(),
                Collections.<AdditionalFileset>emptyList()
        ));

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
    public void testSystemSucceedOverwriteAsCreate() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, Jenkins.ANONYMOUS.getName());
        pmas.add(Item.CREATE, Jenkins.ANONYMOUS.getName());
        j.jenkins.setAuthorizationStrategy(pmas);

        // src: can read by anonymous
        // dest: cannot read by anonymous, bu can create by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            auths.put(Item.EXTENDED_READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
        src.setAssignedLabel(Label.get("test"));
        src.save();

        FreeStyleProject dest = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet("user1"));     // not by anonymous
            dest.addProperty(new AuthorizationMatrixProperty(auths));
        }

        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
                src.getFullName(),
                dest.getFullName(),
                true,
                Collections.<JobcopyOperation>emptyList(),
                Collections.<AdditionalFileset>emptyList()
        ));

        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        dest = j.jenkins.getItemByFullName(dest.getFullName(), FreeStyleProject.class);
        assertEquals("test", dest.getAssignedLabelString());
    }

    @Test
    public void testSystemFailToReadForReadPermission() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, Jenkins.ANONYMOUS.getName());
        pmas.add(Item.CREATE, Jenkins.ANONYMOUS.getName());
        j.jenkins.setAuthorizationStrategy(pmas);

        // src: cannot read by anonymous
        // dest: can create by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet("user1"));    // not by anonymous!
            auths.put(Item.EXTENDED_READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
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

        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());

        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNull(dest);
    }

    @Test
    public void testSystemFailToReadForExtendedReadPermission() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, Jenkins.ANONYMOUS.getName());
        pmas.add(Item.CREATE, Jenkins.ANONYMOUS.getName());
        j.jenkins.setAuthorizationStrategy(pmas);

        // src: cannot read by anonymous
        // dest: can create by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            auths.put(Item.EXTENDED_READ, Sets.newHashSet("user1"));    // not by anonymous!
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
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

        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());

        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNull(dest);
    }

    @Test
    public void testSystemFailToCreate() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, Jenkins.ANONYMOUS.getName());
        // pmas.add(Item.CREATE, Jenkins.ANONYMOUS.getName());
        j.jenkins.setAuthorizationStrategy(pmas);

        // src: can read by anonymous
        // dest: cannot create by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            auths.put(Item.EXTENDED_READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
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

        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());

        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNull(dest);
    }

    @Test
    public void testSystemFailToOverwrite() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, Jenkins.ANONYMOUS.getName());
        j.jenkins.setAuthorizationStrategy(pmas);

        // src: can read by anonymous
        // dest: cannot configure by anonymous
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            auths.put(Item.EXTENDED_READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
        src.setAssignedLabel(Label.get("test"));
        src.save();

        FreeStyleProject dest = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet(Jenkins.ANONYMOUS.getName()));
            auths.put(Item.CONFIGURE, Sets.newHashSet("uset1"));        // not by anonymous!
            dest.addProperty(new AuthorizationMatrixProperty(auths));
        }

        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
                src.getFullName(),
                dest.getFullName(),
                true,
                Collections.<JobcopyOperation>emptyList(),
                Collections.<AdditionalFileset>emptyList()
        ));

        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());

        dest = j.jenkins.getItemByFullName(dest.getFullName(), FreeStyleProject.class);
        assertNotEquals("test", dest.getAssignedLabelString());
    }

    @Test
    public void testUserSucceedCreate() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, "user1");
        pmas.add(Item.CREATE, "user1");
        pmas.add(Computer.BUILD, "user1");
        j.jenkins.setAuthorizationStrategy(pmas);

        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
                new SpecificUserQueueItemAuthenticator(User.get("user1").impersonate())
        );

        // src: can read by user1
        // dest: can create by user1
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet("user1"));
            auths.put(Item.EXTENDED_READ, Sets.newHashSet("user1"));
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
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

        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNotNull(dest);
        assertEquals("test", dest.getAssignedLabelString());
    }

    @Test
    public void testUserSucceedOverwrite() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, "user1");
        pmas.add(Computer.BUILD, "user1");
        j.jenkins.setAuthorizationStrategy(pmas);

        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
                new SpecificUserQueueItemAuthenticator(User.get("user1").impersonate())
        );

        // src: can read by user1
        // dest: can configure by user1
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet("user1"));
            auths.put(Item.EXTENDED_READ, Sets.newHashSet("user1"));
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
        src.setAssignedLabel(Label.get("test"));
        src.save();

        FreeStyleProject dest = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet("user1"));
            auths.put(Item.CONFIGURE, Sets.newHashSet("user1"));
            dest.addProperty(new AuthorizationMatrixProperty(auths));
        }

        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
                src.getFullName(),
                dest.getFullName(),
                true,
                Collections.<JobcopyOperation>emptyList(),
                Collections.<AdditionalFileset>emptyList()
        ));

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
    public void testUserSucceedOverwriteAsCreate() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, "user1");
        pmas.add(Item.CREATE, "user1");
        pmas.add(Computer.BUILD, "user1");
        j.jenkins.setAuthorizationStrategy(pmas);

        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
                new SpecificUserQueueItemAuthenticator(User.get("user1").impersonate())
        );

        // src: can read by user1
        // dest: can configure by user1
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet("user1"));
            auths.put(Item.EXTENDED_READ, Sets.newHashSet("user1"));
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
        src.setAssignedLabel(Label.get("test"));
        src.save();

        FreeStyleProject dest = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet("user2"));     // not by user1
            dest.addProperty(new AuthorizationMatrixProperty(auths));
        }

        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
                src.getFullName(),
                dest.getFullName(),
                true,
                Collections.<JobcopyOperation>emptyList(),
                Collections.<AdditionalFileset>emptyList()
        ));

        j.assertBuildStatusSuccess(p.scheduleBuild2(0));

        dest = j.jenkins.getItemByFullName(dest.getFullName(), FreeStyleProject.class);
        assertEquals("test", dest.getAssignedLabelString());
    }

    @Test
    public void testUserFailToReadForReadPermission() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, "user1");
        pmas.add(Item.CREATE, "user1");
        pmas.add(Computer.BUILD, "user1");
        j.jenkins.setAuthorizationStrategy(pmas);

        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
                new SpecificUserQueueItemAuthenticator(User.get("user1").impersonate())
        );

        // src: cannot read by user1
        // dest: can create by user1
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet("user2"));    // not by user1!
            auths.put(Item.EXTENDED_READ, Sets.newHashSet("user1"));
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
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

        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());

        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNull(dest);
    }

    @Test
    public void testUserFailToReadForExtendedReadPermission() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, "user1");
        pmas.add(Item.CREATE, "user1");
        pmas.add(Computer.BUILD, "user1");
        j.jenkins.setAuthorizationStrategy(pmas);

        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
                new SpecificUserQueueItemAuthenticator(User.get("user1").impersonate())
        );

        // src: cannot read by user1
        // dest: can create by user1
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet("user1"));
            auths.put(Item.EXTENDED_READ, Sets.newHashSet("user2"));    // not by user1!
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
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

        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());

        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNull(dest);
    }

    @Test
    public void testUserFailToCreate() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, "user1");
        // pmas.add(Item.CREATE, "user1");
        pmas.add(Computer.BUILD, "user1");
        j.jenkins.setAuthorizationStrategy(pmas);

        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
                new SpecificUserQueueItemAuthenticator(User.get("user1").impersonate())
        );

        // src: can read by user1
        // dest: cannot create by user1
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet("user1"));
            auths.put(Item.EXTENDED_READ, Sets.newHashSet("user1"));
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
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

        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());

        FreeStyleProject dest = j.jenkins.getItemByFullName("dest", FreeStyleProject.class);
        assertNull(dest);
    }

    @Test
    public void testUserFailToOverwrite() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        ProjectMatrixAuthorizationStrategy pmas = new ProjectMatrixAuthorizationStrategy();
        pmas.add(Jenkins.READ, "user1");
        pmas.add(Computer.BUILD, "user1");
        j.jenkins.setAuthorizationStrategy(pmas);

        QueueItemAuthenticatorConfiguration.get().getAuthenticators().clear();
        QueueItemAuthenticatorConfiguration.get().getAuthenticators().add(
                new SpecificUserQueueItemAuthenticator(User.get("user1").impersonate())
        );

        // src: can read by user1
        // dest: cannot configure by user1
        FreeStyleProject src = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet("user1"));
            auths.put(Item.EXTENDED_READ, Sets.newHashSet("user1"));
            src.addProperty(new AuthorizationMatrixProperty(auths));
        }
        src.setAssignedLabel(Label.get("test"));
        src.save();

        FreeStyleProject dest = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> auths = new HashMap<Permission, Set<String>>();
            auths.put(Item.READ, Sets.newHashSet("user1"));
            auths.put(Item.CONFIGURE, Sets.newHashSet("uset2"));        // not by user1!
            dest.addProperty(new AuthorizationMatrixProperty(auths));
        }

        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new JobcopyBuilder(
                src.getFullName(),
                dest.getFullName(),
                true,
                Collections.<JobcopyOperation>emptyList(),
                Collections.<AdditionalFileset>emptyList()
        ));

        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());

        dest = j.jenkins.getItemByFullName(dest.getFullName(), FreeStyleProject.class);
        assertNotEquals("test", dest.getAssignedLabelString());
    }

    public static class SpecificUserQueueItemAuthenticator extends QueueItemAuthenticator {
        private final Authentication auth;

        public SpecificUserQueueItemAuthenticator(Authentication auth) {
            this.auth = auth;
        }

        @Override
        public Authentication authenticate(Queue.Item item) {
            return auth;
        }
    }
}
