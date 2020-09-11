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
 *   Feb 3, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

/**
 * Default implementation of {@link TableReadConfig}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> The type of {@link ReaderSpecificConfig} used
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class DefaultTableReadConfig<C extends ReaderSpecificConfig<C>> extends AbstractTableReadConfig<C> {

    /**
     * Constructor.
     *
     * @param readerSpecificConfig containing reader specific settings
     */
    public DefaultTableReadConfig(final C readerSpecificConfig) {
        super(readerSpecificConfig);
    }

    private DefaultTableReadConfig(final DefaultTableReadConfig<C> toCopy) {
        super(toCopy);
    }

    @Override
    public TableReadConfig<C> copy() {
        return new DefaultTableReadConfig<>(this);
    }

    /**
     * Sets whether rows should be skipped/omitted in the beginning.
     *
     * @param skipRows whether rows should be skipped/omitted in the beginning.
     */
    public void setSkipRows(final boolean skipRows) {
        m_skipRows = skipRows;
    }

    /**
     * Sets whether the index returned by {@link #getRowIDIdx()} should be used.
     *
     * @param useRowIDIdx whether the row id index should be used
     */
    public void setUseRowIDIdx(final boolean useRowIDIdx) {
        m_useRowIDIdx = useRowIDIdx;
    }

    /**
     * Sets the index of the column containing the row ids. Set a negative value to indicate that there is no such
     * column.
     *
     * @param idx the column index of the row id column
     */
    public void setRowIDIdx(final int idx) {
        m_rowIDIdx = idx > -1 ? idx : -1;
    }

    /**
     * Sets the index of the row containing the column headers. Provide a negative value if there is no such row.
     *
     * @param idx row index of the column header row
     */
    public void setColumnHeaderIdx(final long idx) {
        m_columnHeaderIdx = idx < -1 ? -1 : idx;
    }

    /**
     * Sets whether the column header index returned by {@link #getColumnHeaderIdx()} should be used.
     *
     * @param useColumnHeaderIdx whether the column header index should be used
     */
    public void setUseColumnHeaderIdx(final boolean useColumnHeaderIdx) {
        m_useColumnHeaderIdx = useColumnHeaderIdx;
    }

    /**
     * Sets the number of rows that should be skipped/omitted in the beginning. Used only if {@link #skipRows()} is set
     * <code>true</code>.
     *
     * @param numRowsToSkip the number of rows that should be
     */
    public void setNumRowsToSkip(final long numRowsToSkip) {
        m_numRowsToSkip = numRowsToSkip;
    }

    /**
     * Sets whether to limit the number of rows for the purpose of defining the table specifications.
     *
     * @param limitRowsForSpec whether to limit the number of rows for the purpose of defining the table specifications.
     */
    public void setLimitRowsForSpec(final boolean limitRowsForSpec) {
        m_limitRowsForSpec = limitRowsForSpec;
    }

    /**
     * Sets the maximum number of rows that should be read for the purpose of defining the table specifications. Used
     * only if {@link #limitRowsForSpec()} is set <code>true</code>.
     *
     * @param maxRowsForSpec the number of rows that should be
     */
    public void setMaxRowsForSpec(final long maxRowsForSpec) {
        m_maxRowsForSpec = maxRowsForSpec;
    }

    /**
     * Sets whether empty rows should be skipped.
     *
     * @param skipEmptyRows whether empty rows should be skipped
     */
    public void setSkipEmptyRows(final boolean skipEmptyRows) {
        m_skipEmptyRows = skipEmptyRows;
    }

    /**
     * Sets whether short rows should be allowed or not.
     *
     * @param allowShortRows whether short rows should be allowed
     */
    public void setAllowShortRows(final boolean allowShortRows) {
        m_allowShortRows = allowShortRows;
    }

}
