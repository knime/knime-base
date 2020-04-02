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
 *   Jan 14, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import org.knime.core.node.context.DeepCopy;

/**
 * Configuration for reading a single table.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig}
 */
public interface TableReadConfig<C extends ReaderSpecificConfig<C>> extends DeepCopy<TableReadConfig<C>>, ReaderConfig {

    /**
     * Returns the reader specific configuration.
     *
     * @return the configuration specific to the concrete reader implementation
     */
    C getReaderSpecificConfig();

    /**
     * Retrieves the configured column filter.
     *
     * @return the column filter configuration
     */
    // TODO add once we actually have a UI and a concrete config
    //    ColumnFilterConfig getColumnFilter();

    /**
     * Sets a new column filter
     *
     * @param filter to set must not be <code>null</code>
     */
    // TODO add once we actually have a UI and a concrete config
    //    void setColumnFilter(final ColumnFilterConfig filter);

    /**
     * Returns the index of the row containing the column headers.
     *
     * @return the row index of the column header row
     */
    long getColumnHeaderIdx();

    /**
     * Indicates whether the user set an index for the column header.
     *
     * @return <code>true</code> if the user provided an index for the column header
     */
    boolean useColumnHeaderIdx();

    /**
     * Sets whether the column header index returned by {@link #getColumnHeaderIdx()} should be used.
     *
     * @param useColumnHeaderIdx whether the column header index should be used
     */
    void setUseColumnHeaderIdx(final boolean useColumnHeaderIdx);

    /**
     * Sets the index of the row containing the column headers. Provide a negative value if there is no such row.
     *
     * @param idx row index of the column header row
     */
    void setColumnHeaderIdx(long idx);

    /**
     * Returns the index of the column containing the row ids.
     *
     * @return the index of the column containing the row ids
     */
    int getRowIDIdx();

    /**
     * Sets the index of the column containing the row ids. Set a negative value to indicate that there is no such
     * column.
     *
     * @param idx the column index of the row id column
     */
    void setRowIDIdx(final int idx);

    /**
     * Indicates whether the user set an index for the row id column.
     *
     * @return <code>true</code> if the user provided an index for the row id column
     */
    boolean useRowIDIdx();

    /**
     * Sets whether the index returned by {@link #getRowIDIdx()} should be used.
     *
     * @param useRowIDIdx whether the row id index should be used
     */
    void setUseRowIDIdx(final boolean useRowIDIdx);

    /**
     * Returns whether empty rows should be skipped.
     *
     * @return {@code true} if empty rows should be skipped
     */
    boolean skipEmptyRows();

    /**
     * Sets whether empty rows should be skipped.
     *
     * @param skipEmptyRows whether empty rows should be skipped
     */
    void setSkipEmptyRows(boolean skipEmptyRows);

    /**
     * Returns whether all rows are allowed to have fewer columns than the other rows in the table.</br>
     * The missing columns are filled up with missing values.
     *
     * @return {@code true} if not all rows must have the same size
     */
    boolean allowShortRows();

    /**
     * Sets whether short rows are supported.
     *
     * @param allowShortRows whether short rows are supported
     */
    void setAllowShortRows(final boolean allowShortRows);

    /**
     * Returns whether a certain number of rows are skipped at the beginning. If <code>true</code> the actual number is
     * determined by using <code>setNumRowsToSkip()</code>.
     *
     * @return <code>true</code> if a certain number of rows are skipped at the beginning.
     */
    boolean skipRows();

    /**
     * Sets whether rows should be skipped/omitted in the beginning.
     *
     * @param skipRows whether rows should be skipped/omitted in the beginning.
     */
    void setSkipRows(final boolean skipRows);

    /**
     * Returns the number of rows that should be skipped/omitted in the beginning. Used only if {@link #skipRows()} is
     * set <code>true</code>.
     *
     * @return the number of rows that should be skipped/omitted in the beginning.
     */
    long getNumRowsToSkip();

    /**
     * Sets the number of rows that should be skipped/omitted in the beginning. Used only if {@link #skipRows()} is set
     * <code>true</code>.
     *
     * @param numRowsToSkip the number of rows that should be
     */
    void setNumRowsToSkip(final long numRowsToSkip);

    /**
     * Returns whether to limit the number of rows to a certain maximum. If <code>true</code> the actual number is
     * determined by using <code>setMaxRows()</code>.
     *
     * @return <code>true</code> if number of rows is limited to a certain maximum
     */
    boolean limitRows();

    /**
     * Sets whether to limit the number of rows to a certain maximum.
     *
     * @param limitRows whether to limit the number of rows to a certain maximum.
     */
    void setLimitRows(final boolean limitRows);

    /**
     * Returns the maximum number of rows that should be read. Used only if {@link #limitRows()} is set
     * <code>true</code>.
     *
     * @return the number of rows that should be skipped/omitted in the beginning.
     */
    long getMaxRows();

    /**
     * Sets the maximum number of rows that should be read. Used only if {@link #limitRows()} is set <code>true</code>.
     *
     * @param maxRows the number of rows that should be
     */
    void setMaxRows(final long maxRows);

    /**
     * Returns whether to limit the number of rows for the purpose of defining the table specifications. If
     * <code>true</code> the actual number is determined by using <code>setMaxRowsForSpec()</code>.
     *
     * @return <code>true</code> if number of rows is limited to a certain maximum
     */
    boolean limitRowsForSpec();

    /**
     * Sets whether to limit the number of rows for the purpose of defining the table specifications.
     *
     * @param limitRowsForSpec whether to limit the number of rows for the purpose of defining the table specifications.
     */
    void setLimitRowsForSpec(final boolean limitRowsForSpec);

    /**
     * Returns the maximum number of rows that should be read for the purpose of defining the table specifications. Used
     * only if {@link #limitRowsForSpec()} is set <code>true</code>.
     *
     * @return the number of rows that should be skipped/omitted in the beginning.
     */
    long getMaxRowsForSpec();

    /**
     * Sets the maximum number of rows that should be read for the purpose of defining the table specifications. Used
     * only if {@link #limitRowsForSpec()} is set <code>true</code>.
     *
     * @param maxRowsForSpec the number of rows that should be
     */
    void setMaxRowsForSpec(final long maxRowsForSpec);

}
