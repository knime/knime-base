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
 *   Oct 21, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader2.common;

import java.util.Optional;

import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.config.AbstractMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;

final class MultiTableReadParameters implements NodeParameters {

    TableReadParameters m_tableReadParameters = new TableReadParameters();

    public enum IfSchemaChangesOption {
            @Label(value = "Fail",
                description = CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.DESCRIPTION_FAIL) //
            FAIL, //
            @Label(value = "Use new schema",
                description = CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.DESCRIPTION_USE_NEW_SCHEMA) //
            USE_NEW_SCHEMA, //
    }

    //static final class IfSchemaChangesPersistor implements NodeParametersPersistor<IfSchemaChangesOption> {
    //
    //    private static final String CFG_SAVE_TABLE_SPEC_CONFIG =
    //        "save_table_spec_config" + SettingsModel.CFGKEY_INTERNAL;
    //
    //    private static final String CFG_CHECK_TABLE_SPEC = "check_table_spec";
    //
    //    @Override
    //    public IfSchemaChangesOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
    //        final var saveTableSpecConfig = settings.getBoolean(CFG_SAVE_TABLE_SPEC_CONFIG, true);
    //        if (saveTableSpecConfig) {
    //            if (settings.getBoolean(CFG_CHECK_TABLE_SPEC, false)) {
    //                return IfSchemaChangesOption.FAIL;
    //            } else {
    //                return IfSchemaChangesOption.IGNORE;
    //            }
    //        }
    //        return IfSchemaChangesOption.USE_NEW_SCHEMA;
    //    }
    //
    //    @Override
    //    public void save(final IfSchemaChangesOption ifSchemaChangesOption, final NodeSettingsWO settings) {
    //        settings.addBoolean(CFG_SAVE_TABLE_SPEC_CONFIG,
    //            ifSchemaChangesOption != IfSchemaChangesOption.USE_NEW_SCHEMA);
    //        settings.addBoolean(CFG_CHECK_TABLE_SPEC, ifSchemaChangesOption == IfSchemaChangesOption.FAIL);
    //    }
    //
    //    @Override
    //    public String[][] getConfigPaths() {
    //        return new String[][]{{CFG_SAVE_TABLE_SPEC_CONFIG, CFG_CHECK_TABLE_SPEC}};
    //    }
    //}

    static final class IfSchemaChangesOptionRef implements ParameterReference<IfSchemaChangesOption> {
    }

    static final class UseNewSchema implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(IfSchemaChangesOptionRef.class).isOneOf(IfSchemaChangesOption.USE_NEW_SCHEMA);
        }
    }

    @Widget(title = "If schema changes",
        description = CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.DESCRIPTION)
    @RadioButtonsWidget
    @Layout(CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.class)
    @ValueReference(IfSchemaChangesOptionRef.class)
    public IfSchemaChangesOption m_ifSchemaChangesOption = IfSchemaChangesOption.FAIL;

    enum HowToCombineColumnsOption {
            @Label(value = "Fail if different",
                description = CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.DESCRIPTION_FAIL)
            FAIL(ColumnFilterMode.UNION),

            @Label(value = "Union",
                description = CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.DESCRIPTION_UNION)
            UNION(ColumnFilterMode.UNION),

            @Label(value = "Intersection",
                description = CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.DESCRIPTION_INTERSECTION)
            INTERSECTION(ColumnFilterMode.INTERSECTION);

        private final ColumnFilterMode m_columnFilterMode;

        HowToCombineColumnsOption(final ColumnFilterMode columnFilterMode) {
            m_columnFilterMode = columnFilterMode;
        }

        ColumnFilterMode toColumnFilterMode() {
            return m_columnFilterMode;
        }
    }

//    static class HowToCombineColumnsOptionRef implements ParameterReference<HowToCombineColumnsOption> {
//    }

    @Widget(title = "How to combine columns",
        description = CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.DESCRIPTION)
    @ValueSwitchWidget
//    @ValueReference(HowToCombineColumnsOptionRef.class)
    @Layout(CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.class)
    public HowToCombineColumnsOption m_howToCombineColumns = HowToCombineColumnsOption.FAIL;
    // TODO NOSONAR this setting should be shown when reading multiple files; currently blocked by UIEXT-1805

    static final class AppendPathColumnDefaultProvider implements DefaultValueProvider<String> {
        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            return "File Path";
        }
    }

//    static class AppendPathColumnRef extends ReferenceStateProvider<Boolean> {
//    }

    @Widget(title = "Append file path column",
        description = CommonReaderLayout.MultipleFileHandling.AppendFilePathColumn.DESCRIPTION)
//    @ValueReference(AppendPathColumnRef.class)
    @Layout(CommonReaderLayout.MultipleFileHandling.AppendFilePathColumn.class)
    @OptionalWidget(defaultProvider = AppendPathColumnDefaultProvider.class)
    @TextInputWidget(patternValidation = ColumnNameValidationUtils.ColumnNameValidation.class)
    Optional<String> m_appendPathColumn = Optional.empty();

    void loadFromConfig(final MultiTableReadConfig<?, ?> config) {
        m_tableReadParameters.loadFromConfig(config.getTableReadConfig());

        m_ifSchemaChangesOption =
            config.saveTableSpecConfig() ? IfSchemaChangesOption.FAIL : IfSchemaChangesOption.USE_NEW_SCHEMA;

        m_howToCombineColumns = config.failOnDifferingSpecs() ? HowToCombineColumnsOption.FAIL
            : (config.getSpecMergeMode() == SpecMergeMode.UNION ? HowToCombineColumnsOption.UNION
                : HowToCombineColumnsOption.INTERSECTION);

        m_appendPathColumn = config.appendItemIdentifierColumn() ? Optional.of(config.getItemIdentifierColumnName())
            : Optional.empty();
    }

    void saveToConfig(final AbstractMultiTableReadConfig<?, ? extends DefaultTableReadConfig<?>, ?, ?> config) {
        m_tableReadParameters.saveToConfig(config.getTableReadConfig());

        config.setSaveTableSpecConfig(m_ifSchemaChangesOption == IfSchemaChangesOption.FAIL);
        config.setCheckSavedTableSpec(true); // the option to ignore saved table spec is deprecated

        config.setFailOnDifferingSpecs(m_howToCombineColumns == HowToCombineColumnsOption.FAIL);
        config.setSpecMergeMode(m_howToCombineColumns == HowToCombineColumnsOption.INTERSECTION
            ? SpecMergeMode.INTERSECTION : SpecMergeMode.UNION);

        config.setAppendItemIdentifierColumn(m_appendPathColumn.isPresent());
        config.setItemIdentifierColumnName(m_appendPathColumn.orElse(""));
    }

}