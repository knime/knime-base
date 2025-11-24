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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.ConfigIdSettings;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileReaderParameters.HowToCombineColumnsOption;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderSpecific.ExternalDataTypeParseException;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderSpecific.ExternalDataTypeSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProviders.TransformationSettingsWidgetModification;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProviders.TypeChoicesProvider;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.node.table.reader.DefaultTableTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.config.AbstractMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ImmutableUnknownColumnsTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.UnknownColumnsTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.persistence.Persistable;
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

/**
 * Extend these settings to define the transformation settings for a reader node. Use the {@link ConfigIdSettings}
 * generic to define dependencies of reader specific settings to how the data is read. To enable dialog updates, use
 * {@link Modification} for a suitable implementation of {@link TransformationSettingsWidgetModification} on the
 * implementing class.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @param <T> the type used to represent external data [T]ypes
 */
@SuppressWarnings("restriction")
@Layout(ReaderLayout.Transformation.class)
public abstract class TransformationParameters<T>
    implements NodeParameters, ReaderSpecific.ProductionPathProviderAndTypeHierarchy<T>, ExternalDataTypeSerializer<T> {

    /**
     * The serializable and persistable equivalent of the {@link TypedReaderColumnSpec}
     */
    static final class ColumnSpecSettings implements NodeParameters {

        String m_name;

        String m_type;

        ColumnSpecSettings(final String name, final String type) {
            m_name = name;
            m_type = type;
        }

        ColumnSpecSettings() {
        }
    }

    /**
     * The serializable and persistable equivalent of the {@link TypedReaderTableSpec}
     */
    public static final class TableSpecSettings implements NodeParameters {

        FSLocation m_fsLocation;

        ColumnSpecSettings[] m_spec;

        TableSpecSettings(final FSLocation fsLocation, final ColumnSpecSettings[] spec) {
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
    // Note that this field remains `null` until it is updated by the state provider when specs could be computed.
    TableSpecSettings[] m_specs;

    @Widget(title = "Enforce types", description = ReaderLayout.Transformation.EnforceTypes.DESCRIPTION)
    boolean m_enforceTypes = true;

    static class TransformationElementSettings implements WidgetGroup, Persistable {

        static class ColumnNameRef implements ParameterReference<String> {
        }

        static final class ColumnNameIsNull implements EffectPredicateProvider {
            @Override
            public EffectPredicate init(final PredicateInitializer i) {
                return i.getString(ColumnNameRef.class).isEqualTo(null);
            }
        }

        @ValueReference(ColumnNameRef.class)
        @JsonInclude(Include.ALWAYS) // Necessary for the ColumnNameIsNull PredicateProvider to work
        String m_columnName;

        static class OriginalTypeRef implements ParameterReference<String> {
        }

        @ValueReference(OriginalTypeRef.class)
        String m_originalType;

        static class OriginalTypeLabelRef implements ParameterReference<String> {
        }

        @ValueReference(OriginalTypeLabelRef.class)
        String m_originalTypeLabel;

        @Widget(title = "Include in output", description = "") // TODO NOSONAR UIEXT-1901 add description
        @ArrayWidgetInternal.ElementCheckboxWidget
        boolean m_includeInOutput;

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
                m_originalTypeSupplier = initializer.getValueSupplier(OriginalTypeRef.class);
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
            public String computeState(final NodeParametersInput context) {
                final var originalName = m_originalColumnNameSupplier.get();
                return originalName == null ? "Any unknown column" : originalName;
            }
        }

        static final class SubTitleProvider implements StateProvider<String> {

            private Supplier<String> m_originalTypeLabelSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeOnValueChange(TableSpecSettingsRef.class);
                m_originalTypeLabelSupplier = initializer.getValueSupplier(OriginalTypeLabelRef.class);
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

        @Widget(title = "Column name", description = "")
        @WidgetInternal(hideControlHeader = true)
        @ValueProvider(ColumnNameResetter.class)
        @Effect(predicate = ElementIsEditedAndColumnNameIsNotNull.class, type = EffectType.SHOW)
        @JsonInclude(Include.ALWAYS) // Necessary for comparison against m_columnName
        @TextInputWidget(patternValidation = ColumnNameValidationUtils.ColumnNameValidation.class)
        String m_columnRename;

        @Widget(title = "Column type", description = "")
        @WidgetInternal(hideControlHeader = true)
        // for adding dynamic choices
        @Modification.WidgetReference(TransformationSettingsWidgetModification.TypeChoicesWidgetRef.class)
        @ValueProvider(TypeResetter.class)
        @Effect(predicate = ArrayWidgetInternal.ElementIsEdited.class, type = EffectType.SHOW)
        String m_type;

        TransformationElementSettings() {
        }

        TransformationElementSettings(final String columnName, final boolean includeInOutput, final String columnRename,
            final String type, final String originalType, final String originalTypeLabel) {
            m_columnName = columnName;
            m_includeInOutput = includeInOutput;
            m_columnRename = columnRename;
            m_type = type; // converter fac id
            m_originalType = originalType; // converter fac id
            m_originalTypeLabel = originalTypeLabel;
        }

        static TransformationElementSettings createUnknownElement() {
            return new TransformationElementSettings(null, true, null, TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID,
                TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID, TypeChoicesProvider.DEFAULT_COLUMNTYPE_TEXT);
        }
    }

    static final class TransformationElementSettingsRef implements ParameterReference<TransformationElementSettings[]> {
    }

    @Widget(title = "Transformations", description = ReaderLayout.Transformation.DESCRIPTION)
    @ArrayWidget(elementTitle = "Column", showSortButtons = true, hasFixedSize = true)
    @ArrayWidgetInternal(withEditAndReset = true, withElementCheckboxes = true,
        titleProvider = TransformationElementSettings.TitleProvider.class,
        subTitleProvider = TransformationElementSettings.SubTitleProvider.class)
    @ValueReference(TransformationElementSettingsRef.class)
    // for adding dynamic choices
    @Modification.WidgetReference(TransformationSettingsWidgetModification.//
            TransformationElementSettingsArrayWidgetRef.class)
    @Effect(predicate = FileSystemPortConnectionUtil.ConnectedWithoutFileSystemSpec.class, type = EffectType.HIDE)
    TransformationElementSettings[] m_columnTransformation =
        new TransformationElementSettings[]{TransformationElementSettings.createUnknownElement()};

    /**
     * Call this method in a reader node's ConfigAndSourceSerializer to save the transformation settings to the config
     * used in the model.
     *
     * @param <C> the reader specific config type
     * @param config the config to save the transformation settings to.
     * @param sourcePath the path of the currently selected source.
     * @param configId the config ID to use for the table spec config
     * @param howToCombineColumnsOption how to combine columns option (when multi file selection is enabled, this should
     *            be a parameter, otherwise hardcode it to use the union).
     * @param appendPathColumn optional name of the column to append the file path to (when multi file selection is
     *            enabled, this should be a parameter, otherwise use an empty optional.
     */
    public <C extends ReaderSpecificConfig<C>> void saveToConfig(
        final AbstractMultiTableReadConfig<C, ? extends DefaultTableReadConfig<C>, T, ?> config,
        final String sourcePath, //
        final ConfigID configId, //
        final HowToCombineColumnsOption howToCombineColumnsOption, //
        final Optional<String> appendPathColumn//
    ) {

        if (m_specs == null) {
            return;
        }

        final var individualSpecs = toSpecMap(this, m_specs);
        final var rawSpec = toRawSpec(individualSpecs);
        final var transformations = determineTransformations(rawSpec);
        final var tableTransformation = new DefaultTableTransformation<T>(rawSpec, transformations.getFirst(),
            howToCombineColumnsOption.toColumnFilterMode(), transformations.getSecond(), m_enforceTypes, false);

        DataColumnSpec itemIdentifierColumnSpec = determineAppendPathColumnSpec(appendPathColumn);

        final var tableSpecConfig = DefaultTableSpecConfig.createFromTransformationModel(sourcePath,
            config.getConfigID(), individualSpecs, tableTransformation, itemIdentifierColumnSpec);

        config.setTableSpecConfig(tableSpecConfig);

    }

    private Pair<ArrayList<ColumnTransformation<T>>, UnknownColumnsTransformation>
        determineTransformations(final RawSpec<T> rawSpec) {
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
        for (var column : rawSpec.getUnion()) {

            final var columnName = column.getName().get(); // NOSONAR in the TypedReaderTableSpecsProvider we make sure that names are always present
            if (columnName == null) {
                continue;
            }
            final var position = transformationIndexByColumnName.get(columnName);
            final var transformation = m_columnTransformation[position];
            final var externalType = column.getType();
            final var productionPath = getProductionPathProvider().getAvailableProductionPaths(externalType).stream()
                .filter(p -> p.getConverterFactory().getIdentifier().equals(transformation.m_type)).findFirst()
                .orElseGet(() -> {
                    LOGGER.error(
                        String.format("The column '%s' can't be converted to the configured data type.", column));
                    return getProductionPathProvider().getDefaultProductionPath(externalType);
                });

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
        if (!unknownTransformation.m_type.equals(TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID)) {
            forcedUnknownType = TransformationParametersStateProviders.fromDataTypeId(unknownTransformation.m_type);
        }
        return new ImmutableUnknownColumnsTransformation(unknownColumnsTransformationPosition,
            unknownTransformation.m_includeInOutput, forcedUnknownType != null, forcedUnknownType);
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TransformationParameters.class);

    private DataColumnSpec determineAppendPathColumnSpec(final Optional<String> appendPathColumn) {
        // code taken from DefaultMultiTableReadFactory.createItemIdentifierColumn
        if (appendPathColumn.isPresent()) {
            DataColumnSpecCreator itemIdColCreator = null;
            for (var spec : m_specs) {
                final var path = spec.m_fsLocation;
                // code taken from TableReader.createIdentifierColumnSpec
                final DataColumnSpecCreator creator =
                    new DataColumnSpecCreator(appendPathColumn.get(), SimpleFSLocationCellFactory.TYPE);
                creator.addMetaData(new FSLocationValueMetaData(path.getFileSystemCategory(),
                    path.getFileSystemSpecifier().orElse(null)), true);
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
                ColumnNameValidationUtils.validateColumnName(elem.m_columnRename, state -> {
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

}
