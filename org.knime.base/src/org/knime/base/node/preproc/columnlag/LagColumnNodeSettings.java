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
 *   Jan 26, 2023 (Jonas Klotz, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.columnlag;

import static org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice.ROW_ID;

import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.date.DateAndTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.StringToStringWithRowIDChoiceMigration;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.AllColumnsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class LagColumnNodeSettings implements DefaultNodeSettings {

    /**
     * Constructor called by the framework to get default settings.
     *
     * @param context of the settings creation
     */
    LagColumnNodeSettings(final DefaultNodeSettingsContext context) {
        context.getDataTableSpec(0)//
            .flatMap(LagColumnNodeSettings::findDefaultColumn).ifPresent(column -> {
                m_column = new StringOrEnum<>(column.getName());
            });
    }

    /**
     * @param spec
     * @return the last time column or - if none is present - the last column
     */
    static Optional<DataColumnSpec> findDefaultColumn(final DataTableSpec spec) {
        return spec.stream().reduce(LagColumnNodeSettings::chooseBetterColumn);
    }

    private static DataColumnSpec chooseBetterColumn(final DataColumnSpec previous, final DataColumnSpec next) {
        return isBetterColumn(previous, next) ? next : previous;
    }

    private static boolean isBetterColumn(final DataColumnSpec previous, final DataColumnSpec next) {
        return isTimeColumn(next) || !isTimeColumn(previous);
    }

    private static boolean isTimeColumn(final DataColumnSpec col) {
        return col.getType().isCompatible(DateAndTimeValue.class);
    }

    /**
     * Constructor called by the framework for persistence and JSON conversion.
     */
    LagColumnNodeSettings() {
    }

    @Widget(title = "Column to lag", description = "The column to be lagged.")
    @ChoicesProvider(AllColumnsProvider.class)
    @Migration(ColumnMigration.class)
    @Persist(configKey = "columnV2")
    StringOrEnum<RowIDChoice> m_column = new StringOrEnum<>(ROW_ID);

    static final class ColumnMigration extends StringToStringWithRowIDChoiceMigration {

        protected ColumnMigration() {
            super("column");
        }

        @Override
        public Optional<RowIDChoice> loadEnumFromLegacyString(final String legacyString) {
            if (legacyString == null) {
                // Before migrating the model to WebUI, null had been used to indicate that the RowID is used
                return Optional.of(ROW_ID);
            }
            return super.loadEnumFromLegacyString(legacyString);
        }

    }

    @Widget(title = "Number of copies", description = " <i>L</i> = defines how many lagged column copies to create.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_lag = 1;

    @Persist(configKey = "lag_interval")
    @Widget(title = "Lag per copy",
        description = "<i>I</i> = lag interval (sometimes also called periodicity or seasonality), defines "
            + "how many rows to shift per column copy.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_lagInterval = 1;

    @Widget(title = "Drop incomplete rows at the top of the table",
        description = "If selected, the first rows from the input table are omitted in the output so that the lag "
            + "output column(s) is not missing (unless the reference data is missing).")
    boolean m_skipInitialIncompleteRows;

    @Widget(title = "Drop incomplete rows at the bottom of the table",
        description = "If selected the rows containing the lagged values of the last real data row are "
            + "omitted (no artificial new rows). Otherwise new rows are added, "
            + "which contain missing values in all columns but the new lag output.")
    boolean m_skipLastIncompleteRows = true;

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_lag <= 0) {
            throw new InvalidSettingsException("Lag must be greater than 0: " + m_lag);
        }
        if (m_lagInterval <= 0) {
            throw new InvalidSettingsException("Lag interval must be greater than 0: " + m_lagInterval);
        }
    }

}
