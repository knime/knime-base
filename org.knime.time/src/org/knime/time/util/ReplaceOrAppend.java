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
import java.util.function.BiFunction;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

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
        @Label(value = "Replace", description = "The selected columns will be replaced by the new columns.")
        REPLACE("Replace selected columns"), //
        /** Append a new column with a name based on the existing column name + suffix */
        @Label(value = "Append with suffix", description = """
                The selected columns will be appended to the input table with \
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
     * @return a column rearranger that processes the input columns.
     */
    public ColumnRearranger createRearranger(final Iterable<String> inputColumnNames, final DataTableSpec originalSpec,
        final BiFunction<DataColumnSpec, String, SingleCellFactory> cellFactoryFactory, final String suffix) {

        final var rearranger = new ColumnRearranger(originalSpec);

        if (this == REPLACE) {
            for (String inputName : inputColumnNames) {
                var inputSpec = originalSpec.getColumnSpec(inputName);
                rearranger.replace(cellFactoryFactory.apply(inputSpec, inputName),
                    originalSpec.findColumnIndex(inputName));
            }
        } else {
            var uniqueNameGenerator = new UniqueNameGenerator(originalSpec);

            for (String inputName : inputColumnNames) {
                var inputSpec = originalSpec.getColumnSpec(inputName);
                var newName = uniqueNameGenerator.newName(inputName + suffix);
                var factory = cellFactoryFactory.apply(inputSpec, newName);

                rearranger.append(factory);
            }
        }

        return rearranger;
    }

    /**
     * See {@link #createRearranger(Iterable, DataTableSpec, BiFunction, String)}, which this function defers to.
     *
     * @param inputColumnNames
     * @param originalSpec
     * @param cellFactoryFactory
     * @param suffix
     * @return a column rearranger that processes the input columns.
     */
    public ColumnRearranger createRearranger(final String[] inputColumnNames, final DataTableSpec originalSpec,
        final BiFunction<DataColumnSpec, String, SingleCellFactory> cellFactoryFactory, final String suffix) {

        return createRearranger(Arrays.asList(inputColumnNames), originalSpec, cellFactoryFactory, suffix);
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
    public interface ValueRef extends Reference<ReplaceOrAppend> {
    }

    /**
     * Predicate to check if the selected value is {@link #APPEND}. Will only work for the last usage of
     * {@link ReplaceOrAppend} in a node settings.
     */
    public static final class IsAppend implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ValueRef.class).isOneOf(ReplaceOrAppend.APPEND);
        }
    }

    /**
     * Predicate to check if the selected value is {@link #REPLACE}. Will only work for the last usage of
     * {@link ReplaceOrAppend} in a node settings.
     */
    public static final class IsReplace implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ValueRef.class).isOneOf(ReplaceOrAppend.REPLACE);
        }
    }

    /**
     * Backwards-compatible persistor to store the value of the enum in the node settings. Since the key is hardcoded,
     * it will only work for the last usage of {@link ReplaceOrAppend} in a node settings.
     */
    public static final class Persistor implements NodeSettingsPersistor<ReplaceOrAppend> {

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
