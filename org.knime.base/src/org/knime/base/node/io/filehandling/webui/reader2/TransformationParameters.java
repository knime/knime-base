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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.ConfigIdSettings;
import org.knime.base.node.io.filehandling.webui.reader2.IfSchemaChangesParameters.UseNewSchema;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileReaderParameters.HowToCombineColumnsOption;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderLayout.MultipleFileHandling;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderSpecific.ExternalDataTypeParseException;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderSpecific.ExternalDataTypeSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProviders.TransformationSettingsWidgetModification;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProviders.TypeChoicesProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.setting.datatype.convert.ProductionPathUtils;
import org.knime.core.webui.node.dialog.defaultdialog.setting.datatype.convert.ProductionPathUtils.ProductionPathPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.node.table.reader.DefaultTableTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDLoader;
import org.knime.filehandling.core.node.table.reader.config.tablespec.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.NodeSettingsConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigSerializer;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ImmutableUnknownColumnsTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.UnknownColumnsTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import static org.knime.base.node.io.filehandling.webui.reader2.IfSchemaChangesParameters.IfSchemaChangesOption.USE_NEW_SCHEMA;

/**
 * Extend these settings to define the transformation settings for a reader node. Use the {@link ConfigIdSettings}
 * generic to define dependencies of reader specific settings to how the data is read. To enable dialog updates, use
 * {@link Modification} for a suitable implementation of {@link TransformationSettingsWidgetModification} on the
 * implementing class.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @param <T> the type used to represent external data [T]ypes
 * @since 5.1
 * @noreference non-public API
 * @noextend non-public API
 */
