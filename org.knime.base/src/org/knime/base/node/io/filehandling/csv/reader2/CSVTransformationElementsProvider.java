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
 *   Dec 11, 2023 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import static org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.PRODUCTION_PATH_PROVIDER;

import java.util.Map;
import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.csv.reader2.CSVTableSpec.DependsOnTableReadConfigProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableSpec.TypedReaderTableSpecProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.TransformationElement;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.TransformationElement.ColumnNameRef;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.filehandling.core.node.table.reader.RawSpecFactory;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class CSVTransformationElementsProvider extends DependsOnTableReadConfigProvider<TransformationElement[]> {

    static RawSpec<Class<?>> toRawSpec(final Map<String, TypedReaderTableSpec<Class<?>>> spec) {
        if (spec.isEmpty()) {
            final var emptySpec = new TypedReaderTableSpec<Class<?>>();
            return new RawSpec<>(emptySpec, emptySpec);
        }
        return new RawSpecFactory<>(CSVTransformationSettings.TYPE_HIERARCHY).create(spec.values());
    }

    static class TypeChoicesProvider implements StringChoicesStateProvider {

        private Supplier<String> m_columnNameSupplier;

        private Supplier<Map<String, TypedReaderTableSpec<Class<?>>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_columnNameSupplier = initializer.computeFromValueSupplier(ColumnNameRef.class);
            m_specSupplier = initializer.computeFromProvidedState(TypedReaderTableSpecProvider.class);
        }

        @Override
        public IdAndText[] computeState(final DefaultNodeSettingsContext context) {
            final var columnName = m_columnNameSupplier.get();

            final var union = toRawSpec(m_specSupplier.get()).getUnion();

            final var columnSpecOpt =
                union.stream().filter(colSpec -> colSpec.getName().get().equals(columnName)).findAny();

            if (columnSpecOpt.isEmpty()) {
                return new IdAndText[0];
            }
            final var columnSpec = columnSpecOpt.get();

            final var productionPaths = PRODUCTION_PATH_PROVIDER.getAvailableProductionPaths(columnSpec.getType());

            return productionPaths.stream().map(
                p -> new IdAndText(p.getConverterFactory().getIdentifier(), p.getDestinationType().toPrettyString()))
                .toArray(IdAndText[]::new);
        }
    }

    @Override
    public TransformationElement[] computeState(final DefaultNodeSettingsContext context) {
        return toTransformationElements(m_specSupplier.get());
    }

    static TransformationElement[] toTransformationElements(final Map<String, TypedReaderTableSpec<Class<?>>> specs) {
        final var union = toRawSpec(specs).getUnion();
        final var elements = new TransformationElement[union.size()];

        int i = 0;
        for (var column : union) {
            final var name = column.getName().get(); // NOSONAR in the TypedReaderTableSpecProvider we make sure that names are always present

            final var defPath = PRODUCTION_PATH_PROVIDER.getDefaultProductionPath(column.getType());

            final var type = defPath.getConverterFactory().getIdentifier();
            elements[i] = new TransformationElement(name, true, name, type);
            i++;
        }
        return elements;
    }
}
