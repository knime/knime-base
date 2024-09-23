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
 *   Sep 19, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.webui.reader;

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
import org.knime.base.node.io.filehandling.webui.FileChooserPathAccessor;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.Settings.FileChooserRef;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.ColumnSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.ConfigIdSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TableSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TransformationElementSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TransformationElementSettings.ColumnNameRef;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TransformationElementSettingsReference;
import org.knime.base.node.io.filehandling.webui.reader.ReaderSpecific.ExternalDataTypeSerializer;
import org.knime.base.node.io.filehandling.webui.reader.ReaderSpecific.ProductionPathProviderAndTypeHierarchy;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.WidgetModification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings({"javadoc", "restriction"})
public class CommonReaderTransformationSettingsStateProviders {

    static final NodeLogger LOGGER = NodeLogger.getLogger(CommonReaderTransformationSettingsStateProviders.class);

    /**
     * This object holds copies of the subset of settings in {@link CSVTableReaderNodeSettings} that can affect the spec
     * of the output table. I.e., if any of these settings change, the spec has to be re-calculcated.
     */
    public interface ReaderSpecificDependencies<C extends ReaderSpecificConfig<C>> {

        default void applyToConfig(@SuppressWarnings("unused") final DefaultTableReadConfig<C> config) {
            // do nothing per default
        }
    }

    public interface ReaderSpecificDependenciesProvider<D extends ReaderSpecificDependencies> extends StateProvider<D> {

        @Override
        default public void init(final StateProviderInitializer initializer) {
        }

        public interface GetReferences {
            /**
             * TODO: Rethink this. We originally had to postpone triggers to those state providers which only concern
             * the outside of the array (i.e. not the type choices provider) but we now allow triggering from outside
             * the array.
             *
             * But it is not so straightforward as now setting all these dependencies as triggers in the dependency
             * provider, because we then run into a timing issue: We currently require, that first all element values
             * are determined and then secondly, the choices of these elements are computed (with a second backend
             * call).
             *
             * @return a list of postponed triggers
             */
            default List<Class<? extends Reference<?>>> getDependencyReferences() {
                return List.of();
            }
        }

        public interface Dependent<D extends ReaderSpecificDependencies> {
            @SuppressWarnings("unchecked")
            default Class<? extends ReaderSpecificDependenciesProvider<D>> getDependenciesProvider() {
                // when running into a ClassCastException here, you should extends the Dependent interface
                return (Class<? extends ReaderSpecificDependenciesProvider<D>>)NoAdditionalDependencies.class;
            }
        }
    }

    @SuppressWarnings("unused")
    static final class NoAdditionalDependencies<D extends ReaderSpecificDependencies>
        implements ReaderSpecificDependenciesProvider<ReaderSpecificDependencies> {

        @Override
        public ReaderSpecificDependencies computeState(final DefaultNodeSettingsContext context) {
            return new ReaderSpecificDependencies() {
            };
        }
    }

    static final class SourceIdProvider implements StateProvider<String> {

        private Supplier<FileChooser> m_fileChooserSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_fileChooserSupplier = initializer.computeFromValueSupplier(FileChooserRef.class);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public String computeState(final DefaultNodeSettingsContext context) {
            return m_fileChooserSupplier.get().getFSLocation().getPath();
        }
    }

    public abstract static class PathsProvider<S, D extends ReaderSpecificDependencies>
        implements StateProvider<S>, ReaderSpecificDependenciesProvider.Dependent<D> {

        protected Supplier<D> m_dependenciesSupplier;

        private Supplier<FileChooser> m_fileChooserSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_fileChooserSupplier = initializer.getValueSupplier(FileChooserRef.class);
            m_dependenciesSupplier = initializer.computeFromProvidedState(getDependenciesProvider());
        }

