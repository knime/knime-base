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
 *   Feb 9, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableTableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;

/**
 * {@link TableSpecConfigSerializer} for configs created and stored in KNIME 4.3.x.<br>
 * Only supports loading and throws an {@link IllegalStateException} when
 * {@link TableSpecConfigSerializer#save(TableSpecConfig, NodeSettingsWO)} is called because configs should always be
 * saved with the latest serializer.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class V43TableSpecConfigSerializer<T> implements TableSpecConfigSerializer<T> {

    private static final String CFG_ENFORCE_TYPES = "enforce_types";

    private static final String CFG_INCLUDE_UNKNOWN = "include_unknown_columns" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_NEW_COLUMN_POSITION = "unknown_column_position" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_ORIGINAL_NAMES = "original_names" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_POSITIONAL_MAPPING = "positional_mapping" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_COLUMN_FILTER_MODE = "column_filter_mode" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_KEEP = "keep" + SettingsModel.CFGKEY_INTERNAL;

    /**
     * This option was only introduced with 4.4.0 therefore configs stored with 4.3.0 can't have it.
     */
    private static final boolean SKIP_EMPTY_COLUMNS = false;

    private final ProductionPathSerializer m_productionPathSerializer;

    V43TableSpecConfigSerializer(final ProductionPathSerializer productionPathSerializer) {
        m_productionPathSerializer = productionPathSerializer;
    }

    static boolean isV43OrGreater(final NodeSettingsRO settings) {
        return settings.containsKey(CFG_INCLUDE_UNKNOWN);
    }

    @Override
    public void save(final TableSpecConfig<T> object, final NodeSettingsWO settings) {
        throw new IllegalStateException(
            "This is a compatibility serializer used for loading old settings stored with KNIME AP 4.3.x. "
                + "Use the latest serializer for saving.");
    }

    @Override
    public TableSpecConfig<T> load(final NodeSettingsRO settings, final AdditionalParameters additionalParameters)
        throws InvalidSettingsException {
        return load(settings);
    }

    @Override
    public TableSpecConfig<T> load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Pre44Loader loader = new Pre44Loader(settings, m_productionPathSerializer);
        final ImmutableTableTransformation<T> tableTransformation = loadTableTransformation(settings, loader);
        return loader.createTableSpecConfig(tableTransformation);
    }

    private ImmutableTableTransformation<T> loadTableTransformation(final NodeSettingsRO settings,
        final Pre44Loader loader) throws InvalidSettingsException {
        final boolean includeUnknownColumns = settings.getBoolean(CFG_INCLUDE_UNKNOWN);
        final boolean enforceTypes = settings.getBoolean(CFG_ENFORCE_TYPES);
        final int newColPosition = settings.getInt(CFG_NEW_COLUMN_POSITION);
        final String[] originalNames = settings.getStringArray(CFG_ORIGINAL_NAMES);
        final int[] positions = settings.getIntArray(CFG_POSITIONAL_MAPPING);
        final boolean[] keep = settings.getBooleanArray(CFG_KEEP);
        final ColumnFilterMode columnFilterMode = loadColumnFilterMode(settings);
        final List<ImmutableColumnTransformation<T>> columnTransformations = Pre44Loader.createColumnTransformations(
            loader.getLoadedKnimeSpec(), loader.getProductionPaths(), originalNames, positions, keep);
        final RawSpec<T> rawSpec =
            loader.createRawSpec(loader.getLoadedKnimeSpec(), loader.getProductionPaths(), originalNames);
        return new ImmutableTableTransformation<>(columnTransformations, rawSpec, columnFilterMode, newColPosition,
            includeUnknownColumns, enforceTypes, SKIP_EMPTY_COLUMNS);
    }

    private static ColumnFilterMode loadColumnFilterMode(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        return ColumnFilterMode.valueOf(settings.getString(CFG_COLUMN_FILTER_MODE));
    }

}
