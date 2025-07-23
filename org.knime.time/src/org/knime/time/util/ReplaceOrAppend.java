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
 *   Nov 5, 2024 (Tobias Kampmann): created
 */
package org.knime.time.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;

/**
 * Convenience enum to handle frequently occurring ValueSwitch to choose between replacing an existing column or
 * appending a new one to the table.
 *
 * Only use once! The utilities provided in this file will only work for the last one used in a node settings.
 *
 * @author Tobias Kampmann, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public enum ReplaceOrAppend {

        /** Replace the existing column */
        @Label(value = "Replace", description = "Replaces the selected columns by the new columns.")
        REPLACE("Replace selected columns"), //
        /** Append a new column with a name based on the existing column name + suffix */
        @Label(value = "Append with suffix", description = """
                Appends the selected columns to the input table with \
                a new name that is the previous name plus the provided suffix.
                """)
        APPEND("Append selected columns"); //

    private final String m_oldConfigValue;

    ReplaceOrAppend(final String oldConfigValue) {
        this.m_oldConfigValue = oldConfigValue;
    }

    /**
     * Create a column rearranger that processes multiple columns. If the mode is {@link #APPEND}, the new column names
     * are created by appending a suffix to the old column names, and each is guaranteed to be unique. If the mode is
     * {@link #REPLACE}, the output column names are the same as the input column names.
     *
     * @param inputColumnNames the names of the columns to process.
     * @param originalSpec the spec of the input table
     * @param cellFactoryFactory a function that creates a {@link SingleCellFactory} for a given input column spec and a
     *            given output column name.
     * @param suffix the suffix to append to the column names if the mode is {@link #APPEND}. If the mode is not append,
     *            this argument is ignored.
     * @param afterProcessingAll a method that is run after processing all individual cell factories
     * @return a column rearranger that processes the input columns.
     */
    public ColumnRearranger createRearranger(final Collection<String> inputColumnNames,
        final DataTableSpec originalSpec, final BiFunction<InputColumn, String, SingleCellFactory> cellFactoryFactory,
        final String suffix, final Runnable afterProcessingAll) {
        return createRearranger(inputColumnNames.stream().toArray(String[]::new), originalSpec, cellFactoryFactory,
            suffix, afterProcessingAll);

    }

    /**
     * See {@link #createRearranger(Collection, DataTableSpec, BiFunction, String, Runnable)}, which this function
     * defers to.
     *
     * @param inputColumnNames
     * @param originalSpec
     * @param cellFactoryFactory
     * @param suffix
     * @param afterProcessingAll
     * @return a column rearranger that processes the input columns.
     */
    public ColumnRearranger createRearranger(final String[] inputColumnNames, final DataTableSpec originalSpec,
        final BiFunction<InputColumn, String, SingleCellFactory> cellFactoryFactory, final String suffix,
        final Runnable afterProcessingAll) {

        final var rearranger = new ColumnRearranger(originalSpec);
        /**
         * In case of REPLACE, the uniqueNameGenerator is not used.
         */
        final var uniqueNameGenerator = this == REPLACE ? null : new UniqueNameGenerator(originalSpec);
        final var inputColumns = toInputColumns(inputColumnNames, originalSpec);
        final var factory = constructCombinedCellFactory(inputColumns, cellFactoryFactory, suffix, uniqueNameGenerator,
            afterProcessingAll);
        if (this == REPLACE) {
            rearranger.replace(factory, inputColumns.stream().mapToInt(InputColumn::index).toArray());
        } else {
            rearranger.append(factory);
        }
        return rearranger;

    }

    private CellFactory constructCombinedCellFactory(final Collection<InputColumn> inputColumns,
        final BiFunction<InputColumn, String, SingleCellFactory> cellFactoryFactory, final String suffix,
        final UniqueNameGenerator uniqueNameGenerator, final Runnable afterProcessingAll) {

        final var singleCellFactories = inputColumns.stream()
            .map(
                inputColumn -> constructSingleCellFactory(inputColumn, cellFactoryFactory, suffix, uniqueNameGenerator))
            .toList();
        return new SingleCellFactoryCombination(singleCellFactories, afterProcessingAll);
    }

    private static List<InputColumn> toInputColumns(final String[] inputColumnNames, final DataTableSpec originalSpec) {
        return Arrays.stream(originalSpec.columnsToIndices(inputColumnNames))//
            .mapToObj(i -> new InputColumn(originalSpec.getColumnSpec(i), i))//
            .toList();
    }

    private SingleCellFactory constructSingleCellFactory(final InputColumn inputColumn,
        final BiFunction<InputColumn, String, SingleCellFactory> cellFactoryFactory, final String suffix,
        final UniqueNameGenerator uniqueNameGenerator) {
        final var inputColumnName = inputColumn.spec.getName();
        return cellFactoryFactory.apply(inputColumn,
            this == REPLACE ? inputColumnName : uniqueNameGenerator.newName(inputColumnName + suffix));

    }

    private static final class SingleCellFactoryCombination extends AbstractCellFactory {

        private final List<SingleCellFactory> m_singleCellFactories;

        private final Runnable m_afterProcessingAll;

        /**
         * @param singleCellFactories
         * @param afterProcessingAll
         */
        SingleCellFactoryCombination(final List<SingleCellFactory> singleCellFactories,
            final Runnable afterProcessingAll) {
            super(singleCellFactories.stream().flatMap(fac -> Arrays.stream(fac.getColumnSpecs()))
                .toArray(DataColumnSpec[]::new));
            m_singleCellFactories = singleCellFactories;
            m_afterProcessingAll = afterProcessingAll;

        }

        @Override
        public DataCell[] getCells(final DataRow row, final long rowIndex) {
            return m_singleCellFactories.stream().map(fac -> fac.getCell(row, rowIndex)).toArray(DataCell[]::new);
        }

        @Override
        public void afterProcessing() {
            m_singleCellFactories.forEach(SingleCellFactory::afterProcessing);
            m_afterProcessingAll.run();
        }

    }

    /**
     * a column spec and its input in the to be processed table.
     *
     * @param spec
     * @param index
     */
    public record InputColumn(DataColumnSpec spec, Integer index) {
    }

    private static ReplaceOrAppend getByOldConfigValue(final String oldValue) throws InvalidSettingsException {
        return Arrays.stream(values()) //
            .filter(v -> v.m_oldConfigValue.equals(oldValue)) //
            .findFirst() //
            .orElseThrow(() -> new InvalidSettingsException(
                String.format("Invalid value '%s'. Possible values: %s", oldValue, getOldConfigValues())));
    }

    private static String[] getOldConfigValues() {
        return Arrays.stream(values()).map(v -> v.m_oldConfigValue).toArray(String[]::new);
    }

    /**
     * A reference for the ReplaceOrAppend enum. Will only work for the last usage of {@link ReplaceOrAppend} in a node
     * settings.
     *
     * @see ValueReference
     */
    public interface ValueRef extends ParameterReference<ReplaceOrAppend> {
    }

    /**
     * Predicate to check if the selected value is {@link #APPEND}. Will only work for the last usage of
     * {@link ReplaceOrAppend} in a node settings.
     */
    public static final class IsAppend implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ValueRef.class).isOneOf(ReplaceOrAppend.APPEND);
        }
    }

    /**
     * Predicate to check if the selected value is {@link #REPLACE}. Will only work for the last usage of
     * {@link ReplaceOrAppend} in a node settings.
     */
    public static final class IsReplace implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ValueRef.class).isOneOf(ReplaceOrAppend.REPLACE);
        }
    }

    /**
     * Backwards-compatible persistor to store the value of the enum in the node settings. Since the key is hardcoded,
     * it will only work for the last usage of {@link ReplaceOrAppend} in a node settings.
     */
    public static final class Persistor implements NodeParametersPersistor<ReplaceOrAppend> {

        private static final String CONFIG_KEY = "replace_or_append";

        @Override
        public ReplaceOrAppend load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return ReplaceOrAppend.getByOldConfigValue(settings.getString(CONFIG_KEY));
        }

        @Override
        public void save(final ReplaceOrAppend obj, final NodeSettingsWO settings) {
            settings.addString(CONFIG_KEY, obj.m_oldConfigValue);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CONFIG_KEY}};
        }
    }

}
