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
 *   Feb 18, 2025 (paulbaernreuther): created
 */
package org.knime.time.node.create.createdatetime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
/**
 *
 * @author Paul Baernreuther
 */
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.interval.Interval;
import org.knime.time.node.create.createdatetime.CreateDateTimeNodeSettings.FixedSteps;
import org.knime.time.util.DateTimeType;
import org.knime.workflow.migration.MigrationException;
import org.knime.workflow.migration.MigrationNodeMatchResult;
import org.knime.workflow.migration.NodeMigrationAction;
import org.knime.workflow.migration.NodeMigrationRule;
import org.knime.workflow.migration.model.MigrationNode;

/**
 * Node migration rule for the <em>Spark to Hive</em> node.
 *
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
public class CreateDateTimeNodeMigrationRule extends NodeMigrationRule {
    @Override
    protected Class<? extends NodeFactory<?>> getReplacementNodeFactoryClass(final MigrationNode migrationNode,
        final MigrationNodeMatchResult matchResult) {
        return CreateDateTimeNodeFactory2.class;
    }

    @Override
    protected MigrationNodeMatchResult match(final MigrationNode migrationNode) {
        if ("org.knime.time.node.create.createdatetime.CreateDateTimeNodeFactory"
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

    static final String COLUMN_NAME_CFG_KEY = "column_name";

    static final String ROW_NR_OPTION_SELECTION_CFG_KEY = "rownr_option_selection";

    static final String NR_ROWS_CFG_KEY = "nr_rows";

    static final String START_CFG_KEY = "start";

    static final String DURATION_OR_END_CFG_KEY = "duration_or_end";

    static final String DURATION_CFG_KEY = "duration";

    static final String END_CFG_KEY = "end";

    static final String START_USE_EXEC_TIME_CFG_KEY = "start_use_exec_time";

    static final String END_USE_EXEC_TIME_CFG_KEY = "end_use_exec_time";

    static final String TYPE_CFG_KEY = "type";

    static final Map<String, DateTimeType> TYPE_MAP = Map.of("LOCAL_TIME", DateTimeType.LOCAL_TIME, //
        "LOCAL_DATE", DateTimeType.LOCAL_DATE, //
        "LOCAL_DATE_TIME", DateTimeType.LOCAL_DATE_TIME, //
        "ZONED_DATE_TIME", DateTimeType.ZONED_DATE_TIME //
    );

    @SuppressWarnings({"restriction"})
    @Override
    protected void migrate(final MigrationNode migrationNode, final MigrationNodeMatchResult matchResult)
        throws MigrationException {
        // Ports
        associateEveryOriginalPortWithNew(migrationNode);
        final var pair= performMigration(migrationNode);
        final var newModelSettings = getNewNodeModelSettings(migrationNode);
        DefaultNodeSettings.saveSettings(pair.getFirst().getClass(), pair.getFirst(), newModelSettings);
        pair.getSecond().copyTo(getNewNodeVariableSettings(migrationNode));
    }

    /**
     * @param migrationNode
     * @return
     * @throws MigrationException
     */
    private static Pair<CreateDateTimeNodeSettings, NodeSettingsRO> performMigration(final MigrationNode migrationNode)
        throws MigrationException {
        final NodeSettingsRO origVariableSettings = migrationNode.getOriginalNodeVariableSettings();
        final NodeSettingsRO origModelSettings = migrationNode.getOriginalNodeModelSettings();

        String oldVariableKey = null;
        NodeSettingsRO usedVariableSettings = null;
        String newVariableKey = null;
        final var newSettings = new CreateDateTimeNodeSettings();
        try {
            if (origVariableSettings.children().hasMoreElements()) {
                for (final var key : List.of(START_CFG_KEY, END_CFG_KEY, DURATION_CFG_KEY)) {
                    if (origVariableSettings.containsKey(key)) {
                        oldVariableKey = key;
                        usedVariableSettings = origVariableSettings.getNodeSettings(key);
                        break;
                    }
                }
                if (oldVariableKey == null) {
                    throw new MigrationException("Cannot migrate settings since unsuported variables are used: "
                        + origVariableSettings.children().nextElement());
                }

            }

            if (origModelSettings.getBoolean(START_USE_EXEC_TIME_CFG_KEY, false)) {
                System.out.println("ExecTimeException");
                throw new MigrationException("Cannot migrate settings since start_use_exec_time is set.");
            }
            if (origModelSettings.getBoolean(END_USE_EXEC_TIME_CFG_KEY, false)) {
                System.out.println("ExecTimeException");
                throw new MigrationException("Cannot migrate settings since end_use_exec_time is set.");
            }

            newSettings.m_outputType = TYPE_MAP.get(origModelSettings.getString(TYPE_CFG_KEY));
            newSettings.m_outputColumnName = origModelSettings.getString(COLUMN_NAME_CFG_KEY);
            switch (newSettings.m_outputType) {
                case LOCAL_DATE:
                    newSettings.m_localDateStart = LocalDate.parse(origModelSettings.getString(START_CFG_KEY));
                    if (oldVariableKey == START_CFG_KEY) {
                        newVariableKey = "localDateStart";
                    }
                    newSettings.m_localDateEnd = LocalDate.parse(origModelSettings.getString(END_CFG_KEY));
                    if (oldVariableKey == END_CFG_KEY) {
                        newVariableKey = "localDateEnd";
                    }
                    break;
                case LOCAL_TIME:
                    newSettings.m_localTimeStart = LocalTime.parse(origModelSettings.getString(START_CFG_KEY));
                    if (oldVariableKey == START_CFG_KEY) {
                        newVariableKey = "localTimeStart";
                    }
                    newSettings.m_localTimeEnd = LocalTime.parse(origModelSettings.getString(END_CFG_KEY));
                    if (oldVariableKey == END_CFG_KEY) {
                        newVariableKey = "localTimeEnd";
                    }

                    break;
                case LOCAL_DATE_TIME:
                    newSettings.m_localDateTimeStart = LocalDateTime.parse(origModelSettings.getString(START_CFG_KEY));
                    if (oldVariableKey == START_CFG_KEY) {
                        newVariableKey = "localDateTimeStart";
                    }
                    newSettings.m_localDateTimeEnd = LocalDateTime.parse(origModelSettings.getString(END_CFG_KEY));
                    if (oldVariableKey == END_CFG_KEY) {
                        newVariableKey = "localDateTimeEnd";
                    }
                    break;
                case ZONED_DATE_TIME:
                    newSettings.m_zonedDateTimeStart = ZonedDateTime.parse(origModelSettings.getString(START_CFG_KEY));
                    if (oldVariableKey == START_CFG_KEY) {
                        newVariableKey = "zonedDateTimeStart";
                    }
                    newSettings.m_localDateTimeEnd = LocalDateTime.parse(origModelSettings.getString(END_CFG_KEY));
                    if (oldVariableKey == END_CFG_KEY) {
                        newVariableKey = "localDateTimeEnd";
                    }
                    break;
            }
            newSettings.m_numberOfRows = origModelSettings.getLong(NR_ROWS_CFG_KEY);
            final var useVariableNrRows =
                origModelSettings.getString(ROW_NR_OPTION_SELECTION_CFG_KEY).equals("Variable");
            final var useEnd = origModelSettings.getString(DURATION_OR_END_CFG_KEY).equals("End");

            if (useVariableNrRows) {
                newSettings.m_interval = Interval.parseISO(origModelSettings.getString(DURATION_CFG_KEY));
                if (oldVariableKey == DURATION_CFG_KEY) {
                    newVariableKey = "interval";
                }
                if (useEnd) {
                    newSettings.m_fixedSteps = FixedSteps.INTERVAL_AND_END;
                } else {
                    newSettings.m_fixedSteps = FixedSteps.NUMBER_AND_INTERVAL;
                }
            } else {
                newSettings.m_fixedSteps = FixedSteps.NUMBER_AND_END;
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
        if (usedVariableSettings != null) {
            usedVariableSettings.copyTo(newVariableSettings.addNodeSettings(newVariableKey));
        }
        return new Pair<>(newSettings, newVariableSettings);

    }
}
