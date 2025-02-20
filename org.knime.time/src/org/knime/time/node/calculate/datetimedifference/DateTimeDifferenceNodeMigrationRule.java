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
 *   19 Feb 2025 (Robin Gerling): created
 */
package org.knime.time.node.calculate.datetimedifference;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.time.node.calculate.datetimedifference.DateTimeDifferenceNodeSettings.OutputNumberType;
import org.knime.time.node.calculate.datetimedifference.DateTimeDifferenceNodeSettings.OutputType;
import org.knime.time.node.calculate.datetimedifference.DateTimeDifferenceNodeSettings.SecondDateTimeValueType;
import org.knime.time.util.Granularity;
import org.knime.workflow.migration.MigrationException;
import org.knime.workflow.migration.MigrationNodeMatchResult;
import org.knime.workflow.migration.NodeMigrationAction;
import org.knime.workflow.migration.NodeMigrationRule;
import org.knime.workflow.migration.model.MigrationNode;

/**
 *
 * @author Robin Gerling
 */
public class DateTimeDifferenceNodeMigrationRule extends NodeMigrationRule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends NodeFactory<?>> getReplacementNodeFactoryClass(final MigrationNode migrationNode,
        final MigrationNodeMatchResult matchResult) {
        return DateTimeDifferenceNodeFactory2.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MigrationNodeMatchResult match(final MigrationNode migrationNode) {
        if ("org.knime.time.node.calculate.datetimedifference.DateTimeDifferenceNodeFactory"
            .equals(migrationNode.getOriginalNodeFactoryClassName())) {
            try {
                performMigration(migrationNode);
            } catch (MigrationException ex) {
                return MigrationNodeMatchResult.of(migrationNode, null);
            }

            return MigrationNodeMatchResult.of(migrationNode, NodeMigrationAction.REPLACE);
        }

        return MigrationNodeMatchResult.of(migrationNode, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void migrate(final MigrationNode migrationNode, final MigrationNodeMatchResult matchResult)
        throws MigrationException {
        // Ports
        associateEveryOriginalPortWithNew(migrationNode);
        final var pair = performMigration(migrationNode);
        final var newModelSettings = getNewNodeModelSettings(migrationNode);
        DefaultNodeSettings.saveSettings(pair.getFirst().getClass(), pair.getFirst(), newModelSettings);
        pair.getSecond().copyTo(getNewNodeVariableSettings(migrationNode));
    }

    static final String COLUMN_SELECTION_1_CFG_KEY = "col_select1";

    static final String COLUMN_SELECTION_2_CFG_KEY = "col_select2";

    static final String MODUS_CFG_KEY = "modus";

    static final String FIXED_DATE_TIME_CFG_KEY = "fixed_date_time";

    static final String OUTPUT_CFG_KEY = "output";

    static final String GRANULARITY_CFG_KEY = "granularity";

    static final String NEW_COL_NAME = "new_col_name";

    private static Pair<DateTimeDifferenceNodeSettings, NodeSettingsRO>
        performMigration(final MigrationNode migrationNode) throws MigrationException {
        final NodeSettingsRO origVariableSettings = migrationNode.getOriginalNodeVariableSettings();
        final NodeSettingsRO origModelSettings = migrationNode.getOriginalNodeModelSettings();

        String oldVariableKey = null;
        NodeSettingsRO usedVariableSettings = null;
        String newVariableKey = null;
        final var newSettings = new DateTimeDifferenceNodeSettings();
        try {
            if (origVariableSettings.children().hasMoreElements()) {
                throw new MigrationException("Cannot migrate settings since variable migration is not implemented");
            }

            newSettings.m_firstColumnSelection =
                new ColumnSelection(origModelSettings.getString(COLUMN_SELECTION_1_CFG_KEY), null);
            newSettings.m_secondColumnSelection =
                new ColumnSelection(origModelSettings.getString(COLUMN_SELECTION_2_CFG_KEY), null);
            newSettings.m_secondDateTimeValueType =
                getSecondDateTimeValueType(origModelSettings.getString(MODUS_CFG_KEY));
            newSettings.m_outputType = getOutputType(origModelSettings.getString(OUTPUT_CFG_KEY));
            newSettings.m_granularity = Granularity.fromString(origModelSettings.getString(GRANULARITY_CFG_KEY));
            newSettings.m_outputColumnName = origModelSettings.getString(NEW_COL_NAME);
            newSettings.m_outputNumberType = OutputNumberType.NO_DECIMALS;

            final var fixed_date_time = origModelSettings.getString(FIXED_DATE_TIME_CFG_KEY);
            if (isLocalDate(fixed_date_time)) {
                newSettings.m_localDateFixed = LocalDate.parse(fixed_date_time);
            } else if (isLocalTime(fixed_date_time)) {
                newSettings.m_localTimeFixed = LocalTime.parse(fixed_date_time);
            } else if (isLocalDateTime(fixed_date_time)) {
                newSettings.m_localDateTimeFixed = LocalDateTime.parse(fixed_date_time);
            } else if (isZonedDateTime(fixed_date_time)) {
                newSettings.m_zonedDateTimeFixed = ZonedDateTime.parse(fixed_date_time);
            } else {
                throw new MigrationException("Cannot migrate fixed date time: " + fixed_date_time);
            }

        } catch (IllegalArgumentException ex) {
            System.out.println("IllegalArgumentException");
            System.out.println(ex.getMessage());
            throw new MigrationException("Cannot migrate settings", ex);
        } catch (DateTimeParseException ex) {
            System.out.println("DateTimeParseException");
            System.out.println(ex.getMessage());
            throw new MigrationException("Cannot migrate settings since datetime setting are invalid", ex);
        } catch (InvalidSettingsException ex) {
            System.out.println("InvalidSettingsException");
            System.out.println(ex.getMessage());
            throw new MigrationException("Cannot migrate settings since loading settings failed", ex);
        }

        final NodeSettings newVariableSettings = new NodeSettings("variables");
        return new Pair<>(newSettings, newVariableSettings);

    }

    private static OutputType getOutputType(final String output) throws MigrationException {
        if (output.equals("Granularity")) {
            return OutputType.NUMBER;
        }
        if (output.equals("Duration")) {
            return OutputType.DURATION_OR_PERIOD;
        }
        throw new MigrationException("Cannot migrate setttings. No enum constant with name: " + output);
    }

    private static SecondDateTimeValueType getSecondDateTimeValueType(final String modus) throws MigrationException {
        if (modus.equals(ModusOptions.Use2ndColumn.name())) {
            return SecondDateTimeValueType.COLUMN;
        }
        if (modus.equals(ModusOptions.UseExecutionTime.name())) {
            return SecondDateTimeValueType.EXECUTION_DATE_TIME;
        }
        if (modus.equals(ModusOptions.UseFixedTime.name())) {
            return SecondDateTimeValueType.FIXED_DATE_TIME;
        }
        if (modus.equals(ModusOptions.UsePreviousRow.name())) {
            return SecondDateTimeValueType.PREVIOUS_ROW;
        }
        throw new MigrationException("Cannot migrate setttings. No enum constant with name: " + modus);
    }

    public static boolean isZonedDateTime(final String input) {
        try {
            ZonedDateTime.parse(input);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isLocalDateTime(final String input) {
        try {
            LocalDateTime.parse(input);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isLocalTime(final String input) {
        try {
            LocalTime.parse(input);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isLocalDate(final String input) {
        try {
            LocalDate.parse(input);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

}
