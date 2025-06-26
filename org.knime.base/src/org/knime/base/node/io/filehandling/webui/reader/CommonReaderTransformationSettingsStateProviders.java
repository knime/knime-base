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

import static org.knime.base.node.io.filehandling.webui.reader.ReaderSpecific.toSpecMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.base.node.io.filehandling.webui.FileChooserPathAccessor;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.AdvancedSettingsWithMultipleFileHandling.HowToCombineColumnsOption;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.AdvancedSettingsWithMultipleFileHandling.HowToCombineColumnsOptionRef;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.BaseSettings.FileSelectionRef;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.ColumnSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.ConfigIdRef;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.ConfigIdSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TableSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TableSpecSettingsRef;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TransformationElementSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TransformationElementSettings.ColumnNameRef;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TransformationElementSettingsRef;
import org.knime.base.node.io.filehandling.webui.reader.ReaderSpecific.ExternalDataTypeSerializer;
import org.knime.base.node.io.filehandling.webui.reader.ReaderSpecific.ProductionPathProviderAndTypeHierarchy;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup.WidgetGroupModifier;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.StringChoice;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.StringChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider.TypeReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;
import org.knime.filehandling.core.util.WorkflowContextUtil;

/**
 *
 * A utility class containing state providers for common reader transformation settings. Refer to
 * {@link TransformationSettingsWidgetModification} as a starting point for using these state providers.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
public final class CommonReaderTransformationSettingsStateProviders {

    static final NodeLogger LOGGER = NodeLogger.getLogger(CommonReaderTransformationSettingsStateProviders.class);

    public static class SourceIdProvider implements StateProvider<String> {

        protected Supplier<FileSelection> m_fileSelectionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_fileSelectionSupplier = initializer.computeFromValueSupplier(FileSelectionRef.class);
            initializer.computeAfterOpenDialog();
        }

        @Override
        public String computeState(final DefaultNodeSettingsContext context) {
            return m_fileSelectionSupplier.get().getFSLocation().getPath();
        }
    }

    /**
     * Implemented by all stateProviders which need the {@link FileChooser} transformed into a list of {@link FSPath}
     * objects. Note that it is not possible to make this an independent StateProvider<FSPath[]> since the file system
     * needs to be kept open while the paths are accessed.
     *
     * @param <S> the provided state
     */
    abstract static class PathsProvider<S> implements StateProvider<S> {

        protected Supplier<FileSelection> m_fileSelectionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_fileSelectionSupplier = initializer.getValueSupplier(FileSelectionRef.class);
        }

        @Override
        public S computeState(final DefaultNodeSettingsContext context) throws StateComputationFailureException {
            final var fileSelection = m_fileSelectionSupplier.get();
            if (!WorkflowContextUtil.hasWorkflowContext() // no workflow context available
                // no file selected (yet)
                || fileSelection.getFSLocation().equals(new FSLocation(FSCategory.LOCAL, ""))) {
                return computeStateFromPaths(Collections.emptyList());
            }

            var fsConnection = FileSystemPortConnectionUtil.getFileSystemConnection(context);
            if (fileSelection.getFSLocation().getFSCategory() == FSCategory.CONNECTED && fsConnection.isEmpty()) {
                return computeStateFromPaths(Collections.emptyList());
            }

            try (final var accessor = new FileChooserPathAccessor(fileSelection, fsConnection)) {
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

        abstract S computeStateFromPaths(List<FSPath> paths) throws StateComputationFailureException;
    }

    public static class FSLocationsProvider extends PathsProvider<FSLocation[]> {
        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            initializer.computeAfterOpenDialog();
            initializer.computeOnValueChange(FileSelectionRef.class);
        }

        @Override
        FSLocation[] computeStateFromPaths(final List<FSPath> paths) {
            return paths.stream().map(FSPath::toFSLocation).toArray(FSLocation[]::new);
        }
    }

    /**
     * This state provider uses the extracted fs paths and the reader specific dependencies to configure a reader and
     * read the them to table specs.
     *
     * @param <C> The reader specific [C]onfiguration
     * @param <I> The config [I]d settings used within the transformation settings
     * @param <T> the type used to represent external data [T]ypes
     */
    public abstract static class TypedReaderTableSpecsProvider<//
            C extends ReaderSpecificConfig<C>, I extends ConfigIdSettings<C>, T>
        extends PathsProvider<Map<String, TypedReaderTableSpec<T>>> implements ReaderSpecific.ConfigAndReader<C, T> {

        private Supplier<I> m_configIdSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            m_configIdSupplier = initializer.getValueSupplier(ConfigIdRef.class, getConfigIdTypeReference());
        }

        /**
         *
         * Boilerplate method to provide the correct type reference for the config id settings.
         *
         * @return new TypeReference<>() { } (the type inference will fill in the correct type).
         */
        protected abstract TypeReference<I> getConfigIdTypeReference();

        @Override
        protected Map<String, TypedReaderTableSpec<T>> computeStateFromPaths(final List<FSPath> paths)
            throws StateComputationFailureException {
            final var config = getMultiTableReadConfig().getTableReadConfig();
            final var configIdSettings = m_configIdSupplier.get();
            try {
                configIdSettings.applyToConfig(config);
            } catch (IllegalArgumentException e) {
                LOGGER.error(e);
                throw new StateComputationFailureException(e.getMessage());
            }

            final var tableReader = getTableReader();
            final var exec = new ExecutionMonitor();
            final var specs = new HashMap<String, TypedReaderTableSpec<T>>();
            for (var path : paths) {
                try {
                    specs.put(path.toFSLocation().getPath(),
                        MultiTableUtils.assignNamesIfMissing(tableReader.readSpec(path, config, exec)));
                } catch (IOException | IllegalArgumentException e) {
                    LOGGER.error(e);
                    return Collections.emptyMap();
                }
            }
            return specs;
        }

        /**
         * Extend this interface by one with a default implementation to let the implementation of
         * {@link TableSpecSettingsProvider} and {@link TransformationElementSettingsProvider} extend it.
         *
         * @param <T> the type used to represent external data [T]ypes
         *
         */
        public interface Dependent<T> {

            /**
             * @param <C> The reader specific [C]onfiguration
             * @param <I> The config [I]d settings used within the transformation settings
             * @return the class of the reader specific implementation of {@link TypedReaderTableSpecsProvider}
             */
            <C extends ReaderSpecificConfig<C>, I extends ConfigIdSettings<C>>
                Class<? extends TypedReaderTableSpecsProvider<C, I, T>> getTypedReaderTableSpecsProvider();
        }
    }

    abstract static class DependsOnTypedReaderTableSpecProvider<P, S, T>
        implements StateProvider<P>, TypedReaderTableSpecsProvider.Dependent<T>, ExternalDataTypeSerializer<S, T> {

        protected Supplier<Map<String, TypedReaderTableSpec<T>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initFileChanges(initializer);
            initializer.computeOnValueChange(ConfigIdRef.class);
            m_specSupplier = initializer.computeFromProvidedState(getTypedReaderTableSpecsProvider());
        }

        protected void initFileChanges(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            initializer.computeOnValueChange(FileSelectionRef.class);
        }
    }

    /**
     *
     * Transforms the extracted {@link TypedReaderTableSpec}s into a serializable form.
     *
     * @param <S> the type used to [S]erialize external data types
     * @param <T> the type used to represent external data [T]ypes
     */
    public abstract static class TableSpecSettingsProvider<S, T>
        extends DependsOnTypedReaderTableSpecProvider<List<TableSpecSettings<S>>, S, T> {

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

    /**
     * This value provider is used to change the displayed array layout elements in the transformation settings dialog.
     *
     * @param <S> the type used to [S]erialize external data types
     * @param <T> the type used to represent external data [T]ypes
     */
    public abstract static class TransformationElementSettingsProvider<S, T>
        extends DependsOnTypedReaderTableSpecProvider<TransformationElementSettings[], S, T>
        implements ProductionPathProviderAndTypeHierarchy<T> {

        private Supplier<HowToCombineColumnsOption> m_howToCombineColumnsSup;

        private Supplier<TransformationElementSettings[]> m_existingSettings;

        private Supplier<List<TableSpecSettings<S>>> m_existingSpecs;

        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            if (hasMultipleFileHandling()) {
                m_howToCombineColumnsSup = initializer.computeFromValueSupplier(HowToCombineColumnsOptionRef.class);
            } else {
                m_howToCombineColumnsSup = () -> HowToCombineColumnsOption.FAIL;
            }
            m_existingSettings = initializer.getValueSupplier(TransformationElementSettingsRef.class);
            m_existingSpecs = initializer.getValueSupplier(
                /** Contains a wildcard instead of S, since this is a common field */
                TableSpecSettingsRef.class,
                /** So we need to rectify the type by type reference */
                getTableSpecSettingsTypeReference());
        }

        /**
         *
         * Boilerplate method to provide the correct type reference for the specs.
         *
         * @return new TypeReference<>() { } (the type inference will fill in the correct type).
         */
        protected abstract TypeReference<List<TableSpecSettings<S>>> getTableSpecSettingsTypeReference();

        /**
         * @return true if the node supports handling multiple files
         * @see CommonReaderNodeSettings.AdvancedSettingsWithMultipleFileHandling
         */
        protected abstract boolean hasMultipleFileHandling();

        @Override
        public TransformationElementSettings[] computeState(final DefaultNodeSettingsContext context) {
            return toTransformationElements(m_specSupplier.get(), m_howToCombineColumnsSup.get(),
                m_existingSettings.get(), getExistingSpecsUnion());
        }

        TransformationElementSettings[] toTransformationElements(final Map<String, TypedReaderTableSpec<T>> specs,
            final HowToCombineColumnsOption howToCombineColumnsOption,
            final TransformationElementSettings[] existingSettings, final TypedReaderTableSpec<T> existingSpecsUnion) {

            /**
             * List of existing elements before the unknown element
             */
            final List<TransformationElementSettings> elementsBeforeUnknown = new ArrayList<>();

            final var existingColumnTypesByName = existingSpecsUnion.stream().filter(s -> s.getName().isPresent())
                .collect(Collectors.toMap(s -> s.getName().get(), TypedReaderColumnSpec::getType));
            final Collection<String> existingElementNamesWithChangedType = new HashSet<>();
            /**
             *
             * The existing unknown element.
             */
            TransformationElementSettings foundUnknownElement = null;
            /**
             * List of existing elements after the unknown element
             */
            final List<TransformationElementSettings> elementsAfterUnknown = new ArrayList<>();

            final var rawSpec = toRawSpec(specs);
            final var newSpecs = howToCombineColumnsOption.toColumnFilterMode().getRelevantSpec(rawSpec);
            final var newSpecsByName = newSpecs.stream().filter(column -> column.getName().isPresent())
                .collect(Collectors.toMap(column -> column.getName().get(), Function.identity()));

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
                final var existingType = existingColumnTypesByName.get(newSpec.getName().orElse(null));
                if (existingType != null && !existingType.equals(newSpec.getType())) {
                    // element has different type than before
                    existingElementNamesWithChangedType.add(existingElement.m_columnName);
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
            final Predicate<String> isNewColumn = colName -> existingElementNamesWithChangedType.contains(colName)
                || !existingColumnNames.contains(colName);

            final var newElements =
                newSpecs.stream().filter(colSpec -> isNewColumn.test(colSpec.getName().orElse(null)))
                    .map(colSpec -> createNewElement(colSpec, unknownElementsType.orElse(null),
                        unknownElement.m_includeInOutput))
                    .toList();

            return Stream.concat( //
                Stream.concat(elementsBeforeUnknown.stream(), newElements.stream()),
                Stream.concat(Stream.of(unknownElement), elementsAfterUnknown.stream()))
                .toArray(TransformationElementSettings[]::new);

        }

        private TypedReaderTableSpec<T> getExistingSpecsUnion() {
            final var existingSpecs = toSpecMap(this, m_existingSpecs.get());
            final var existingRawSpecs = toRawSpec(existingSpecs);
            return existingRawSpecs.getUnion();
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
            return Optional.of(fromDataTypeId(unknownElement.m_type));
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

    /**
     * This state provider determines the possible values in the individual type choices of elements within the
     * transformation settings array layout.
     *
     * @param <T> the type used to represent external data [T]ypes
     */
    public abstract static class TypeChoicesProvider<T> implements StringChoicesProvider,
        ProductionPathProviderAndTypeHierarchy<T>, TypedReaderTableSpecsProvider.Dependent<T> {

        static final String DEFAULT_COLUMNTYPE_ID = "<default-columntype>";

        static final String DEFAULT_COLUMNTYPE_TEXT = "Default columntype";

        private Supplier<String> m_columnNameSupplier;

        private Supplier<Map<String, TypedReaderTableSpec<T>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_columnNameSupplier = initializer.getValueSupplier(ColumnNameRef.class);
            initializer.computeOnValueChange(CommonReaderTransformationSettings.TableSpecSettingsRef.class);
            m_specSupplier = initializer.computeFromProvidedState(getTypedReaderTableSpecsProvider());
        }

        @Override
        public List<StringChoice> computeState(final DefaultNodeSettingsContext context) {
            final var columnName = m_columnNameSupplier.get();

            if (columnName == null) { // i.e., any unknown column
                final var defaultChoice = new StringChoice(DEFAULT_COLUMNTYPE_ID, DEFAULT_COLUMNTYPE_TEXT);
                final var dataTypeChoices = getProductionPathProvider().getAvailableDataTypes().stream()
                    .sorted((t1, t2) -> t1.toPrettyString().compareTo(t2.toPrettyString()))
                    .map(type -> new StringChoice(getDataTypeId(type), type.toPrettyString())).toList();
                return Stream.concat(Stream.of(defaultChoice), dataTypeChoices.stream()).toList();
            }

            final var union = toRawSpec(m_specSupplier.get()).getUnion();
            final var columnSpecOpt =
                union.stream().filter(colSpec -> colSpec.getName().get().equals(columnName)).findAny();
            if (columnSpecOpt.isEmpty()) {
                return List.of();
            }
            final var columnSpec = columnSpecOpt.get();
            final var productionPaths = getProductionPathProvider().getAvailableProductionPaths(columnSpec.getType());
            return productionPaths.stream().map(
                p -> new StringChoice(p.getConverterFactory().getIdentifier(), p.getDestinationType().toPrettyString()))
                .toList();
        }

    }

    static String getDataTypeId(final DataType type) {
        return DataTypeStringSerializer.typeToString(type);
    }

    static DataType fromDataTypeId(final String id) {
        return DataTypeStringSerializer.stringToType(id);
    }

    /**
     * Put a reader specific implementation of this class in a {@link Modification} annotation on the reader specific
     * implementation of {@link CommonReaderTransformationSettings}. This ensures, that the common fields within these
     * settings are updated as expected.
     *
     * For filling the abstract methods within this class, some abstract state providers need to be implemented. But
     * none of the to be overwritten methods of these state providers apart from the boilerplate methods to construct
     * {@link TypeReference}s should be implemented directly, since they stem from interfaces which are used for
     * multiple state providers and for the {@link CommonReaderTransformationSettingsPersistor}. Instead, extend these
     * interfaces (in {@link ReaderSpecific}) by ones that overwrite all methods with default implementations and use
     * those in the respective places.
     *
     * @param <S> the type used to [S]erialize external data types
     * @param <T> the type used to represent external data [T]ypes
     */
    public abstract static class TransformationSettingsWidgetModification<S, T> implements WidgetGroup.Modifier {

        static class ConfigIdSettingsRef implements Modification.Reference {
        }

        static class SpecsRef implements Modification.Reference {
        }

        static class TypeChoicesWidgetRef implements Modification.Reference {
        }

        static final class TransformationElementSettingsArrayWidgetRef implements Modification.Reference {
        }

        static final class AppendPathColumnRef extends ReferenceStateProvider<Boolean>
            implements Modification.Reference {
        }

        protected static final class SourceIdRef extends ReferenceStateProvider<String>
            implements Modification.Reference {
        }

        protected static final class FSLocationsRef extends ReferenceStateProvider<FSLocation[]>
            implements Modification.Reference {
        }

        static final class PathColumnNameRef extends ReferenceStateProvider<String> implements Modification.Reference {
        }

        static final class ColumnFilterModeRef extends ReferenceStateProvider<ColumnFilterMode>
            implements Modification.Reference {
        }

        @Override
        public void modify(final WidgetGroupModifier group) {
            group.find(SpecsRef.class).addAnnotation(ValueProvider.class).withValue(getSpecsValueProvider()).modify();
            group.find(TypeChoicesWidgetRef.class).addAnnotation(ChoicesProvider.class)
                .withValue(getTypeChoicesProvider()).modify();
            group.find(TransformationElementSettingsArrayWidgetRef.class).addAnnotation(ValueProvider.class)
                .withValue(getTransformationSettingsValueProvider()).modify();
            if (!hasMultipleFileHandling()) {
                // remove value providers to fall back to defaults
                group.find(AppendPathColumnRef.class).removeAnnotation(ValueProvider.class);
                group.find(PathColumnNameRef.class).removeAnnotation(ValueProvider.class);
                group.find(ColumnFilterModeRef.class).removeAnnotation(ValueProvider.class);
            }
        }

        /**
         * @return the value provider for the specs.
         */
        protected abstract Class<? extends TableSpecSettingsProvider<S, T>> getSpecsValueProvider();

        /**
         * @return the value provider for the transformation settings array layout.
         */
        protected abstract Class<? extends TransformationElementSettingsProvider<S, T>>
            getTransformationSettingsValueProvider();

        /**
         * @return the choices provider for the types of array layout elements.
         */
        protected abstract Class<? extends TypeChoicesProvider<T>> getTypeChoicesProvider();

        /**
         * @return true if the node supports handling multiple files
         * @see CommonReaderNodeSettings.AdvancedSettingsWithMultipleFileHandling
         */
        protected abstract boolean hasMultipleFileHandling();
    }

    private CommonReaderTransformationSettingsStateProviders() {
        // Utility class
    }
}
