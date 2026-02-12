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
 */
package org.knime.base.node.preproc.tablestructure;

import static org.knime.core.node.util.CheckUtils.checkNotNull;
import static org.knime.core.node.util.CheckUtils.checkSettingNotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;

import org.knime.base.node.preproc.tablestructure.TableStructureValidatorNodeParameters.CaseMatchingBehavior;
import org.knime.base.node.preproc.tablestructure.TableStructureValidatorNodeParameters.ColumnExistenceHandling;
import org.knime.base.node.preproc.tablestructure.TableStructureValidatorNodeParameters.DataTypeHandling;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;

/**
 * Configuration object for a couple of columns.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
final class TableStructureValidatorColConfiguration {

    private String[] m_names;
    private DataTypeHandling m_dataTypeHandling = DataTypeHandling.FAIL;
    private ColumnExistenceHandling m_columnExistenceHandling = ColumnExistenceHandling.FAIL;
    private boolean m_caseInsensitiveNameMatching;

    /**
     * Constructor for list of column names
     *
     * @param names the names
     */
    TableStructureValidatorColConfiguration(final String[] names) {
        m_names = checkNotNull(names);
    }

    String[] getNames() {
        return m_names;
    }

    void setDataTypeHandling(final DataTypeHandling dataTypeHandling) {
        m_dataTypeHandling = dataTypeHandling;
    }

    boolean isCaseInsensitiveNameMatching() {
        return m_caseInsensitiveNameMatching;
    }

    void setCaseInsensitiveNameMatching(final boolean caseInsensitiveNameMatching) {
        m_caseInsensitiveNameMatching = caseInsensitiveNameMatching;
    }

    ColumnExistenceHandling getColumnExistenceHandling() {
        return m_columnExistenceHandling;
    }

    void setColumnExistenceHandling(final ColumnExistenceHandling columnExistenceHandling) {
        m_columnExistenceHandling = columnExistenceHandling;
    }

    /**
     * Checks this configuration against a given input {@link DataColumnSpec}.
     *
     * @param referenceColSpec the reference {@link DataColumnSpec}
     * @param inputColSpec the input {@link DataColumnSpec}
     * @param conflicts conflicts the {@link TableStructureValidatorColConflicts}
     * @return <code>true</code> if a decorator for this column should be created.
     */
    @SuppressWarnings("java:S1151") // number of lines switch case statement
    boolean applyColConfiguration(final DataColumnSpec referenceColSpec, final DataColumnSpec inputColSpec,
        final TableStructureValidatorColConflicts conflicts) {
        boolean nameShouldBeChanged = !inputColSpec.getName().equals(referenceColSpec.getName());
        switch (m_dataTypeHandling) {
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
                throw new IllegalArgumentException("Unknown type...");
        }
    }

    private static boolean isNotEqualType(final DataColumnSpec referenceColSpec, final DataColumnSpec inputColSpec) {
        return !referenceColSpec.getType().equals(inputColSpec.getType());
    }

    /**
     * Loads settings from the {@link TableStructureValidatorNodeParameters}.
     *
     * @param modelSettings {@link TableStructureValidatorNodeParameters}
     * @return a new {@link TableStructureValidatorColConfiguration} containing the contents of the model settings
     * @throws InvalidSettingsException if any setting is missing
     */
    static TableStructureValidatorColConfiguration load(final TableStructureValidatorNodeParameters modelSettings)
        throws InvalidSettingsException {

        final var referenceTableStructureColumns = modelSettings.m_referenceStructureColumns;

        TableStructureValidatorColConfiguration dataValidatorColConfiguration =
            new TableStructureValidatorColConfiguration(checkSettingNotNull(
                Arrays.stream(referenceTableStructureColumns).map(col -> col.m_columnName).toArray(String[]::new),
                "No reference table column names specified."));
        dataValidatorColConfiguration.setCaseInsensitiveNameMatching(
            modelSettings.m_columnNameMatchingBehavior  == CaseMatchingBehavior.CASE_INSENSITIVE);
        dataValidatorColConfiguration.setDataTypeHandling(modelSettings.m_dataTypeHandling);
        dataValidatorColConfiguration.setColumnExistenceHandling(modelSettings.m_missingColumnHandling);
        return dataValidatorColConfiguration;
    }

    /**
     * Creates a {@link TableStructureValidatorCellDecorator} for the given parameters and this configuration.
     *
     * @param refColumnSpec the reference {@link DataColumnSpec}
     * @param originalColumnSpec the original {@link DataColumnSpec}
     * @param conflicts the {@link TableStructureValidatorColConflicts}
     * @return {@link TableStructureValidatorCellDecorator}
     */
    TableStructureValidatorCellDecorator createCellValidator(final DataColumnSpec refColumnSpec,
        final DataColumnSpec originalColumnSpec, final TableStructureValidatorColConflicts conflicts) {

        DataColumnSpecCreator renamedColumnSpec = new DataColumnSpecCreator(originalColumnSpec);
        renamedColumnSpec.setName(refColumnSpec.getName());

        TableStructureValidatorCellDecorator decorator = TableStructureValidatorCellDecorator.forColumn(
            originalColumnSpec.getName(), renamedColumnSpec.createSpec());

        boolean rejectButDomainCheck = isNotCompatible(refColumnSpec, originalColumnSpec)
                && DataTypeHandling.FAIL == m_dataTypeHandling;

        if (!rejectButDomainCheck
                && !EnumSet.of(DataTypeHandling.NONE, DataTypeHandling.FAIL).contains(m_dataTypeHandling)
                && isNotEqualType(refColumnSpec, originalColumnSpec)) {
            decorator =
                TableStructureValidatorCellDecorator.conversionCellDecorator(decorator, m_dataTypeHandling,
                    getConversionType(refColumnSpec), refColumnSpec, conflicts);
        }

        return decorator;
    }

    private static boolean isNotCompatible(final DataColumnSpec refColumnSpec,
        final DataColumnSpec originalColumnSpec) {
        return !refColumnSpec.getType().isASuperTypeOf(originalColumnSpec.getType());
    }

    private static ConversionType getConversionType(final DataColumnSpec refColumnSpec) {
        DataType type = refColumnSpec.getType();

        // NOTE: The sequence here is important as we go from the most smallest general type to the most general one
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

}
