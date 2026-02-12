/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   12.02.2026 (mgohm): created
 */
package org.knime.base.node.preproc.tablestructure;

import static org.knime.core.node.util.CheckUtils.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.ColumnExistenceHandling;
import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.DataTypeHandling;
import org.knime.base.node.preproc.datavalidator.DataValidatorConfiguration.RejectBehavior;
import org.knime.base.node.preproc.datavalidator.DataValidatorSpecNodeParameters.ColumnNameMatchingEnum;
import org.knime.base.node.preproc.tablestructure.TableStructureValidatorColConflicts.TableStructureValidatorColConflict;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * {@link WebUINodeModel} for the Table Structure Validator node.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings({"deprecation", "restriction"})
class TableStructureValidatorNodeModel extends WebUINodeModel<TableStructureValidatorNodeParameters> {

    /**
     * Constructor of the Table Structure Validator node model.
     */
    TableStructureValidatorNodeModel() {
        super(new PortType[]{BufferedDataTable.TYPE},
            new PortType[]{BufferedDataTable.TYPE, BufferedDataTable.TYPE},
            TableStructureValidatorNodeParameters.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs,
        final TableStructureValidatorNodeParameters modelSettings) throws InvalidSettingsException {
        final var columnRearranger =
                createRearranger(inSpecs[0], new TableStructureValidatorColConflicts(), modelSettings);
        return new DataTableSpec[]{columnRearranger.createSpec(), TableStructureValidatorColConflicts.CONFLICTS_SPEC};
    }

    @SuppressWarnings("java:S1941") // column re-arranger and return table creation can't be moved
    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec,
        final TableStructureValidatorNodeParameters modelSettings) throws Exception {
        final var in = ((BufferedDataTable)inData[0]).getDataTableSpec();
        var conflicts = new TableStructureValidatorColConflicts();
        var columnRearranger = createRearranger(in, conflicts, modelSettings);

        if (!conflicts.isEmpty() && modelSettings.m_validationFailureBehavior == RejectBehavior.FAIL_NODE) {
            throw new InvalidSettingsException("Validation failed:\n" + conflicts);
        }

        var returnTable = exec.createColumnRearrangeTable(
            (BufferedDataTable)inData[0], columnRearranger, exec.createSubExecutionContext(0.9));

        if (!conflicts.isEmpty()) {
            if (modelSettings.m_validationFailureBehavior == RejectBehavior.OUTPUT_TO_PORT_CHECK_DATA) {
                return new PortObject[]{InactiveBranchPortObject.INSTANCE,
                    createConflictsTable(conflicts, exec.createSubExecutionContext(0.1))};
            } else {
                throw new InvalidSettingsException("Validation failed:\n" + conflicts);
            }
        }

        return new PortObject[]{returnTable, InactiveBranchPortObject.INSTANCE};
    }

    /**
     * Creates a table containing the validation conflicts, according to the specification defined in
     * {@link TableStructureValidatorColConflicts#CONFLICTS_SPEC}.
     *
     * @param conflicts the list of conflicts to be included in the table
     * @param exec the execution context to create the table in
     * @return {@link BufferedDataTable} containing the conflicts
     */
    private static BufferedDataTable createConflictsTable(final TableStructureValidatorColConflicts conflicts,
        final ExecutionContext exec) {
        var createDataContainer = exec.createDataContainer(TableStructureValidatorColConflicts.CONFLICTS_SPEC);
        var index = 0;
        for (TableStructureValidatorColConflict conflict : conflicts) {
            createDataContainer.addRowToTable(conflict.toDataRow(RowKey.createRowKey(index)));
            index++;
        }
        createDataContainer.close();
        return createDataContainer.getTable();
    }

