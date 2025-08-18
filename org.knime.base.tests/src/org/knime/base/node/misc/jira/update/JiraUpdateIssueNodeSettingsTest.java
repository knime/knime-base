/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.update;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraUpdateIssueNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraUpdateIssueNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraUpdateIssueNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraUpdateIssueNodeSettings::new) //
            .testNodeSettingsStructure(JiraUpdateIssueNodeSettings::new) //
            .build();
    }
}
