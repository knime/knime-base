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
 * -------------------------------------------------------------------
 *
 * History
 *   Aug 12, 2006 (wiswedel): created
 */
package org.knime.base.node.preproc.domain.dialog2;

import java.util.List;
import java.util.Optional;

import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.LegacyColumnFilterPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoice;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Settings for the Domain Calculator node.
 *
 * @author wiswedel, University of Konstanz
 */
@SuppressWarnings("restriction")
@Layout(DomainNodeSettings.PossibleValuesSection.class)
public final class DomainNodeSettings implements NodeParameters {

    /**
     * Constructor for persistence and conversion from JSON.
     */
    public DomainNodeSettings() {
        // Default constructor
    }

    @Section(title = "Possible Values")
    interface PossibleValuesSection {
    }

    @Section(title = "Min & Max Values")
    @After(PossibleValuesSection.class)
    interface MinMaxValuesSection {
    }

    // Possible Values Section
    @Persistor(PossibleValuesColumnsPersistor.class)
    @Widget(title = "Columns for possible values calculation", 
        description = "Select columns for which possible values shall be determined and put in the table specification. "
            + "For all non-selected columns, the possible value domain will be dropped or retained, depending on the selection below.")
    @ChoicesProvider(NominalValueColumnsProvider.class)
    @Layout(PossibleValuesSection.class)
    ColumnFilter m_possibleValuesColumns = new ColumnFilter();

    @Persistor(PossValRetainUnselectedPersistor.class)
    @Widget(title = "For columns not selected above", 
        description = "Choose what to do with possible value domains for columns not selected for calculation.")
    @RadioButtonsWidget(horizontal = false)
    @Layout(PossibleValuesSection.class)
    PossibleValuesRetainMode m_possValRetainUnselected = PossibleValuesRetainMode.RETAIN;

    @Persistor(MaxPossibleValuesPersistor.class)
    @Widget(title = "Restrict number of possible values", 
        description = "Limit the number of different values that are stored as possible values. "
            + "If there are more possible values than the specified limit, all values are discarded and the column's "
            + "meta information won't support querying the possible values.")
    @OptionalWidget
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Layout(PossibleValuesSection.class)
    Optional<Integer> m_maxPossibleValues = Optional.empty();

    // Min & Max Values Section
    @Persistor(MinMaxColumnsPersistor.class)
    @Widget(title = "Columns for min/max calculation", 
        description = "Select columns for which min and max values shall be determined. "
            + "For all non-selected columns, the min/max domain will be dropped or retained, depending on the selection below.")
    @ChoicesProvider(BoundedValueColumnsProvider.class)
    @Layout(MinMaxValuesSection.class)
    ColumnFilter m_minMaxColumns = new ColumnFilter();

    @Persistor(MinMaxRetainUnselectedPersistor.class)
    @Widget(title = "For columns not selected above", 
        description = "Choose what to do with min/max domains for columns not selected for calculation.")
    @RadioButtonsWidget(horizontal = false)
    @Layout(MinMaxValuesSection.class)
    MinMaxRetainMode m_minMaxRetainUnselected = MinMaxRetainMode.RETAIN;

    // Enums for retention modes
    enum PossibleValuesRetainMode implements EnumChoice {
        @Label("Retain possible value domain")
        RETAIN,

        @Label("Drop possible value domain")
        DROP;
    }

    enum MinMaxRetainMode implements EnumChoice {
        @Label("Retain min/max domain")
        RETAIN,

        @Label("Drop min/max domain")
        DROP;
    }

    // Column providers
    static final class NominalValueColumnsProvider extends CompatibleColumnsProvider {
        protected NominalValueColumnsProvider() {
            super(NominalValue.class);
        }
    }

    static final class BoundedValueColumnsProvider extends CompatibleColumnsProvider {
        protected BoundedValueColumnsProvider() {
            super(BoundedValue.class);
        }
    }

    // Persistors
    static final class PossibleValuesColumnsPersistor extends LegacyColumnFilterPersistor {
        PossibleValuesColumnsPersistor() {
            super(DomainNodeModel.CFG_POSSVAL_COLS);
        }
    }

    static final class MinMaxColumnsPersistor extends LegacyColumnFilterPersistor {
        MinMaxColumnsPersistor() {
            super(DomainNodeModel.CFG_MIN_MAX_COLS);
        }
    }

    static final class PossValRetainUnselectedPersistor implements NodeParametersPersistor<PossibleValuesRetainMode> {
        @Override
        public PossibleValuesRetainMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            boolean retain = settings.getBoolean(DomainNodeModel.CFG_POSSVAL_RETAIN_UNSELECTED);
            return retain ? PossibleValuesRetainMode.RETAIN : PossibleValuesRetainMode.DROP;
        }

        @Override
        public void save(final PossibleValuesRetainMode obj, final NodeSettingsWO settings) {
            settings.addBoolean(DomainNodeModel.CFG_POSSVAL_RETAIN_UNSELECTED, obj == PossibleValuesRetainMode.RETAIN);
        }

        @Override
        public String[] getConfigPaths() {
            return new String[]{DomainNodeModel.CFG_POSSVAL_RETAIN_UNSELECTED};
        }
    }

    static final class MinMaxRetainUnselectedPersistor implements NodeParametersPersistor<MinMaxRetainMode> {
        @Override
        public MinMaxRetainMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            boolean retain = settings.getBoolean(DomainNodeModel.CFG_MIN_MAX_RETAIN_UNSELECTED);
            return retain ? MinMaxRetainMode.RETAIN : MinMaxRetainMode.DROP;
        }

        @Override
        public void save(final MinMaxRetainMode obj, final NodeSettingsWO settings) {
            settings.addBoolean(DomainNodeModel.CFG_MIN_MAX_RETAIN_UNSELECTED, obj == MinMaxRetainMode.RETAIN);
        }

        @Override
        public String[] getConfigPaths() {
            return new String[]{DomainNodeModel.CFG_MIN_MAX_RETAIN_UNSELECTED};
        }
    }

    static final class MaxPossibleValuesPersistor implements NodeParametersPersistor<Optional<Integer>> {
        @Override
        public Optional<Integer> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            int maxPossValues = settings.getInt(DomainNodeModel.CFG_MAX_POSS_VALUES);
            return maxPossValues >= 0 ? Optional.of(maxPossValues) : Optional.empty();
        }

        @Override
        public void save(final Optional<Integer> obj, final NodeSettingsWO settings) {
            int value = obj.orElse(-1);
            settings.addInt(DomainNodeModel.CFG_MAX_POSS_VALUES, value);
        }

        @Override
        public String[] getConfigPaths() {
            return new String[]{DomainNodeModel.CFG_MAX_POSS_VALUES};
        }
    }
}
