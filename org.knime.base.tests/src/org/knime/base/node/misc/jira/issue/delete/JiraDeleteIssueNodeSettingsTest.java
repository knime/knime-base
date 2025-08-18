/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.issue.delete;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraDeleteIssueNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraDeleteIssueNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraDeleteIssueNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraDeleteIssueNodeSettings::new) //
            .testNodeSettingsStructure(JiraDeleteIssueNodeSettings::new) //
            .build();
    }
}
