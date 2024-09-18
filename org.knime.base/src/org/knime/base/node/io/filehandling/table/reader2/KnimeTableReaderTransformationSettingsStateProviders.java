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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings;
import org.knime.base.node.io.filehandling.table.reader.KnimeTableReader;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.ColumnSpecSettings;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.TableSpecSettings;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.TransformationElementSettings;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.TransformationElementSettings.ColumnNameRef;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettings.TransformationElementSettingsReference;
import org.knime.base.node.io.filehandling.webui.FileChooserPathAccessor;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.Settings.FileChooserRef;
import org.knime.base.node.preproc.manipulator.TableManipulatorConfig;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
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
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class KnimeTableReaderTransformationSettingsStateProviders {

    private static final NodeLogger LOGGER =
        NodeLogger.getLogger(KnimeTableReaderTransformationSettingsStateProviders.class);

    /**
     * This object holds copies of the subset of settings in {@link CSVTableReaderNodeSettings} that can affect the spec
     * of the output table. I.e., if any of these settings change, the spec has to be re-calculcated.
     */
    static final class Dependencies {

        //??? the dependencies (and thus also the DependenciesProvider) has less fields than the CSV reader's

        //        final ConfigIdSettings m_configId;

        final FileChooser m_source;

        //        Dependencies(final ConfigIdSettings configId, final FileChooser fileChooser) {
        Dependencies(final FileChooser fileChooser) {
            //            m_configId = configId;
            m_source = fileChooser;
        }
    }

    static final class DependenciesProvider implements StateProvider<Dependencies> {

        //        private Supplier<ConfigIdSettings> m_configIdSupplier;

        private Supplier<FileChooser> m_fileChooserSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            //            m_configIdSupplier = initializer.getValueSupplier(ConfigIdReference.class);
            m_fileChooserSupplier = initializer.getValueSupplier(FileChooserRef.class);
        }

        @Override
        public Dependencies computeState(final DefaultNodeSettingsContext context) {
            //            return new Dependencies(m_configIdSupplier.get(), m_fileChooserSupplier.get());
            return new Dependencies(m_fileChooserSupplier.get());
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

            var fsConnection = FileSystemPortConnectionUtil.getFileSystemConnection(context);
            if (fileChooser.getFSLocation().getFSCategory() == FSCategory.CONNECTED && fsConnection.isEmpty()) {
                return computeStateFromPaths(Collections.emptyList());
            }

            try (final FileChooserPathAccessor accessor = new FileChooserPathAccessor(fileChooser, fsConnection)) {
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

    // ??? different type (DataType instead of Class<?>)
    static final class TypedReaderTableSpecsProvider
        extends PathsProvider<Map<String, TypedReaderTableSpec<DataType>>> {
        @Override
        Map<String, TypedReaderTableSpec<DataType>> computeStateFromPaths(final List<FSPath> paths) {
            // ??? different reader here
            final var tableReader = new KnimeTableReader();
            final var exec = new ExecutionMonitor();
            final var specs = new HashMap<String, TypedReaderTableSpec<DataType>>();
            // ??? different config here
            final var tmConfig = new TableManipulatorConfig();
            final var config = new DefaultTableReadConfig<>(tmConfig);
            //            final var dependencies = m_dependenciesSupplier.get();
            //            dependencies.m_configId.applyToConfig(config);
            for (var path : paths) {
                try {
                    specs.put(path.toFSLocation().getPath(),
                        MultiTableUtils.assignNamesIfMissing(tableReader.readSpec(path, config, exec)));
                } catch (IOException e) {
                    LOGGER.error(e);
                    return Collections.emptyMap();
                }
            }
            return specs;
        }
    }

    abstract static class DependsOnTypedReaderTableSpecProvider<S> implements StateProvider<S> {

        protected Supplier<Map<String, TypedReaderTableSpec<DataType>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_specSupplier = initializer.computeFromProvidedState(TypedReaderTableSpecsProvider.class);
            //            initializer.computeOnValueChange(ConfigIdReference.class);
            initializer.computeOnValueChange(FileChooserRef.class);
        }
    }

    static final class TableSpecSettingsProvider extends DependsOnTypedReaderTableSpecProvider<TableSpecSettings[]> {

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

        private Supplier<CommonReaderNodeSettings.AdvancedSettings.HowToCombineColumnsOption> m_howToCombineColumnsOptionSupplier;

        private Supplier<TransformationElementSettings[]> m_existingSettings;

        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            m_howToCombineColumnsOptionSupplier = initializer
                .computeFromValueSupplier(CommonReaderNodeSettings.AdvancedSettings.HowToCombineColumnsOptionRef.class);
            m_existingSettings = initializer.getValueSupplier(TransformationElementSettingsReference.class);
        }

        @Override
        public TransformationElementSettings[] computeState(final DefaultNodeSettingsContext context) {
            return toTransformationElements(m_specSupplier.get(), m_howToCombineColumnsOptionSupplier.get(),
                m_existingSettings.get());
        }

        static TransformationElementSettings[] toTransformationElements(
            final Map<String, TypedReaderTableSpec<DataType>> specs,
            final CommonReaderNodeSettings.AdvancedSettings.HowToCombineColumnsOption howToCombineColumnsOption,
            final TransformationElementSettings[] existingSettings) {

            /**
             * List of existing elements before the unknown element
             */
            final List<TransformationElementSettings> elementsBeforeUnknown = new ArrayList<>();
            /**
             * The existing unknown element.
             */
            TransformationElementSettings foundUnknownElement = null;
            /**
             * List of existing elements after the unknown element
             */
            final List<TransformationElementSettings> elementsAfterUnknown = new ArrayList<>();

            final var rawSpec = toRawSpec(specs);
            final var newSpecs = howToCombineColumnsOption.toColumnFilterMode().getRelevantSpec(rawSpec);
            final var newSpecsByName =
                newSpecs.stream().collect(Collectors.toMap(column -> column.getName().get(), Function.identity()));

            for (int i = 0; i < existingSettings.length; i++) { // NOSONAR
                final var existingElement = existingSettings[i];
                if (existingElement.m_columnName == null) {
                    foundUnknownElement = existingSettings[i];
                    continue;
                }
                final var newSpec = newSpecsByName.get(existingElement.m_columnName);
                if (newSpec == null) {
                    // element does not exist anymore -> it is removed
                    continue;
                }
                final var targetList = foundUnknownElement == null ? elementsBeforeUnknown : elementsAfterUnknown;
                targetList.add(mergeExistingWithNew(existingElement, newSpec));
            }

            /**
             * foundUnknownElement can only be null when the dialog is opened for the first time
             */
            final var unknownElement = foundUnknownElement == null
                ? TransformationElementSettings.createUnknownElement() : foundUnknownElement;

            final var existingColumnNames = Stream.concat(elementsBeforeUnknown.stream(), elementsAfterUnknown.stream())
                .map(element -> element.m_columnName).collect(Collectors.toSet());

            final var unknownElementsType = getUnknownElementsType(unknownElement);
            final var newElements =
                newSpecs.stream().filter(colSpec -> !existingColumnNames.contains(colSpec.getName().get()))
                    .map(colSpec -> createNewElement(colSpec, unknownElementsType.orElse(null),
                        unknownElement.m_includeInOutput))
                    .toList();

            return Stream.concat( //
                Stream.concat(elementsBeforeUnknown.stream(), newElements.stream()),
                Stream.concat(Stream.of(unknownElement), elementsAfterUnknown.stream()))
                .toArray(TransformationElementSettings[]::new);

        }

        private static TransformationElementSettings mergeExistingWithNew(
            final TransformationElementSettings existingElement, final TypedReaderColumnSpec<DataType> newSpec) {
            final var newElement = createNewElement(newSpec);
            if (!newElement.m_originalType.equals(existingElement.m_originalType)) {
                return newElement;
            }
            newElement.m_type = existingElement.m_type;
            newElement.m_columnRename = existingElement.m_columnRename;
            newElement.m_includeInOutput = existingElement.m_includeInOutput;
            return newElement;

        }

        private static Optional<DataType> getUnknownElementsType(final TransformationElementSettings unknownElement) {
            if (TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID.equals(unknownElement.m_type)) {
                return Optional.empty();
            }
            return PRODUCTION_PATH_PROVIDER.getAvailableDataTypes().stream()
                .filter(type -> type.getName().equals(unknownElement.m_type)).findFirst();
        }

        /**
         * @return a new element as if it would be constructed as unknown new when the <any unknown column> element is
         *         configured like the default.
         */
        private static TransformationElementSettings createNewElement(final TypedReaderColumnSpec<DataType> colSpec) {
            return createNewElement(colSpec, null, true);
        }

        private static TransformationElementSettings createNewElement(final TypedReaderColumnSpec<DataType> colSpec,
            final DataType unknownElementsType, final boolean includeInOutput) {
            final var name = colSpec.getName().get(); // NOSONAR in the TypedReaderTableSpecProvider we make sure that names are always present
            final var defPath = PRODUCTION_PATH_PROVIDER.getDefaultProductionPath(colSpec.getType());

            final var path = Optional.ofNullable(unknownElementsType)
                .flatMap(type -> findProductionPath(colSpec.getType(), type)).orElse(defPath);
            final var type = path.getConverterFactory().getIdentifier();
            final var defType = defPath.getConverterFactory().getIdentifier();
            return new TransformationElementSettings(name, includeInOutput, name, type, defType,
                defPath.getDestinationType().toPrettyString());

        }

        private static Optional<ProductionPath> findProductionPath(final DataType from, final DataType to) {
            return PRODUCTION_PATH_PROVIDER.getAvailableProductionPaths(from).stream()
                .filter(path -> to.equals(path.getDestinationType())).findFirst();
        }
    }

    static final class TypeChoicesProvider implements StringChoicesStateProvider {

        static final String DEFAULT_COLUMNTYPE_ID = "<default-columntype>";

        static final String DEFAULT_COLUMNTYPE_TEXT = "Default columntype";

        private Supplier<String> m_columnNameSupplier;

        private Supplier<Map<String, TypedReaderTableSpec<DataType>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_columnNameSupplier = initializer.getValueSupplier(ColumnNameRef.class);
            initializer.computeOnValueChange(
                KnimeTableReaderTransformationSettings.PersistorSettings.TableSpecSettingsRef.class);
            m_specSupplier = initializer.computeFromProvidedState(TypedReaderTableSpecsProvider.class);
        }

        @Override
        public IdAndText[] computeState(final DefaultNodeSettingsContext context) {
            final var columnName = m_columnNameSupplier.get();

            if (columnName == null) {
                final var defaultChoice = new IdAndText(DEFAULT_COLUMNTYPE_ID, DEFAULT_COLUMNTYPE_TEXT);
                final var dataTypeChoices = PRODUCTION_PATH_PROVIDER.getAvailableDataTypes().stream()
                    .sorted((t1, t2) -> t1.toPrettyString().compareTo(t2.toPrettyString()))
                    .map(type -> new IdAndText(type.getName(), type.toPrettyString())).toList();
                return Stream.concat(Stream.of(defaultChoice), dataTypeChoices.stream()).toArray(IdAndText[]::new);
            }

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
