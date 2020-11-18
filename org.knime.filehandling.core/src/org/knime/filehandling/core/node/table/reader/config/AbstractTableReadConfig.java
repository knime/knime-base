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
 *   Aug 13, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import org.knime.filehandling.core.node.table.reader.read.Read;

/**
 * Abstract implementation of a {@link TableReadConfig}
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> The type of {@link ReaderSpecificConfig}
 */
abstract class AbstractTableReadConfig<C extends ReaderSpecificConfig<C>> implements TableReadConfig<C> {

    /**
     * Index of the column header row.
     */
    protected long m_columnHeaderIdx = -1;

    /**
     * Index of the row ID column.
     */
    protected int m_rowIDIdx = -1;

    /**
     * Indicates if a column header row exists.
     */
    protected boolean m_useColumnHeaderIdx = true;

    /**
     * Indicates if a row ID column exists.
     */
    protected boolean m_useRowIDIdx = false;

    protected String m_prefixForGeneratedRowIds = "Row";

    /**
     * Indicates whether a source prefix (consisting of an application dependent prefix and the index of the source)
     * should be prepended to row ids read from the source. Not used if new row keys are generated.
     */
    protected boolean m_prependSourcePrefixToRowIDs = false;

    /**
     * Indicates if short rows are supported.
     */
    protected boolean m_allowShortRows;

    /**
     * Indicates if empty rows should be skipped.
     */
    protected boolean m_skipEmptyRows;

    /**
     * Indicates if the first rows should be skipped.
     */
    protected boolean m_skipRows = false;

    /**
     * The number of rows to skip at the beginning of the table.
     */
    protected long m_numRowsToSkip = 1;

    /**
     * Indicates if the number of rows should be limited.
     */
    protected boolean m_limitRows = false;

    /**
     * The maximum number of rows to read.
     */
    protected long m_maxRows = 50;

    /**
     * Indicates if the number of rows should be limited for creating the spec.
     */
    protected boolean m_limitRowsForSpec = true;

    /**
     * The maximum number of rows to read for creating the table spec.
     */
    protected long m_maxRowsForSpec = 50;

    /**
     * Indicates if the {@link Read} should be decorated by the framework.
     */
    protected boolean m_decorateRead = true;

    private final C m_readerSpecificConfig;

    /**
     * Constructor.
     *
     * @param readerSpecificConfig the {@link ReaderSpecificConfig} corresponding to the used reader implementation
     */
    public AbstractTableReadConfig(final C readerSpecificConfig) {
        m_readerSpecificConfig = readerSpecificConfig;
    }

    /**
     * Copy constructor.
     *
     * @param toCopy the instance to copy
     */
    protected AbstractTableReadConfig(final AbstractTableReadConfig<C> toCopy) {
        m_readerSpecificConfig = toCopy.m_readerSpecificConfig.copy();
        m_columnHeaderIdx = toCopy.m_columnHeaderIdx;
        m_rowIDIdx = toCopy.m_rowIDIdx;
        m_useColumnHeaderIdx = toCopy.m_useColumnHeaderIdx;
        m_useRowIDIdx = toCopy.m_useRowIDIdx;
        m_prefixForGeneratedRowIds = toCopy.m_prefixForGeneratedRowIds;
        m_prependSourcePrefixToRowIDs = toCopy.m_prependSourcePrefixToRowIDs;
        m_allowShortRows = toCopy.m_allowShortRows;
        m_skipEmptyRows = toCopy.m_skipEmptyRows;
        m_skipRows = toCopy.m_skipRows;
        m_numRowsToSkip = toCopy.m_numRowsToSkip;
        m_limitRows = toCopy.m_limitRows;
        m_maxRows = toCopy.m_maxRows;
        m_limitRowsForSpec = toCopy.m_limitRowsForSpec;
        m_maxRowsForSpec = toCopy.m_maxRowsForSpec;
        m_decorateRead = toCopy.m_decorateRead;
    }

    @Override
    public C getReaderSpecificConfig() {
        return m_readerSpecificConfig;
    }

    @Override
    public long getColumnHeaderIdx() {
        return m_columnHeaderIdx;
    }

    @Override
    public int getRowIDIdx() {
        return m_rowIDIdx;
    }

    @Override
    public boolean useRowIDIdx() {
        return m_useRowIDIdx;
    }

    @Override
    public String getPrefixForGeneratedRowIDs() {
        return m_prefixForGeneratedRowIds;
    }

    @Override
    public boolean prependSourceIdxToRowID() {
        return m_prependSourcePrefixToRowIDs;
    }

    @Override
    public boolean useColumnHeaderIdx() {
        return m_useColumnHeaderIdx;
    }

    @Override
    public boolean skipEmptyRows() {
        return m_skipEmptyRows;
    }

    @Override
    public boolean allowShortRows() {
        return m_allowShortRows;
    }

    @Override
    public boolean skipRows() {
        return m_skipRows;
    }

    @Override
    public long getNumRowsToSkip() {
        return m_numRowsToSkip;
    }

    @Override
    public boolean limitRows() {
        return m_limitRows;
    }

    /**
     * Sets whether to limit the number of rows to a certain maximum.
     *
     * @param limitRows whether to limit the number of rows to a certain maximum.
     */
    public void setLimitRows(final boolean limitRows) {
        m_limitRows = limitRows;
    }

    @Override
    public long getMaxRows() {
        return m_maxRows;
    }

    /**
     * Sets the maximum number of rows that should be read. Used only if {@link TableReadConfig#limitRows()} is set
     * <code>true</code>.
     *
     * @param maxRows the number of rows that should be
     */
    public void setMaxRows(final long maxRows) {
        m_maxRows = maxRows;
    }

    @Override
    public boolean limitRowsForSpec() {
        return m_limitRowsForSpec;
    }

    @Override
    public long getMaxRowsForSpec() {
        return m_maxRowsForSpec;
    }

    @Override
    public boolean decorateRead() {
        return m_decorateRead;
    }

}