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
 *   Jan 31, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.flowvariable.tablecoltovariable4;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.EnumSettingsModelStringPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * The settings for the "Table Column to Variable" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class TableColumnToVariable4NodeSettings implements DefaultNodeSettings {

    enum MissingOperation {
            @Label("Ignore")
            IGNORE, //
            @Label("Fail")
            FAIL
    }

    @Widget(title = "Column name", description = """
            Name of the column for the values.
            """)
    @ChoicesWidget(choices = ColumnChoiceProvider.class)
    @Persist(configKey = "column")
    String m_column = "";

    @Widget(title = "Skip missing values", description = """
            <ul>
                <li><strong>Ignore</strong>: rows with a missing value in the selected column will be skipped</li>
                <li><strong>Fail</strong>: the node execution will fail if a row contains a missing value in the \
                selected column.</li>
            </ul>
            """)
    @ValueSwitchWidget
    @Persistor(MissingOperationPersistor.class)
    MissingOperation m_missingOperation = MissingOperation.IGNORE;

    static final class ColumnChoiceProvider implements ChoicesProvider {

        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            var spec = context.getPortObjectSpec(0);
            if (spec.isEmpty()) {
                return new String[0];
            }
            if (spec.get() instanceof DataTableSpec tableSpec) {
                return tableSpec.getColumnNames();
            } else {
                throw new IllegalArgumentException("PortObjectSpec is not an instance of DataTableSpec");
            }
        }
    }

    static final class MissingOperationPersistor extends EnumSettingsModelStringPersistor<MissingOperation> {

        MissingOperationPersistor() {
            super("skip_missing", MissingOperation.class);
        }

        @Override
        public MissingOperation load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean("skip_missing", true) ? MissingOperation.IGNORE : MissingOperation.FAIL;
        }

        @Override
        public void save(final MissingOperation obj, final NodeSettingsWO settings) {
            settings.addBoolean("skip_missing", obj == MissingOperation.IGNORE);
        }
    }
}
