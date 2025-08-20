/*
 * ------------------------------------------------------------------------
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
 */
package org.knime.base.node.preproc.columntrans2;

import org.knime.base.node.preproc.pmml.columntrans2.Many2OneCol2PMMLNodeModel;
import org.knime.base.node.preproc.pmml.columntrans2.Many2OneCol2PMMLNodeModel.IncludeMethod;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelStringPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.LegacyColumnFilterPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Predicate;
import org.knime.node.parameters.updates.PredicateProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.AllColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Settings for the Web UI dialog of the Many to One node.
 *
 * @author KNIME GmbH
 * @since 5.5
 */
@SuppressWarnings("restriction")
@Layout(Many2OneCol2NodeSettings.IncludeMethodSection.class)
public final class Many2OneCol2NodeSettings implements NodeParameters {

    /**
     * Constructor for persistence and conversion from JSON.
     */
    public Many2OneCol2NodeSettings() {
    }

    Many2OneCol2NodeSettings(final NodeParametersInput context) {
        this();
        m_includedColumns = new ColumnFilter(ColumnSelectionUtil.getAllColumnsOfFirstPort(context)).withIncludeUnknownColumns();
    }

    @Section(title = "Column Selection")
    interface ColumnSelectionSection {
    }

    @Section(title = "Include Method")
    @After(ColumnSelectionSection.class)
    interface IncludeMethodSection {
    }

    @Section(title = "Options")
    @After(IncludeMethodSection.class)
    interface OptionsSection {
    }

    @Persistor(IncludedColumnsPersistor.class)
    @Widget(title = "Columns", 
        description = "Select those columns which should be condensed into the aggregated result column.")
    @ChoicesProvider(AllColumnsProvider.class)
    @Layout(ColumnSelectionSection.class)
    ColumnFilter m_includedColumns = new ColumnFilter();

    @Persistor(AppendedColumnNamePersistor.class)
    @Widget(title = "Appended column name", 
        description = "Name of the aggregate column that will be created.")
    @Layout(ColumnSelectionSection.class)
    String m_appendedColumnName = "Condensed Column";

    @Persistor(IncludeMethodPersistor.class)
    @Widget(title = "Include method", 
        description = "Choose the method to determine the matching column: " +
            "&lt;ul&gt;" +
            "&lt;li&gt;Binary: Only the column with value \"1\" matches&lt;/li&gt;" +
            "&lt;li&gt;Maximum: The column with the maximum value in each row matches&lt;/li&gt;" +
            "&lt;li&gt;Minimum: The column with the minimum value in each row matches&lt;/li&gt;" +
            "&lt;li&gt;RegExpPattern: The column matching the provided regular expression pattern matches&lt;/li&gt;" +
            "&lt;/ul&gt;")
    @Layout(IncludeMethodSection.class)
    IncludeMethodOptions m_includeMethod = IncludeMethodOptions.BINARY;

    enum IncludeMethodOptions {
        @Label("Binary")
        BINARY,

        @Label("Maximum")
        MAXIMUM,

        @Label("Minimum")
        MINIMUM,

        @Label("RegExpPattern")
        REGEXP_PATTERN;
    }

    static class IsRegExpPattern implements PredicateProvider {
        @Override
        public Predicate init(final PredicateProvider.PredicateInitializer i) {
            return i.getEnum(IncludeMethodOptions.class).isOneOf(IncludeMethodOptions.REGEXP_PATTERN);
        }
    }

    @Persistor(PatternPersistor.class)
    @Widget(title = "Include Pattern", 
        description = "Enter the regular expression pattern if RegExpPattern was chosen as include method.")
    @Effect(predicate = IsRegExpPattern.class, type = Effect.Type.SHOW)
    @Layout(IncludeMethodSection.class)
    String m_pattern = "[^0]*";

    @Persistor(KeepColumnsPersistor.class)
    @Widget(title = "Keep original columns", 
        description = "If checked, the selected columns are kept in the output table, otherwise they are deleted.")
    @Layout(OptionsSection.class)
    boolean m_keepColumns = true;

    static final class IncludedColumnsPersistor extends LegacyColumnFilterPersistor {
        IncludedColumnsPersistor() {
            super(Many2OneCol2PMMLNodeModel.SELECTED_COLS);
        }
    }

    static final class AppendedColumnNamePersistor extends SettingsModelStringPersistor {
        AppendedColumnNamePersistor() {
            super(Many2OneCol2PMMLNodeModel.CONDENSED_COL_NAME);
        }
    }

    static final class IncludeMethodPersistor implements NodeParametersPersistor<IncludeMethodOptions> {

        @Override
        public IncludeMethodOptions load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final String methodName = settings.getString(Many2OneCol2PMMLNodeModel.INCLUDE_METHOD, IncludeMethod.Binary.name());
            final IncludeMethod method = IncludeMethod.valueOf(methodName);
            
            switch (method) {
                case Binary:
                    return IncludeMethodOptions.BINARY;
                case Maximum:
                    return IncludeMethodOptions.MAXIMUM;
                case Minimum:
                    return IncludeMethodOptions.MINIMUM;
                case RegExpPattern:
                    return IncludeMethodOptions.REGEXP_PATTERN;
                default:
                    return IncludeMethodOptions.BINARY;
            }
        }

        @Override
        public void save(final IncludeMethodOptions obj, final NodeSettingsWO settings) {
            IncludeMethod method;
            switch (obj) {
                case BINARY:
                    method = IncludeMethod.Binary;
                    break;
                case MAXIMUM:
                    method = IncludeMethod.Maximum;
                    break;
                case MINIMUM:
                    method = IncludeMethod.Minimum;
                    break;
                case REGEXP_PATTERN:
                    method = IncludeMethod.RegExpPattern;
                    break;
                default:
                    method = IncludeMethod.Binary;
                    break;
            }
            settings.addString(Many2OneCol2PMMLNodeModel.INCLUDE_METHOD, method.name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{Many2OneCol2PMMLNodeModel.INCLUDE_METHOD}};
        }
    }

    static final class PatternPersistor extends SettingsModelStringPersistor {
        PatternPersistor() {
            super(Many2OneCol2PMMLNodeModel.RECOGNICTION_REGEX);
        }
    }

    static final class KeepColumnsPersistor extends SettingsModelBooleanPersistor {
        KeepColumnsPersistor() {
            super(Many2OneCol2PMMLNodeModel.KEEP_COLS);
        }
    }
}