    private static ColumnRearranger createRearranger(final DataTableSpec in,
        final TableStructureValidatorColConflicts conflicts, final TableStructureValidatorNodeParameters modelSettings)
        throws InvalidSettingsException {
        // create the reference spec from the model settings
        final var referenceTableSpec =
            new DataTableSpec(Arrays.stream(modelSettings.m_referenceStructureColumns).map(col -> {
                final DataColumnSpecCreator colSpecCreator =
                    new DataColumnSpecCreator(col.m_columnName, col.m_columnType);
                return colSpecCreator.createSpec();
            }).toArray(DataColumnSpec[]::new));

        // sort the entries according to the reference spec - and the best case insensitive matchings.
        final var colToValidation = applyColumnValidation(in, modelSettings, conflicts);
        @SuppressWarnings("unchecked")
        Entry<String, ColumnValidationContainer>[] array = colToValidation.entrySet().toArray(new Entry[0]);
        Arrays.sort(array, sortAccordingToSpecComparator(referenceTableSpec));

        final var validatedColumnNamesAndDecorators = extractValidatedColumnNamesAndCreateValidatorDecorators(
            modelSettings, array, referenceTableSpec, conflicts, in);
        final var validatedColumnNamesOfInputSpec = validatedColumnNamesAndDecorators.columnNames();
        final var decorators = validatedColumnNamesAndDecorators.decorators();

        final var columnRearranger = new ColumnRearranger(in);

        switch (modelSettings.m_additionalColumnsHandling) {
            case REJECT:
                addUnknownColumnConflicts(validatedColumnNamesOfInputSpec, in, conflicts);
                break;
            case REMOVE:
                columnRearranger.keepOnly(validatedColumnNamesOfInputSpec);
                break;
            default:
                // IGNORE is done by permute.
                break;
        }
        // sort the names etc.
        columnRearranger.permute(validatedColumnNamesOfInputSpec);

        for (Entry<String, TableStructureValidatorCellDecorator> decorator : decorators.entrySet()) {
            columnRearranger.replace(createCellFactory(decorator.getValue(), in.findColumnIndex(decorator.getKey())),
                decorator.getKey());
        }

        if (modelSettings.m_missingColumnHandling != ColumnExistenceHandling.NONE) {
            handleMissingColumns(columnRearranger, referenceTableSpec);
        }

        return columnRearranger;
    }

    private static ColumnNamesAndDecorators extractValidatedColumnNamesAndCreateValidatorDecorators(
        final TableStructureValidatorNodeParameters modelSettings,
        final Entry<String, ColumnValidationContainer>[] array,
        final DataTableSpec referenceTableSpec,
        final TableStructureValidatorColConflicts conflicts,
        final DataTableSpec in) throws InvalidSettingsException {
        var namesOfInputSpec = new String[array.length];
        var index = 0;
        Map<String, TableStructureValidatorCellDecorator> decorators = new HashMap<>();
        for (Entry<String, ColumnValidationContainer> arr : array) {
            namesOfInputSpec[index] = arr.getKey();
            final var colValidationContainer = arr.getValue();

            final var colIndex = referenceTableSpec.findColumnIndex(colValidationContainer.getRefColName());
            if (colIndex < 0) {
                throw new InvalidSettingsException("The configured column '%s' is not present in the reference table."
                        .formatted(colValidationContainer.getRefColName()));
            }

            final var shouldBeTraversed = isAffectedByValidationConfiguration(
                referenceTableSpec.getColumnSpec(colIndex), in.getColumnSpec(arr.getKey()), modelSettings, conflicts);

            if (shouldBeTraversed) {
                decorators.put(arr.getKey(), createCellValidator(modelSettings,
                    referenceTableSpec.getColumnSpec(colIndex), in.getColumnSpec(arr.getKey()), conflicts));
            }
            index++;
        }

        return new ColumnNamesAndDecorators(namesOfInputSpec, decorators);
    }

    @SuppressWarnings("java:S6218") // different record instances are not compared or printed
    record ColumnNamesAndDecorators(
        String[] columnNames, Map<String, TableStructureValidatorCellDecorator> decorators) {
    }

