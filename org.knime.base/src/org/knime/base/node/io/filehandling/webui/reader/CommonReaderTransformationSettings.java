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

import java.util.List;
import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.ConfigIdSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.SourceIdProvider;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.TransformationSettingsWidgetModification;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.TypeChoicesProvider;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.LatentWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.WidgetModification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.internal.InternalArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 * @param <C> the type of the config settings
 * @param <S> the type of the serializable form for external data types
 */
@SuppressWarnings({"javadoc", "restriction"})
@Layout(CommonReaderLayout.Transformation.class)
public abstract class CommonReaderTransformationSettings<C extends ConfigIdSettings<?>, S>
    implements PersistableSettings, WidgetGroup {

    protected CommonReaderTransformationSettings(final C configId) {
        m_persistorSettings = new PersistorSettings<>(configId);
    }

    CommonReaderTransformationSettings() {
        // Default constructor required as per {@link PersistablSettings} contract
    }

    public static class ConfigIdSettings<C extends ReaderSpecificConfig<C>>
        implements WidgetGroup, PersistableSettings {
        /**
         * @param config
         */
        protected void applyToConfig(final DefaultTableReadConfig<C> config) {
            // Do nothing per default
        }
    }

    static final class ColumnSpecSettings<S> implements WidgetGroup, PersistableSettings {

        String m_name;

        S m_type;

        ColumnSpecSettings(final String name, final S type) {
            m_name = name;
            m_type = type;
        }

        ColumnSpecSettings() {
        }
    }

    public static final class TableSpecSettings<T> implements WidgetGroup, PersistableSettings {

        String m_sourceId;

        List<ColumnSpecSettings<T>> m_spec;

        TableSpecSettings(final String sourceId, final List<ColumnSpecSettings<T>> spec) {
            m_sourceId = sourceId;
            m_spec = spec;
        }

        TableSpecSettings() {
        }
    }

    static class TableSpecSettingsRef implements Reference<List<TableSpecSettings<?>>> {
    }

    /**
     * TODO NOSONAR UIEXT-1946 These settings are sent to the frontend where they are not needed. They are merely held
     * here to be used in the CSVTransformationSettingsPersistor. We should look for an alternative mechanism to provide
     * these settings to the persistor. This would then also allow us to use non-serializable types like the
     * TypedReaderTableSpec instead of the TableSpecSettings, saving us the back-and-forth conversion.
     */
    static class PersistorSettings<C extends ConfigIdSettings<?>, S> implements WidgetGroup, PersistableSettings {

        private PersistorSettings(final C configId) {
            CheckUtils.checkArgumentNotNull(configId);
            m_configId = configId;
        }

        PersistorSettings() {
            // Default constructor required as per {@link PersistablSettings} contract
        }

        @WidgetModification.WidgetReference(TransformationSettingsWidgetModification.ConfigIdSettingsRef.class) // for adding dynamic ref
        C m_configId;

        @ValueProvider(SourceIdProvider.class)
        String m_sourceId = "";

        @WidgetModification.WidgetReference(TransformationSettingsWidgetModification.FsLocationRef.class) // for adding dynamic provider
        FSLocation[] m_fsLocations = new FSLocation[0];

        @ValueReference(TableSpecSettingsRef.class)
        @WidgetModification.WidgetReference(TransformationSettingsWidgetModification.SpecsRef.class) // for adding dynamic and provider
        List<TableSpecSettings<S>> m_specs = List.of();

        @ValueProvider(CommonReaderNodeSettings.AdvancedSettings.AppendPathColumnRef.class)
        boolean m_appendPathColumn;

        @ValueProvider(CommonReaderNodeSettings.AdvancedSettings.FilePathColumnNameRef.class)
        String m_filePathColumnName = "File Path";

        @ValueProvider(TakeColumnsFromProvider.class)
        ColumnFilterMode m_takeColumnsFrom = ColumnFilterMode.UNION;
    }

    PersistorSettings<C, S> m_persistorSettings = new PersistorSettings<>();

    static class TakeColumnsFromProvider implements StateProvider<ColumnFilterMode> {

        private Supplier<CommonReaderNodeSettings.AdvancedSettings.HowToCombineColumnsOption> m_howToCombineColumnsOptionSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_howToCombineColumnsOptionSupplier = initializer
                .computeFromValueSupplier(CommonReaderNodeSettings.AdvancedSettings.HowToCombineColumnsOptionRef.class);
        }

        @Override
        public ColumnFilterMode computeState(final DefaultNodeSettingsContext context) {
            return m_howToCombineColumnsOptionSupplier.get().toColumnFilterMode();
        }
    }

    @Widget(title = "Enforce types", description = CommonReaderLayout.Transformation.EnforceTypes.DESCRIPTION,
        hideFlowVariableButton = true)
    boolean m_enforceTypes = true;

    static class TransformationElementSettings implements WidgetGroup, PersistableSettings {

        static class ColumnNameRef implements Reference<String> {
        }

        static final class ColumnNameIsNull implements PredicateProvider {
            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getString(ColumnNameRef.class).isEqualTo(null);
            }
        }

        @ValueReference(ColumnNameRef.class)
        @JsonInclude(Include.ALWAYS) // Necessary for the ColumnNameIsNull PredicateProvider to work
        @LatentWidget // Necessary for the ColumnNameIsNull PredicateProvider to work
        String m_columnName;

        static class OriginalTypeRef implements Reference<String> {
        }

        @ValueReference(OriginalTypeRef.class)
        String m_originalType;

        static class OriginalTypeLabelRef implements Reference<String> {
        }

        @ValueReference(OriginalTypeLabelRef.class)
        String m_originalTypeLabel;

        @Widget(title = "Include in output", description = "", hideFlowVariableButton = true) // TODO NOSONAR UIEXT-1901 add description
        @InternalArrayWidget.ElementCheckboxWidget
        boolean m_includeInOutput;

        static final class ColumnNameResetter implements StateProvider<String> {

            private Supplier<String> m_originalColumnNameSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeOnButtonClick(InternalArrayWidget.ElementResetButton.class);
                m_originalColumnNameSupplier = initializer.getValueSupplier(ColumnNameRef.class);
            }

            @Override
            public String computeState(final DefaultNodeSettingsContext context) {
                return m_originalColumnNameSupplier.get();
            }
        }

        static final class TypeResetter implements StateProvider<String> {

            private Supplier<String> m_originalTypeSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeOnButtonClick(InternalArrayWidget.ElementResetButton.class);
                m_originalTypeSupplier = initializer.getValueSupplier(OriginalTypeRef.class);
            }

            @Override
            public String computeState(final DefaultNodeSettingsContext context) {
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
            public String computeState(final DefaultNodeSettingsContext context) {
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
            public String computeState(final DefaultNodeSettingsContext context) {
                return m_originalTypeLabelSupplier.get();
            }
        }

        static final class ElementIsEditedAndColumnNameIsNotNull implements PredicateProvider {
            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getPredicate(InternalArrayWidget.ElementIsEdited.class)
                    .and(i.getPredicate(ColumnNameIsNull.class).negate());
            }
        }

        @Widget(title = "Column name", description = "", hideTitle = true, hideFlowVariableButton = true)
        @ValueProvider(ColumnNameResetter.class)
        @Effect(predicate = ElementIsEditedAndColumnNameIsNotNull.class, type = EffectType.SHOW)
        @JsonInclude(Include.ALWAYS) // Necessary for comparison against m_columnName
        String m_columnRename;

        @Widget(title = "Column type", description = "", hideTitle = true, hideFlowVariableButton = true)
        @WidgetModification.WidgetReference(TransformationSettingsWidgetModification.TypeChoicesWidgetRef.class) // for adding dynamic choices
        @ValueProvider(TypeResetter.class)
        @Effect(predicate = InternalArrayWidget.ElementIsEdited.class, type = EffectType.SHOW)
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

    static final class TransformationElementSettingsReference implements Reference<TransformationElementSettings[]> {
    }

    @Widget(title = "Transformations", description = CommonReaderLayout.Transformation.DESCRIPTION)
    // TODO NOSONAR UIEXT-1901 this description is currently not shown
    @ArrayWidget(elementTitle = "Column", showSortButtons = true, hasFixedSize = true)
    @InternalArrayWidget(withEditAndReset = true, withElementCheckboxes = true,
        titleProvider = TransformationElementSettings.TitleProvider.class,
        subTitleProvider = TransformationElementSettings.SubTitleProvider.class)
    @ValueReference(TransformationElementSettingsReference.class)
    @WidgetModification.WidgetReference(TransformationSettingsWidgetModification.TransformationElementSettingsArrayWidgetRef.class) // for adding dynamic choices
    @Effect(predicate = FileSystemPortConnectionUtil.ConnectedWithoutFileSystemSpec.class, type = EffectType.HIDE)
    TransformationElementSettings[] m_columnTransformation =
        new TransformationElementSettings[]{TransformationElementSettings.createUnknownElement()};

}