@SuppressWarnings("restriction")
public abstract class TransformationParameters<T>
    implements NodeParameters, ReaderSpecific.ProductionPathProviderAndTypeHierarchy<T>, ExternalDataTypeSerializer<T> {

    /**
     * For adjusting inclusion, naming and types of the read columns and any unknown columns.
     */
    @Section(title = "Table Transformation", description = Transformation.DESCRIPTION)
    @Advanced
    @Effect(predicate = UseNewSchema.class, type = EffectType.HIDE)
    @After(MultipleFileHandling.class)
    interface Transformation {
        String DESCRIPTION =
            """
                    Use this option to modify the structure of the table. You can deselect each column to filter it out of the
                    output table, use the arrows to reorder the columns, or change the column name or column type of each
                    column. Note that the positions of columns are reset in the dialog if a new file or folder is selected.
                    Whether and where to add unknown columns during execution is specified via the special row &lt;any unknown
                    new column&gt;. It is also possible to select the type new columns should be converted to. Note that the
                    node will fail if this conversion is not possible e.g. if the selected type is Integer but the new column is
                    of type Double.
                    """;

        /**
         * Enforce type mappings as configured in the dialog.
         */
        interface EnforceTypes {
        }

        /**
         * The actual per-column transformations.
         */
        @After(EnforceTypes.class)
        interface ColumnTransformation {
        }
    }

    /**
     * The serializable and persistable equivalent of the {@link TypedReaderColumnSpec}. Visible for testing classes in
     * org.knime.base.node.io.filehandling.webui.testing
     */
    public static final class ColumnSpecSettings implements NodeParameters {

        /**
         * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        public String m_name;

        /**
         * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        public String m_type;

        boolean m_hasType;

        /**
         * Visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        public ColumnSpecSettings(final String name, final String type, final boolean hasType) {
            m_name = name;
            m_type = type;
            m_hasType = hasType;
        }

        ColumnSpecSettings() {
        }
    }

    /**
     * The serializable and persistable equivalent of the {@link TypedReaderTableSpec}
     */
    public static final class TableSpecSettings implements NodeParameters {

        /**
         * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        public FSLocation m_fsLocation;

        /**
         * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        public String m_sourceIdentifier;

        /**
         * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        public ColumnSpecSettings[] m_spec;

        /**
         * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        public TableSpecSettings(final String sourceIdentifier, final FSLocation fsLocation,
            final ColumnSpecSettings[] spec) {
            m_sourceIdentifier = sourceIdentifier;
            m_fsLocation = fsLocation;
            m_spec = spec;
        }

        TableSpecSettings() {
        }
    }

    static class TableSpecSettingsRef implements ParameterReference<TableSpecSettings[]> {
    }

    @ValueReference(TableSpecSettingsRef.class)
    // for adding dynamic and provider
    @Modification.WidgetReference(TransformationSettingsWidgetModification.SpecsRef.class)

    /*
     * Note that this field remains `null` until it is updated by the state provider when specs could be computed.
     * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
     */
    public TableSpecSettings[] m_specs;

    @Widget(title = "Enforce types", description = """
            Controls how columns whose type changes are dealt with.
            If selected, the mapping to the KNIME type you configured is attempted.
            The node will fail if that is not possible.
            If unselected, the KNIME type corresponding to the new type is used.
            """)
    @Layout(Transformation.EnforceTypes.class)
    boolean m_enforceTypes = true;

    /**
     * Visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
     */
    public static class TransformationElementSettings implements WidgetGroup, Persistable {

        static class ColumnNameRef implements ParameterReference<String> {
        }

        static final class ColumnNameIsNull implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getString(ColumnNameRef.class).isEqualTo(null);
            }
        }

        /**
         * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        @ValueReference(ColumnNameRef.class)
        @JsonInclude(Include.ALWAYS) // Necessary for the ColumnNameIsNull PredicateProvider to work
        public String m_columnName;

        static class OriginalProductionPathRef implements ParameterReference<String> {
        }

        /**
         * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        @ValueReference(OriginalProductionPathRef.class)
        @Persistor(OriginalPathPersistor.class)
        public String m_originalProductionPath;

        static final class OriginalPathPersistor extends ProductionPathPersistor {

            private static final String CFG_ORIGINAL_PRODUCTION_PATH = "originalProductionPath";

            OriginalPathPersistor() {
                super(CFG_ORIGINAL_PRODUCTION_PATH);
            }

            @Override
            public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
                if (settings.containsKey(CFG_ORIGINAL_PRODUCTION_PATH)) {
                    return super.load(settings);
                }
                return TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID;
            }

            @Override
            public void save(final String param, final NodeSettingsWO settings) {
                if (TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID.equals(param)) {
                    // do not save default value
                    return;
                }
                super.save(param, settings);
            }
        }

        static class OriginalProductionPathLabelRef implements ParameterReference<String> {
        }

        @ValueReference(OriginalProductionPathLabelRef.class)
        String m_originalTypeLabel;

        /**
         * Visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        @Widget(title = "Include in output", description = "") // TODO NOSONAR UIEXT-1901 add description
        @ArrayWidgetInternal.ElementCheckboxWidget
        public boolean m_includeInOutput;

        static final class ColumnNameResetter implements StateProvider<String> {

            private Supplier<String> m_originalColumnNameSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeOnButtonClick(ArrayWidgetInternal.ElementResetButton.class);
                m_originalColumnNameSupplier = initializer.getValueSupplier(ColumnNameRef.class);
            }

            @Override
            public String computeState(final NodeParametersInput context) {
                return m_originalColumnNameSupplier.get();
            }
        }

        static final class TypeResetter implements StateProvider<String> {

            private Supplier<String> m_originalTypeSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeOnButtonClick(ArrayWidgetInternal.ElementResetButton.class);
                m_originalTypeSupplier = initializer.getValueSupplier(OriginalProductionPathRef.class);
            }

            @Override
            public String computeState(final NodeParametersInput context) {
                return m_originalTypeSupplier.get();
            }
        }

        static final class TitleProvider implements StateProvider<String> {

            private Supplier<String> m_originalColumnNameSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeOnValueChange(TableSpecSettingsRef.class);
                m_originalColumnNameSupplier = initializer.getValueSupplier(ColumnNameRef.class);
            }

            @Override
            public String computeState(final NodeParametersInput context) throws StateComputationFailureException {
                final var originalName = m_originalColumnNameSupplier.get();
                return originalName == null ? "Any unknown column" : originalName;
            }
        }

        static final class SubTitleProvider implements StateProvider<String> {

            private Supplier<String> m_originalTypeLabelSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeOnValueChange(TableSpecSettingsRef.class);
                m_originalTypeLabelSupplier = initializer.getValueSupplier(OriginalProductionPathLabelRef.class);
            }

            @Override
            public String computeState(final NodeParametersInput context) {
                return m_originalTypeLabelSupplier.get();
            }
        }

        static final class ElementIsEditedAndColumnNameIsNotNull implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getPredicate(ArrayWidgetInternal.ElementIsEdited.class)
                    .and(i.getPredicate(ColumnNameIsNull.class).negate());
            }
        }

        /**
         * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        @Widget(title = "Column name", description = "")
        @WidgetInternal(hideControlHeader = true)
        @ValueProvider(ColumnNameResetter.class)
        @Effect(predicate = ElementIsEditedAndColumnNameIsNotNull.class, type = EffectType.SHOW)
        @JsonInclude(Include.ALWAYS) // Necessary for comparison against m_columnName
        @TextInputWidget(patternValidation = ColumnNameValidationUtils.ColumnNameValidation.class)
        public String m_columnRename;

        /**
         * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        @Widget(title = "Column type", description = "")
        @WidgetInternal(hideControlHeader = true)
        // for adding dynamic choices
        @Modification.WidgetReference(TransformationSettingsWidgetModification.TypeChoicesWidgetRef.class)
        @ValueProvider(TypeResetter.class)
        @Effect(predicate = ArrayWidgetInternal.ElementIsEdited.class, type = EffectType.SHOW)
        @Persistor(PathPersistor.class)
        public String m_productionPath;

        static final class PathPersistor extends ProductionPathPersistor {

            private static final String CFG_UNKNOWNS_COLUMN_DATA_TYPE = "unknownsColumnDataType";

            private static final String CFG_PRODUCTION_PATH = "productionPath";

            PathPersistor() {
                super(CFG_PRODUCTION_PATH);
            }

            @Override
            public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
                if (settings.containsKey(CFG_UNKNOWNS_COLUMN_DATA_TYPE)) {
                    return settings.getString(CFG_UNKNOWNS_COLUMN_DATA_TYPE);
                }
                return super.load(settings);
            }

            @Override
            public void save(final String param, final NodeSettingsWO settings) {
                if (ProductionPathUtils.isPathIdentifier(param)) {
                    super.save(param, settings);
                } else {
                    settings.addString(CFG_UNKNOWNS_COLUMN_DATA_TYPE, param);
                }
            }
        }

        TransformationElementSettings() {
        }

        /**
         * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        @SuppressWarnings("javadoc")
        public TransformationElementSettings(final String columnName, final boolean includeInOutput,
            final String columnRename, final String type, final String originalType, final String originalTypeLabel) {
            m_columnName = columnName;
            m_includeInOutput = includeInOutput;
            m_columnRename = columnRename;
            m_productionPath = type; // converter fac id
            m_originalProductionPath = originalType; // converter fac id
            m_originalTypeLabel = originalTypeLabel;
        }

        /**
         * Constructor for concrete columns.
         *
         * @param columnName
         * @param includeInOutput
         * @param columnRename
         * @param productionPath
         * @param defaultProductionPath
         */
        TransformationElementSettings(final String columnName, final boolean includeInOutput, final String columnRename,
            final ProductionPath productionPath, final ProductionPath defaultProductionPath) {
            this(columnName, includeInOutput, columnRename, //
                ProductionPathUtils.getPathIdentifier(productionPath), //
                ProductionPathUtils.getPathIdentifier(defaultProductionPath), //
                defaultProductionPath.getDestinationType().toPrettyString() //
            );
        }

        /**
         * Constructor for unknown columns.
         *
         * @param includeInOutput
         * @param dataType Override data type for unknown columns. Leave null for using the default type.
         */
        TransformationElementSettings(final boolean includeInOutput, final DataType dataType) {
            this(null, includeInOutput, null,
                dataType == null ? TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID : getDataTypeId(dataType),
                TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID, //
                TypeChoicesProvider.DEFAULT_COLUMNTYPE_TEXT //
            );
        }

        /**
         * visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
         */
        public static TransformationElementSettings createUnknownElement() {
            return new TransformationElementSettings(true, null);
        }
    }

    static final class TransformationElementSettingsRef implements ParameterReference<TransformationElementSettings[]> {
    }

    @Widget(title = "Transformations", description = Transformation.DESCRIPTION)
    @ArrayWidget(elementTitle = "Column", showSortButtons = true, hasFixedSize = true)
    @ArrayWidgetInternal(withEditAndReset = true, withElementCheckboxes = true,
        titleProvider = TransformationElementSettings.TitleProvider.class,
        subTitleProvider = TransformationElementSettings.SubTitleProvider.class)
    @ValueReference(TransformationElementSettingsRef.class)
    // for adding dynamic choices
    @Modification.WidgetReference(TransformationSettingsWidgetModification.//
            TransformationElementSettingsArrayWidgetRef.class)
    @Effect(predicate = FileSystemPortConnectionUtil.ConnectedWithoutFileSystemSpec.class, type = EffectType.HIDE)
    @Layout(Transformation.ColumnTransformation.class)
    // visible for testing classes in org.knime.base.node.io.filehandling.webui.testing
    public TransformationElementSettings[] m_columnTransformation =
        new TransformationElementSettings[]{TransformationElementSettings.createUnknownElement()};

    /**
     * Call this method in a reader node's ConfigAndSourceSerializer to save the transformation settings to the config
     * used in the model.
     *
     * @param <C> the reader specific config type
     * @param config the config to save the transformation settings to.
     * @param sourcePath the path of the currently selected source.
     * @param configId the config ID to use for the table spec config
     * @param multiFileReaderParameters for determining how to combine columns and whether a path column is appended.
     * @param ifSchemaChangesParameters for determining whether to save the transformations at all
     */
    public <C extends ReaderSpecificConfig<C>> void saveToConfig(final MultiTableReadConfig<C, T> config,
        final String sourcePath, //
        final ConfigID configId, //
        final MultiFileReaderParameters multiFileReaderParameters, //
        final IfSchemaChangesParameters ifSchemaChangesParameters //
    ) {
        saveToConfig(config, sourcePath, configId, multiFileReaderParameters, ifSchemaChangesParameters, false);
    }

    /**
     * Call this method in a reader node's ConfigAndSourceSerializer to save the transformation settings to the config
     * used in the model.
     *
     * @param <C> the reader specific config type
     * @param config the config to save the transformation settings to.
     * @param sourcePath the path of the currently selected source.
     * @param configId the config ID to use for the table spec config
     * @param multiFileReaderParameters for determining how to combine columns and whether a path column is appended.
     * @param ifSchemaChangesParameters for determining whether to save the transformations at all
     * @param skipEmptyColumns whether to skip empty columns when determining the transformations
     */
    public <C extends ReaderSpecificConfig<C>> void saveToConfig(final MultiTableReadConfig<C, T> config,
        final String sourcePath, //
        final ConfigID configId, //
        final MultiFileReaderParameters multiFileReaderParameters, //
        final IfSchemaChangesParameters ifSchemaChangesParameters, //
        final boolean skipEmptyColumns //
    ) {

        if (m_specs == null) {
            return;
        }

        if (ifSchemaChangesParameters.m_ifSchemaChangesOption == USE_NEW_SCHEMA) {
            // the old readers did not save the spec at all if that option was set, and the old reader code still
            // implicitly assumes that (i.e. setting the USE_NEW_SCHEMA is not sufficient to actually use the new schema
            // if the old schema is still present)
            return;
        }

        final var individualSpecs = toSpecMap(this, m_specs);
        final var rawSpec = toRawSpec(individualSpecs.values());
        final var transformations = determineTransformations(rawSpec, multiFileReaderParameters.m_howToCombineColumns);
        final var tableTransformation = new DefaultTableTransformation<T>(rawSpec, transformations.getFirst(),
            multiFileReaderParameters.m_howToCombineColumns.toColumnFilterMode(), transformations.getSecond(),
            m_enforceTypes, skipEmptyColumns);

        final var appendPathColumnOpt = multiFileReaderParameters.m_appendPathColumn;
        DataColumnSpec itemIdentifierColumnSpec =
            appendPathColumnOpt.isPresent() ? determineAppendPathColumnSpec(appendPathColumnOpt.get()) : null;

        final var tableSpecConfig = DefaultTableSpecConfig.createFromTransformationModel(sourcePath, configId,
            individualSpecs, tableTransformation, itemIdentifierColumnSpec);

        config.setTableSpecConfig(tableSpecConfig);

    }

    private Pair<ArrayList<ColumnTransformation<T>>, UnknownColumnsTransformation>

        determineTransformations(final RawSpec<T> rawSpec, final HowToCombineColumnsOption howToCombineColumns) {
        final Map<String, Integer> transformationIndexByColumnName = new HashMap<>();
        int unknownColumnsTransformationPosition = -1;
        for (int i = 0; i < m_columnTransformation.length; i++) {
            final var columnName = m_columnTransformation[i].m_columnName;
            if (columnName == null) {
                unknownColumnsTransformationPosition = i;
            } else {
                transformationIndexByColumnName.put(columnName, i);
            }
        }
        final var transformations = new ArrayList<ColumnTransformation<T>>();
        for (var column : howToCombineColumns.toColumnFilterMode().getRelevantSpec(rawSpec)) {

            // in the TypedReaderTableSpecsProvider we make sure that names are always present
            final var columnName = column.getName().orElseThrow(IllegalStateException::new);
            final var position = transformationIndexByColumnName.get(columnName);
            final var transformation = m_columnTransformation[position];
            ProductionPath productionPath;
            try {
                productionPath =
                    ProductionPathUtils.fromPathIdentifier(transformation.m_productionPath, getProducerRegistry()); // validate path id
            } catch (InvalidSettingsException e) {
                LOGGER.error(String.format(
                    "The column '%s' can't be converted to the configured data type. Unknown production path: %s",
                    column, transformation.m_productionPath), e);
                productionPath = getProductionPathProvider().getDefaultProductionPath(column.getType());
            }
            transformations.add(new ImmutableColumnTransformation<>(column, productionPath,
                transformation.m_includeInOutput, position, transformation.m_columnRename));
        }
        final var unknownColumnsTransformation = determineUnknownTransformation(
            m_columnTransformation[unknownColumnsTransformationPosition], unknownColumnsTransformationPosition);
        return new Pair<>(transformations, unknownColumnsTransformation);
    }

    private static ImmutableUnknownColumnsTransformation determineUnknownTransformation(
        final TransformationElementSettings unknownTransformation, final int unknownColumnsTransformationPosition) {
        DataType forcedUnknownType = null;
        if (!unknownTransformation.m_productionPath.equals(TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID)) {
            forcedUnknownType =
                TransformationParametersStateProviders.fromDataTypeId(unknownTransformation.m_productionPath);
        }
        return new ImmutableUnknownColumnsTransformation(unknownColumnsTransformationPosition,
            unknownTransformation.m_includeInOutput, forcedUnknownType != null, forcedUnknownType);
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TransformationParameters.class);

    private DataColumnSpec determineAppendPathColumnSpec(final String appendPathColumn) {
        // code taken from DefaultMultiTableReadFactory.createItemIdentifierColumn
        DataColumnSpecCreator itemIdColCreator = null;
        for (var spec : m_specs) {
            final var path = spec.m_fsLocation;
            // code taken from TableReader.createIdentifierColumnSpec
            final DataColumnSpecCreator creator =
                new DataColumnSpecCreator(appendPathColumn, SimpleFSLocationCellFactory.TYPE);
            creator.addMetaData(
                new FSLocationValueMetaData(path.getFileSystemCategory(), path.getFileSystemSpecifier().orElse(null)),
                true);
            final var itemIdCol = creator.createSpec();
            if (itemIdColCreator == null) {
                itemIdColCreator = new DataColumnSpecCreator(itemIdCol);
            } else {
                itemIdColCreator.merge(itemIdCol);
            }
        }
        if (itemIdColCreator != null) {
            return itemIdColCreator.createSpec();
        }
        return null;
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_specs != null) {
            validateSpecs();
        }
        validateColumnTransformations();
    }

    private void validateSpecs() throws InvalidSettingsException {
        for (TableSpecSettings tSpec : m_specs) {
            for (ColumnSpecSettings cSpec : tSpec.m_spec) {
                try {
                    toExternalType(cSpec.m_type);
                } catch (ExternalDataTypeParseException e) {
                    throw new InvalidSettingsException(
                        String.format("The type '%s' for column '%s' is invalid.", cSpec.m_type, cSpec.m_name), e);
                }
            }
        }
    }

    private void validateColumnTransformations() throws InvalidSettingsException {
        for (TransformationElementSettings elem : m_columnTransformation) {
            if (elem.m_columnRename != null && !elem.m_columnRename.equals(elem.m_columnName)) {
                ColumnNameValidationUtils.validateColumnName(elem.m_columnRename, state -> { // NOSONAR complexity is OK
                    switch (state) {
                        case EMPTY:
                            return String.format("The new name for column '%s' is empty.", elem.m_columnName);
                        case BLANK:
                            return String.format("The new name for column '%s' is blank.", elem.m_columnName);
                        case NOT_TRIMMED:
                            return String.format(
                                "The new name for column '%s' starts or ends with whitespace characters.",
                                elem.m_columnName);
                        default:
                            throw new IllegalStateException("Unknown InvalidColumnNameState: " + state);
                    }
                });
            }
        }
    }

    static final String ROOT_CFG_KEY = "table_spec_config_Internals";

    /**
     * The factory method to create the table spec config serializer.
     *
     * @param configIdLoader the config ID loader
     * @return the table spec config serializer
     */
    protected abstract TableSpecConfigSerializer<T> createTableSpecConfigSerializer(ConfigIDLoader configIdLoader);

    /**
     * The key used in the settings to store all settigns within the config ID.
     *
     * @return the config ID settings key
     */
    protected abstract String getConfigIdSettingsKey();

    private TableSpecConfigSerializer<T> createTableSpecConfigSerializer() {
        final ConfigIDLoader configIdLoader =
            s -> new NodeSettingsConfigID(s.getNodeSettings(getConfigIdSettingsKey()));
        return createTableSpecConfigSerializer(configIdLoader);
    }

    /**
     * Use this method in a {@link NodeParametersMigration} to load transformation settings from legacy table spec
     *
     * @param settings the settings to load from
     * @throws InvalidSettingsException if loading the settings failed
     */
    public void loadFromLegacySettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(ROOT_CFG_KEY)) {
            final var tableSpecConfigSerializer = createTableSpecConfigSerializer();
            final var tableSpecConfig = tableSpecConfigSerializer.load(settings.getNodeSettings(ROOT_CFG_KEY));
            loadFromTableSpecConfig((DefaultTableSpecConfig<T>)tableSpecConfig);
        }
    }

    /**
     * Use this method in a {@link NodeParametersMigration} to load transformation settings from legacy table spec
     *
     * @param settings the settings to load from
     * @param columnFilterMode the column filter mode to use if stored outside the table spec config (which is the case
     *            for older workflows created with 4.2 and potentially 4.3)
     * @throws InvalidSettingsException if loading the settings failed
     */
    public void loadFromLegacySettings(final NodeSettingsRO settings, final ColumnFilterMode columnFilterMode)
        throws InvalidSettingsException {
        if (settings.containsKey(ROOT_CFG_KEY)) {
            final var tableSpecConfigSerializer = createTableSpecConfigSerializer();

            final var additionalParameters =
                TableSpecConfigSerializer.AdditionalParameters.create().withColumnFilterMode(columnFilterMode);
            final var tableSpecConfig =
                tableSpecConfigSerializer.load(settings.getNodeSettings(ROOT_CFG_KEY), additionalParameters);
            loadFromTableSpecConfig((DefaultTableSpecConfig<T>)tableSpecConfig);
        }
    }

    private void loadFromTableSpecConfig(final DefaultTableSpecConfig<T> tableSpecConfig) {
        try {
            final var fsLocations = tableSpecConfig.getItemIdentifierColumn().map(colSpec -> {
                // read metadata and set first fs location with empty path as placeholder
                return colSpec.getMetaDataOfType(FSLocationValueMetaData.class)
                    .map(FSLocationValueMetaData::getFSLocationSpecs).stream()//
                    .flatMap(Set::stream)//
                    .map(locSpec -> new FSLocation(locSpec.getFileSystemCategory(),
                        locSpec.getFileSystemSpecifier().orElse(null), ""))
                    .toArray(FSLocation[]::new);
            });
            final var items = tableSpecConfig.getItems();
            final var specs = IntStream.range(0, items.size()).mapToObj(i -> {
                final var key = items.get(i);
                // Use a placeholder FSLocation with empty path when there's no item identifier column
                // to avoid NPEs during serialization (null is not supported there as of now)
                final var fsLocation = fsLocations.filter(locs -> i < locs.length).map(locs -> locs[i])
                    .orElseGet(() -> new FSLocation(FSCategory.RELATIVE, null, ""));
                final var spec = tableSpecConfig.getSpec(key);
                final var colSpecs =
                    spec.stream()
                        .map(colSpec -> new ColumnSpecSettings(colSpec.getName().get(),
                            toSerializableType(colSpec.getType()), colSpec.hasType()))
                        .toArray(ColumnSpecSettings[]::new);
                return new TableSpecSettings(key, fsLocation, colSpecs);
            }).toArray(TableSpecSettings[]::new);
            m_specs = specs;
            m_enforceTypes = tableSpecConfig.getTableTransformation().enforceTypes();
        } catch (final Exception e) { // NOSONAR The dialog can still work even if loading persistor settings fails
            LOGGER.error("Error while loading persistor settings from table spec config.", e);
        }

        final var transformationElements =
            tableSpecConfig.getTableTransformation().stream().map(this::getTransformationElement).toList();
        final var unknownTransformationElement =
            getTransformationElement(tableSpecConfig.getTableTransformation().getTransformationForUnknownColumns());
        m_columnTransformation = Stream.concat(transformationElements.stream(), Stream.of(unknownTransformationElement))
            .sorted(new ColumnTransformationComparator(unknownTransformationElement)) //
            .map(Pair::getSecond) //
            .toArray(TransformationElementSettings[]::new);
    }

    private record ColumnTransformationComparator(Pair<Integer, TransformationElementSettings> m_unknownElement)
        implements Comparator<Pair<Integer, TransformationElementSettings>> {

        @Override
        public int compare(final Pair<Integer, TransformationElementSettings> a,
            final Pair<Integer, TransformationElementSettings> b) {
            var aPos = a.getFirst();
            var bPos = b.getFirst();
            if (!Objects.equals(aPos, bPos)) {
                return Integer.compare(aPos, bPos);
            }
            /*
             * Workaround for legacy behaviour: The "unknown columns" placeholder might have the same index as a known
             * column. It should be inserted at its position, shifting all subsequent columns (including the one with
             * the same index) to the right. This comparator implicitly assumes that there is only one
             * "unknown columns" element.
             */
            if (a == m_unknownElement) {
                return -1;
            }
            if (b == m_unknownElement) {
                return 1;
            }
            return 0;
        }
    }

    private Pair<Integer, TransformationElementSettings>
        getTransformationElement(final ColumnTransformation<T> knownTransformation) {
        return new Pair<>(knownTransformation.getPosition(), toTransformationElementSettings(knownTransformation));
    }

    private static Pair<Integer, TransformationElementSettings>
        getTransformationElement(final UnknownColumnsTransformation unknownColumnsTransformation) {
        return new Pair<>(unknownColumnsTransformation.getPosition(),
            toTransformationElementSettings(unknownColumnsTransformation));
    }

    private TransformationElementSettings toTransformationElementSettings(final ColumnTransformation<T> t) {
        final var defaultProductionPath =
            getProductionPathProvider().getDefaultProductionPath(t.getExternalSpec().getType());
        return new TransformationElementSettings( //
            t.getOriginalName(), //
            t.keep(), //
            t.getName(), //
            t.getProductionPath(), //
            defaultProductionPath);
    }

    private static TransformationElementSettings toTransformationElementSettings(final UnknownColumnsTransformation t) {
        return new TransformationElementSettings(t.keep(), getForcedType(t));
    }

    private static DataType getForcedType(final UnknownColumnsTransformation t) {
        if (t.forceType()) {
            return t.getForcedType();
        }
        return null;
    }

    static String getDataTypeId(final DataType type) {
        return DataTypeSerializer.typeToString(type);
    }

}
