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
 *   Feb 5, 2025 (david): created
 */
package org.knime.base.node.preproc.constantvalue;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.knime.base.node.io.filehandling.csv.reader.api.StringReadAdapterFactory;
import org.knime.base.node.io.filehandling.webui.reader.DataTypeStringSerializer;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory.FromString;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.StringChoicesStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.IdAndText;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;

/**
 * Settings for the Constant Value Column WebUI node.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class ConstantValueColumnNodeSettings implements DefaultNodeSettings {

    @Widget(title = "", description = "")
    @ArrayWidget( //
        addButtonText = "Add column", //
        elementDefaultValueProvider = NewColumnSettings.InitialNewColumnSettingsValueProvider.class, //
        elementTitle = "Constant column" //
    )
    @ValueReference(NewColumnSettingsArrayRef.class)
    NewColumnSettings[] m_newColumnSettings = new NewColumnSettings[]{new NewColumnSettings()};

    static final class NewColumnSettings implements DefaultNodeSettings {

        NewColumnSettings() {
        }

        NewColumnSettings(final DefaultNodeSettingsContext context) {
            m_columnNameToReplace = context.getDataTable(0) //
                .stream() //
                .map(DataTable::getDataTableSpec) //
                .map(DataTableSpec::stream).flatMap(Function.identity()) //
                .map(DataColumnSpec::getName) //
                .findFirst() //
                .orElse("");
        }

        @Widget(title = "Column type", description = "The type of the new column.")
        @ChoicesWidget(choicesProvider = SupportedDataTypeChoicesProvider.class)
        String m_type = SupportedDataTypeChoicesProvider.getDataTypeId(DataType.getType(StringCell.class));

        @Widget(title = "Fill value", description = """
                Whether to use a custom value for the new column, or fill it with \
                missing cells.
                """)
        @ValueReference(CustomOrMissingValue.Ref.class)
        @ValueSwitchWidget
        CustomOrMissingValue m_customOrMissingValue = CustomOrMissingValue.MISSING;

        @Widget(title = "Custom value", description = "The value to be used when filling the output column.")
        @Effect(predicate = CustomOrMissingValue.IsMissing.class, type = EffectType.HIDE)
        String m_value = "";

        @Widget(title = "Output column", description = "Whether to replace an existing column or append a new one.")
        @ValueReference(AppendOrReplace.Ref.class)
        @ValueSwitchWidget
        AppendOrReplace m_replaceOrAppend = AppendOrReplace.APPEND;

        @Widget(title = "New column", description = "The name of the new column.")
        @Effect(predicate = AppendOrReplace.IsReplace.class, type = EffectType.HIDE)
        @TextInputWidget(placeholder = "New column name")
        String m_columnNameToAppend = "New column";

        @Widget(title = "Replace column", description = "The name of the column to replace.")
        @ChoicesWidget(choices = ReplaceableColumnProvider.class)
        @Effect(predicate = AppendOrReplace.IsReplace.class, type = EffectType.SHOW)
        String m_columnNameToReplace;

        static final class ReplaceableColumnProvider implements ColumnChoicesProvider {

            @Override
            public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
                return context.getDataTableSpec(0) //
                    .map(DataTableSpec::stream) //
                    .map(s -> s.toArray(DataColumnSpec[]::new)) //
                    .orElseGet(() -> new DataColumnSpec[0]);
            }
        }

        enum AppendOrReplace {

                @Label(value = "Append", description = "Append a new column")
                APPEND, //
                @Label(value = "Replace", description = "Replace an existing column")
                REPLACE;

            static final class Ref implements Reference<AppendOrReplace> {
            }

            static final class IsReplace implements PredicateProvider {
                @Override
                public Predicate init(final PredicateInitializer i) {
                    return i.getEnum(AppendOrReplace.Ref.class).isOneOf(REPLACE);
                }
            }
        }

        //
        //        enum SupportedColumnType {
        //                @Label(value = "String")
        //                STRING(StringCellFactory.TYPE, StringCellFactory::create), //
        //                @Label(value = "Number (integer)")
        //                INTEGER(IntCellFactory.TYPE, IntCellFactory::create), //
        //                @Label(value = "Number (long)")
        //                LONG(LongCellFactory.TYPE, LongCellFactory::create), //
        //                @Label(value = "Number (double)")
        //                DOUBLE(DoubleCellFactory.TYPE, DoubleCellFactory::create), //
        //                @Label(value = "Boolean")
        //                BOOLEAN(BooleanCellFactory.TYPE, BooleanCellFactory::create);
        //
        //            final DataType m_correspondingKnimeType;
        //
        //            Function<String, DataCell> m_cellFactory;
        //
        //            SupportedColumnType( //
        //                final DataType correspondingKnimeType, //
        //                final Function<String, DataCell> cellFactory //
        //            ) {
        //                m_correspondingKnimeType = correspondingKnimeType;
        //                m_cellFactory = cellFactory;
        //            }
        //
        //            Optional<DataCell> createCellFromString(final String value) {
        //                try {
        //                    return Optional.of(m_cellFactory.apply(value));
        //                } catch (NumberFormatException ex) {
        //                    return Optional.empty();
        //                }
        //            }
        //
        //            boolean isValidData(final String value) {
        //                return createCellFromString(value).isPresent();
        //            }
        //
        //            static SupportedColumnType fromDataType(final DataType type) {
        //                return Arrays.stream(values()) //
        //                    .filter(v -> v.m_correspondingKnimeType.equals(type)) //
        //                    .findFirst() //
        //                    .orElseThrow(() -> new IllegalArgumentException("Unsupported data type: " + type));
        //            }
        //        }
        //
        enum CustomOrMissingValue {

                @Label(value = "Custom value", description = "Use a custom value to fill the new constant column")
                CUSTOM, //
                @Label(value = "Missing", description = "Fill the new constant column with missing cells")
                MISSING;

            static final class Ref implements Reference<CustomOrMissingValue> {
            }

            static final class IsMissing implements PredicateProvider {
                @Override
                public Predicate init(final PredicateInitializer i) {
                    return i.getEnum(CustomOrMissingValue.Ref.class).isOneOf(MISSING);
                }
            }
        }

        static final class InitialNewColumnSettingsValueProvider implements StateProvider<NewColumnSettings> {

            private Supplier<NewColumnSettings[]> m_currentNewColumnSettings;

            @Override
            public NewColumnSettings computeState(final DefaultNodeSettingsContext context) {
                var newSettings = new NewColumnSettings(context);

                var existingNewColumns = m_currentNewColumnSettings.get();
                var numberOfAppendedColumns = Arrays.stream(existingNewColumns) //
                    .filter(s -> s.m_replaceOrAppend == AppendOrReplace.APPEND) //
                    .count();

                newSettings.m_columnNameToAppend = "New column " + (numberOfAppendedColumns + 1);
                return newSettings;
            }

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
                m_currentNewColumnSettings = initializer.computeFromValueSupplier(NewColumnSettingsArrayRef.class);
            }
        }
    }

    static final class NewColumnSettingsArrayRef implements Reference<NewColumnSettings[]> {
    }

    static final class SupportedDataTypeChoicesProvider implements StringChoicesStateProvider {

        static final String DEFAULT_COLUMNTYPE_ID = "<default-columntype>";

        static final String DEFAULT_COLUMNTYPE_TEXT = "Default columntype";

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public IdAndText[] computeState(final DefaultNodeSettingsContext context) {
            final var defaultChoice = new IdAndText(DEFAULT_COLUMNTYPE_ID, DEFAULT_COLUMNTYPE_TEXT);
            final var dataTypeChoices = getProductionPathProvider().getAvailableDataTypes().stream()
                .sorted((t1, t2) -> t1.toPrettyString().compareTo(t2.toPrettyString()))
                .map(type -> new IdAndText(getDataTypeId(type), type.toPrettyString())).toList();

            System.out.println("dataTypeChoices: " + dataTypeChoices);

            return Stream.concat(Stream.of(defaultChoice), dataTypeChoices.stream()).toArray(IdAndText[]::new);
        }

        static String getDataTypeId(final DataType type) {
            return DataTypeStringSerializer.typeToString(type);
        }

        static DataType getDataType(final String id) {
            System.out.println("id: " + id);
            return DataTypeStringSerializer.stringToType(id);
        }

        static Optional<DataCell> createDataCellFromString(final String value, final DataType type,
            final ExecutionContext ctx) {

            var factory = type.getCellFactory(ctx).orElseThrow();
            try {
                return Optional.of(FromString.class.cast(factory).createCell(value));
            } catch (RuntimeException ex) { // NOSONAR some classes break the contract of createCell, so we need to catch everything
                return Optional.empty();
            }
        }

        static Optional<DataCell> createDataCellFromString(final String value, final String type,
            final ExecutionContext ctx) {
            return createDataCellFromString(value, getDataType(type), ctx);
        }

        private static ProductionPathProvider<Class<?>> getProductionPathProvider() {
            return StringReadAdapterFactory.INSTANCE.createProductionPathProvider();
        }
    }
}
