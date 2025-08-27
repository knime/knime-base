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
 */
package org.knime.base.node.preproc.caseconvert;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;

final class CaseConvertNodeParameters implements NodeParameters {

    enum Mode {
            @Label("Uppercase")
            UPPERCASE, //
            @Label("Lowercase")
            LOWERCASE, //
    }

    @Widget(title = "Input columns to convert", description = """
            Choose the string-compatible input columns whose textual values should be case-converted.
            Move columns between the 'Input columns to convert' and 'String columns' lists. Only columns
            compatible with the String data type are shown.
            """)
    @ChoicesProvider(StringColumnsProvider.class)
    @TwinlistWidget(includedLabel = "Columns to convert", excludedLabel = "Available columns")
    @Persist(configKey = CaseConvertNodeModel.CFG_INCLUDED_COLUMNS)
    String[] m_columns = new String[]{};

    @Widget(title = "Casing", description = "Choose if you want to convert to uppercase or lowercase.")
    @RadioButtonsWidget(horizontal = true)
    @Persistor(ModePersistor.class)
    Mode m_mode = Mode.UPPERCASE;

    private static final class ModePersistor implements NodeParametersPersistor<Mode> {

        @Override
        public Mode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getBoolean(CaseConvertNodeModel.CFG_UPPERCASE) ? Mode.UPPERCASE : Mode.LOWERCASE;
        }

        @Override
        public void save(final Mode obj, final NodeSettingsWO settings) {
            final var isTrueValue = obj == Mode.UPPERCASE;
            settings.addBoolean(CaseConvertNodeModel.CFG_UPPERCASE, isTrueValue);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CaseConvertNodeModel.CFG_UPPERCASE}};
        }
    }
}
