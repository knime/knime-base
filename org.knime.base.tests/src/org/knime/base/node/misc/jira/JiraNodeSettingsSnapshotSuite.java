/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages({
    "org.knime.base.node.misc.jira",
    "org.knime.base.node.misc.jira.conn",
    "org.knime.base.node.misc.jira.search",
    "org.knime.base.node.misc.jira.issue.get",
    "org.knime.base.node.misc.jira.issue.delete",
    "org.knime.base.node.misc.jira.issue.subtask",
    "org.knime.base.node.misc.jira.issue.link",
    "org.knime.base.node.misc.jira.issue.transition",
    "org.knime.base.node.misc.jira.issue.attach",
    "org.knime.base.node.misc.jira.issue.comment",
    "org.knime.base.node.misc.jira.update",
    "org.knime.base.node.misc.jira.agile.boards",
    "org.knime.base.node.misc.jira.webhook"
})
public class JiraNodeSettingsSnapshotSuite { }
