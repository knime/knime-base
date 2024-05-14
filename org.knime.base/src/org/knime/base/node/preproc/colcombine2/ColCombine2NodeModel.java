/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 */
package org.knime.base.node.preproc.colcombine2;

import java.util.Arrays;
import java.util.HashSet;

import org.knime.base.node.preproc.colcombine2.ColCombine2NodeSettings.DelimiterInputs;
import org.knime.base.node.preproc.colcombine2.ColCombine2NodeSettings.QuoteInputs;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;

/**
 * This is the model implementation of ColCombine. Takes the contents of a set of columns and combines them into one
 * string column.
 *
 * @author Bernd Wiswedel, Daniel Bogenrieder, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class ColCombine2NodeModel extends WebUISimpleStreamableFunctionNodeModel<ColCombine2NodeSettings> {

    ColCombine2NodeSettings m_modelSettings;

    /**
     * @param configuration
     * @param modelSettingsClass
     */
    protected ColCombine2NodeModel(final WebUINodeConfiguration configuration,
        final Class<ColCombine2NodeSettings> modelSettingsClass) {
        super(configuration, modelSettingsClass);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        DataTableSpec spec = inSpecs[0];

        if (spec.containsName(m_modelSettings.m_outputColumnName)) {
            throw new InvalidSettingsException("Column already exits: " + m_modelSettings.m_outputColumnName);
        }

        String[] selectedColumns = m_modelSettings.m_columnFilter.getNonMissingSelected(spec.getColumnNames(), spec);
        String[] selectedColumnsWithMissing = m_modelSettings.m_columnFilter.getSelected(spec.getColumnNames(), spec);
        if (selectedColumnsWithMissing.length > selectedColumns.length && m_modelSettings.m_failIfMissingColumns) {
            String[] missingColumns = Arrays.stream(selectedColumnsWithMissing).distinct()
                .filter(x -> !Arrays.asList(selectedColumns).contains(x)).toArray(String[]::new);
            throw new InvalidSettingsException(
                "Input table does not match selected include columns, unable to find column(s): "
                    + ConvenienceMethods.getShortStringFrom(new HashSet<>(Arrays.asList(missingColumns)), 3));
        }

        var arranger = createColumnRearranger(spec);
        return new DataTableSpec[]{arranger.createSpec()};
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final ColCombine2NodeSettings modelSettings) {
        m_modelSettings = modelSettings;
        var result = new ColumnRearranger(spec);
        DataColumnSpec append =
            new DataColumnSpecCreator(m_modelSettings.m_outputColumnName, StringCell.TYPE).createSpec();
        String[] selectedColumns = m_modelSettings.m_columnFilter.getNonMissingSelected(spec.getColumnNames(), spec);
        final var indices = new int[selectedColumns.length];
        var j = 0;
        for (var k = 0; k < spec.getNumColumns() && j < selectedColumns.length; k++) {
            DataColumnSpec cs = spec.getColumnSpec(k);
            if (selectedColumns[j].equals(cs.getName())) {
                indices[j] = k;
                j++;
            }
        }

        // ", " -> ","
        // "  " -> "  " (do not let the resulting string be empty)
        // " bla bla " -> "bla bla"
        final var delimTrim = trimDelimString(m_modelSettings.m_delimiter);
        result.append(new SingleCellFactory(append) {
            @Override
            public DataCell getCell(final DataRow row) {
                var cellContents = new String[indices.length];
                for (var i = 0; i < indices.length; i++) {
                    DataCell c = row.getCell(indices[i]);
                    String s = c instanceof StringValue sv? sv.getStringValue() : c.toString();
                    cellContents[i] = s;
                }
                return new StringCell(handleContent(cellContents, delimTrim));
            }
        });
        if (m_modelSettings.m_removeInputColumns) {
            result.remove(m_modelSettings.m_columnFilter.getSelected(spec.getColumnNames(), spec));
        }
        return result;
    }

    /**
     * Concatenates the elements of the array, used from cell factory.
     *
     * @param cellContents The cell contents
     * @param delimTrim The trimmed delimiter (used as argument to not do the trimming over and over again.)
     * @return The concatenated string.
     */
    private String handleContent(final String[] cellContents, final String delimTrim) {

        var b = new StringBuilder();

        for (var i = 0; i < cellContents.length; i++) {

            b.append(i > 0 ? delimTrim : "");
            String s = cellContents[i];

            if (m_modelSettings.m_delimiterInputs == DelimiterInputs.QUOTE) {
                if (m_modelSettings.m_quoteInputs == QuoteInputs.ALL || s.contains(delimTrim)
                    || s.contains(Character.toString(m_modelSettings.m_quoteCharacter))) {
                    quoteCellContent(b, s);
                } else {
                    b.append(s);
                }
            } else {
                // replace occurrences of the delimiter
                b.append(s.replace(delimTrim, m_modelSettings.m_replacementDelimiter));
            }
        }
        return b.toString();
    }

    private void quoteCellContent(final StringBuilder b, final String s) {
        b.append(m_modelSettings.m_quoteCharacter);

        for (var j = 0; j < s.length(); j++) {
            var tempChar = s.charAt(j);
            if (tempChar == m_modelSettings.m_quoteCharacter || tempChar == '\\') {
                b.append('\\');
            }
            b.append(tempChar);
        }

        b.append(m_modelSettings.m_quoteCharacter);
    }

    @Override
    protected void validateSettings(final ColCombine2NodeSettings settings) throws InvalidSettingsException {
        if (m_modelSettings != null) {
            if (m_modelSettings.m_outputColumnName == null || m_modelSettings.m_outputColumnName.trim().length() == 0) {
                throw new InvalidSettingsException("Name of new column must not be empty");
            }

            if (m_modelSettings.m_delimiter == null) {
                throw new InvalidSettingsException("Delimiter must not be null");
            }
            m_modelSettings.m_delimiter = trimDelimString(m_modelSettings.m_delimiter);

            if (m_modelSettings.m_delimiterInputs == DelimiterInputs.QUOTE) {
                if (Character.isWhitespace(m_modelSettings.m_quoteCharacter)) {
                    throw new InvalidSettingsException("Can't use white space as quote char");
                }
                if (m_modelSettings.m_delimiter.contains(Character.toString(m_modelSettings.m_quoteCharacter))) {
                    throw new InvalidSettingsException("Delimiter String \"" + m_modelSettings.m_delimiter
                        + "\" must not contain quote character ('" + m_modelSettings.m_quoteCharacter + "')");
                }
            } else {
                if ((m_modelSettings.m_delimiter.length() > 0)
                    && (m_modelSettings.m_replacementDelimiter.contains(m_modelSettings.m_delimiter))) {
                    throw new InvalidSettingsException("Replacement string \"" + m_modelSettings.m_replacementDelimiter
                        + "\" must not contain delimiter string \"" + m_modelSettings.m_delimiter + "\"");
                }
            }
        }
    }

    /**
     * ', ' gets ','. ' ' gets ' ' (do not let the resulting string be empty) ' blah blah ' gets 'blah blah'
     *
     * @param delimString string to trim
     * @return the trimmed string
     */
    static String trimDelimString(final String delimString) {
        return delimString.trim().length() == 0 ? delimString : delimString.trim();

    }

    /**
     * A new configuration to store the settings. Only Columns of Type String are available.
     *
     * @return filter configuration
     */
    static final DataColumnSpecFilterConfiguration createDCSFilterConfiguration() {
        return new DataColumnSpecFilterConfiguration("column-filter");
    }
}
