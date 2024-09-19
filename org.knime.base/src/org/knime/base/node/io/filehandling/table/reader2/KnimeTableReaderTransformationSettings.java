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
 *   May 8, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.table.reader2;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.function.Supplier;

import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettingsStateProviders.FSLocationsProvider;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettingsStateProviders.SourceIdProvider;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettingsStateProviders.TableSpecSettingsProvider;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettingsStateProviders.TransformationElementSettingsProvider;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderTransformationSettingsStateProviders.TypeChoicesProvider;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.ConfigIdSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TableSpecSettings;
import org.knime.base.node.preproc.manipulator.TableManipulatorConfig;
import org.knime.base.node.preproc.manipulator.TableManipulatorConfigSerializer.DataTypeSerializer;
import org.knime.base.node.preproc.manipulator.mapping.DataTypeTypeHierarchy;
import org.knime.base.node.preproc.manipulator.mapping.DataValueReadAdapterFactory;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.config.base.JSONConfig;
import org.knime.core.node.config.base.JSONConfig.WriterConfig;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
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
import org.knime.filehandling.core.node.table.reader.DefaultProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class KnimeTableReaderTransformationSettings implements WidgetGroup, PersistableSettings {

    // ??? this is different
    static final ProductionPathProvider<DataType> PRODUCTION_PATH_PROVIDER =
        new DefaultProductionPathProvider<>(DataValueReadAdapterFactory.INSTANCE.getProducerRegistry(),
            DataValueReadAdapterFactory.INSTANCE::getDefaultType);

    static final TypeHierarchy<DataType, DataType> TYPE_HIERARCHY = DataTypeTypeHierarchy.INSTANCE;

    static final class KnimeTableReaderConfigIdSettings implements ConfigIdSettings<TableManipulatorConfig> {
        @Override
        public void applyToConfig(final DefaultTableReadConfig<TableManipulatorConfig> config) {
        }
    }

    /**
     * Serializes a given {@link DataType} into a string
     *
     * @param type the to-be-serialized {@link DataType}
     * @return the serialized string
     */
    public static String typeToString(final DataType type) {
        final var settings = new NodeSettings("type");
        DataTypeSerializer.SERIALIZER_INSTANCE.save(type, settings);
        return JSONConfig.toJSONString(settings, WriterConfig.DEFAULT);
    }

    /**
     * De-serializes a string that has been generated via {@link DataTypeSerializationUtil#typeToString} into a
     * {@link DataType}.
     *
     * @param string the previously serialized string
     * @return the de-serialized {@link DataType}
     */
    public static DataType stringToType(final String string) {
        try {
            final var settings = new NodeSettings("type");
            JSONConfig.readJSON(settings, new StringReader(string));
            return DataTypeSerializer.SERIALIZER_INSTANCE.load(settings);
        } catch (IOException | InvalidSettingsException e) {
            return DataType.getMissingCell().getType(); // TODO
        }
    }

    @WidgetModification(PersistorSettings.SetConfigIdSettingsValueRef.class)
    static final class PersistorSettings
        extends CommonReaderTransformationSettings.PersistorSettings<KnimeTableReaderConfigIdSettings, String> {

        static final class SetConfigIdSettingsValueRef extends
            CommonReaderTransformationSettings.PersistorSettings.SetStateProviders<KnimeTableReaderConfigIdSettings, String> {

            static final class KnimeTableReaderConfigIdSettingsValueRef
                implements Reference<KnimeTableReaderConfigIdSettings> {
            }

            @Override
            protected Class<? extends Reference<KnimeTableReaderConfigIdSettings>> getConfigIdSettingsValueRef() {
                return KnimeTableReaderConfigIdSettingsValueRef.class;
            }

            @Override
            protected Class<? extends Reference<List<TableSpecSettings<String>>>> getSpecsValueRef() {
                return TableSpecSettingsRef.class;
            }

            @Override
            protected Class<? extends StateProvider<List<TableSpecSettings<String>>>> getSpecsValueProvider() {
                return TableSpecSettingsProvider.class;
            }
        }

        @ValueProvider(SourceIdProvider.class)
        String m_sourceId = "";

        @ValueProvider(FSLocationsProvider.class)
        FSLocation[] m_fsLocations = new FSLocation[0];

        static class TableSpecSettingsRef implements Reference<List<TableSpecSettings<String>>> {
        }

        @ValueProvider(CommonReaderNodeSettings.AdvancedSettings.AppendPathColumnRef.class)
        boolean m_appendPathColumn;

        @ValueProvider(CommonReaderNodeSettings.AdvancedSettings.FilePathColumnNameRef.class)
        String m_filePathColumnName = "File Path";

        @ValueProvider(TakeColumnsFromProvider.class)
        ColumnFilterMode m_takeColumnsFrom = ColumnFilterMode.UNION;
    }

    PersistorSettings m_persistorSettings = new PersistorSettings();

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
                initializer.computeOnValueChange(PersistorSettings.TableSpecSettingsRef.class);
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
                initializer.computeOnValueChange(PersistorSettings.TableSpecSettingsRef.class);
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

        @Widget(title = "Column name", description = "", hideTitle = true, hideFlowVariableButton = true) // TODO NOSONAR UIEXT-1901 add description
        @ValueProvider(ColumnNameResetter.class)
        @Effect(predicate = ElementIsEditedAndColumnNameIsNotNull.class, type = EffectType.SHOW)
        @JsonInclude(Include.ALWAYS) // Necessary for comparison against m_columnName
        String m_columnRename;

        @Widget(title = "Column type", description = "", hideTitle = true, hideFlowVariableButton = true) // TODO NOSONAR UIEXT-1901 add description
        @ChoicesWidget(choicesProvider = TypeChoicesProvider.class)
        @ValueProvider(TypeResetter.class)
        @Effect(predicate = InternalArrayWidget.ElementIsEdited.class, type = EffectType.SHOW)
        String m_type;

        // extra field source type serialized

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
    @ValueProvider(TransformationElementSettingsProvider.class)
    @ValueReference(TransformationElementSettingsReference.class)
    @Effect(predicate = FileSystemPortConnectionUtil.ConnectedWithoutFileSystemSpec.class, type = EffectType.HIDE)
    TransformationElementSettings[] m_columnTransformation =
        new TransformationElementSettings[]{TransformationElementSettings.createUnknownElement()};
}
