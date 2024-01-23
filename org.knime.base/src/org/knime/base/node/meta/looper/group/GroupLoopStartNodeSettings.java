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
 *   5 Jan 2023 (chaubold): created
 */
package org.knime.base.node.meta.looper.group;

import java.util.stream.Stream;

import org.knime.base.node.preproc.stringreplacer.StringReplacerSettings;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * The GroupLoopStartNodeSettings define the WebUI dialog of the Group Loop Start Node. The serialization must go via
 * the {@link StringReplacerSettings}.
 *
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 5.3
 */
@SuppressWarnings("restriction")
final class GroupLoopStartNodeSettings implements DefaultNodeSettings {

    /**
     * Constructor for persistence and conversion to JSON.
     */
    GroupLoopStartNodeSettings() {
    }

    /**
     * Constructor for auto-configuration if no settings are available.
     */
    GroupLoopStartNodeSettings(final DefaultNodeSettingsContext context) {
        m_columnFilter = ColumnFilter.createDefault(AllColumns.class, context);
    }

    @Persist(configKey = GroupLoopStartConfigKeys.COLUMN_NAMES, settingsModel = SettingsModelColumnFilter2.class)
    @Widget(title = "Column selection", description = "The columns used to identify the groups.")
    @ChoicesWidget(choices = AllColumns.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Persist(configKey = GroupLoopStartConfigKeys.SORTED_INPUT_TABLE, settingsModel = SettingsModelBoolean.class)
    @Widget(title = "Is the input already sorted by group column(s)?", description = """
            If checked, the input data table will not be sorted before looping
            starts. The table must already be sorted properly by the columns to
            group on. If sorting is switched off, but input table is not properly
            sorted, execution will be canceled.
            """)
    boolean m_alreadySorted;

    static final class AllColumns implements ColumnChoicesProvider {

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .toArray(DataColumnSpec[]::new);
        }

    }
}
