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

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.core.node.InvalidSettingsException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Parameters for skipping first lines of file.
 *
 * @author Paul Bärnreuther
 */
public final class SkipFirstLinesOfFileParameters implements NodeParameters {

    static final class SkipFirstLinesRef extends ReferenceStateProvider<Long> {
    }

    @Widget(title = "Skip first lines of file", description = """
            Use this option to skip lines that do not fit in the table structure (e.g. multi-line comments).
            <br/>
            The specified number of lines are skipped in the input file before the parsing starts. Skipping lines
            prevents parallel reading of individual files.
            """)
    @ValueReference(SkipFirstLinesRef.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(CSVTableReaderLayoutAdditions.FileFormat.class)
    long m_skipFirstLines;

    /**
     * Save the settings to the given config.
     *
     * @param csvConfig the config to save to
     */
    public void saveToConfig(final CSVTableReaderConfig csvConfig) {
        csvConfig.setSkipLines(m_skipFirstLines > 0);
        csvConfig.setNumLinesToSkip(m_skipFirstLines);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_skipFirstLines < 0) {
            throw new InvalidSettingsException("The number of lines to skip must be non-negative.");
        }
    }
}
