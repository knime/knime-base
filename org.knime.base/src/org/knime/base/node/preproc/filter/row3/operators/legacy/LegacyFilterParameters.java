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
 *   Sep 24, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.filter.row3.operators.legacy;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.OptionalInt;

import org.knime.base.data.filter.row.v2.IndexedRowReadPredicate;
import org.knime.base.node.preproc.filter.row3.RowIdentifiers;
import org.knime.base.node.preproc.filter.row3.operators.rownumber.RowNumberFilterSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.LongValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.config.base.JSONConfig;
import org.knime.core.node.config.base.JSONConfig.WriterConfig;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.node.parameters.NodeParameters;

/**
 * Parameters class to handle legacy filter settings (from first webui migration using DynamicValuesInput).
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings({ "restriction", "deprecation" })
public class LegacyFilterParameters implements FilterValueParameters {

    String m_legacySettings; // string since it needs to be serializable

    private LoadedLegacyFilterParameters m_loadedCache;

    private static String settingsToString(final NodeSettingsRO settings) throws IOException {
        final var w = new StringWriter();
        JSONConfig.writeJSON(settings, w, WriterConfig.DEFAULT);
        return w.toString();
    }

    private static NodeSettingsRO stringToNodeSettings(final String str) throws IOException {
        final var readSettings = new NodeSettings("legacySettings");
        final var reader = new StringReader(str);
        JSONConfig.readJSON(readSettings, reader);
        return readSettings;
    }

    void setFromNodeSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            m_legacySettings = settingsToString(settings);
        } catch (IOException e) {
            throw new InvalidSettingsException("Could not serialize legacy settings", e);
        }

    }

    LoadedLegacyFilterParameters getLoaded() throws InvalidSettingsException {
        if (m_loadedCache == null) {
            m_loadedCache = loadToLegacyParameters();
        }
        return m_loadedCache;
    }

    LoadedLegacyFilterParameters loadToLegacyParameters() throws InvalidSettingsException {
        try {
            final var nodeSettingsRO = stringToNodeSettings(m_legacySettings);
            return LoadedLegacyFilterParameters.fromNodeSettings(nodeSettingsRO);
        } catch (final IOException e) {
            throw new InvalidSettingsException("Could not deserialize legacy settings", e);
        }

    }

    static class LoadedLegacyFilterParameters implements NodeParameters {
        /**
         * Legacy filter operator.
         */
        // keep name, set directly by framework based on legacy config
        LegacyFilterOperator m_operator;

        // keep name, set directly by framework based on legacy config
        DynamicValuesInput m_predicateValues;

        static LoadedLegacyFilterParameters fromNodeSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            return NodeParametersUtil.loadSettings(settings, LoadedLegacyFilterParameters.class);
        }

    }

    /**
     * Create a predicate for the given column based on the legacy filter settings.
     *
     * @param column column to filter on
     * @param spec input table spec
     * @param optionalTableSize optional table size, needed for some operators like "Last N Rows", -1 if unknown
     * @return predicate for the given column
     * @throws InvalidSettingsException if the settings are invalid
     */
    public IndexedRowReadPredicate toPredicate(final StringOrEnum<RowIdentifiers> column, final DataTableSpec spec,
        final long optionalTableSize) throws InvalidSettingsException {

        final var rowIdentifierChoice = column.getEnumChoice();
        if (rowIdentifierChoice.isPresent()) {
            return switch (rowIdentifierChoice.get()) {
                case ROW_ID -> getRowKeyPredicate();
                case ROW_NUMBER -> getRowNumberPredicate(optionalTableSize);
            };
        }
        final var columnIndex = spec.findColumnIndex(column.getStringChoice());
        if (columnIndex < 0) {
            throw new InvalidSettingsException("Column \"%s\" could not be found in input table".formatted(column));
        }
        final var loaded = getLoaded();
        return loaded.m_operator.translateToPredicate(loaded.m_predicateValues, columnIndex,
            spec.getColumnSpec(columnIndex).getType());

    }

    private IndexedRowReadPredicate getRowKeyPredicate() throws InvalidSettingsException {
        final var loaded = getLoaded();
        return PredicateFactories //
            .getRowKeyPredicateFactory(loaded.m_operator) //
            .orElseThrow(() -> new InvalidSettingsException( //
                "Unsupported operator \"%s\" for RowID comparison".formatted(loaded.m_operator.name()))) //
            .createPredicate(OptionalInt.empty(), loaded.m_predicateValues);
    }

    private IndexedRowReadPredicate getRowNumberPredicate(final long optionalTableSize)
        throws InvalidSettingsException {
        final var loaded = getLoaded();
        final var predicateFactory = PredicateFactories //
            .getRowNumberPredicateFactory(loaded.m_operator);
        if (predicateFactory.isPresent()) {
            return predicateFactory.get().createPredicate(OptionalInt.empty(), loaded.m_predicateValues);
        }
        return toFilterSpec().toOffsetFilter(optionalTableSize).asPredicate();

    }

    /**
     * If supported, get the criterion as a row number filter specification.
     *
     * @return row number filter specification
     * @throws InvalidSettingsException if the filter criterion contains an unsupported operator or the value is missing
     */
    public RowNumberFilterSpec toFilterSpec() throws InvalidSettingsException {
        final var loaded = getLoaded();
        final var value = (loaded.m_predicateValues.getCellAt(0)//
            .filter(cell -> !cell.isMissing())//
            .map(LongValue.class::cast)//
            .orElseThrow(() -> new InvalidSettingsException("Row number value is missing")))//
                .getLongValue();
        if (loaded.m_operator == LegacyFilterOperator.FIRST_N_ROWS
            || loaded.m_operator == LegacyFilterOperator.LAST_N_ROWS) {
            CheckUtils.checkSetting(value >= 0, "Number of rows must not be negative: %d", value);
        } else {
            CheckUtils.checkSetting(value > 0, "Row number must be larger than zero: %d", value);
        }
        return new RowNumberFilterSpec(loaded.m_operator, value);
    }

    @Override
    public DataValue[] stash() {
        try {
            final var loaded = getLoaded();
            return loaded.m_predicateValues.getCellAt(0).stream().toArray(DataValue[]::new);
        } catch (final InvalidSettingsException e) {
            // Illegal since settings have been validated before
            throw new IllegalStateException("Could not load legacy settings", e);
        }
    }

}
