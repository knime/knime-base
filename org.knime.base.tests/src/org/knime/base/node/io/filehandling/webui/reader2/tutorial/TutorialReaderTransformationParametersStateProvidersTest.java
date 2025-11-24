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
 */
package org.knime.base.node.io.filehandling.webui.reader2.tutorial;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileReaderParameters.HowToCombineColumnsOption;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParameters;
// TODO (#4): If your T is not Class<?>, extend TransformationParametersUpdatesTest<T, ...> instead of TransformationParametersUpdatesTestClassBased
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProviderTestUtils.TransformationParametersUpdatesTestClassBased;
import org.knime.core.data.DataType;
import org.knime.core.data.def.LongCell;
import org.knime.core.util.Pair;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;

/**
 *
 * @author KNIME AG, Zurich, Switzerland
 */
@Disabled("TODO (#7): Enable this test class when you have implemented the required methods")
final class TutorialReaderTransformationParametersStateProvidersTest
    extends TransformationParametersUpdatesTestClassBased<TutorialReaderNodeParameters> {

    // TODO (#6): Add trigger paths for your reader-specific parameters
    // e.g., List.of("tutorialReaderParameters", "myTutorialSpecificParams", "myReaderSpecificSetting")
    static final List<List<String>> TRIGGER_PATHS = List.of( //
        List.of("tutorialReaderParameters", "skipFirstDataRowsParams", "skipFirstDataRows"), //
        List.of("tutorialReaderParameters", "myTutorialSpecificParameters", "myParameterAfterSource") // TODO (#6): Example path - adjust to your actual parameters
    );

    @Override
    protected void setSourcePath(final TutorialReaderNodeParameters settings, final FSLocation fsLocation) {
        settings.m_tutorialReaderParameters.m_multiFileSelectionParams.m_source.m_path = fsLocation;
    }

    @Override
    protected void setHowToCombineColumns(final TutorialReaderNodeParameters settings,
        final HowToCombineColumnsOption howToCombineColumns) {
        settings.m_tutorialReaderParameters.m_multiFileReaderParams.m_howToCombineColumns = howToCombineColumns;

    }

    @Override
    // TODO (#4): Adjust Class<?> to match your TableReader's T type parameter if needed
    protected TransformationParameters<Class<?>> getTransformationSettings(final TutorialReaderNodeParameters params) {
        return params.m_transformationParameters;
    }

    @Override
    protected void writeFileWithIntegerAndStringColumn(final String filePath) throws IOException {
        /**
         * TODO (#7): Implement file writing for your format.
         *
         * When this method was called, a future read of the file at the file path should result in a spec with an
         * integer column "intCol" followed by a string column "stringCol".
         */
        throw new UnsupportedOperationException("Implement this method for your file format");
    }

    @Override
    // TODO (#4): Adjust Class<?> to match your TableReader's T type parameter if needed
    protected ProductionPathProvider<Class<?>> getProductionPathProvider() {
        return TutorialReaderSpecific.PRODUCTION_PATH_PROVIDER;
    }

    @Override
    protected Pair<DataType, Collection<IntOrString>> getUnreachableType() {
        /**
         * TODO (#7): Possibly change the unreachable type according to your format.
         */
        return new Pair<>(LongCell.TYPE, List.of(IntOrString.STRING));
    }

    @Override
    protected List<String> getPathToTransformationSettings() {
        return List.of("transformationParameters");
    }

    @Override
    protected TutorialReaderNodeParameters constructNewSettings() {
        return new TutorialReaderNodeParameters();
    }

    @Override
    protected String getFileName() {
        return "test.TODO (#7)"; // TODO (#7): Set the file extension for your format
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
