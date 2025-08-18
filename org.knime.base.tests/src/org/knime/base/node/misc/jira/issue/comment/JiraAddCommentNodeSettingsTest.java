/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.issue.comment;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraAddCommentNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraAddCommentNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraAddCommentNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraAddCommentNodeSettings::new) //
            .testNodeSettingsStructure(JiraAddCommentNodeSettings::new) //
            .build();
    }
}
