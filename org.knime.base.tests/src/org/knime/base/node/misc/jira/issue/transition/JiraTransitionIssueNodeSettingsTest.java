/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.issue.transition;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraTransitionIssueNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraTransitionIssueNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraTransitionIssueNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraTransitionIssueNodeSettings::new) //
            .testNodeSettingsStructure(JiraTransitionIssueNodeSettings::new) //
            .build();
    }
}
