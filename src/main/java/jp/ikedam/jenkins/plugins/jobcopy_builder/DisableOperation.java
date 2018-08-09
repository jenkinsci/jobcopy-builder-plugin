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

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.PrintStream;
import java.io.Serializable;

/**
 * Disables the job if the job copied from is enabled.
 */
public class DisableOperation extends AbstractXmlJobcopyOperation implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor to initialize with the input parameters.
     * <p>
     * For there's no input parameters. Nothing to do.
     */
    @DataBoundConstructor
    public DisableOperation() {
    }

    /**
     * Returns modified XML Document of the job configuration.
     * <p>
     * Disables the job: updates the value in /PROJECT/disabled to true.
     *
     * @param doc    XML Document of the job to be copied (job/NAME/config.xml)
     * @param env    Variables defined in the build.
     * @param logger The output stream to log.
     * @return modified XML Document. Return null if an error occurs.
     * @see jp.ikedam.jenkins.plugins.jobcopy_builder.AbstractXmlJobcopyOperation#perform(org.w3c.dom.Document, hudson.EnvVars, java.io.PrintStream)
     */
    @Override
    public Document perform(Document doc, EnvVars env, PrintStream logger) {
        logger.print("Disabling Job...");
        try {
            // Retrieve the node holding the enable/disable configuration.
            Node disabledNode = getNode(doc, "/*/disabled");
            if (disabledNode == null) {
                logger.println("Failed to fetch disabled node.");
                return null;
            }

            logger.println(String.format("%s: %s -> true", getXpath(disabledNode), disabledNode.getTextContent()));
            disabledNode.setTextContent("true");

            return doc;
        } catch (Exception e) {
            logger.print("Error occurred in XML operation");
            e.printStackTrace(logger);
            return null;
        }
    }

    /**
     * The internal class to work with views.
     * <p>
     * The following files are used (put in main/resource directory in the source tree).
     * <dl>
     * <dt>config.jelly</dt>
     * <dd>shown in the job configuration page, as an additional view to a Jobcopy build step.</dd>
     * </dl>
     */
    @Extension
    public static class DescriptorImpl extends Descriptor<JobcopyOperation> {
        /**
         * Returns the string to be shown in a job configuration page,
         * in the dropdown of &quot;Add Copy Operation&quot;.
         *
         * @return the display name
         * @see hudson.model.Descriptor#getDisplayName()
         */
        @Override
        public String getDisplayName() {
            return Messages.DisableOperation_DisplayName();
        }
    }
}