    private static void handleMissingColumns(
        final ColumnRearranger columnRearranger,
        final DataTableSpec referenceTableSpec) {
        var i = 0;
        final var resultSpec = columnRearranger.createSpec();
        for (DataColumnSpec colSpec : referenceTableSpec) {
            // configured but not existing columns are filled with missing values.
            if (!resultSpec.containsName(colSpec.getName())) {
                final var handledMissingFactory = createMissingValsOnlyCellFactory(colSpec);
                if (i < columnRearranger.getColumnCount()) {
                    columnRearranger.insertAt(i, handledMissingFactory);
                } else {
                    /*
                     * At this point, we have more columns configured than the input provides, and also
                     * might have skipped some in between (since `i` is incremented unconditionally). Thus, we
                     * are in a weird state of trying to merge and output possibly disjoint specs together.
                     *
                     * This seemed to never have worked prior to AP-21796, and without re-working column handling
                     * of this old node, simply append missing columns seems like a fair solution.
                     */
                    columnRearranger.append(handledMissingFactory);
                }
            }
            i++;
        }
    }

    private static void addUnknownColumnConflicts(final String[] validatedColumnNamesOfInputSpec,
        final DataTableSpec in, final TableStructureValidatorColConflicts conflicts) {
        if (validatedColumnNamesOfInputSpec.length < in.getNumColumns()) {
            final var validatedColumns = new HashSet<>(Arrays.asList(validatedColumnNamesOfInputSpec));
            final var allColumnsOfInSpec = new HashSet<>(Arrays.asList(in.getColumnNames()));
            allColumnsOfInSpec.removeAll(validatedColumns);
            // add the difference the unknown columns to the conflicts
            for (String col : allColumnsOfInSpec) {
                conflicts.addConflict(TableStructureValidatorColConflicts.unknownColumn(col));
            }
        }
    }

    private static SingleCellFactory createCellFactory(
        final TableStructureValidatorCellDecorator decorator, final int index) {

        return new SingleCellFactory(false, decorator.getDataColumnSpec()) {

            @Override
            public DataCell getCell(final DataRow row) {
                return decorator.handleCell(row.getKey(), row.getCell(index));
            }
        };
    }

    private static SingleCellFactory createMissingValsOnlyCellFactory(final DataColumnSpec spec) {

        return new SingleCellFactory(false, spec) {

            @Override
            public DataCell getCell(final DataRow row) {
                return DataType.getMissingCell();
            }
        };
    }

    private static Comparator<Entry<String, ColumnValidationContainer>> sortAccordingToSpecComparator(
        final DataTableSpec referenceTableSpec) {
        return (o1, o2) -> Integer.compare(
            referenceTableSpec.findColumnIndex(o1.getValue().getRefColName()),
            referenceTableSpec.findColumnIndex(o2.getValue().getRefColName()));
    }

    @SuppressWarnings("java:S3047") // first two for loops need to stay separated
    private static Map<String, ColumnValidationContainer> applyColumnValidation(final DataTableSpec in,
        final TableStructureValidatorNodeParameters modelSettings,
        final TableStructureValidatorColConflicts conflicts) {
        Map<String, ColumnValidationContainer> toReturn = new LinkedHashMap<>();
        Map<String, ColumnValidationContainer> directMatches = new LinkedHashMap<>();
        List<ColumnValidationContainer> caseInsensitiveMatches = new ArrayList<>();
        findDirectAndCaseInsensitiveMatches(directMatches, caseInsensitiveMatches, modelSettings);

        // try first to find direct matches.
        for (DataColumnSpec s : in) {
            final var name = s.getName();
            var directColumnValidationContainer = directMatches.remove(name);

            if (directColumnValidationContainer != null) {
                directColumnValidationContainer.setInputColName(name);
                toReturn.put(name, directColumnValidationContainer);
            }
        }

        // now check the case insensitive matches
        for (DataColumnSpec s : in) {
            final var name = s.getName();
            if (!toReturn.containsKey(name)) {
                var caseInsensitiveMatch =
                        removeFirstMatchingCaseInsensitiveValidationContainers(name, caseInsensitiveMatches);
                if (caseInsensitiveMatch != null) {
                    caseInsensitiveMatch.setInputColName(name);
                    toReturn.put(name, caseInsensitiveMatch);
                }
            }
        }

        // check for not satisfied validations
        for (Entry<String, ColumnValidationContainer> colToValidation : directMatches.entrySet()) {
            if (!colToValidation.getValue().isSatisfied()
                    && ColumnExistenceHandling.FAIL == modelSettings.m_missingColumnHandling) {
                conflicts.addConflict(TableStructureValidatorColConflicts.missingColumn(colToValidation.getKey()));
            }
        }

        return toReturn;
    }

