/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraTicketCreatorNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraTicketCreatorNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraTicketCreatorNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraTicketCreatorNodeSettings::new) //
            .testNodeSettingsStructure(JiraTicketCreatorNodeSettings::new) //
            .build();
    }
}
