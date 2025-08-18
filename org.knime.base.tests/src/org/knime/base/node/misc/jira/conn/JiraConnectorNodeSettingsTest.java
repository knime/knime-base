/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.conn;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraConnectorNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraConnectorNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraConnectorNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraConnectorNodeSettings::new) //
            .testNodeSettingsStructure(JiraConnectorNodeSettings::new) //
            .build();
    }
}
