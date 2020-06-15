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
 *   Jun 12, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Default implementation of a {@link ConfigSerializer} for {@link DefaultTableReadConfig}.</br>
 * Delegates to a dedicated serializer to handle the {@link ReaderSpecificConfig}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig}
 */
public final class DefaultTableReadConfigSerializer<C extends ReaderSpecificConfig<C>>
    implements ConfigSerializer<DefaultTableReadConfig<C>> {

    private static final String CFG_ALLOW_SHORT_ROWS = "allow_short_rows";

    private static final String CFG_SKIP_EMPTY_ROWS = "skip_empty_rows";

    private static final String CFG_USE_ROW_ID_IDX = "use_row_id_idx";

    private static final String CFG_READER_SPECIFIC_CONFIG = "reader_specific_config";

    private static final String CFG_COLUMN_HEADER_IDX = "column_header_idx";

    private static final String CFG_ROW_ID_IDX = "row_id_idx";

    private static final String CFG_USE_COLUMN_HEADER_IDX = "use_column_header_idx";

    private static final String CFG_SKIP_ROWS = "skip_rows";

    private static final String CFG_NUM_ROWS_TO_SKIP = "num_rows_to_skip";

    private static final String CFG_LIMIT_ROWS = "limit_rows";

    private static final String CFG_MAX_ROWS = "max_rows";

    private static final String CFG_LIMIT_ROWS_FOR_SPEC = "limit_rows_for_spec";

    private static final String CFG_MAX_ROWS_FOR_SPEC = "max_rows_for_spec";

    private final ConfigSerializer<C> m_readerSpecificConfigSerializer;

    /**
     * Constructor.
     *
     * @param readerSpecificConfigSerializer serializer for {@link ReaderSpecificConfig}
     */
    public DefaultTableReadConfigSerializer(final ConfigSerializer<C> readerSpecificConfigSerializer) {
        m_readerSpecificConfigSerializer = readerSpecificConfigSerializer;
    }

    @Override
    public void loadInDialog(final DefaultTableReadConfig<C> config, final NodeSettingsRO settings,
        final PortObjectSpec[] specs) throws NotConfigurableException {
        config.setRowIDIdx(settings.getInt(CFG_ROW_ID_IDX, -1));
        config.setColumnHeaderIdx(settings.getLong(CFG_COLUMN_HEADER_IDX, -1));
        m_readerSpecificConfigSerializer.loadInDialog(config.getReaderSpecificConfig(),
            ReaderConfigUtils.getOrEmpty(settings, CFG_READER_SPECIFIC_CONFIG), specs);
        config.setUseRowIDIdx(settings.getBoolean(CFG_USE_ROW_ID_IDX, false));
        config.setUseColumnHeaderIdx(settings.getBoolean(CFG_USE_COLUMN_HEADER_IDX, true));
        config.setSkipEmptyRows(settings.getBoolean(CFG_SKIP_EMPTY_ROWS, false));
        config.setAllowShortRows(settings.getBoolean(CFG_ALLOW_SHORT_ROWS, false));

        config.setSkipRows(settings.getBoolean(CFG_SKIP_ROWS, false));
        config.setNumRowsToSkip(settings.getLong(CFG_NUM_ROWS_TO_SKIP, 1));

        config.setLimitRows(settings.getBoolean(CFG_LIMIT_ROWS, false));
        config.setMaxRows(settings.getLong(CFG_MAX_ROWS, 50));

        config.setLimitRowsForSpec(settings.getBoolean(CFG_LIMIT_ROWS_FOR_SPEC, false));
        config.setMaxRowsForSpec(settings.getLong(CFG_MAX_ROWS_FOR_SPEC, 50));

    }

    @Override
    public void loadInModel(final DefaultTableReadConfig<C> config, final NodeSettingsRO settings)
        throws InvalidSettingsException {
        config.setRowIDIdx(settings.getInt(CFG_ROW_ID_IDX));
        config.setColumnHeaderIdx(settings.getLong(CFG_COLUMN_HEADER_IDX));
        m_readerSpecificConfigSerializer.loadInModel(config.getReaderSpecificConfig(),
            settings.getNodeSettings(CFG_READER_SPECIFIC_CONFIG));
        config.setUseRowIDIdx(settings.getBoolean(CFG_USE_ROW_ID_IDX));
        config.setUseColumnHeaderIdx(settings.getBoolean(CFG_USE_COLUMN_HEADER_IDX));
        config.setSkipEmptyRows(settings.getBoolean(CFG_SKIP_EMPTY_ROWS));
        config.setAllowShortRows(settings.getBoolean(CFG_ALLOW_SHORT_ROWS));

        config.setSkipRows(settings.getBoolean(CFG_SKIP_ROWS));
        config.setNumRowsToSkip(settings.getLong(CFG_NUM_ROWS_TO_SKIP));

        config.setLimitRows(settings.getBoolean(CFG_LIMIT_ROWS));
        config.setMaxRows(settings.getLong(CFG_MAX_ROWS));

        config.setLimitRowsForSpec(settings.getBoolean(CFG_LIMIT_ROWS_FOR_SPEC));
        config.setMaxRowsForSpec(settings.getLong(CFG_MAX_ROWS_FOR_SPEC));

    }

    @Override
    public void saveInModel(final DefaultTableReadConfig<C> config, final NodeSettingsWO settings) {
        settings.addInt(CFG_ROW_ID_IDX, config.getRowIDIdx());
        settings.addLong(CFG_COLUMN_HEADER_IDX, config.getColumnHeaderIdx());
        m_readerSpecificConfigSerializer.saveInModel(config.getReaderSpecificConfig(),
            settings.addNodeSettings(CFG_READER_SPECIFIC_CONFIG));
        settings.addBoolean(CFG_USE_COLUMN_HEADER_IDX, config.useColumnHeaderIdx());
        settings.addBoolean(CFG_USE_ROW_ID_IDX, config.useRowIDIdx());
        settings.addBoolean(CFG_ALLOW_SHORT_ROWS, config.allowShortRows());
        settings.addBoolean(CFG_SKIP_EMPTY_ROWS, config.skipEmptyRows());

        settings.addBoolean(CFG_SKIP_ROWS, config.skipRows());
        settings.addLong(CFG_NUM_ROWS_TO_SKIP, config.getNumRowsToSkip());

        settings.addBoolean(CFG_LIMIT_ROWS, config.limitRows());
        settings.addLong(CFG_MAX_ROWS, config.getMaxRows());

        settings.addBoolean(CFG_LIMIT_ROWS_FOR_SPEC, config.limitRowsForSpec());
        settings.addLong(CFG_MAX_ROWS_FOR_SPEC, config.getMaxRowsForSpec());

    }

    @Override
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getInt(CFG_ROW_ID_IDX);
        settings.getLong(CFG_COLUMN_HEADER_IDX);
        m_readerSpecificConfigSerializer.validate(settings.getNodeSettings(CFG_READER_SPECIFIC_CONFIG));
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
    public void saveInDialog(final DefaultTableReadConfig<C> config, final NodeSettingsWO settings)
        throws InvalidSettingsException {
        saveInModel(config, settings);
    }

}
