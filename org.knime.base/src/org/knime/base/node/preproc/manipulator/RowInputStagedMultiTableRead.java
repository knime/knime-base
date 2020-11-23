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
 *   Aug 3, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.manipulator;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import org.knime.base.node.preproc.manipulator.table.Table;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.streamable.RowInput;
import org.knime.filehandling.core.node.table.reader.GenericDefaultStagedMultiTableReader;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.config.GenericDefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.GenericTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.read.GenericRead;
import org.knime.filehandling.core.node.table.reader.read.ReadUtils;
import org.knime.filehandling.core.node.table.reader.rowkey.GenericRowKeyGeneratorContextFactory;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformationUtils;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;

/**
 * Implementation of a {@link StagedMultiTableRead} for {@link RowInput}.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
final class RowInputStagedMultiTableRead
    extends GenericDefaultStagedMultiTableReader<Table, TableManipulatorConfig, DataType, DataValue> {

    RowInputStagedMultiTableRead(final RowInputTableReader reader, final String rootPath,
        final Map<Table, TypedReaderTableSpec<DataType>> individualSpecs,
        final GenericRowKeyGeneratorContextFactory<Table, DataValue> rowKeyGenFactory,
        final Supplier<ReadAdapter<DataType, DataValue>> readAdapterSupplier,
        final TableTransformation<DataType> defaultTransformation,
        final TableReadConfig<TableManipulatorConfig> tableReadConfig) {
        super(reader, rootPath, individualSpecs, rowKeyGenFactory, readAdapterSupplier, defaultTransformation,
            tableReadConfig);
    }

    @Override
    public RowInputMultiTableRead withTransformation(final Collection<Table> tables, final TableTransformation<DataType> transformationModel) {
        final GenericTableSpecConfig<Table> tableSpecConfig =
            GenericDefaultTableSpecConfig.<Table, DataType>createFromTransformationModel(getRootItem(),
                transformationModel, getIndividualSpecs());
        return new RowInputMultiTableRead(tables,
            p -> createRead(p, getTableReadConfig()), () -> {
                RowInputTableReaderFactory factory = createIndividualTableReaderFactory(transformationModel);
                return factory::create;
            }, tableSpecConfig, TableTransformationUtils.toDataTableSpec(transformationModel));
    }

    private RowInputTableReaderFactory createIndividualTableReaderFactory(
        final TableTransformation<DataType> transformationModel) {
        return new RowInputTableReaderFactory(getIndividualSpecs(), getTableReadConfig(),
            TableTransformationUtils.getOutputTransformations(transformationModel), this::createTypeMapper,
            getRowKeyGenFactory().createContext(getTableReadConfig()));
    }

    @SuppressWarnings("resource")
    private GenericRead<Table, DataValue> createRead(final Table path, final TableReadConfig<TableManipulatorConfig> config) throws IOException {
        final GenericRead<Table, DataValue> rawRead = ((RowInputTableReader)getReader()).read(path, config);
        return ReadUtils.decorateForReading(config, rawRead);
    }
}
