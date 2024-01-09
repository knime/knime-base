/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Dec 15, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.column.renamer;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Settings of the Column Renamer node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class ColumnRenamerSettings implements DefaultNodeSettings {

    ColumnRenamerSettings(final DefaultNodeSettingsContext context) {
        // pick the last column because a typical scenario is to rename columns appended by the previous node
        var initialColumn = context.getDataTableSpec(0)//
            .filter(s -> s.getNumColumns() > 0)//
            .map(s -> s.getColumnSpec(s.getNumColumns() - 1).getName());
        if (initialColumn.isPresent()) {
            // initialize as identity and let the NodeModel warn the user
            var renaming = new Renaming();
            renaming.m_oldName = initialColumn.get();
            renaming.m_newName = renaming.m_oldName;
            m_renamings = new Renaming[]{renaming};
        }
    }

    ColumnRenamerSettings() {
        // persistence and JSON conversion constructor
    }

    @Widget(title = "Renamings", description = "Allows to define new names for columns.")
    @ArrayWidget(addButtonText = "Add column")
    public Renaming[] m_renamings = new Renaming[0];

    // TODO: UIEXT-1007 migrate String to ColumnSelection

    static final class Renaming implements DefaultNodeSettings {

        @HorizontalLayout
        interface RenamingLayout {
        }

        @Widget(title = "Column", description = "The column to rename.")
        @ChoicesWidget(choices = AllColumns.class)
        @Layout(RenamingLayout.class)
        public String m_oldName;

        @Widget(title = "New name",
            description = "The new column name. Must not be empty or consist only of whitespaces.")
        @TextInputWidget(pattern = "\\S+.*")
        @Layout(RenamingLayout.class)
        public String m_newName;

        String getOldName() {
            return m_oldName;
        }

        String getNewName() {
            return m_newName;
        }
    }

    private static final class AllColumns implements ChoicesProvider {

        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            var spec = context.getDataTableSpecs()[0];
            return spec == null ? new String[0] : spec.getColumnNames();
        }

    }

}
