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
package org.knime.base.node.io.filehandling.csv.reader2.common;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.csv.reader2.common.CommonReaderTransformationParameters.ConfigIdSettings;
import org.knime.base.node.io.filehandling.csv.reader2.common.CommonReaderTransformationParametersStateProviders.FSLocationsProvider;
import org.knime.base.node.io.filehandling.csv.reader2.common.CommonReaderTransformationParametersStateProviders.SourceIdProvider;
import org.knime.base.node.io.filehandling.csv.reader2.common.CommonReaderTransformationParametersStateProviders.TransformationSettingsWidgetModification;
import org.knime.base.node.io.filehandling.csv.reader2.common.CommonReaderTransformationParametersStateProviders.TypeChoicesProvider;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.WidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
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

/**
 * Extend these settings to define the transformation settings for a reader node. Use the {@link ConfigIdSettings}
 * generic to define dependencies of reader specific settings to how the data is read. To enable dialog updates, use
 * {@link Modification} for a suitable implementation of {@link TransformationSettingsWidgetModification} on the
 * implementing class.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @param <I> The config [I]d settings used within the transformation settings
 * @param <S> the type used to [S]erialize external data types
 */
@SuppressWarnings("restriction")
@Layout(CommonReaderLayout.Transformation.class)
public abstract class CommonReaderTransformationParameters<I extends ConfigIdSettings<?>, S>
    implements Persistable, WidgetGroup {

    /**
     * @param configId the initial value of the config id. It can be independent from the respective values in the
     *            settings and just serves as a starting point.
     */
    protected CommonReaderTransformationParameters(final I configId) {
        m_persistorSettings = new PersistorSettings<>(configId);
    }

    CommonReaderTransformationParameters() {
        // Default constructor required as per {@link PersistablSettings} contract
    }

    /**
     * This class needs to be implemented if there exist reader specific settings that need to be applied to the reader
     * specific config before reading data.
     *
     * @param <C> the reader specific config.
     */
    public static class ConfigIdSettings<C extends ReaderSpecificConfig<C>> implements WidgetGroup, Persistable {
        /**
         * @param tableReadConfig to apply settings to
         */
        protected void applyToConfig(final DefaultTableReadConfig<C> tableReadConfig) { // TODO get rid of this - parameters are applied to settings in e.g., CSVTableReaderNodeParameters#saveToConfig
            // Do nothing per default
        }
    }

    /**
     * The serializable equivalent of the {@link TypedReaderColumnSpec}
     *
     * @param <S> the serializable type for external data
     */
    static final class ColumnSpecSettings<S> implements WidgetGroup, Persistable {

        String m_name;

        S m_type;

        ColumnSpecSettings(final String name, final S type) {
            m_name = name;
            m_type = type;
        }

        ColumnSpecSettings() {
        }
    }

    /**
     * The serializable equivalent of the {@link TypedReaderTableSpec}
     *
     * @param <S> the serializable type for external data
     */
    public static final class TableSpecSettings<S> implements WidgetGroup, Persistable {

        String m_sourceId;

        List<ColumnSpecSettings<S>> m_spec;

        TableSpecSettings(final String sourceId, final List<ColumnSpecSettings<S>> spec) {
            m_sourceId = sourceId;
            m_spec = spec;
        }

        TableSpecSettings() {
        }
    }

    static class TableSpecSettingsRef implements ParameterReference<List<TableSpecSettings<?>>> {
    }

    static class ConfigIdRef implements ParameterReference<ConfigIdSettings<?>> {
    }

    /**
     * TODO NOSONAR UIEXT-1946 These settings are sent to the frontend where they are not needed. They are merely held
     * here to be used in the {@link CommonReaderTransformationParametersPersistor} and the
     * {@link CommonReaderTransformationParametersStateProviders}. We should look for an alternative mechanism to
     * provide these settings to the persistor. This would then also allow us to use non-serializable types like the
     * TypedReaderTableSpec instead of the TableSpecSettings, saving us the back-and-forth conversion.
     */
    static class PersistorSettings<I extends ConfigIdSettings<?>, S> implements WidgetGroup, Persistable {

        private PersistorSettings(final I configId) {
            CheckUtils.checkArgumentNotNull(configId);
            m_configId = configId;
        }

        PersistorSettings() {
            // Default constructor required as per {@link PersistablSettings} contract
        }

        @ValueReference(ConfigIdRef.class)
        I m_configId;

        @ValueProvider(SourceIdProvider.class)
        // for replacing it with an own provider if the file is accessed indirectly
        @Modification.WidgetReference(TransformationSettingsWidgetModification.SourceIdRef.class)
        String m_sourceId = "";

        @ValueProvider(FSLocationsProvider.class)
        // for replacing it with an own provider if the file is accessed indirectly
        @Modification.WidgetReference(TransformationSettingsWidgetModification.FSLocationsRef.class)
        FSLocation[] m_fsLocations = new FSLocation[0];

        @ValueReference(TableSpecSettingsRef.class)
        // for adding dynamic and provider
        @Modification.WidgetReference(TransformationSettingsWidgetModification.SpecsRef.class)
        List<TableSpecSettings<S>> m_specs = List.of();

        @ValueProvider(CommonTableReaderNodeParameters.AppendPathColumnRef.class)
        // for removing the setting when multi file support is disabled
        @Modification.WidgetReference(TransformationSettingsWidgetModification.AppendPathColumnRef.class)
        Optional<String> m_appendPathColumn = Optional.empty();

        @ValueProvider(TakeColumnsFromProvider.class)
        // for removing the setting when multi file support is disabled
        @Modification.WidgetReference(TransformationSettingsWidgetModification.ColumnFilterModeRef.class)
        ColumnFilterMode m_takeColumnsFrom = ColumnFilterMode.UNION;

    }

    PersistorSettings<I, S> m_persistorSettings = new PersistorSettings<>();

    static class TakeColumnsFromProvider implements StateProvider<ColumnFilterMode> {

        private Supplier<CommonTableReaderNodeParameters.HowToCombineColumnsOption> m_howToCombineColumnsSup;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_howToCombineColumnsSup = initializer
                .computeFromValueSupplier(CommonTableReaderNodeParameters.HowToCombineColumnsOptionRef.class);
        }

        @Override
        public ColumnFilterMode computeState(final NodeParametersInput context) {
            return m_howToCombineColumnsSup.get().toColumnFilterMode();
        }
    }

    @Widget(title = "Enforce types", description = CommonReaderLayout.Transformation.EnforceTypes.DESCRIPTION)
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

        static final class DontPersist implements NodeParametersPersistor<String> {

            @Override
            public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return null;
            }

            @Override
            public void save(final String obj, final NodeSettingsWO settings) {
                // do nothing
            }

            @Override
            public String[][] getConfigPaths() {
                return new String[0][0];
            }

        }

        @ValueReference(ColumnNameRef.class)
        @JsonInclude(Include.ALWAYS) // Necessary for the ColumnNameIsNull PredicateProvider to work
        @Persistor(DontPersist.class)
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

    @Widget(title = "Transformations", description = CommonReaderLayout.Transformation.DESCRIPTION)
    // TODO NOSONAR UIEXT-1901 this description is currently not shown
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

}
