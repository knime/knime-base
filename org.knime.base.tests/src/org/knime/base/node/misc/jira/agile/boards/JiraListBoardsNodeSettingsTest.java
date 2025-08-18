/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.agile.boards;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraListBoardsNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraListBoardsNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraListBoardsNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraListBoardsNodeSettings::new) //
            .testNodeSettingsStructure(JiraListBoardsNodeSettings::new) //
            .build();
    }
}
