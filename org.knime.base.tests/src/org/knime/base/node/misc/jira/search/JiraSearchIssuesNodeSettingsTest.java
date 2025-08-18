/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.search;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraSearchIssuesNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraSearchIssuesNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraSearchIssuesNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraSearchIssuesNodeSettings::new) //
            .testNodeSettingsStructure(JiraSearchIssuesNodeSettings::new) //
            .build();
    }
}
