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
 *   May 28, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.PRODUCTION_PATH_PROVIDER;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableSpec.TypedReaderTableSpecProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationElementsProvider.TypeChoicesProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.TransformationElement.ColumnNameRef;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ButtonReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider.StateProviderInitializer;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
class CSVTransformationElementsProviderTest extends LocalWorkflowContextTest {

    @TempDir
    Path m_tempFolder;

    @Test
    void testCSVTransformationElementsProvider() throws IOException {
        final var file = m_tempFolder.resolve("file.csv").toAbsolutePath().toString();
        final var typedReaderTableSpecProvider = CSVTableSpecTest.createTypedReaderTableSpecProvider(file);

        final var transformationElementsProvider = new CSVTransformationElementsProvider();
        transformationElementsProvider
            .init(getCSVTransformationElementsProviderStateProviderInitializer(typedReaderTableSpecProvider));

        final var transformationElements = transformationElementsProvider.computeState(null);

        assertThat(transformationElements).hasSize(2);

        assertThat(transformationElements[0].m_columnName).isEqualTo("intCol");
        assertThat(transformationElements[0].m_includeInOutput).isTrue();
        assertThat(transformationElements[0].m_columnRename).isEqualTo("intCol");
        assertThat(transformationElements[0].m_type).isEqualTo(
            PRODUCTION_PATH_PROVIDER.getDefaultProductionPath(Integer.class).getConverterFactory().getIdentifier());

        assertThat(transformationElements[1].m_columnName).isEqualTo("stringCol");
        assertThat(transformationElements[1].m_includeInOutput).isTrue();
        assertThat(transformationElements[1].m_columnRename).isEqualTo("stringCol");
        assertThat(transformationElements[1].m_type).isEqualTo(
            PRODUCTION_PATH_PROVIDER.getDefaultProductionPath(String.class).getConverterFactory().getIdentifier());
    }

    private static final StateProviderInitializer getCSVTransformationElementsProviderStateProviderInitializer(
        final TypedReaderTableSpecProvider typedReaderTableSpecProvider) {
        return new StateProviderInitializer() {
            @Override
            public <T> Supplier<T> getValueSupplier(final Class<? extends Reference<T>> ref) {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public <T> void computeOnValueChange(final Class<? extends Reference<T>> ref) {
                // Do nothing
            }

            @Override
            public void computeOnButtonClick(final Class<? extends ButtonReference> ref) {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public <T> Supplier<T> computeFromValueSupplier(final Class<? extends Reference<T>> ref) {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> Supplier<T>
                computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
                if (stateProviderClass.equals(TypedReaderTableSpecProvider.class)) {
                    return () -> (T)typedReaderTableSpecProvider.computeState(null);
                }
                throw new IllegalStateException(
                    String.format("Unexpected dependency %s", stateProviderClass.getSimpleName()));
            }

            @Override
            public void computeBeforeOpenDialog() {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public void computeAfterOpenDialog() {
                throw new IllegalAccessError("Should not be called within this test");
            }
        };
    }

    @Test
    void testTypeChoicesProviderIntCol() throws IOException {
        final var file = m_tempFolder.resolve("file.csv").toAbsolutePath().toString();
        final var typedReaderTableSpecProvider = CSVTableSpecTest.createTypedReaderTableSpecProvider(file);

        testTypeChoicesProviderIntCol(typedReaderTableSpecProvider, "intCol", Integer.class);
        testTypeChoicesProviderIntCol(typedReaderTableSpecProvider, "stringCol", String.class);
    }

    private static void testTypeChoicesProviderIntCol(final TypedReaderTableSpecProvider typedReaderTableSpecProvider,
        final String columnName, final Class<?> type) {
        final var typeChoicesProvider = new TypeChoicesProvider();
        typeChoicesProvider
            .init(getTypeChoicesProviderStateProviderInitializer(columnName, typedReaderTableSpecProvider));

        final var productionPaths = PRODUCTION_PATH_PROVIDER.getAvailableProductionPaths(type);
        final var typesIdAndText = typeChoicesProvider.computeState(null);

        assertThat(typesIdAndText).hasSize(productionPaths.size());
        for (int i = 0; i < productionPaths.size(); i++) {
            assertThat(typesIdAndText[i].id()).isEqualTo(productionPaths.get(i).getConverterFactory().getIdentifier());
            assertThat(typesIdAndText[i].text())
                .isEqualTo(productionPaths.get(i).getDestinationType().toPrettyString());
        }
    }

    private static final StateProviderInitializer getTypeChoicesProviderStateProviderInitializer(final String columnName,
        final TypedReaderTableSpecProvider typedReaderTableSpecProvider) {
        return new StateProviderInitializer() {
            @Override
            public <T> Supplier<T> getValueSupplier(final Class<? extends Reference<T>> ref) {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public <T> void computeOnValueChange(final Class<? extends Reference<T>> ref) {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public void computeOnButtonClick(final Class<? extends ButtonReference> ref) {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> Supplier<T> computeFromValueSupplier(final Class<? extends Reference<T>> ref) {
                if (ref.equals(ColumnNameRef.class)) {
                    return () -> (T)columnName;
                }
                throw new IllegalStateException(String.format("Unexpected dependency %s", ref.getSimpleName()));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> Supplier<T>
                computeFromProvidedState(final Class<? extends StateProvider<T>> stateProviderClass) {
                if (stateProviderClass.equals(TypedReaderTableSpecProvider.class)) {
                    return () -> (T)typedReaderTableSpecProvider.computeState(null);
                }
                throw new IllegalStateException(
                    String.format("Unexpected dependency %s", stateProviderClass.getSimpleName()));
            }

            @Override
            public void computeBeforeOpenDialog() {
                throw new IllegalAccessError("Should not be called within this test");
            }

            @Override
            public void computeAfterOpenDialog() {
                throw new IllegalAccessError("Should not be called within this test");
            }
        };
    }
}
