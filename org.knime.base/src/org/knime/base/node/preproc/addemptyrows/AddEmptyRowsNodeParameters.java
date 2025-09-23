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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.preproc.addemptyrows;

import java.util.Optional;

import org.knime.base.node.util.EnumBooleanPersistor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

/**
 * Node parameters for Add Empty Rows.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
class AddEmptyRowsNodeParameters implements NodeParameters {

    @Section(title = "Number of rows in output")
    interface RowCountSection {
    }

    @Section(title = "Fill Data")
    @After(RowCountSection.class)
    interface FillDataSection {
    }

    // ====== Row Count Settings ======

    enum CountMode {
        @Label("At least")
        AT_LEAST,
        @Label("Additional")
        ADDITIONAL
    }

    @Widget(title = "Mode", description = """
            Choose whether to add a fixed number of rows to the input table ("Additional"), independent of
            the input table's row count; or to add rows to the table only in case there are less rows than
            indicated ("At least"). The latter will not change the input table if there are already more rows
            than specified in the corresponding field.
            """)
    @Layout(RowCountSection.class)
    @RadioButtonsWidget
    @ValueReference(CountModeRef.class)
    @Persistor(CountModePersistor.class)
    CountMode m_countMode = CountMode.ADDITIONAL;

    @Widget(title = "Count", description = "Number of rows to add or minimum number of rows in the output table.")
    @Layout(RowCountSection.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Persist(configKey = AddEmptyRowsConfig.CFG_KEY_ROW_COUNT)
    int m_rowCount = 15;

    @Widget(title = "RowID Prefix", description = "Prefix for the row IDs of newly added rows.")
    @Layout(RowCountSection.class)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    @Persist(configKey = AddEmptyRowsConfig.CFG_KEY_NEW_ROW_KEY_PREFIX)
    String m_newRowKeyPrefix = "Empty ";

    // ====== Fill Data Settings ======

    @Widget(title = "Number (Float)", description = "Fill value for Float columns in newly added rows.")
    @Layout(FillDataSection.class)
    @OptionalWidget(defaultProvider = DefaultDoubleProvider.class)
    @Persistor(DoubleFillValuePersistor.class)
    Optional<Double> m_fillValueDouble = Optional.empty();

    @Widget(title = "Number (Integer)", description = "Fill value for Integer columns in newly added rows.")
    @Layout(FillDataSection.class)
    @OptionalWidget(defaultProvider = DefaultIntProvider.class)
    @Persistor(IntFillValuePersistor.class)
    Optional<Integer> m_fillValueInt = Optional.empty();

    @Widget(title = AddEmptyRowsConfig.CFG_KEY_STRING, description = "Fill value for String columns in newly added rows.")
    @Layout(FillDataSection.class)
    @OptionalWidget(defaultProvider = DefaultStringProvider.class)
    @Persistor(StringFillValuePersistor.class)
    Optional<String> m_fillValueString = Optional.empty();

    // ====== Value References ======

    static final class CountModeRef implements ParameterReference<CountMode> {
    }

    // ====== Default Value Providers ======

    static final class DefaultDoubleProvider implements DefaultValueProvider<Double> {
        @Override
        public Double computeState(final NodeParametersInput parametersInput) {
            return 0.0;
        }
    }

    static final class DefaultIntProvider implements DefaultValueProvider<Integer> {
        @Override
        public Integer computeState(final NodeParametersInput parametersInput) {
            return 0;
        }
    }

    static final class DefaultStringProvider implements DefaultValueProvider<String> {
        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            return "empty";
        }
    }

    // ====== Custom Persistors ======

    static final class CountModePersistor extends EnumBooleanPersistor<CountMode> {

        protected CountModePersistor() {
            super(AddEmptyRowsConfig.CFG_KEY_AT_LEAST_MODE, CountMode.class, CountMode.AT_LEAST);
        }

    }

    static final class DoubleFillValuePersistor implements NodeParametersPersistor<Optional<Double>> {
        @Override
        public Optional<Double> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            NodeSettingsRO doubleSet = settings.getNodeSettings(AddEmptyRowsConfig.CFG_KEY_DOUBLE);
            return doubleSet.getBoolean(AddEmptyRowsConfig.CFG_KEY_USE_MISSING) ? Optional.empty()
                    : Optional.of(doubleSet.getDouble(AddEmptyRowsConfig.CFG_KEY_FILL_VALUE));
        }

        @Override
        public void save(final Optional<Double> obj, final NodeSettingsWO settings) {
            NodeSettingsWO doubleSet = settings.addNodeSettings(AddEmptyRowsConfig.CFG_KEY_DOUBLE);
            if (obj.isEmpty()) {
                doubleSet.addBoolean(AddEmptyRowsConfig.CFG_KEY_USE_MISSING, true);
                doubleSet.addDouble(AddEmptyRowsConfig.CFG_KEY_FILL_VALUE, 0.0); // default value when missing
            } else {
                doubleSet.addBoolean(AddEmptyRowsConfig.CFG_KEY_USE_MISSING, false);
                doubleSet.addDouble(AddEmptyRowsConfig.CFG_KEY_FILL_VALUE, obj.get());
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{AddEmptyRowsConfig.CFG_KEY_DOUBLE, AddEmptyRowsConfig.CFG_KEY_USE_MISSING},
                {AddEmptyRowsConfig.CFG_KEY_DOUBLE, AddEmptyRowsConfig.CFG_KEY_FILL_VALUE}};
        }
    }

    static final class IntFillValuePersistor implements NodeParametersPersistor<Optional<Integer>> {
        @Override
        public Optional<Integer> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            NodeSettingsRO intSet = settings.getNodeSettings(AddEmptyRowsConfig.CFG_KEY_INT);
            return intSet.getBoolean(AddEmptyRowsConfig.CFG_KEY_USE_MISSING) ? Optional.empty()
                : Optional.of(intSet.getInt(AddEmptyRowsConfig.CFG_KEY_FILL_VALUE));
        }

        @Override
        public void save(final Optional<Integer> obj, final NodeSettingsWO settings) {
            NodeSettingsWO intSet = settings.addNodeSettings(AddEmptyRowsConfig.CFG_KEY_INT);
            if (obj.isEmpty()) {
                intSet.addBoolean(AddEmptyRowsConfig.CFG_KEY_USE_MISSING, true);
                intSet.addInt(AddEmptyRowsConfig.CFG_KEY_FILL_VALUE, 0); // default value when missing
            } else {
                intSet.addBoolean(AddEmptyRowsConfig.CFG_KEY_USE_MISSING, false);
                intSet.addInt(AddEmptyRowsConfig.CFG_KEY_FILL_VALUE, obj.get());
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{AddEmptyRowsConfig.CFG_KEY_INT, AddEmptyRowsConfig.CFG_KEY_USE_MISSING},
                {AddEmptyRowsConfig.CFG_KEY_INT, AddEmptyRowsConfig.CFG_KEY_FILL_VALUE}};
        }
    }

    static final class StringFillValuePersistor implements NodeParametersPersistor<Optional<String>> {
        @Override
        public Optional<String> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            NodeSettingsRO stringSet = settings.getNodeSettings(AddEmptyRowsConfig.CFG_KEY_STRING);
            return stringSet.getBoolean(AddEmptyRowsConfig.CFG_KEY_USE_MISSING) ? Optional.empty()
                : Optional.of(stringSet.getString(AddEmptyRowsConfig.CFG_KEY_FILL_VALUE));
        }

        @Override
        public void save(final Optional<String> obj, final NodeSettingsWO settings) {
            NodeSettingsWO stringSet = settings.addNodeSettings(AddEmptyRowsConfig.CFG_KEY_STRING);
            if (obj.isEmpty()) {
                stringSet.addBoolean(AddEmptyRowsConfig.CFG_KEY_USE_MISSING, true);
                stringSet.addString(AddEmptyRowsConfig.CFG_KEY_FILL_VALUE, ""); // default value when missing
            } else {
                stringSet.addBoolean(AddEmptyRowsConfig.CFG_KEY_USE_MISSING, false);
                stringSet.addString(AddEmptyRowsConfig.CFG_KEY_FILL_VALUE, obj.get());
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{AddEmptyRowsConfig.CFG_KEY_STRING, AddEmptyRowsConfig.CFG_KEY_USE_MISSING},
                {AddEmptyRowsConfig.CFG_KEY_STRING, AddEmptyRowsConfig.CFG_KEY_FILL_VALUE}};
        }
    }

}
