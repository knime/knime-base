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
 *   10 Jan 2023 (ivan.prigarin): created
 */
package org.knime.base.node.preproc.filter.columnref;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 *
 * @author Ivan Prigarin, KNIME GbmH, Konstanz, Germany
 * @since 5.1
 */
@SuppressWarnings("restriction")
public final class ColumnFilterRefNodeSettings implements DefaultNodeSettings {

    private static final String REFERENCE_MODE_KEY = "inexclude";

    @Persistor(ColumnReferenceModePersistor.class)
    @Widget(title = "Include or exclude columns from the reference table",
        description = "Includes or excludes columns that appear in the reference table from the first table.")
    @ValueSwitchWidget
    ColumnReferenceMode m_columnReferenceMode = ColumnReferenceMode.INCLUDE;

    @Persist(configKey = "type_compatibility")
    @Widget(title = "Ensure type compatibility",
        description = "Ensures that the matching columns don't only have the "
            + "same name but also the same type. Columns are only included or "
            + "excluded if the column type of the first table is a super-type "
            + "of the column type from the second table. If this option is not selected, only "
            + "the column names need to match.")
    boolean m_typeCompatibility;

    enum ColumnReferenceMode {
            @Label("Include")
            INCLUDE,

            @Label("Exclude")
            EXCLUDE;
    }

    private static final class ColumnReferenceModePersistor implements NodeSettingsPersistor<ColumnReferenceMode> {

        @Override
        public ColumnReferenceMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getString(REFERENCE_MODE_KEY).equals(ColumnFilterRefNodeModel.INCLUDE)) {
                return ColumnReferenceMode.INCLUDE;
            } else {
                return ColumnReferenceMode.EXCLUDE;
            }
        }

        @Override
        public void save(final ColumnReferenceMode obj, final NodeSettingsWO settings) {
            var value = obj == ColumnReferenceMode.INCLUDE ? ColumnFilterRefNodeModel.INCLUDE
                : ColumnFilterRefNodeModel.EXCLUDE;
            settings.addString(REFERENCE_MODE_KEY, value);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{REFERENCE_MODE_KEY}};
        }
    }

}