    private static void findDirectAndCaseInsensitiveMatches(final Map<String, ColumnValidationContainer> directMatches,
        final List<ColumnValidationContainer> caseInsensitiveMatches,
        final TableStructureValidatorNodeParameters modelSettings) {
        final var referenceColumnNames =
                Arrays.stream(modelSettings.m_referenceStructureColumns).map(c -> c.m_columnName).toList();
        for (String name : referenceColumnNames) {
            directMatches.computeIfAbsent(name, matchedName -> {
                var configurationContainer = new ColumnValidationContainer(matchedName, null);
                if (modelSettings.m_columnNameMatchingBehavior  == ColumnNameMatchingEnum.CASE_INSENSITIVE) {
                    caseInsensitiveMatches.add(configurationContainer);
                }
                return configurationContainer;
            });
        }
    }

    private static ColumnValidationContainer removeFirstMatchingCaseInsensitiveValidationContainers(final String name,
        final List<ColumnValidationContainer> caseInsensitiveMatches) {
        var items = caseInsensitiveMatches.iterator();
        final var upperName = name.toUpperCase(Locale.ROOT);
        while (items.hasNext()) {
            var next = items.next();
            if (next.isSatisfied()) {
                items.remove();
            }
            if (!next.isSatisfied() && upperName.equals(next.getRefColName().toUpperCase(Locale.ROOT))) {
                next.setInputColName(name);
                items.remove();
                return next;
            }
        }
        return null;
    }

    static final class ColumnValidationContainer {

        private final String m_refColName;
        private String m_inputColName;

        private ColumnValidationContainer(final String refColName, final String inputColName) {
            m_refColName = checkNotNull(refColName);
            m_inputColName = inputColName;
        }

        String getRefColName() {
            return m_refColName;
        }

        String getInputColName() {
            return m_inputColName;
        }

        void setInputColName(final String inputColName) {
            m_inputColName = inputColName;
        }

        boolean isSatisfied() {
            return m_inputColName != null;
        }

    }

    @SuppressWarnings("java:S1151") // number of lines switch case statement
    private static boolean isAffectedByValidationConfiguration(final DataColumnSpec referenceColSpec,
        final DataColumnSpec inputColSpec,
        final TableStructureValidatorNodeParameters modelSettings,
        final TableStructureValidatorColConflicts conflicts) {
        final var nameShouldBeChanged = !inputColSpec.getName().equals(referenceColSpec.getName());
        switch (modelSettings.m_dataTypeHandling) {
            case NONE:
                return nameShouldBeChanged;
            case FAIL:
                if (!referenceColSpec.getType().isASuperTypeOf(inputColSpec.getType())) {
                    conflicts.addConflict(TableStructureValidatorColConflicts.invalidType(inputColSpec.getName(),
                        referenceColSpec.getType(),
                        inputColSpec.getType()));
                }
                return nameShouldBeChanged;
            case CONVERT_FAIL:
                return nameShouldBeChanged || isNotEqualType(referenceColSpec, inputColSpec);
            default:
                throw new IllegalArgumentException("Unknown data type handling: " + modelSettings.m_dataTypeHandling);
        }
    }

    private static boolean isNotEqualType(final DataColumnSpec referenceColSpec, final DataColumnSpec inputColSpec) {
        return !referenceColSpec.getType().equals(inputColSpec.getType());
    }

