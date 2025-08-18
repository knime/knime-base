/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.issue.attach;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraAttachFileNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraAttachFileNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraAttachFileNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraAttachFileNodeSettings::new) //
            .testNodeSettingsStructure(JiraAttachFileNodeSettings::new) //
            .build();
    }
}
