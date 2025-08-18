/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.issue.subtask;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraCreateSubtaskNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraCreateSubtaskNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraCreateSubtaskNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraCreateSubtaskNodeSettings::new) //
            .testNodeSettingsStructure(JiraCreateSubtaskNodeSettings::new) //
            .build();
    }
}
