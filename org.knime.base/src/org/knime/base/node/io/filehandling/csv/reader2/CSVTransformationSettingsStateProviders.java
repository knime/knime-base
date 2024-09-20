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
package org.knime.base.node.io.filehandling.csv.reader2;

import java.util.List;
import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader2.CSVReaderSpecific.ConfigAndReader;
import org.knime.base.node.io.filehandling.csv.reader2.CSVReaderSpecific.ProductionPathProviderAndTypeHierarchy;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.LimitMemoryPerColumnRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.MaximumNumberOfColumnsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.ConfigIdSettings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.DependenciesProvider.ConfigIdReference;
import org.knime.base.node.io.filehandling.webui.reader.ClassNoopSerializer;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.ReaderSpecificDependencies;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.ReaderSpecificDependenciesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class CSVTransformationSettingsStateProviders {

    /**
     * This object holds copies of the subset of settings in {@link CSVTableReaderNodeSettings} that can affect the spec
     * of the output table. I.e., if any of these settings change, the spec has to be re-calculcated.
     */
    static final class Dependencies

        implements ReaderSpecificDependencies<CSVTableReaderConfig> {
        final ConfigIdSettings m_configId;

        // The settings below are NOT a part of the CSV reader's ConfigId even though they probably should be.
        final boolean m_limitMemoryPerColumn;

        final int m_maximumNumberOfColumns;

        Dependencies(final ConfigIdSettings configId, final boolean limitMemoryPerColumn,
            final int maximumNumberOfColumns) {
            m_configId = configId;
            m_limitMemoryPerColumn = limitMemoryPerColumn;
            m_maximumNumberOfColumns = maximumNumberOfColumns;
        }

        @Override
        public void applyToConfig(final DefaultTableReadConfig<CSVTableReaderConfig> config) {
            m_configId.applyToConfig(config);
            final var csvConfig = config.getReaderSpecificConfig();
            csvConfig.limitCharsPerColumn(m_limitMemoryPerColumn);
            csvConfig.setMaxColumns(m_maximumNumberOfColumns);

        }

    }

    static final class DependenciesProvider implements ReaderSpecificDependenciesProvider<Dependencies> {

        private Supplier<ConfigIdSettings> m_configIdSupplier;

        private Supplier<Boolean> m_limitMemoryPerColumnSupplier;

        private Supplier<Integer> m_maximumNumberOfColumnsSupplier;

        static final class ConfigIdReference implements Reference<ConfigIdSettings> {
        }

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_configIdSupplier = initializer.getValueSupplier(ConfigIdReference.class);
            m_limitMemoryPerColumnSupplier = initializer.getValueSupplier(LimitMemoryPerColumnRef.class);
            m_maximumNumberOfColumnsSupplier = initializer.getValueSupplier(MaximumNumberOfColumnsRef.class);
        }

        @Override
        public Dependencies computeState(final DefaultNodeSettingsContext context) {
            return new Dependencies(m_configIdSupplier.get(), m_limitMemoryPerColumnSupplier.get(),
                m_maximumNumberOfColumnsSupplier.get());
        }

        interface GetReferences extends ReaderSpecificDependenciesProvider.GetReferences {

            @Override
            default List<Class<? extends Reference<?>>> getDependencyReferences() {
                return List.of(ConfigIdReference.class, LimitMemoryPerColumnRef.class, MaximumNumberOfColumnsRef.class);
            }

        }

        interface Dependent extends ReaderSpecificDependenciesProvider.Dependent<Dependencies> {
            @Override
            default Class<? extends ReaderSpecificDependenciesProvider<Dependencies>> getDependenciesProvider() {
                return DependenciesProvider.class;
            }
        }
    }

    static final class FSLocationsProvider
        extends CommonReaderTransformationSettingsStateProviders.FSLocationsProvider<Dependencies> {
    }

    static final class TypedReaderTableSpecsProvider extends
        CommonReaderTransformationSettingsStateProviders.TypedReaderTableSpecsProvider<CSVTableReaderConfig, Class<?>, Dependencies>
        implements ConfigAndReader {

        interface Dependent
            extends CommonReaderTransformationSettingsStateProviders.TypedReaderTableSpecsProvider.Dependent<Class<?>> {
            @Override
            default
                Class<? extends CommonReaderTransformationSettingsStateProviders.TypedReaderTableSpecsProvider<?, Class<?>, ?>>
                getTypedReaderTableSpecsProvider() {
                return TypedReaderTableSpecsProvider.class;
            }
        }
    }

    static final class TableSpecSettingsProvider
        extends CommonReaderTransformationSettingsStateProviders.TableSpecSettingsProvider<Class<?>, Class<?>>
        implements TypedReaderTableSpecsProvider.Dependent, ClassNoopSerializer {
    }

    static final class TransformationElementSettingsProvider
        extends CommonReaderTransformationSettingsStateProviders.TransformationElementSettingsProvider<Class<?>>
        implements ProductionPathProviderAndTypeHierarchy, TypedReaderTableSpecsProvider.Dependent {
    }

    static final class TypeChoicesProvider
        extends CommonReaderTransformationSettingsStateProviders.TypeChoicesProvider<Class<?>>
        implements ProductionPathProviderAndTypeHierarchy, TypedReaderTableSpecsProvider.Dependent {
    }

    static final class TransformationSettingsWidgetModification extends
        CommonReaderTransformationSettingsStateProviders.TransformationSettingsWidgetModification<ConfigIdSettings, Class<?>, Class<?>> {

        @Override
        protected Class<ConfigIdReference> getConfigIdSettingsValueRef() {
            return ConfigIdReference.class;
        }

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

        @Override
        protected Class<FSLocationsProvider> getFsLocationProvider() {
            return FSLocationsProvider.class;
        }
    }

    private CSVTransformationSettingsStateProviders() {
        // Not intended to be initialized
    }

}