    private static TableStructureValidatorCellDecorator createCellValidator(
        final TableStructureValidatorNodeParameters modelSettings, final DataColumnSpec refColumnSpec,
        final DataColumnSpec originalColumnSpec, final TableStructureValidatorColConflicts conflicts) {
        var renamedColumnSpec = new DataColumnSpecCreator(originalColumnSpec);
        renamedColumnSpec.setName(refColumnSpec.getName());
        var decorator = TableStructureValidatorCellDecorator.forColumn(
            originalColumnSpec.getName(), renamedColumnSpec.createSpec());
        final var reject  = isNotCompatible(refColumnSpec, originalColumnSpec)
            && DataTypeHandling.FAIL == modelSettings.m_dataTypeHandling;

        if (!reject
                && !EnumSet.of(DataTypeHandling.NONE, DataTypeHandling.FAIL).contains(modelSettings.m_dataTypeHandling)
                && isNotEqualType(refColumnSpec, originalColumnSpec)) {
            decorator =
                TableStructureValidatorCellDecorator.conversionCellDecorator(decorator,
                    modelSettings.m_dataTypeHandling, getConversionType(refColumnSpec), refColumnSpec, conflicts);
        }
        return decorator;
    }

    private static boolean isNotCompatible(final DataColumnSpec refColumnSpec,
        final DataColumnSpec originalColumnSpec) {
        return !refColumnSpec.getType().isASuperTypeOf(originalColumnSpec.getType());
    }

    private static ConversionType getConversionType(final DataColumnSpec refColumnSpec) {
        final var type = refColumnSpec.getType();

        // NOTE: The sequence here is important as we go from the most specific general type to the most general one
        if (BooleanCell.TYPE.isASuperTypeOf(type)) {
            return ConversionType.BOOLEAN;
        }
        if (IntCell.TYPE.isASuperTypeOf(type)) {
            return ConversionType.INT;
        }
        if (LongCell.TYPE.isASuperTypeOf(type)) {
            return ConversionType.LONG;
        }
        if (DoubleCell.TYPE.isASuperTypeOf(type)) {
            return ConversionType.DOUBLE;
        }
        if (StringCell.TYPE.isASuperTypeOf(type)) {
            return ConversionType.STRING;
        }
        throw new IllegalArgumentException("Type cannot be converted, " + type + " only "
            + Arrays.toString(ConversionType.values()) + " are supported types.");
    }

    enum ConversionType {

        BOOLEAN(BooleanCell.TYPE) {
            @Override
            public DataCell convertCell(final DataCell decoratedCell) {
                return Boolean.parseBoolean(decoratedCell.toString()) ? BooleanCell.TRUE : BooleanCell.FALSE;
            }
        }, //
        DOUBLE(DoubleCell.TYPE) {
            @Override
            public DataCell convertCell(final DataCell decoratedCell) {
                return new DoubleCell(Double.valueOf(decoratedCell.toString().replace(",", ".")));
            }
        }, //
        INT(IntCell.TYPE) {
            @Override
            public DataCell convertCell(final DataCell decoratedCell) {
                return new IntCell(Integer.valueOf(decoratedCell.toString()));
            }
        }, //
        LONG(LongCell.TYPE) {
            @Override
            public DataCell convertCell(final DataCell decoratedCell) {
                return new LongCell(Long.valueOf(decoratedCell.toString()));
            }
        }, //
        STRING(StringCell.TYPE) {
            @Override
            public DataCell convertCell(final DataCell decoratedCell) {
                return new StringCell(decoratedCell.toString());
            }
        };

        private final DataType m_dataType;

        ConversionType(final DataType dataType) {
            m_dataType = dataType;
        }

        @Override
        public String toString() {
            return name().substring(0, 1).toUpperCase(Locale.ROOT) + name().substring(1).toLowerCase(Locale.ROOT);
        }

        /**
         * Converts the given {@link DataCell}.
         *
         * @param decoratedCell the {@link DataCell} to convert
         * @return the converted {@link DataCell}
         */
        public abstract DataCell convertCell(final DataCell decoratedCell);

        /**
         * Retrieves the target {@link DataType}.
         *
         * @return the target {@link DataType}
         */
        public DataType getTargetType() {
            return m_dataType;
        }

    }

    @Override
    protected void reset() {
        // no op
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no op
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec) throws IOException,
        CanceledExecutionException {
        // no op
    }

}
