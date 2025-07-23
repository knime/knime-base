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
 *   Feb 5, 2025 (david): created
 */
package org.knime.base.node.preproc.constantvalue;

import static org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.validateColumnName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.base.node.preproc.constantvalue.ConstantValueColumnNodeSettings.NewColumnSettings;
import org.knime.base.node.preproc.constantvalue.ConstantValueColumnNodeSettings.NewColumnSettings.AppendOrReplace;
import org.knime.base.node.preproc.constantvalue.ConstantValueColumnNodeSettings.NewColumnSettings.CustomOrMissingValue;
import org.knime.base.node.preproc.constantvalue.ConstantValueColumnNodeSettings.SupportedDataTypeChoicesProvider;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationMessageBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationMessageBuilder.ColumnNameSettingContext;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;

/**
 * New model for WebUI Constant Value Column node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ConstantValueColumnNodeModel2
    extends WebUISimpleStreamableFunctionNodeModel<ConstantValueColumnNodeSettings> {

    /**
     * @param configuration
     */
    protected ConstantValueColumnNodeModel2(final WebUINodeConfiguration configuration) {
        super(configuration, ConstantValueColumnNodeSettings.class);
    }

    @Override
    protected void validateSettings(final ConstantValueColumnNodeSettings settings) throws InvalidSettingsException {
        var firstInvalidCustomValue = Arrays.stream(settings.m_newColumnSettings) //
            .filter(s -> s.m_customOrMissingValue == CustomOrMissingValue.CUSTOM) //
            .filter(ConstantValueColumnNodeModel2::hasInvalidValue) //
            .findFirst();
        if (firstInvalidCustomValue.isPresent()) {
            throw new InvalidSettingsException(
                "The value '" + firstInvalidCustomValue.get().m_value + "' is not a valid value for the selected type "
                    + firstInvalidCustomValue.get().m_type.toPrettyString() + ".");
        }

        // check no column is appended twice
        var outputColumnNames = Arrays.stream(settings.m_newColumnSettings) //
            .filter(s -> s.m_replaceOrAppend == AppendOrReplace.APPEND) //
            .map(s -> s.m_columnNameToAppend) //
            .toList();
        var firstDuplicateColumnName = findFirstDuplicate(outputColumnNames);
        if (firstDuplicateColumnName.isPresent()) {
            throw new InvalidSettingsException(
                "The column name '" + firstDuplicateColumnName.get() + "' is appended multiple times.");
        }
        if (!outputColumnNames.isEmpty()) {
            for (var i = 0; i < settings.m_newColumnSettings.length; i++) {
                final var columnSetting = settings.m_newColumnSettings[i];
                if (columnSetting.m_replaceOrAppend == AppendOrReplace.APPEND) {
                    final var invalidColNameToErrorMessage = new ColumnNameValidationMessageBuilder("new column name")
                        .withSpecificSettingContext(ColumnNameSettingContext.INSIDE_NON_COMPACT_ARRAY_LAYOUT) //
                        .withArrayItemIdentifier(String.format("Constant column %d", i + 1)).build();
                    validateColumnName(columnSetting.m_columnNameToAppend, invalidColNameToErrorMessage);
                }

            }
        }

        // check no column is replaced twice
        var replacedColumnNames = Arrays.stream(settings.m_newColumnSettings) //
            .filter(s -> s.m_replaceOrAppend == AppendOrReplace.REPLACE) //
            .map(s -> s.m_columnNameToReplace) //
            .toList();
        var firstDuplicateReplacedColumnName = findFirstDuplicate(replacedColumnNames);
        if (firstDuplicateReplacedColumnName.isPresent()) {
            throw new InvalidSettingsException(
                "The column name '" + firstDuplicateReplacedColumnName.get() + "' is replaced multiple times.");
        }
    }

    private static boolean hasInvalidValue(final NewColumnSettings s) {
        return SupportedDataTypeChoicesProvider.createDataCellFromString(s.m_value, s.m_type, null).isEmpty();
    }

    /** Get the first duplicate element in a list, if it exists */
    private static <T> Optional<T> findFirstDuplicate(final List<T> values) {
        var seen = new HashSet<T>();

        for (var value : values) {
            if (!seen.add(value)) {
                return Optional.of(value);
            }
        }

        return Optional.empty();
    }

    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final ConstantValueColumnNodeSettings modelSettings) throws InvalidSettingsException {

        if (modelSettings.m_newColumnSettings.length == 0) {
            setWarningMessage("No columns to append or replace.");
        }

        // before we start, we need to check that all replaced columns exist in the input table
        var firstInvalidReplaceColumnName = Arrays.stream(modelSettings.m_newColumnSettings) //
            .filter(s -> s.m_replaceOrAppend == AppendOrReplace.REPLACE) //
            .filter(s -> !ArrayUtils.contains(spec.getColumnNames(), s.m_columnNameToReplace)) //
            .findFirst();
        if (firstInvalidReplaceColumnName.isPresent()) {
            throw new InvalidSettingsException(
                "The column '" + firstInvalidReplaceColumnName.get().m_columnNameToReplace
                    + "' does not exist so cannot be replaced.");
        }

        var rearranger = new ColumnRearranger(spec);
        var uniqueNameGenerator = new UniqueNameGenerator(spec);

        for (var newColumnSettings : modelSettings.m_newColumnSettings) {
            var outputColumnName = newColumnSettings.m_replaceOrAppend == AppendOrReplace.APPEND //
                ? uniqueNameGenerator.newName(newColumnSettings.m_columnNameToAppend) //
                : newColumnSettings.m_columnNameToReplace;

            var outputColumnSpec = new DataColumnSpecCreator( //
                outputColumnName, //
                newColumnSettings.m_type //
            ).createSpec();

            var newColumnCellFactory = new ConstantColumnCellFactory(outputColumnSpec, newColumnSettings, null);

            if (newColumnSettings.m_replaceOrAppend == AppendOrReplace.APPEND) {
                rearranger.append(newColumnCellFactory);
            } else {
                rearranger.replace(newColumnCellFactory, newColumnSettings.m_columnNameToReplace);
            }
        }

        return rearranger;
    }

    private static final class ConstantColumnCellFactory extends SingleCellFactory {

        private final NewColumnSettings m_singleColumnSettings;

        private final ExecutionContext m_ctx;

        public ConstantColumnCellFactory(final DataColumnSpec newColSpec,
            final ConstantValueColumnNodeSettings.NewColumnSettings singleColumnSettings, final ExecutionContext ctx) {
            super(newColSpec);

            m_singleColumnSettings = singleColumnSettings;
            m_ctx = ctx;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            if (m_singleColumnSettings.m_customOrMissingValue == CustomOrMissingValue.MISSING) {
                return new MissingCell("Missing cell from 'Constant Value Column'");
            }

            var dataCell = SupportedDataTypeChoicesProvider.createDataCellFromString(m_singleColumnSettings.m_value,
                m_singleColumnSettings.m_type, m_ctx);

            return dataCell.orElseThrow(() -> new IllegalStateException("Could not create cell of type "
                + m_singleColumnSettings.m_type + " from string '" + m_singleColumnSettings.m_value + "'."));
        }
    }
}
