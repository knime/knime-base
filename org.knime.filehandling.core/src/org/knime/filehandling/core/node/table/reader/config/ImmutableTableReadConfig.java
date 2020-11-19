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
 *   Aug 12, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.read.Read;

/**
 * An immutable implementation of {@link TableReadConfig}, i.e. it is guaranteed that instances of this class don't
 * change after their initialization.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig}
 */
public final class ImmutableTableReadConfig<C extends ReaderSpecificConfig<C>> implements TableReadConfig<C> {

    /**
     * Index of the column header row.
     */
    private final long m_columnHeaderIdx;

    /**
     * Index of the row ID column.
     */
    private final int m_rowIDIdx;

    /**
     * Indicates if a column header row exists.
     */
    private final boolean m_useColumnHeaderIdx;

    /**
     * Indicates if a row ID column exists.
     */
    private final boolean m_useRowIDIdx;

    /**
     * Indicates if short rows are supported.
     */
    private final boolean m_allowShortRows;

    /**
     * Indicates if empty rows should be skipped.
     */
    private final boolean m_skipEmptyRows;

    /**
     * Indicates if the first rows should be skipped.
     */
    private final boolean m_skipRows;

    /**
     * The number of rows to skip at the beginning of the table.
     */
    private final long m_numRowsToSkip;

    /**
     * Indicates if the number of rows should be limited.
     */
    private final boolean m_limitRows;

    /**
     * The maximum number of rows to read.
     */
    private final long m_maxRows;

    /**
     * Indicates if the number of rows should be limited for creating the spec.
     */
    private final boolean m_limitRowsForSpec;

    /**
     * The maximum number of rows to read for creating the table spec.
     */
    private final long m_maxRowsForSpec;

    /**
     * Indicates if the {@link Read} should be decorated by the framework.
     */
    private final boolean m_decorateRead;

    private final C m_readerSpecificConfig;

    /**
     * @param tableReadConfig
     */
    public ImmutableTableReadConfig(final TableReadConfig<C> tableReadConfig) {
        m_columnHeaderIdx = tableReadConfig.getColumnHeaderIdx();
        m_rowIDIdx = tableReadConfig.getRowIDIdx();
        m_useColumnHeaderIdx = tableReadConfig.useColumnHeaderIdx();
        m_useRowIDIdx = tableReadConfig.useRowIDIdx();
        m_allowShortRows = tableReadConfig.allowShortRows();
        m_skipEmptyRows = tableReadConfig.skipEmptyRows();
        m_skipRows = tableReadConfig.skipRows();
        m_numRowsToSkip = tableReadConfig.getNumRowsToSkip();
        m_limitRows = tableReadConfig.limitRows();
        m_maxRows = tableReadConfig.getMaxRows();
        m_limitRowsForSpec = tableReadConfig.limitRowsForSpec();
        m_maxRowsForSpec = tableReadConfig.getMaxRowsForSpec();
        m_decorateRead = tableReadConfig.decorateRead();
        m_readerSpecificConfig =
            CheckUtils.checkArgumentNotNull(tableReadConfig, "The tableReadConfig must not be null")
                .getReaderSpecificConfig().copy();
    }

    @Override
    public TableReadConfig<C> copy() {
        return new ImmutableTableReadConfig<>(this);
    }

    @Override
    public C getReaderSpecificConfig() {
        return m_readerSpecificConfig.copy();
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

    @Override
    public long getMaxRows() {
        return m_maxRows;
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
