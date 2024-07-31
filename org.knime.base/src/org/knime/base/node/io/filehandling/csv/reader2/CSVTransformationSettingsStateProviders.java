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

import static org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.PRODUCTION_PATH_PROVIDER;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReader;
import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.LimitMemoryPerColumnRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.MaximumNumberOfColumnsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.FileChooserRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.ColumnSpecSettings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.ConfigIdSettings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.PersistorSettings.ConfigIdReference;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.TableSpecSettings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.TransformationElementSettings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.TransformationElementSettings.ColumnNameRef;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.RawSpecFactory;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class CSVTransformationSettingsStateProviders implements WidgetGroup, PersistableSettings {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CSVTransformationSettingsStateProviders.class);

    /**
     * This object holds copies of the subset of settings in {@link CSVTableReaderNodeSettings} that can affect the spec
     * of the output table. I.e., if any of these settings change, the spec has to be re-calculcated.
     */
    static final class Dependencies {

        final ConfigIdSettings m_configId;

        final FileChooser m_source;

        // The settings below are NOT a part of the CSV reader's ConfigId even though they probably should be.
        final boolean m_limitMemoryPerColumn;

        final int m_maximumNumberOfColumns;

        Dependencies(final ConfigIdSettings configId, final FileChooser fileChooser,
            final boolean limitMemoryPerColumn, final int maximumNumberOfColumns) {
            m_configId = configId;
            m_source = fileChooser;
            m_limitMemoryPerColumn = limitMemoryPerColumn;
            m_maximumNumberOfColumns = maximumNumberOfColumns;
        }
    }

    static final class DependenciesProvider implements StateProvider<Dependencies> {

        private Supplier<ConfigIdSettings> m_configIdSupplier;

        private Supplier<FileChooser> m_fileChooserSupplier;

        private Supplier<Boolean> m_limitMemoryPerColumnSupplier;

        private Supplier<Integer> m_maximumNumberOfColumnsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_configIdSupplier = initializer.getValueSupplier(ConfigIdReference.class);
            m_fileChooserSupplier = initializer.getValueSupplier(FileChooserRef.class);
            m_limitMemoryPerColumnSupplier = initializer.getValueSupplier(LimitMemoryPerColumnRef.class);
            m_maximumNumberOfColumnsSupplier = initializer.getValueSupplier(MaximumNumberOfColumnsRef.class);
        }

        @Override
        public Dependencies computeState(final DefaultNodeSettingsContext context) {
            return new Dependencies(m_configIdSupplier.get(), m_fileChooserSupplier.get(),
                m_limitMemoryPerColumnSupplier.get(), m_maximumNumberOfColumnsSupplier.get());
        }
    }

    static final class SourceIdProvider implements StateProvider<String> {

        private Supplier<Dependencies> m_dependenciesSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_dependenciesSupplier = initializer.computeFromProvidedState(DependenciesProvider.class);
            initializer.computeAfterOpenDialog();
            initializer.computeOnValueChange(FileChooserRef.class);
        }

        @Override
        public String computeState(final DefaultNodeSettingsContext context) {
            return m_dependenciesSupplier.get().m_source.getFSLocation().getPath();
        }
    }

    abstract static class PathsProvider<S> implements StateProvider<S> {

        protected Supplier<Dependencies> m_dependenciesSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_dependenciesSupplier = initializer.computeFromProvidedState(DependenciesProvider.class);
        }

        @Override
        public S computeState(final DefaultNodeSettingsContext context) {
            final var fileChooser = m_dependenciesSupplier.get().m_source;
            if (!WorkflowContextUtil.hasWorkflowContext() // no workflow context available
                || fileChooser.getFSLocation().equals(new FSLocation(FSCategory.LOCAL, ""))) { // no file selected (yet)
                return computeStateFromPaths(Collections.emptyList());
            }

            try (final FileChooserPathAccessor accessor = new FileChooserPathAccessor(fileChooser)) {
                return computeStateFromPaths(accessor.getFSPaths(s -> {
                    switch (s.getType()) {
                        case INFO -> LOGGER.info(s.getMessage());
                        case WARNING -> LOGGER.info(s.getMessage());
                        case ERROR -> LOGGER.error(s.getMessage());
                    }
                }));
            } catch (IOException | InvalidSettingsException e) {
                LOGGER.error(e);
                return computeStateFromPaths(Collections.emptyList());
            }
        }

        abstract S computeStateFromPaths(List<FSPath> paths);
    }

    static final class FSLocationsProvider extends PathsProvider<FSLocation[]> {
        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            initializer.computeAfterOpenDialog();
            initializer.computeOnValueChange(FileChooserRef.class);
        }

        @Override
        FSLocation[] computeStateFromPaths(final List<FSPath> paths) {
            return paths.stream().map(FSPath::toFSLocation).toArray(FSLocation[]::new);
        }
    }

    static final class TypedReaderTableSpecsProvider
        extends PathsProvider<Map<String, TypedReaderTableSpec<Class<?>>>> {
        @Override
        Map<String, TypedReaderTableSpec<Class<?>>> computeStateFromPaths(final List<FSPath> paths) {
            final var csvTableReader = new CSVTableReader();
            final var exec = new ExecutionMonitor();
            final var specs = new HashMap<String, TypedReaderTableSpec<Class<?>>>();
            final var csvConfig = new CSVTableReaderConfig();
            final var config = new DefaultTableReadConfig<>(csvConfig);
            final var dependencies = m_dependenciesSupplier.get();
            dependencies.m_configId.applyToConfig(config);
            csvConfig.limitCharsPerColumn(dependencies.m_limitMemoryPerColumn);
            csvConfig.setMaxColumns(dependencies.m_maximumNumberOfColumns);
            for (var path : paths) {
                try {
                    specs.put(path.toFSLocation().getPath(),
                        MultiTableUtils.assignNamesIfMissing(csvTableReader.readSpec(path, config, exec)));
                } catch (IOException e) {
                    LOGGER.error(e);
                    return Collections.emptyMap();
                }
            }
            return specs;
        }
    }

    abstract static class DependsOnTypedReaderTableSpecProvider<S> implements StateProvider<S> {

        protected Supplier<Map<String, TypedReaderTableSpec<Class<?>>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_specSupplier = initializer.computeFromProvidedState(TypedReaderTableSpecsProvider.class);
            initializer.computeOnValueChange(ConfigIdReference.class);
            initializer.computeOnValueChange(FileChooserRef.class);
            initializer.computeOnValueChange(LimitMemoryPerColumnRef.class);
            initializer.computeOnValueChange(MaximumNumberOfColumnsRef.class);
        }
    }

    static final class TableSpecSettingsProvider extends DependsOnTypedReaderTableSpecProvider<TableSpecSettings[]> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public TableSpecSettings[] computeState(final DefaultNodeSettingsContext context) {
            return m_specSupplier.get().entrySet().stream()
                .map(e -> new TableSpecSettings(e.getKey(),
                    e.getValue().stream().map(spec -> new ColumnSpecSettings(spec.getName().get(), spec.getType()))
                        .toArray(ColumnSpecSettings[]::new)))
                .toArray(TableSpecSettings[]::new);
        }
    }

    static final class TransformationElementSettingsProvider
        extends DependsOnTypedReaderTableSpecProvider<TransformationElementSettings[]> {

        @Override
        public TransformationElementSettings[] computeState(final DefaultNodeSettingsContext context) {
            return toTransformationElements(m_specSupplier.get());
        }

        static TransformationElementSettings[]
            toTransformationElements(final Map<String, TypedReaderTableSpec<Class<?>>> specs) {
            final var union = toRawSpec(specs).getUnion();
            final var elements = new TransformationElementSettings[union.size()];
            int i = 0;
            for (var column : union) {
                final var name = column.getName().get(); // NOSONAR in the TypedReaderTableSpecProvider we make sure that names are always present
                final var defPath = PRODUCTION_PATH_PROVIDER.getDefaultProductionPath(column.getType());
                final var type = defPath.getConverterFactory().getIdentifier();
                elements[i] = new TransformationElementSettings(name, true, name, type, type);
                i++;
            }
            return elements;
        }
    }

    static final class TypeChoicesProvider implements StringChoicesStateProvider {

        private Supplier<String> m_columnNameSupplier;

        private Supplier<Map<String, TypedReaderTableSpec<Class<?>>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_columnNameSupplier = initializer.computeFromValueSupplier(ColumnNameRef.class);
            m_specSupplier = initializer.computeFromProvidedState(TypedReaderTableSpecsProvider.class);
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

    static RawSpec<Class<?>> toRawSpec(final Map<String, TypedReaderTableSpec<Class<?>>> spec) {
        if (spec.isEmpty()) {
            final var emptySpec = new TypedReaderTableSpec<Class<?>>();
            return new RawSpec<>(emptySpec, emptySpec);
        }
        return new RawSpecFactory<>(CSVTransformationSettings.TYPE_HIERARCHY).create(spec.values());
    }

    private CSVTransformationSettingsStateProviders() {
        // Not intended to be initialized
    }
}
