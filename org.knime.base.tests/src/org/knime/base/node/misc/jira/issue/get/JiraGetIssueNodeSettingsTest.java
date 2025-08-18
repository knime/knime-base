/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.issue.get;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraGetIssueNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraGetIssueNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraGetIssueNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraGetIssueNodeSettings::new) //
            .testNodeSettingsStructure(JiraGetIssueNodeSettings::new) //
            .build();
    }
}
