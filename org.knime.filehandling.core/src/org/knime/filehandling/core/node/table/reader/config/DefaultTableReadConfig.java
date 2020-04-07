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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Default implementation of {@link TableReadConfig}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DefaultTableReadConfig<C extends ReaderSpecificConfig<C>> implements TableReadConfig<C> {

    private static final String CFG_ALLOW_SHORT_ROWS = "allow_short_rows";

    private static final String CFG_SKIP_EMPTY_ROWS = "skip_empty_rows";

    private static final String CFG_USE_ROW_ID_IDX = "use_row_id_idx";

    private static final String CFG_READER_SPECIFIC_CONFIG = "reader_specific_config";

    private static final String CFG_COLUMN_FILTER = "column_filter";

    private static final String CFG_COLUMN_HEADER_IDX = "column_header_idx";

    private static final String CFG_ROW_ID_IDX = "row_id_idx";

    private static final String CFG_USE_COLUMN_HEADER_IDX = "use_column_header_idx";

    private static final String CFG_SKIP_ROWS = "skip_rows";

    private static final String CFG_NUM_ROWS_TO_SKIP = "num_rows_to_skip";

    private static final String CFG_LIMIT_ROWS = "limit_rows";

    private static final String CFG_MAX_ROWS = "max_rows";

    private static final String CFG_LIMIT_ROWS_FOR_SPEC = "limit_rows_for_spec";

    private static final String CFG_MAX_ROWS_FOR_SPEC = "max_rows_for_spec";

    private final C m_readerSpecificConfig;

    private long m_columnHeaderIdx = -1;

    private int m_rowIDIdx = -1;

    private boolean m_useColumnHeaderIdx = true;

    private boolean m_useRowIDIdx = false;

    private boolean m_allowShortRows;

    private boolean m_skipEmptyRows;

    private boolean m_skipRows = false;

    private long m_numRowsToSkip = 0;

    private boolean m_limitRows = false;

    private long m_maxRows = 0;

    private boolean m_limitRowsForSpec = false;

    private long m_maxRowsForSpec = 0;

    DefaultTableReadConfig(final C readerSpecificConfig) {
        m_readerSpecificConfig = readerSpecificConfig;
    }

    private DefaultTableReadConfig(final DefaultTableReadConfig<C> toCopy) {
        m_readerSpecificConfig = toCopy.m_readerSpecificConfig.copy();
        m_columnHeaderIdx = toCopy.m_columnHeaderIdx;
        m_rowIDIdx = toCopy.m_rowIDIdx;
        m_useColumnHeaderIdx = toCopy.m_useColumnHeaderIdx;
        m_useRowIDIdx = toCopy.m_useRowIDIdx;

        m_allowShortRows = toCopy.m_allowShortRows;

        m_skipEmptyRows = toCopy.m_skipEmptyRows;

        m_skipRows = toCopy.m_skipRows;
        m_numRowsToSkip = toCopy.m_numRowsToSkip;

        m_limitRows = toCopy.m_limitRows;
        m_maxRows = toCopy.m_maxRows;

        m_limitRowsForSpec = toCopy.m_limitRowsForSpec;
        m_maxRowsForSpec = toCopy.m_maxRowsForSpec;
    }

    @Override
    public TableReadConfig<C> copy() {
        return new DefaultTableReadConfig<>(this);
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
    public void setUseRowIDIdx(final boolean useRowIDIdx) {
        m_useRowIDIdx = useRowIDIdx;
    }

    @Override
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getInt(CFG_ROW_ID_IDX);
        settings.getLong(CFG_COLUMN_HEADER_IDX);
//        settings.getBooleanArray(CFG_COLUMN_FILTER);
        m_readerSpecificConfig.validate(settings.getNodeSettings(CFG_READER_SPECIFIC_CONFIG));
        settings.getBoolean(CFG_USE_ROW_ID_IDX);
        settings.getBoolean(CFG_USE_COLUMN_HEADER_IDX);
        settings.getBoolean(CFG_ALLOW_SHORT_ROWS);
        settings.getBoolean(CFG_SKIP_EMPTY_ROWS);

        settings.getBoolean(CFG_SKIP_ROWS);
        settings.getLong(CFG_NUM_ROWS_TO_SKIP);

        settings.getBoolean(CFG_LIMIT_ROWS);
        settings.getLong(CFG_MAX_ROWS);

        settings.getBoolean(CFG_LIMIT_ROWS_FOR_SPEC);
        settings.getLong(CFG_MAX_ROWS_FOR_SPEC);
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowIDIdx = settings.getInt(CFG_ROW_ID_IDX);
        m_columnHeaderIdx = settings.getLong(CFG_COLUMN_HEADER_IDX);
        m_readerSpecificConfig.loadInModel(settings.getNodeSettings(CFG_READER_SPECIFIC_CONFIG));
        m_useRowIDIdx = settings.getBoolean(CFG_USE_ROW_ID_IDX);
        m_useColumnHeaderIdx = settings.getBoolean(CFG_USE_COLUMN_HEADER_IDX);
        m_skipEmptyRows = settings.getBoolean(CFG_SKIP_EMPTY_ROWS);
        m_allowShortRows = settings.getBoolean(CFG_ALLOW_SHORT_ROWS);

        m_skipRows = settings.getBoolean(CFG_SKIP_ROWS);
        m_numRowsToSkip = settings.getLong(CFG_NUM_ROWS_TO_SKIP);

        m_limitRows = settings.getBoolean(CFG_LIMIT_ROWS);
        m_maxRows = settings.getLong(CFG_MAX_ROWS);

        m_limitRowsForSpec = settings.getBoolean(CFG_LIMIT_ROWS_FOR_SPEC);
        m_maxRowsForSpec = settings.getLong(CFG_MAX_ROWS_FOR_SPEC);
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings) {
        m_rowIDIdx = settings.getInt(CFG_ROW_ID_IDX, -1);
        m_columnHeaderIdx = settings.getLong(CFG_COLUMN_HEADER_IDX, -1);
        m_readerSpecificConfig.loadInDialog(ReaderConfigUtils.getOrEmpty(settings, CFG_READER_SPECIFIC_CONFIG));
        m_useRowIDIdx = settings.getBoolean(CFG_USE_ROW_ID_IDX, false);
        m_useColumnHeaderIdx = settings.getBoolean(CFG_COLUMN_HEADER_IDX, true);
        m_skipEmptyRows = settings.getBoolean(CFG_SKIP_EMPTY_ROWS, false);
        m_allowShortRows = settings.getBoolean(CFG_ALLOW_SHORT_ROWS, false);

        m_skipRows = settings.getBoolean(CFG_SKIP_ROWS, false);
        m_numRowsToSkip = settings.getLong(CFG_NUM_ROWS_TO_SKIP, 0);

        m_limitRows = settings.getBoolean(CFG_LIMIT_ROWS, false);
        m_maxRows = settings.getLong(CFG_MAX_ROWS, 0);

        m_limitRowsForSpec = settings.getBoolean(CFG_LIMIT_ROWS_FOR_SPEC, false);
        m_maxRowsForSpec = settings.getLong(CFG_MAX_ROWS_FOR_SPEC, 0);
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        settings.addInt(CFG_ROW_ID_IDX, m_rowIDIdx);
        settings.addLong(CFG_COLUMN_HEADER_IDX, m_columnHeaderIdx);
        m_readerSpecificConfig.save(settings.addNodeSettings(CFG_READER_SPECIFIC_CONFIG));
        settings.addBoolean(CFG_USE_COLUMN_HEADER_IDX, m_useColumnHeaderIdx);
        settings.addBoolean(CFG_USE_ROW_ID_IDX, m_useRowIDIdx);
        settings.addBoolean(CFG_ALLOW_SHORT_ROWS, m_allowShortRows);
        settings.addBoolean(CFG_SKIP_EMPTY_ROWS, m_skipEmptyRows);

        settings.addBoolean(CFG_SKIP_ROWS, m_skipRows);
        settings.addLong(CFG_NUM_ROWS_TO_SKIP, m_numRowsToSkip);

        settings.addBoolean(CFG_LIMIT_ROWS, m_limitRows);
        settings.addLong(CFG_MAX_ROWS, m_maxRows);

        settings.addBoolean(CFG_LIMIT_ROWS_FOR_SPEC, m_limitRowsForSpec);
        settings.addLong(CFG_MAX_ROWS_FOR_SPEC, m_maxRowsForSpec);
    }

    @Override
    public void setRowIDIdx(final int idx) {
        m_rowIDIdx = idx > -1 ? idx : -1;
    }

    @Override
    public void setColumnHeaderIdx(final long idx) {
        m_columnHeaderIdx = idx < -1 ? -1 : idx;
    }

    @Override
    public boolean useColumnHeaderIdx() {
        return m_useColumnHeaderIdx;
    }

    @Override
    public void setUseColumnHeaderIdx(final boolean useColumnHeaderIdx) {
        m_useColumnHeaderIdx = useColumnHeaderIdx;
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
    public void setSkipRows(final boolean skipRows) {
        m_skipRows = skipRows;
    }

    @Override
    public long getNumRowsToSkip() {
        return m_numRowsToSkip;
    }

    @Override
    public void setNumRowsToSkip(final long numRowsToSkip) {
        m_numRowsToSkip = numRowsToSkip;
    }

    @Override
    public boolean limitRows() {
        return m_limitRows;
    }

    @Override
    public void setLimitRows(final boolean limitRows) {
        m_limitRows = limitRows;
    }

    @Override
    public long getMaxRows() {
        return m_maxRows;
    }

    @Override
    public void setMaxRows(final long maxRows) {
        m_maxRows = maxRows;
    }

    @Override
    public boolean limitRowsForSpec() {
        return m_limitRowsForSpec;
    }

    @Override
    public void setLimitRowsForSpec(final boolean limitRowsForSpec) {
        m_limitRowsForSpec = limitRowsForSpec;
    }

    @Override
    public long getMaxRowsForSpec() {
        return m_maxRowsForSpec;
    }

    @Override
    public void setMaxRowsForSpec(final long maxRowsForSpec) {
        m_maxRowsForSpec = maxRowsForSpec;
    }

    @Override
    public void setSkipEmptyRows(final boolean skipEmptyRows) {
        m_skipEmptyRows = skipEmptyRows;
    }

    @Override
    public void setAllowShortRows(final boolean allowShortRows) {
        m_allowShortRows = allowShortRows;
    }

}
