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
 *   May 15, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.table.reader2;

import static org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.PRODUCTION_PATH_PROVIDER;
import static org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.TYPE_HIERARCHY;

import java.util.Map;
import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.table.reader.KnimeTableReader;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.SetStateProvidersAndReferences.KnimeTableReaderConfigIdSettingsValueRef;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.Settings.FileChooserRef;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.Dependencies;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.DependenciesProvider;
import org.knime.base.node.preproc.manipulator.TableManipulatorConfig;
import org.knime.core.data.DataType;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.RawSpecFactory;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class KnimeTableReaderTransformationSettingsStateProviders {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(KnimeTableReaderTransformationSettingsStateProviders.class);

    static final class FSLocationsProvider
        extends CommonReaderTransformationSettingsStateProviders.FSLocationsProvider<Dependencies> {
        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            initializer.computeAfterOpenDialog();
            initializer.computeOnValueChange(FileChooserRef.class);
        }

        @Override
        protected Class<? extends DependenciesProvider<Dependencies>> getDependenciesProvider() {
            return CommonReaderTransformationSettingsStateProviders.NoAdditionalDependencies.class;
        }

    }

    static final class TypedReaderTableSpecsProvider extends
        CommonReaderTransformationSettingsStateProviders.TypedReaderTableSpecsProvider<TableManipulatorConfig, DataType, Dependencies> {

        @Override
        protected TableReader<TableManipulatorConfig, DataType, ?> getTableReader() {
            return new KnimeTableReader();
        }

        @Override
        protected TableManipulatorConfig getReaderSpecificConfig() {
            return new TableManipulatorConfig();
        }

        @Override
        protected Class<? extends DependenciesProvider<Dependencies>> getDependenciesProvider() {
            return CommonReaderTransformationSettingsStateProviders.NoAdditionalDependencies.class;
        }

    }

    abstract static class DependsOnTypedReaderTableSpecProvider<S> implements StateProvider<S> {

        protected Supplier<Map<String, TypedReaderTableSpec<DataType>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_specSupplier = initializer.computeFromProvidedState(TypedReaderTableSpecsProvider.class);
            initializer.computeOnValueChange(KnimeTableReaderConfigIdSettingsValueRef.class);
            initializer.computeOnValueChange(FileChooserRef.class);
        }
    }

    static final class TableSpecSettingsProvider
        extends CommonReaderTransformationSettingsStateProviders.TableSpecSettingsProvider<String, DataType> {

        @Override
        protected String toSerializableType(final DataType value) {
            return KnimeTableReaderTransformationSettings.typeToString(value);

        }

        @Override
        public
            Class<? extends CommonReaderTransformationSettingsStateProviders.TypedReaderTableSpecsProvider<?, DataType, ?>>
            getTypedReaderTableSpecsProvider() {
            return TypedReaderTableSpecsProvider.class;
        }

    }

    static final class TransformationElementSettingsProvider
        extends CommonReaderTransformationSettingsStateProviders.TransformationElementSettingsProvider<DataType> {

        @Override
        public ProductionPathProvider<DataType> getProductionPathProvider() {
            return PRODUCTION_PATH_PROVIDER;
        }

        @Override
        public TypeHierarchy<DataType, DataType> getTypeHierarchy() {
            return TYPE_HIERARCHY;
        }

        @Override
        public
            Class<? extends CommonReaderTransformationSettingsStateProviders.TypedReaderTableSpecsProvider<?, DataType, ?>>
            getTypedReaderTableSpecsProvider() {
            return TypedReaderTableSpecsProvider.class;
        }

    }

    static final class TypeChoicesProvider
        extends CommonReaderTransformationSettingsStateProviders.TypeChoicesProvider<DataType> {

        @Override
        public ProductionPathProvider<DataType> getProductionPathProvider() {
            return PRODUCTION_PATH_PROVIDER;
        }

        @Override
        public TypeHierarchy<DataType, DataType> getTypeHierarchy() {
            return TYPE_HIERARCHY;
        }

        @Override
        public
            Class<? extends CommonReaderTransformationSettingsStateProviders.TypedReaderTableSpecsProvider<?, DataType, ?>>
            getTypedReaderTableSpecsProvider() {
            return TypedReaderTableSpecsProvider.class;
        }

    }

    static RawSpec<DataType> toRawSpec(final Map<String, TypedReaderTableSpec<DataType>> spec) {
        if (spec.isEmpty()) {
            final var emptySpec = new TypedReaderTableSpec<DataType>();
            return new RawSpec<>(emptySpec, emptySpec);
        }
        return new RawSpecFactory<>(KnimeTableReaderTransformationSettings.TYPE_HIERARCHY).create(spec.values());
    }

    private KnimeTableReaderTransformationSettingsStateProviders() {
        // Not intended to be initialized
    }
}
