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
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.webui.FileChooserPathAccessor;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileReaderParameters.HowToCombineColumnsOption;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileReaderParameters.HowToCombineColumnsOptionRef;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderSpecific.ExternalDataTypeSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderSpecific.ProductionPathProviderAndTypeHierarchy;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.HasTableSpec;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.TableSpecSettingsRef;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.ColumnNameRef;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.TransformationElementSettingsRef;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.TransformationElementsMapper;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.TypeChoicesMapper;
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
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;
import org.knime.filehandling.core.util.WorkflowContextUtil;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.StateProvider;
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
 * @noreference non-public API
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
     * read them to table specs.
     *
     * @param <C> The reader specific [C]onfiguration
     * @param <T> the type used to represent external data [T]ypes
     * @param <M> The [M]ulti-table read configuration
     */
    public abstract static class TypedReaderTableSpecsProvider<C extends ReaderSpecificConfig<C>, T, //
            M extends AbstractMultiTableReadConfig<C, DefaultTableReadConfig<C>, T, M>>
        implements StateProvider<Collection<DependsOnTypedReaderTableSpecProvider.TypedReaderTableSpecWithLocation<T>>>,
        ReaderSpecific.ConfigAndReader<C, T, M> {

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
        public final Collection<DependsOnTypedReaderTableSpecProvider.TypedReaderTableSpecWithLocation<T>>
            computeState(final NodeParametersInput context) throws StateComputationFailureException {
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
                }), context);
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

        private Collection<DependsOnTypedReaderTableSpecProvider.TypedReaderTableSpecWithLocation<T>>
            computeStateFromPaths(final List<FSPath> paths, final NodeParametersInput input)
                throws StateComputationFailureException {
            final M config = createMultiTableReadConfig(input);
            try {
                applyParametersToConfig(config);
            } catch (IllegalArgumentException e) {
                LOGGER.error(e);
                throw new StateComputationFailureException();
            }

            final var tableReader = createTableReader();
            final var exec = new ExecutionMonitor();
            final var specs =
                new ArrayList<DependsOnTypedReaderTableSpecProvider.TypedReaderTableSpecWithLocation<T>>();
            for (var path : paths) {
                try {
                    final var spec = MultiTableUtils
                        .assignNamesIfMissing(tableReader.readSpec(path, config.getTableReadConfig(), exec));
                    specs.add(new DependsOnTypedReaderTableSpecProvider.TypedReaderTableSpecWithLocation<>(
                        path.toFSLocation(), path.toString(), spec));
                } catch (IOException | IllegalArgumentException e) {
                    LOGGER.error(e);
                    return Collections.emptyList();
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

        record TypedReaderTableSpecWithLocation<T>(FSLocation location, String sourceId, TypedReaderTableSpec<T> spec)
            implements HasTableSpec<T> {
            @Override
            public TypedReaderTableSpec<T> getTableSpec() {
                return spec;
            }
        }

        protected Supplier<Collection<TypedReaderTableSpecWithLocation<T>>> m_specSupplier;

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
        extends DependsOnTypedReaderTableSpecProvider<TableSpecSettingsWithFsLocation[], T> {

        @Override
        public TableSpecSettingsWithFsLocation[] computeState(final NodeParametersInput context) {

            final var suppliedSpecs = m_specSupplier.get();

            if (suppliedSpecs == null) {
                return null; // NOSONAR we deliberately return null here to indicate that no computation is possible
            }

            return suppliedSpecs.stream()
                .map(e -> new TableSpecSettingsWithFsLocation(e.sourceId(), e.location(),
                    e.spec().stream()
                        .map(spec -> new ColumnSpecSettings(spec.getName().get(), toSerializableType(spec.getType()),
                            spec.hasType()))
                        .toArray(ColumnSpecSettings[]::new)))
                .toArray(TableSpecSettingsWithFsLocation[]::new);
        }
    }

    /**
     * This value provider is used to change the displayed array layout elements in the transformation settings dialog.
     *
     * @param <T> the type used to represent external data [T]ypes
     */
    public abstract static class TransformationElementSettingsProvider<T>
        extends DependsOnTypedReaderTableSpecProvider<TransformationElementSettings[], T>
        implements TransformationElementsMapper<T>, ProductionPathProviderAndTypeHierarchy<T> {

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
            return toTransformationElements(suppliedSpecs == null ? List.of() : suppliedSpecs,
                m_howToCombineColumnsSup.get(), m_existingSettings.get(),
                getExistingSpecsUnion(this, m_existingSpecs.get()));
        }
    }

    /**
     * This state provider determines the possible values in the individual type choices of elements within the
     * transformation settings array layout.
     *
     * @param <T> the type used to represent external data [T]ypes
     */
    public abstract static class TypeChoicesProvider<T> implements StringChoicesProvider,
        ProductionPathProviderAndTypeHierarchy<T>, TypedReaderTableSpecsProvider.Dependent<T>, TypeChoicesMapper<T> {

        private Supplier<String> m_columnNameSupplier;

        private Supplier<? extends Collection<? extends HasTableSpec<T>>> m_specSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_columnNameSupplier = initializer.getValueSupplier(ColumnNameRef.class);
            initializer.computeOnValueChange(TableSpecSettingsRef.class);
            m_specSupplier = initializer.computeFromProvidedState(getTypedReaderTableSpecsProvider());
        }

        @Override
        public List<StringChoice> computeState(final NodeParametersInput context) {
            final var columnName = m_columnNameSupplier.get();
            final var suppliedSpecs = m_specSupplier.get();
            final List<TypedReaderTableSpec<T>> specsOnly =
                suppliedSpecs == null ? List.of() : suppliedSpecs.stream().map(HasTableSpec::getTableSpec).toList();
            return computeTypeChoices(specsOnly, columnName);
        }

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
    public abstract static class TransformationSettingsWidgetModification<T> extends
        TransformationParametersStateProvidersCommon.AbstractTransformationSettingsWidgetModification<TableSpecSettingsWithFsLocation> {

        /**
         * @return the value provider for the specs.
         */
        protected abstract Class<? extends TransformationParametersStateProviders.TableSpecSettingsProvider<T>>
            getSpecsValueProvider();

        /**
         * @return the value provider for the transformation settings array layout.
         */
        protected abstract
            Class<? extends TransformationParametersStateProviders.TransformationElementSettingsProvider<T>>
            getTransformationSettingsValueProvider();

        /**
         * @return the choices provider for the types of array layout elements.
         */
        protected abstract Class<? extends TransformationParametersStateProviders.TypeChoicesProvider<T>>
            getTypeChoicesProvider();
    }

    private TransformationParametersStateProviders() {
        // Utility class
    }
}
