/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.webhook;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraWebhookNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraWebhookNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraWebhookNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraWebhookNodeSettings::new) //
            .testNodeSettingsStructure(JiraWebhookNodeSettings::new) //
            .build();
    }
}
