/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME AG, Zurich, Switzerland
 * ---------------------------------------------------------------------
 */
package org.knime.base.node.misc.jira.issue.link;

import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

@SuppressWarnings("restriction")
final class JiraLinkIssuesNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    JiraLinkIssuesNodeSettingsTest() {
        super(config());
    }

    private static SnapshotTestConfiguration config() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(JiraLinkIssuesNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, JiraLinkIssuesNodeSettings::new) //
            .testNodeSettingsStructure(JiraLinkIssuesNodeSettings::new) //
            .build();
    }
}
