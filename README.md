Job Copy Builder plugin
=======================

Japanese version of this document is README_ja.md

Jenkins plugin to copy a job in a build step.

What's this?
------------

Job Copy Builder is a [Jenkins](http://jenkins-ci.org/) plugin.
This plugin provides Copy Job build step:

* It makes a new job from an existing job.
	* This can be configured as a build step, so you can copy multiple jobs in one build execution with multiple build steps.
* You specify following parameters.
	* From Job Name
		* Variable expressions can be used.
	* To Job Name
		* Variable expressions can be used.
	* Overwite
		* Specifies whether to overwrite if the destination job already exists.
* Additional operations will be performed when copying.
	* Enable Job: Enabling the destination job if the source job is disabled.
	* Disable Job: Disabling the destination job if the source job is enabled.
	* Replace String: Replace strings in a job configuration.
		* Source and destination strings can contain variable expressions.
* Additional operation can be extended by using [the Jenkins extention point featere] (https://wiki.jenkins-ci.org/display/JENKINS/Extension+points).

Limitations
-----------

* The job contains Copy Job build steps must run on the master node.

How does this work?
-------------------

This plugin works as following:

1. Reads the configuration xml (config.xml) of the copying job.
2. Applies the operations to the configuration xml string.
3. Create a new job with the processed configuration xml string.

Extension point
---------------

New additional operations can be added with extending `JobcopyOperation`, overriding the following method:

```java
public abstract String JobcopyOperation::perform(String xmlString, String encoding, EnvVars env, PrintStream logger);
```

Or, you can use `AbstractXmlJobcopyOperation`, which provides you a parsed XML Document node, overriding the following method:

```java
public abstract String AbstractXmlJobcopyOperation::perform(Document doc, EnvVars env, PrintStream logger);
```

TODO
----

* Add new operation that replaces strings with regular expressions.

