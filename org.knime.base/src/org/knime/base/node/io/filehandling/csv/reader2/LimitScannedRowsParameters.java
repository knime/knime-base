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
 *   Nov 24, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.util.Optional;

import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.core.node.InvalidSettingsException;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Parameters for limiting the number of scanned rows.
 *
 * @author Paul Bärnreuther
 */
public final class LimitScannedRowsParameters implements NodeParameters {

    static final class MaxDataRowsScannedDefaultProvider implements DefaultValueProvider<Long> {
        @Override
        public Long computeState(final NodeParametersInput context) {
            return 10000L;
        }
    }

    static class MaxDataRowsScannedRef extends ReferenceStateProvider<Optional<Long>> {
    }

    @Widget(title = "Limit scanned rows", description = """
            If enabled, only the specified number of input <i>rows</i> are used to analyze the file (i.e to
            determine the column types). This option is recommended for long files where the first <i>n</i> rows are
            representative for the whole file. The "Skip first data rows" option has no effect on the scanning. Note
            also, that this option and the "Limit number of rows" option are independent from each other, i.e., if
            the value in "Limit number of rows" is smaller than the value specified here, we will still read as many
            rows as specified here.
            """)
    @ValueReference(MaxDataRowsScannedRef.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(CSVTableReaderLayoutAdditions.ColumnAndDataTypeDetection.LimitScannedRows.class)
    @OptionalWidget(defaultProvider = MaxDataRowsScannedDefaultProvider.class)
    Optional<Long> m_maxDataRowsScanned = Optional.of(10000L);

    /**
     * Save the settings to the given config.
     *
     * @param tableReadConfig the config to save to
     */
    public void saveToConfig(final DefaultTableReadConfig<?> tableReadConfig) {
        tableReadConfig.setLimitRowsForSpec(m_maxDataRowsScanned.isPresent());
        tableReadConfig.setMaxRowsForSpec(m_maxDataRowsScanned.orElse(0L));
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_maxDataRowsScanned.isPresent() && m_maxDataRowsScanned.get() < 0) {
            throw new InvalidSettingsException("The maximum number of data rows scanned must be non-negative.");
        }
    }
}
