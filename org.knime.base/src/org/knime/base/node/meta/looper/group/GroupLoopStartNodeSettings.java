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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * The GroupLoopStartNodeSettings define the WebUI dialog of the Group Loop Start Node.
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
    @Widget(title = "Category columns", description = "The columns used to identify the groups.")
    @ChoicesWidget(choices = AllColumns.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Sorting option",
        description = """
                Enhance performance by selecting 'Input is already sorted by category columns' if your data is
                already sorted by the group columns. Be cautious: if the input is not properly sorted, the node
                will fail. If you want to make sure that this node executes, do not choose this option.
                """,
        advanced = true)
    @RadioButtonsWidget
    @Persist(customPersistor = YesOrNoPersistor.class)
    YesOrNo m_alreadySorted = YesOrNo.NO;

    static final class AllColumns implements ColumnChoicesProvider {

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0).map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .toArray(DataColumnSpec[]::new);
        }

    }

    enum YesOrNo {
            @Label("Automatically sort data")
            NO, //
            @Label("Input data already sorted by category columns")
            YES
    }

    private static final class YesOrNoPersistor implements FieldNodeSettingsPersistor<YesOrNo> {
        @Override
        public YesOrNo load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(GroupLoopStartConfigKeys.SORTED_INPUT_TABLE, false) ? YesOrNo.YES : YesOrNo.NO;
        }

        @Override
        public void save(final YesOrNo yesOrNo, final NodeSettingsWO settings) {
            settings.addBoolean(GroupLoopStartConfigKeys.SORTED_INPUT_TABLE, yesOrNo == YesOrNo.YES);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{GroupLoopStartConfigKeys.SORTED_INPUT_TABLE};
        }
    }

}
