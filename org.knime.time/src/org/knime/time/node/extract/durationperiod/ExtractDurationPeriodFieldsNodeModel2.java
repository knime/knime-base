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
 *   Nov 20, 2024 (david): created
 */
package org.knime.time.node.extract.durationperiod;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;

/**
 * The node model for the "Extract Duration/Period Fields" node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
public class ExtractDurationPeriodFieldsNodeModel2
    extends WebUISimpleStreamableFunctionNodeModel<ExtractDurationPeriodFieldsNodeSettings> {

    /**
     * @param configuration
     */
    protected ExtractDurationPeriodFieldsNodeModel2(final WebUINodeConfiguration configuration) {
        super(configuration, ExtractDurationPeriodFieldsNodeSettings.class);
    }

    @Override
    protected void validateSettings(final ExtractDurationPeriodFieldsNodeSettings settings)
        throws InvalidSettingsException {

        var firstDuplicateColumnName = Arrays.stream(settings.m_extractFields) //
            .filter(extractField -> !extractField.m_outputcolumnName.isEmpty())
            .collect(Collectors.groupingBy(extractField -> extractField.m_outputcolumnName)) //
            .entrySet().stream() //
            .filter(entry -> entry.getValue().size() > 1) //
            .map(Map.Entry::getKey) //
            .findFirst();
        if (firstDuplicateColumnName.isPresent()) {
            throw new InvalidSettingsException("The output column name '%s' is used multiple times." //
                .formatted(firstDuplicateColumnName.get()));
        }
    }

    @Override
    public ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final ExtractDurationPeriodFieldsNodeSettings modelSettings) throws InvalidSettingsException {

        String selectedColumnName = modelSettings.m_selectedColumn;
        int selectedColumnIndex = spec.findColumnIndex(selectedColumnName);

        // on initial configure, before dialogue first opens, the selected column will probably not exist,
        // so only warn if the column is selected but does not exist.
        if (selectedColumnIndex == -1 && selectedColumnName != null) {
            throw new InvalidSettingsException("The selected input column '%s' does not exist in the input table." //
                .formatted(selectedColumnName));
        }

        var columnRearranger = new ColumnRearranger(spec);
        var uniqueNameGenerator = new UniqueNameGenerator(spec);

        for (var extractedFieldSetting : modelSettings.m_extractFields) {
            var name = uniqueNameGenerator.newName( //
                extractedFieldSetting.m_outputcolumnName.isEmpty() //
                    ? extractedFieldSetting.m_field.niceName() //
                    : extractedFieldSetting.m_outputcolumnName //
            );

            columnRearranger.append(new ExtractedPartCellFactory( //
                name, //
                selectedColumnIndex, //
                extractedFieldSetting.m_field //
            ));
        }

        return columnRearranger;
    }

    static class ExtractedPartCellFactory extends SingleCellFactory {

        private final ExtractableField m_extractableField;

        private final int m_referenceColumnIndex;

        ExtractedPartCellFactory( //
            final String outputColumnName, //
            final int referenceColumnIndex, //
            final ExtractableField extractableField //
        ) {
            super(createNewColumnSpec(outputColumnName));

            this.m_extractableField = extractableField;
            this.m_referenceColumnIndex = referenceColumnIndex;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            var cell = row.getCell(m_referenceColumnIndex);

            if (cell.isMissing()) {
                return DataType.getMissingCell();
            }

            if (!m_extractableField.isCompatibleWith(cell.getType())) {
                throw new IllegalStateException(
                    "The column type '%s' is not compatible with the selected extractable field '%s'." //
                        .formatted(cell.getType().getName(), m_extractableField.name()) //
                );
            }
            return m_extractableField.extractNumberCellFrom(cell);
        }

        private static DataColumnSpec createNewColumnSpec(final String newColumnName) {
            return new DataColumnSpecCreator(newColumnName, LongCell.TYPE).createSpec();
        }
    }
}
