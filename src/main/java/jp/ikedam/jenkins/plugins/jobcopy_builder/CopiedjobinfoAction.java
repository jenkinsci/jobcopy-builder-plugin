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

import hudson.model.Action;
import hudson.model.TopLevelItem;

import java.io.Serializable;

/**
 * Action holds the information of the jobs that the build copied from and to.
 * <p>
 * the information will be shown in the build's Summary page,
 * using summary.jelly.
 */
public class CopiedjobinfoAction implements Action, Serializable {
    private static final long serialVersionUID = 1L;

    private String fromJobName;
    private String fromUrl;
    private String toJobName;
    private String toUrl;
    private boolean failed;

    /**
     * constructor.
     *
     * @param fromItem job that was copied from.
     * @param toItem   job that was copied to.
     * @param failed   whether the job is copied incompletely.
     */
    public CopiedjobinfoAction(TopLevelItem fromItem, TopLevelItem toItem, boolean failed) {
        this.fromJobName = fromItem.getName();
        this.fromUrl = fromItem.getUrl();
        this.toJobName = toItem.getName();
        this.toUrl = toItem.getUrl();
        this.failed = failed;
    }

    /**
     * Returns the name of the job copied from.
     *
     * @return the name of the job copied from
     */
    public String getFromJobName() {
        return this.fromJobName;
    }

    /**
     * Returns the URI (path) of the job copied from.
     * <p>
     * This URI might be lost,
     * in the case that the job is removed or renamed.
     *
     * @return the URI (path) of the job copied from.
     */
    public String getFromUrl() {
        return this.fromUrl;
    }

    /**
     * Returns the name of the job copied to.
     *
     * @return the name of the job copied to
     */
    public String getToJobName() {
        return this.toJobName;
    }

    /**
     * Returns the URI (path) of the job copied to.
     * <p>
     * This URI might be lost,
     * in the case that the job is removed or renamed.
     *
     * @return the URI (path) of the job copied to.
     */
    public String getToUrl() {
        return this.toUrl;
    }

    /**
     * Returns whether the job is copied incompletely
     *
     * @return whether the job is copied incompletely
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * Returns null not for being displayed in the link list.
     *
     * @return null
     * @see hudson.model.Action#getIconFileName()
     */
    @Override
    public String getIconFileName() {
        return null;
    }

    /**
     * Returns null not for being displayed in the link list.
     *
     * @return null
     * @see hudson.model.Action#getUrlName()
     */
    @Override
    public String getUrlName() {
        return null;
    }

    /**
     * Returns the display name.
     * <p>
     * This will be never used, for not displayed in the link list.
     *
     * @return the display name.
     * @see hudson.model.Action#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return Messages.CopiedjobinfoAction_DisplayName();
    }
}
