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
package org.knime.base.node.preproc.manipulator;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.knime.base.node.io.filehandling.webui.reader2.ColumnSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader2.DataTypeSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.TableSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationElementSettings;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.TableSpecSettingsRef;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.TransformationElementSettingsRef;
import org.knime.base.node.preproc.manipulator.TableManipulatorParameters.ColumnFilterModeOption;
import org.knime.base.node.preproc.manipulator.TableManipulatorParameters.ColumnFilterModeOptionRef;
import org.knime.base.node.preproc.manipulator.framework.RowInputTableReader;
import org.knime.base.node.preproc.manipulator.table.EmptyTable;
import org.knime.core.data.DataType;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;

/**
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH, Germany
 */
final class TableManipulatorTransformationParametersStateProviders {

    record TypedReaderTableSpecWithKey(String sourceId, TypedReaderTableSpec<DataType> spec)
        implements TransformationParametersStateProvidersCommon.HasTableSpec<DataType> {
        @Override
        public TypedReaderTableSpec<DataType> getTableSpec() {
            return spec;
        }
    }

    static final class TypedReaderTableSpecsProvider implements StateProvider<Collection<TypedReaderTableSpecWithKey>> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Collection<TypedReaderTableSpecWithKey> computeState(final NodeParametersInput parametersInput) {
            final var config = new TableManipulatorMultiTableReadConfig();
            final var reader = new RowInputTableReader();
            final var inTableSpecs = parametersInput.getInTableSpecs();

            return IntStream.range(0, inTableSpecs.length).filter(i -> inTableSpecs[i] != null).mapToObj(i -> {
                var table = new EmptyTable(inTableSpecs[i]);
                try {
                    var outSpec = reader.readSpec(table, config.getTableReadConfig(), null);
                    return new TypedReaderTableSpecWithKey(String.valueOf(i), outSpec);
                } catch (IOException e) {
                    throw new IllegalStateException("Could not read spec for table at index " + i, e);
                }
            }).toList();
        }

        interface Dependent {
            default Class<TypedReaderTableSpecsProvider> getTypedReaderTableSpecsProvider() {
                return TypedReaderTableSpecsProvider.class;
            }
        }
    }

    abstract static class DependsOnTypedReaderTableSpecProvider<P>
        implements StateProvider<P>, TypedReaderTableSpecsProvider.Dependent, DataTypeSerializer {

        protected Supplier<Collection<TypedReaderTableSpecWithKey>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initTriggers(initializer);
            m_specSupplier = initializer.computeFromProvidedState(getTypedReaderTableSpecsProvider());
        }

        private static void initTriggers(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
        }
    }

    static final class TableSpecSettingsProvider extends DependsOnTypedReaderTableSpecProvider<TableSpecSettings[]> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            // The "Column Filter Mode" option does not influence the table specs, but we need to
            // recompute anyway to trigger transitive updates in case this option changes.
            initializer.computeOnValueChange(ColumnFilterModeOptionRef.class);
        }

        @Override
        public TableSpecSettings[] computeState(final NodeParametersInput context) {

            final var suppliedSpecs = m_specSupplier.get();
            if (suppliedSpecs == null) {
                return null; // NOSONAR we deliberately return null here to indicate that no computation is possible
            }

            return suppliedSpecs.stream()
                .map(e -> new TableSpecSettings(e.sourceId(),
                    e.spec().stream().map(spec -> new ColumnSpecSettings(spec.getName().get(),
                        toSerializableType(spec.getType()), spec.hasType())).toArray(ColumnSpecSettings[]::new)))
                .toArray(TableSpecSettings[]::new);
        }
    }

    static final class TransformationElementSettingsProvider
        extends DependsOnTypedReaderTableSpecProvider<TransformationElementSettings[]>
        implements TableManipulatorSpecific.ProductionPathProviderAndTypeHierarchy,
        TransformationParametersStateProvidersCommon.TransformationElementsMapper<DataType> {

        private Supplier<ColumnFilterModeOption> m_columnFilterModeOption;

        private Supplier<TransformationElementSettings[]> m_existingSettings;

        private Supplier<TableSpecSettings[]> m_existingSpecs;

        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            m_existingSettings = initializer.getValueSupplier(TransformationElementSettingsRef.class);
            m_existingSpecs = initializer.getValueSupplier(TableSpecSettingsRef.class);
            m_columnFilterModeOption = initializer.computeFromValueSupplier(ColumnFilterModeOptionRef.class);
        }

        @Override
        public TransformationElementSettings[] computeState(final NodeParametersInput parametersInput) {
            final var suppliedSpecs = m_specSupplier.get();
            final var existingSpecs = m_existingSpecs.get();
            return toTransformationElements(suppliedSpecs == null ? List.of() : suppliedSpecs,
                m_columnFilterModeOption.get().toColumnFilterMode(), m_existingSettings.get(),
                getExistingSpecsUnion(this, existingSpecs));
        }
    }

    static final class TypeChoicesProvider implements StringChoicesProvider,
        TableManipulatorSpecific.ProductionPathProviderAndTypeHierarchy, TypedReaderTableSpecsProvider.Dependent,
        TransformationParametersStateProvidersCommon.TypeChoicesMapper<DataType> {

        private Supplier<String> m_columnNameSupplier;

        private Supplier<? extends Collection<? extends TransformationParametersStateProvidersCommon.HasTableSpec<DataType>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_columnNameSupplier = initializer.getValueSupplier(TransformationElementSettings.ColumnNameRef.class);
            initializer.computeOnValueChange(TableSpecSettingsRef.class);
            m_specSupplier = initializer.computeFromProvidedState(getTypedReaderTableSpecsProvider());
        }

        @Override
        public List<StringChoice> computeState(final NodeParametersInput context) {
            final var columnName = m_columnNameSupplier.get();
            final var suppliedSpecs = m_specSupplier.get();
            final List<TypedReaderTableSpec<DataType>> specsOnly = suppliedSpecs == null ? List.of() : suppliedSpecs
                .stream().map(TransformationParametersStateProvidersCommon.HasTableSpec::getTableSpec).toList();
            return computeTypeChoices(specsOnly, columnName);
        }

    }

    static final class TransformationSettingsWidgetModification extends
        TransformationParametersStateProvidersCommon.AbstractTransformationSettingsWidgetModification<TableSpecSettings> {

        @Override
        protected Class<TableSpecSettingsProvider> getSpecsValueProvider() {
            return TableSpecSettingsProvider.class;
        }

        @Override
        protected Class<TypeChoicesProvider> getTypeChoicesProvider() {
            return TypeChoicesProvider.class;
        }

        @Override
        protected Class<TransformationElementSettingsProvider> getTransformationSettingsValueProvider() {
            return TransformationElementSettingsProvider.class;
        }

    }

    private TableManipulatorTransformationParametersStateProviders() {
        // Not intended to be initialized
    }

}
