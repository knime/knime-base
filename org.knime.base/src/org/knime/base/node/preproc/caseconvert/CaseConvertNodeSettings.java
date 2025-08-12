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

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.FilteredInputTableColumnsProvider;

/**
 * Settings for the Case Converter node (WebUI) using DefaultNodeSettings.
 *
 * Preserves legacy config keys: "include" (String[]) and "uppercase" (boolean) so that the
 * {@link CaseConvertNodeModel} and {@link CaseConvertWebUINodeModel} can load them unchanged.
 */
@SuppressWarnings("restriction")
final class CaseConvertNodeSettings extends DefaultNodeSettings {

    @Section(title = "Column selection & conversion mode",
        description = "Select the string-compatible input columns to convert and choose whether their contents"
            + " should be transformed to upper or lower case. Only columns compatible with string values are"
            + " offered. If no columns are selected the node will pass the input table through unchanged and"
            + " issue a warning.")
    interface Options {
    }

    @Widget(title = "Input columns to convert",
        description = "Choose the string-compatible input columns whose textual values should be case converted."
            + " Move columns between the 'Columns to convert' and 'Available columns' lists. Only columns"
            + " compatible with the String data type are shown.")
    @Layout(Options.class)
    @ChoicesProvider(StringColumnsProvider.class)
    @TwinlistWidget(includedLabel = "Columns to convert", excludedLabel = "Available columns")
    @Persist(configKey = CaseConvertNodeModel.CFG_INCLUDED_COLUMNS)
    String[] m_columns = new String[]{};

    @Widget(title = "Convert to UPPERCASE",
        description = "Enable to convert all characters in the selected columns to upper case using the current"
            + " locale. Disable to convert them to lower case instead.")
    @Layout(Options.class)
    @Persist(configKey = CaseConvertNodeModel.CFG_UPPERCASE)
    boolean m_uppercase = true;

    /**
     * Choices provider that lists only string-compatible columns from the first input table.
     */
    static final class StringColumnsProvider implements FilteredInputTableColumnsProvider {
        @Override
        public boolean isIncluded(final DataColumnSpec col) {
            return col.getType().isCompatible(StringValue.class);
        }

        @Override
        public int getInputTableIndex() {
            return 0; // first and only input port
        }
    }
}
