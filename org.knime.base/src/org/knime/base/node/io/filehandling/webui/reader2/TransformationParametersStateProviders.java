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
package org.knime.base.node.io.filehandling.webui.reader2;

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
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileReaderParameters.HowToCombineColumnsOption;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileReaderParameters.HowToCombineColumnsOptionRef;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderSpecific.ExternalDataTypeSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderSpecific.ProductionPathProviderAndTypeHierarchy;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParameters.ColumnSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParameters.TableSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParameters.TableSpecSettingsRef;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParameters.TransformationElementSettings;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParameters.TransformationElementSettings.ColumnNameRef;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParameters.TransformationElementSettingsRef;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.DefaultFileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractFileChooserPathAccessor;
import org.knime.filehandling.core.node.table.reader.config.AbstractMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;
import org.knime.filehandling.core.util.WorkflowContextUtil;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;

/**
 *
 * A utility class containing state providers for common reader transformation settings. Refer to
 * {@link TransformationSettingsWidgetModification} as a starting point for using these state providers.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @author Paul Bärnreuther
 * @since 5.10
 */
@SuppressWarnings("restriction")
public final class TransformationParametersStateProviders {

    static final NodeLogger LOGGER = NodeLogger.getLogger(TransformationParametersStateProviders.class);

    private interface SourceParameter {

        boolean isEmpty();

        boolean isConnected();

        AbstractFileChooserPathAccessor getPathAccessor(Optional<FSConnection> fsConnection); // NOSONAR private interface

    }

