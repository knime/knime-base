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

    private static final String CFG_USE_ROW_ID_IDX = "use_row_id_idx";

    private static final String CFG_USE_SPEC_READ_END_IDX = "use_spec_read_end_idx";

    private static final String CFG_USE_READ_END_IDX = "use_read_end_idx";

    private static final String CFG_SPEC_READ_END_IDX = "spec_read_end_idx";

    private static final String CFG_READ_END_IDX = "read_end_idx";

    private static final String CFG_READ_START_IDX = "read_start_idx";

    private static final String CFG_READER_SPECIFIC_CONFIG = "reader_specific_config";

    private static final String CFG_COLUMN_FILTER = "column_filter";

    private static final String CFG_COLUMN_HEADER_IDX = "column_header_idx";

    private static final String CFG_ROW_ID_IDX = "row_id_idx";

    private static final String CFG_USE_COLUMN_HEADER_IDX = "use_column_header_idx";

    private final C m_readerSpecificConfig;

    private long m_columnHeaderIdx = -1;

    private int m_rowIDIdx = -1;

    private long m_readStartIdx = -1;

    private long m_readEndIdx = -1;

    private boolean m_useReadEndIdx = false;

    private long m_specReadEndIdx = -1;

    private boolean m_useSpecReadEndIdx = false;

    private boolean m_useColumnHeaderIdx = false;

    private boolean m_useRowIDIdx = false;

    DefaultTableReadConfig(final C readerSpecificConfig) {
        m_readerSpecificConfig = readerSpecificConfig;
    }

    private DefaultTableReadConfig(final DefaultTableReadConfig<C> toCopy) {
        m_readerSpecificConfig = toCopy.m_readerSpecificConfig.copy();
        m_columnHeaderIdx = toCopy.m_columnHeaderIdx;
        m_rowIDIdx = toCopy.m_rowIDIdx;
        m_readStartIdx = toCopy.m_readStartIdx;
        m_readEndIdx = toCopy.m_readEndIdx;
        m_specReadEndIdx = toCopy.m_specReadEndIdx;
        m_useSpecReadEndIdx = toCopy.m_useSpecReadEndIdx;
        m_useReadEndIdx = toCopy.m_useReadEndIdx;
        m_useColumnHeaderIdx = toCopy.m_useColumnHeaderIdx;
        m_useRowIDIdx = toCopy.m_useRowIDIdx;
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
    public long getReadStartIdx() {
        return m_readStartIdx;
    }

    @Override
    public long getReadEndIdx() {
        return m_readEndIdx;
    }

    @Override
    public boolean useReadEndIdx() {
        return m_useReadEndIdx;
    }

    @Override
    public long getSpecReadEndIdx() {
        return m_specReadEndIdx;
    }

    @Override
    public boolean useSpecReadEndIdx() {
        return m_useSpecReadEndIdx;
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
        settings.getBooleanArray(CFG_COLUMN_FILTER);
        m_readerSpecificConfig.validate(settings.getNodeSettings(CFG_READER_SPECIFIC_CONFIG));
        settings.getLong(CFG_READ_START_IDX);
        settings.getLong(CFG_READ_END_IDX);
        settings.getLong(CFG_SPEC_READ_END_IDX);
        settings.getBoolean(CFG_USE_READ_END_IDX);
        settings.getBoolean(CFG_USE_SPEC_READ_END_IDX);
        settings.getBoolean(CFG_USE_ROW_ID_IDX);
        settings.getBoolean(CFG_USE_COLUMN_HEADER_IDX);
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_rowIDIdx = settings.getInt(CFG_ROW_ID_IDX);
        m_columnHeaderIdx = settings.getLong(CFG_COLUMN_HEADER_IDX);
        m_readerSpecificConfig.loadInModel(settings.getNodeSettings(CFG_READER_SPECIFIC_CONFIG));
        m_readStartIdx = settings.getLong(CFG_READ_START_IDX);
        m_readEndIdx = settings.getLong(CFG_READ_END_IDX);
        m_specReadEndIdx = settings.getLong(CFG_SPEC_READ_END_IDX);
        m_useReadEndIdx = settings.getBoolean(CFG_USE_READ_END_IDX);
        m_useSpecReadEndIdx = settings.getBoolean(CFG_USE_SPEC_READ_END_IDX);
        m_useRowIDIdx = settings.getBoolean(CFG_USE_ROW_ID_IDX);
        m_useColumnHeaderIdx = settings.getBoolean(CFG_USE_COLUMN_HEADER_IDX);
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings) {
        m_rowIDIdx = settings.getInt(CFG_ROW_ID_IDX, -1);
        m_columnHeaderIdx = settings.getLong(CFG_COLUMN_HEADER_IDX, -1);
        m_readerSpecificConfig.loadInDialog(ReaderConfigUtils.getOrEmpty(settings, CFG_READER_SPECIFIC_CONFIG));
        m_readStartIdx = settings.getLong(CFG_READ_START_IDX, -1);
        m_readEndIdx = settings.getLong(CFG_READ_END_IDX, -1);
        m_specReadEndIdx = settings.getLong(CFG_SPEC_READ_END_IDX, -1);
        m_useReadEndIdx = settings.getBoolean(CFG_USE_READ_END_IDX, false);
        m_useSpecReadEndIdx = settings.getBoolean(CFG_USE_SPEC_READ_END_IDX, false);
        m_useRowIDIdx = settings.getBoolean(CFG_USE_ROW_ID_IDX, false);
        m_useColumnHeaderIdx = settings.getBoolean(CFG_COLUMN_HEADER_IDX, false);
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        settings.addInt(CFG_ROW_ID_IDX, m_rowIDIdx);
        settings.addLong(CFG_COLUMN_HEADER_IDX, m_columnHeaderIdx);
        m_readerSpecificConfig.save(settings.addNodeSettings(CFG_READER_SPECIFIC_CONFIG));
        settings.addLong(CFG_READ_START_IDX, m_readStartIdx);
        settings.addLong(CFG_READ_END_IDX, m_readEndIdx);
        settings.addLong(CFG_SPEC_READ_END_IDX, m_specReadEndIdx);
        settings.addBoolean(CFG_USE_READ_END_IDX, m_useReadEndIdx);
        settings.addBoolean(CFG_USE_SPEC_READ_END_IDX, m_useSpecReadEndIdx);
        settings.addBoolean(CFG_USE_COLUMN_HEADER_IDX, m_useColumnHeaderIdx);
        settings.addBoolean(CFG_USE_ROW_ID_IDX, m_useRowIDIdx);
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
    public void setReadStartIdx(final long value) {
        m_readStartIdx = value > -1 ? value : -1;
    }

    @Override
    public void setReadEndIdx(final long value) {
        m_readEndIdx = value > -1 ? value : -1;
    }

    @Override
    public void setSpecReadEndIdx(final long value) {
        m_specReadEndIdx = value > -1 ? value : -1;
    }

    @Override
    public void setUseReadEndIdx(final boolean useReadEndIdx) {
        m_useReadEndIdx = useReadEndIdx;
    }

    @Override
    public void setUseSpecReadEndIdx(final boolean useSpecReadEndIdx) {
        m_useSpecReadEndIdx = useSpecReadEndIdx;
    }

    @Override
    public boolean useColumnHeaderIdx() {
        return m_useColumnHeaderIdx;
    }

    @Override
    public void setUseColumnHeaderIdx(final boolean useColumnHeaderIdx) {
        m_useColumnHeaderIdx = useColumnHeaderIdx;
    }

}