        @Override
        public S computeState(final DefaultNodeSettingsContext context) {
            final var fileChooser = m_fileChooserSupplier.get();
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

    public static abstract class FSLocationsProvider<D extends ReaderSpecificDependencies>
        extends PathsProvider<FSLocation[], D> {
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

    public static abstract class TypedReaderTableSpecsProvider<C extends ReaderSpecificConfig<C>, T, D extends ReaderSpecificDependencies<C>>
        extends PathsProvider<Map<String, TypedReaderTableSpec<T>>, D> implements ReaderSpecific.ConfigAndReader<C, T> {

        @Override
        Map<String, TypedReaderTableSpec<T>> computeStateFromPaths(final List<FSPath> paths) {
            final var tableReader = getTableReader();
            final var exec = new ExecutionMonitor();
            final var specs = new HashMap<String, TypedReaderTableSpec<T>>();
            final var config = getMultiTableReadConfig().getTableReadConfig();
            final var dependencies = m_dependenciesSupplier.get();
            dependencies.applyToConfig(config);
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

        public interface Dependent<T> {
            Class<? extends TypedReaderTableSpecsProvider<?, T, ?>> getTypedReaderTableSpecsProvider();
        }
    }

    abstract static class DependsOnTypedReaderTableSpecProvider<S, T> implements StateProvider<S>,
        TypedReaderTableSpecsProvider.Dependent<T>, ReaderSpecificDependenciesProvider.GetReferences {

        protected Supplier<Map<String, TypedReaderTableSpec<T>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_specSupplier = initializer.computeFromProvidedState(getTypedReaderTableSpecsProvider());
            getDependencyReferences().forEach(initializer::computeOnValueChange);
            initializer.computeOnValueChange(FileChooserRef.class);
        }

    }

    public static abstract class TableSpecSettingsProvider<S, T>
        extends DependsOnTypedReaderTableSpecProvider<List<TableSpecSettings<S>>, T>
        implements ExternalDataTypeSerializer<S, T> {

        @Override
        public List<TableSpecSettings<S>> computeState(final DefaultNodeSettingsContext context) {

            return m_specSupplier.get().entrySet().stream()
                .map(e -> new TableSpecSettings<>(e.getKey(),
                    e.getValue().stream()
                        .map(spec -> new ColumnSpecSettings<>(spec.getName().get(), toSerializableType(spec.getType())))
                        .toList()))
                .toList();
        }
    }

    public static abstract class TransformationElementSettingsProvider<T>
        extends DependsOnTypedReaderTableSpecProvider<TransformationElementSettings[], T>
        implements ProductionPathProviderAndTypeHierarchy<T> {

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

        TransformationElementSettings[] toTransformationElements(final Map<String, TypedReaderTableSpec<T>> specs,
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

        private TransformationElementSettings mergeExistingWithNew(final TransformationElementSettings existingElement,
            final TypedReaderColumnSpec<T> newSpec) {
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
            return Optional.of(DataTypeStringSerializer.stringToType(unknownElement.m_type));
        }

        /**
         * @return a new element as if it would be constructed as unknown new when the <any unknown column> element is
         *         configured like the default.
         */
        private TransformationElementSettings createNewElement(final TypedReaderColumnSpec<T> colSpec) {
            return createNewElement(colSpec, null, true);
        }

        private TransformationElementSettings createNewElement(final TypedReaderColumnSpec<T> colSpec,
            final DataType unknownElementsType, final boolean includeInOutput) {
            final var name = colSpec.getName().get(); // NOSONAR in the TypedReaderTableSpecProvider we make sure that names are always present
            final var defPath = getProductionPathProvider().getDefaultProductionPath(colSpec.getType());

            final var path = Optional.ofNullable(unknownElementsType)
                .flatMap(type -> findProductionPath(colSpec.getType(), type)).orElse(defPath);
            final var type = path.getConverterFactory().getIdentifier();
            final var defType = defPath.getConverterFactory().getIdentifier();
            return new TransformationElementSettings(name, includeInOutput, name, type, defType,
                defPath.getDestinationType().toPrettyString());

        }

        private Optional<ProductionPath> findProductionPath(final T from, final DataType to) {
            return getProductionPathProvider().getAvailableProductionPaths(from).stream()
                .filter(path -> to.equals(path.getDestinationType())).findFirst();
        }
    }

    public static abstract class TypeChoicesProvider<T> implements StringChoicesStateProvider,
        ProductionPathProviderAndTypeHierarchy<T>, TypedReaderTableSpecsProvider.Dependent<T> {

        // TODO: Non-public
        public static final String DEFAULT_COLUMNTYPE_ID = "<default-columntype>";

        // TODO: Non-public
        public static final String DEFAULT_COLUMNTYPE_TEXT = "Default columntype";

        private Supplier<String> m_columnNameSupplier;

        private Supplier<Map<String, TypedReaderTableSpec<T>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_columnNameSupplier = initializer.getValueSupplier(ColumnNameRef.class);
            initializer.computeOnValueChange(CommonReaderTransformationSettings.TableSpecSettingsRef.class);
            m_specSupplier = initializer.computeFromProvidedState(getTypedReaderTableSpecsProvider());
        }

        @Override
        public IdAndText[] computeState(final DefaultNodeSettingsContext context) {
            final var columnName = m_columnNameSupplier.get();

            if (columnName == null) {
                final var defaultChoice = new IdAndText(DEFAULT_COLUMNTYPE_ID, DEFAULT_COLUMNTYPE_TEXT);
                final var dataTypeChoices = getProductionPathProvider().getAvailableDataTypes().stream()
                    .sorted((t1, t2) -> t1.toPrettyString().compareTo(t2.toPrettyString()))
                    .map(type -> new IdAndText(getDataTypeId(type), type.toPrettyString())).toList();
                return Stream.concat(Stream.of(defaultChoice), dataTypeChoices.stream()).toArray(IdAndText[]::new);
            }

            final var union = toRawSpec(m_specSupplier.get()).getUnion();
            final var columnSpecOpt =
                union.stream().filter(colSpec -> colSpec.getName().get().equals(columnName)).findAny();
            if (columnSpecOpt.isEmpty()) {
                return new IdAndText[0];
            }
            final var columnSpec = columnSpecOpt.get();
            final var productionPaths = getProductionPathProvider().getAvailableProductionPaths(columnSpec.getType());
            return productionPaths.stream().map(
                p -> new IdAndText(p.getConverterFactory().getIdentifier(), p.getDestinationType().toPrettyString()))
                .toArray(IdAndText[]::new);
        }

        static String getDataTypeId(final DataType type) {
            return DataTypeStringSerializer.typeToString(type);
        }
    }

    public static abstract class TransformationSettingsWidgetModification<C extends ConfigIdSettings<?>, S, T>
        implements WidgetModification.Modifier {

        static class ConfigIdSettingsRef implements WidgetModification.Reference {
        }

        static class SpecsRef implements WidgetModification.Reference {
        }

        static class TypeChoicesWidgetRef implements WidgetModification.Reference {
        }

        static final class TransformationElementSettingsArrayWidgetRef implements WidgetModification.Reference {
        }

        static class FsLocationRef implements WidgetModification.Reference {
        }

        @Override
        public void modify(final WidgetGroupModifier group) {
            group.find(ConfigIdSettingsRef.class).addAnnotation(ValueReference.class)
                .withValue(getConfigIdSettingsValueRef()).build();
            group.find(SpecsRef.class).addAnnotation(ValueProvider.class).withValue(getSpecsValueProvider()).build();
            group.find(TypeChoicesWidgetRef.class).addAnnotation(ChoicesWidget.class)
                .withProperty("choicesProvider", getTypeChoicesProvider()).build();
            group.find(TransformationElementSettingsArrayWidgetRef.class).addAnnotation(ValueProvider.class)
                .withValue(getTransformationSettingsValueProvider()).build();
            group.find(FsLocationRef.class).addAnnotation(ValueProvider.class).withValue(getFsLocationProvider())
                .build();
        }

        protected abstract Class<? extends Reference<C>> getConfigIdSettingsValueRef();

        protected abstract Class<? extends TableSpecSettingsProvider<S, T>> getSpecsValueProvider();

        protected abstract Class<? extends TypeChoicesProvider<T>> getTypeChoicesProvider();

        protected abstract Class<? extends TransformationElementSettingsProvider<T>>
            getTransformationSettingsValueProvider();

        protected abstract Class<? extends FSLocationsProvider<?>> getFsLocationProvider();
    }

    private CommonReaderTransformationSettingsStateProviders() {
        // Utility class
    }
}