    /**
     * This state provider uses the extracted fs paths and the reader specific dependencies to configure a reader and
     * read the them to table specs.
     *
     * @param <C> The reader specific [C]onfiguration
     * @param <T> the type used to represent external data [T]ypes
     * @param <M> The [M]ulti-table read configuration
     */
    public abstract static class TypedReaderTableSpecsProvider<C extends ReaderSpecificConfig<C>, T, //
            M extends AbstractMultiTableReadConfig<C, DefaultTableReadConfig<C>, T, M>>
        implements StateProvider<Map<FSLocation, TypedReaderTableSpec<T>>>, ReaderSpecific.ConfigAndReader<C, T, M> {

        /**
         * Set this to false if the implementation is for single file selection only.
         *
         * @return true if multi file selection is supported
         */
        protected boolean isMultiFileSelection() {
            return true;
        }

        private Supplier<SourceParameter> m_fileSelectionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {

            if (isMultiFileSelection()) {
                final var multiFileSelectionProvider =
                    initializer.getValueSupplier(MultiFileSelectionParameters.FileSelectionRef.class);
                m_fileSelectionSupplier = () -> toSourceParameters(multiFileSelectionProvider.get());
            } else {
                final var singleFileSelectionProvider =
                    initializer.getValueSupplier(SingleFileSelectionParameters.FileSelectionRef.class);
                m_fileSelectionSupplier = () -> toSourceParameters(singleFileSelectionProvider.get());
            }
        }

        private static SourceParameter
            toSourceParameters(final MultiFileSelection<DefaultFileChooserFilters> fileSelection) {
            return new SourceParameter() {

                @Override
                public boolean isEmpty() {
                    return isEmptyLocalPath(fileSelection.getFSLocation());
                }

                @Override
                public boolean isConnected() {
                    return isConnectedPath(fileSelection.getFSLocation());
                }

                @Override
                public AbstractFileChooserPathAccessor getPathAccessor(final Optional<FSConnection> fsConnection) {
                    return new FileChooserPathAccessor(fileSelection, fsConnection);
                }
            };
        }

        private static SourceParameter toSourceParameters(final FileSelection fileSelection) {
            return new SourceParameter() {

                @Override
                public boolean isEmpty() {
                    return isEmptyLocalPath(fileSelection.getFSLocation());
                }

                @Override
                public boolean isConnected() {
                    return isConnectedPath(fileSelection.getFSLocation());
                }

                @Override
                public AbstractFileChooserPathAccessor getPathAccessor(final Optional<FSConnection> fsConnection) {
                    return new FileChooserPathAccessor(fileSelection, fsConnection);
                }
            };
        }

        private static boolean isEmptyLocalPath(final FSLocation location) {
            return location.getPath().isEmpty() && location.getFSCategory() == FSCategory.LOCAL;
        }

        private static boolean isConnectedPath(final FSLocation location) {
            return location.getFSCategory() == FSCategory.CONNECTED;
        }

        @Override
        public final Map<FSLocation, TypedReaderTableSpec<T>> computeState(final NodeParametersInput context)
            throws StateComputationFailureException {
            final var fileSelection = m_fileSelectionSupplier.get();
            if (!WorkflowContextUtil.hasWorkflowContext() // no workflow context available
                // no file selected (yet)
                || fileSelection.isEmpty()) {
                return null; // NOSONAR we deliberately return null here to indicate that no computation is possible
            }

            var fsConnection = FileSystemPortConnectionUtil.getFileSystemConnection(context);
            if (fileSelection.isConnected() && fsConnection.isEmpty()) {
                return null; // NOSONAR we deliberately return null here to indicate that no computation is possible
            }

            try (final var accessor = fileSelection.getPathAccessor(fsConnection)) {
                return computeStateFromPaths(accessor.getFSPaths(s -> {
                    switch (s.getType()) {
                        case INFO -> LOGGER.info(s.getMessage());
                        case WARNING -> LOGGER.info(s.getMessage());
                        case ERROR -> LOGGER.error(s.getMessage());
                    }
                }));
            } catch (IOException | InvalidSettingsException e) {
                LOGGER.error(e);
                return null; // NOSONAR we deliberately return null here to indicate that no computation is possible
            }
        }

        /**
         * Overwrite this method and use suppliers constructed via {@link StateProviderInitializer#getValueSupplier} in
         * {@link #init(StateProviderInitializer)} to apply parameters to the given config. All parameters that are
         * necessary to read the table specs must be applied here. Note that it might be best to use references to
         * widget groups here in contrast to listing the individual triggers.
         *
         * @param config the multi-table read config to apply parameters to
         */
        protected abstract void applyParametersToConfig(M config);

        private Map<FSLocation, TypedReaderTableSpec<T>> computeStateFromPaths(final List<FSPath> paths)
            throws StateComputationFailureException {
            final M config = createMultiTableReadConfig();
            try {
                applyParametersToConfig(config);
            } catch (IllegalArgumentException e) {
                LOGGER.error(e);
                throw new StateComputationFailureException();
            }

            final var tableReader = createTableReader();
            final var exec = new ExecutionMonitor();
            final var specs = new HashMap<FSLocation, TypedReaderTableSpec<T>>();
            for (var path : paths) {
                try {
                    specs.put(path.toFSLocation(), MultiTableUtils
                        .assignNamesIfMissing(tableReader.readSpec(path, config.getTableReadConfig(), exec)));
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
             * @return the class of the reader specific implementation of {@link TypedReaderTableSpecsProvider}
             */
            <C extends ReaderSpecificConfig<C>, //
                    S extends AbstractMultiTableReadConfig<C, DefaultTableReadConfig<C>, T, S>>
                        Class<? extends TypedReaderTableSpecsProvider<C, T, S>> getTypedReaderTableSpecsProvider();

            /**
             * List all triggers here (using initializer.computeOnValueChange(...)) that should lead to a recomputation
             * of the table specs. I.e. the fields referenced here should be a subset of the dependencies of
             * {@link TypedReaderTableSpecsProvider#applyParametersToConfig(AbstractMultiTableReadConfig)}.
             *
             * @param initializer the initializer
             */
            void initConfigIdTriggers(final StateProviderInitializer initializer);
        }
    }

    abstract static class DependsOnTypedReaderTableSpecProvider<P, T>
        implements StateProvider<P>, TypedReaderTableSpecsProvider.Dependent<T>, ExternalDataTypeSerializer<T> {

        /**
         * Set this to false if the implementation is for single file selection only.
         *
         * @return true if multi file selection is supported
         */
        protected boolean isMultiFileSelection() {
            return true;
        }

        protected Supplier<Map<FSLocation, TypedReaderTableSpec<T>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initTriggers(initializer);
            m_specSupplier = initializer.computeFromProvidedState(getTypedReaderTableSpecsProvider());
        }

        private void initTriggers(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            initConfigIdTriggers(initializer);

            if (isMultiFileSelection()) {
                initializer.computeOnValueChange(MultiFileSelectionParameters.FileSelectionRef.class);
            } else {
                initializer.computeOnValueChange(SingleFileSelectionParameters.FileSelectionRef.class);
            }
        }
    }

    /**
     *
     * Transforms the extracted {@link TypedReaderTableSpec}s into a serializable form.
     *
     * @param <T> the type used to represent external data [T]ypes
     */
    public abstract static class TableSpecSettingsProvider<T>
        extends DependsOnTypedReaderTableSpecProvider<TableSpecSettings[], T> {

        @Override
        public TableSpecSettings[] computeState(final NodeParametersInput context) {

            final var suppliedSpecs = m_specSupplier.get();

            if (suppliedSpecs == null) {
                return null; // NOSONAR we deliberately return null here to indicate that no computation is possible
            }

            return suppliedSpecs.entrySet().stream()
                .map(e -> new TableSpecSettings(e.getKey(),
                    e.getValue().stream()
                        .map(spec -> new ColumnSpecSettings(spec.getName().get(), toSerializableType(spec.getType())))
                        .toArray(ColumnSpecSettings[]::new)))
                .toArray(TableSpecSettings[]::new);
        }
    }

    /**
     * This value provider is used to change the displayed array layout elements in the transformation settings dialog.
     *
     * @param <T> the type used to represent external data [T]ypes
     */
    public abstract static class TransformationElementSettingsProvider<T>
        extends DependsOnTypedReaderTableSpecProvider<TransformationElementSettings[], T>
        implements ProductionPathProviderAndTypeHierarchy<T> {

        private Supplier<MultiFileReaderParameters.HowToCombineColumnsOption> m_howToCombineColumnsSup;

        private Supplier<TransformationElementSettings[]> m_existingSettings;

        private Supplier<TableSpecSettings[]> m_existingSpecs;

        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            if (hasMultipleFileHandling()) {
                m_howToCombineColumnsSup = initializer.computeFromValueSupplier(HowToCombineColumnsOptionRef.class);
            } else {
                m_howToCombineColumnsSup = () -> HowToCombineColumnsOption.FAIL;
            }
            m_existingSettings = initializer.getValueSupplier(TransformationElementSettingsRef.class);
            m_existingSpecs = initializer.getValueSupplier(TableSpecSettingsRef.class);
        }

        /**
         * @return true if the node supports handling multiple files
         */
        protected abstract boolean hasMultipleFileHandling();

        @Override
        public TransformationElementSettings[] computeState(final NodeParametersInput context) {

            final var suppliedSpecs = m_specSupplier.get();

            return toTransformationElements(suppliedSpecs == null ? Map.of() : suppliedSpecs,
                m_howToCombineColumnsSup.get(), m_existingSettings.get(), getExistingSpecsUnion());
        }

        TransformationElementSettings[] toTransformationElements(final Map<FSLocation, TypedReaderTableSpec<T>> specs,
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

        private Supplier<Map<FSLocation, TypedReaderTableSpec<T>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_columnNameSupplier = initializer.getValueSupplier(ColumnNameRef.class);
            initializer.computeOnValueChange(TransformationParameters.TableSpecSettingsRef.class);
            m_specSupplier = initializer.computeFromProvidedState(getTypedReaderTableSpecsProvider());
        }

        @Override
        public List<StringChoice> computeState(final NodeParametersInput context) {
            final var columnName = m_columnNameSupplier.get();

            if (columnName == null) { // i.e., any unknown column
                final var defaultChoice = new StringChoice(DEFAULT_COLUMNTYPE_ID, DEFAULT_COLUMNTYPE_TEXT);
                final var dataTypeChoices = getProductionPathProvider().getAvailableDataTypes().stream()
                    .sorted((t1, t2) -> t1.toPrettyString().compareTo(t2.toPrettyString()))
                    .map(type -> new StringChoice(getDataTypeId(type), type.toPrettyString())).toList();
                return Stream.concat(Stream.of(defaultChoice), dataTypeChoices.stream()).toList();
            }

            final var suppliedSpecs = m_specSupplier.get();

            final var union = toRawSpec(suppliedSpecs == null ? Map.of() : suppliedSpecs).getUnion();
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
        return DataTypeSerializer.typeToString(type);
    }

    static DataType fromDataTypeId(final String id) {
        return DataTypeSerializer.stringToType(id);
    }

    /**
     * Put a reader specific implementation of this class in a {@link Modification} annotation on the reader specific
     * implementation of {@link TransformationParameters}. This ensures, that the common fields within these settings
     * are updated as expected.
     *
     * For filling the abstract methods within this class, some abstract state providers need to be implemented. But
     * none of the to be overwritten methods of these state providers should be implemented directly, since they stem
     * from interfaces which are used for multiple state providers. Instead, extend these interfaces (in
     * {@link ReaderSpecific}) by ones that overwrite all methods with default implementations and use those in the
     * respective places.
     *
     * @param <T> the type used to represent external data [T]ypes
     */
    public abstract static class TransformationSettingsWidgetModification<T> implements Modification.Modifier {

        static class ConfigIdSettingsRef implements Modification.Reference {
        }

        static class SpecsRef implements Modification.Reference {
        }

        static class TypeChoicesWidgetRef implements Modification.Reference {
        }

        static final class TransformationElementSettingsArrayWidgetRef implements Modification.Reference {
        }

        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            group.find(SpecsRef.class).addAnnotation(ValueProvider.class).withValue(getSpecsValueProvider()).modify();
            group.find(TypeChoicesWidgetRef.class).addAnnotation(ChoicesProvider.class)
                .withValue(getTypeChoicesProvider()).modify();
            group.find(TransformationElementSettingsArrayWidgetRef.class).addAnnotation(ValueProvider.class)
                .withValue(getTransformationSettingsValueProvider()).modify();
        }

        /**
         * @return the value provider for the specs.
         */
        protected abstract Class<? extends TableSpecSettingsProvider<T>> getSpecsValueProvider();

        /**
         * @return the value provider for the transformation settings array layout.
         */
        protected abstract Class<? extends TransformationElementSettingsProvider<T>>
            getTransformationSettingsValueProvider();

        /**
         * @return the choices provider for the types of array layout elements.
         */
        protected abstract Class<? extends TypeChoicesProvider<T>> getTypeChoicesProvider();

    }

    private TransformationParametersStateProviders() {
        // Utility class
    }
}
