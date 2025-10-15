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
 *   Nov 21, 2025: created
 */
package org.knime.base.node.io.filehandling.table.reader2;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderParameters;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParameters;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProviderTestUtils.TransformationParametersUpdatesTestDataTypeBased;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.util.Pair;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;

/**
 * @author Paul Baernreuther
 */
final class KnimeTableReaderTransformationParametersStateProvidersTest
    extends TransformationParametersUpdatesTestDataTypeBased<KnimeTableReaderNodeParameters> {

    static final List<List<String>> TRIGGER_PATHS = List.of( //
        List.of("readerParameters", "firstColumnContainsRowIds"), //
        List.of("readerParameters", "skipFirstDataRows") //
    );

    @Override
    protected ReaderParameters getReaderParameters(final KnimeTableReaderNodeParameters settings) {
        return settings.m_readerParameters;
    }

    @Override
    protected TransformationParameters<DataType>
        getTransformationSettings(final KnimeTableReaderNodeParameters settings) {
        return settings.m_transformationParameters;
    }

    @Override
    protected void writeFileWithIntegerAndStringColumn(final String filePath) throws IOException {
        createTableFile(filePath);
    }

    static void createTableFile(final String file) throws IOException {
        final var spec = new DataTableSpec(new DataColumnSpecCreator("intCol", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("stringCol", StringCell.TYPE).createSpec());
        final var cont = new DataContainer(spec);
        for (int i = 0; i < 2; i++) {
            final var s = Integer.toString(i);
            cont.addRowToTable(new DefaultRow(s, new IntCell(i), new StringCell(s)));
        }
        cont.close();

        try (var table = cont.getCloseableTable()) {
            DataContainer.writeToZip(table, new File(file), new ExecutionMonitor());
        } catch (CanceledExecutionException e) {
            // do nothing
        }
    }

    @Override
    protected ProductionPathProvider<DataType> getProductionPathProvider() {
        return KnimeTableReaderSpecific.PRODUCTION_PATH_PROVIDER;
    }

    @Override
    protected Pair<DataType, Collection<IntOrString>> getUnreachableType() {
        return new Pair<>(ListCell.getCollectionType(StringCell.TYPE), List.of(IntOrString.INT, IntOrString.STRING));
    }

    @Override
    protected List<String> getPathToTransformationSettings() {
        return List.of("transformationParameters");
    }

    @Override
    protected KnimeTableReaderNodeParameters constructNewSettings() {
        return new KnimeTableReaderNodeParameters();
    }

    @Override
    protected String getFileName() {
        return "test.table";
    }

    @ParameterizedTest
    @MethodSource("getDependencyTriggerReferences")
    void testTableSpecSettingsProvider(final List<String> triggerPath) throws IOException {
        testTableSpecSettingsProvider(sim -> sim.simulateValueChange(triggerPath));
    }

    static Stream<Arguments> getDependencyTriggerReferences() {
        return TRIGGER_PATHS.stream().map(Arguments::of);
    }

}
