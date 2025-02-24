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
package org.knime.time.node.convert.stringtodatetime;

import java.time.format.DateTimeParseException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat;
import org.knime.core.webui.node.dialog.defaultdialog.setting.temporalformat.TemporalFormat.FormatTemporalType;
import org.knime.time.util.ActionIfExtractionFails;
import org.knime.time.util.DateTimeType;
import org.knime.time.util.ReplaceOrAppend;
import org.knime.workflow.migration.MigrationException;
import org.knime.workflow.migration.MigrationNodeMatchResult;
import org.knime.workflow.migration.NodeMigrationAction;
import org.knime.workflow.migration.NodeMigrationRule;
import org.knime.workflow.migration.model.MigrationNode;

/**
 *
 * @author Robin Gerling
 */
public class StringToDateTimeNodeMigrationRule extends NodeMigrationRule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected Class<? extends NodeFactory<?>> getReplacementNodeFactoryClass(final MigrationNode migrationNode,
        final MigrationNodeMatchResult matchResult) {
        return StringToDateTimeNodeFactory2.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected MigrationNodeMatchResult match(final MigrationNode migrationNode) {
        if ("org.knime.time.node.convert.stringtodatetime.StringToDateTimeNodeFactory"
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

    static final String COLUMN_SELECTION_CFG_KEY = "col_select";

    static final String REPLACE_OR_APPEND_CFG_KEY = "replace_or_append";

    static final String SUFFIX_CFG_KEY = "suffix";

    static final String DATE_FORMAT_CFG_KEY = "date_format";

    static final String CANCEL_ON_FAIL_CFG_KEY = "cancel_on_fail";

    static final String TYPE_ENUM_CFG_KEY = "typeEnum";

    static final String LOCALE_CFG_KEY = "locale";

    static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {

        ColumnFilterPersistor() {
            super(COLUMN_SELECTION_CFG_KEY);
        }
    }

    private static FormatTemporalType convertDateTimeType(final String dateTimeType) throws MigrationException {
        final var dtt = DateTimeType.valueOf(dateTimeType);
        if (dtt == DateTimeType.LOCAL_DATE) {
            return FormatTemporalType.DATE;
        }
        if (dtt == DateTimeType.LOCAL_TIME) {
            return FormatTemporalType.TIME;
        }
        if (dtt == DateTimeType.LOCAL_DATE_TIME) {
            return FormatTemporalType.DATE_TIME;
        }
        if (dtt == DateTimeType.ZONED_DATE_TIME) {
            return FormatTemporalType.ZONED_DATE_TIME;
        }
        throw new MigrationException(String.format("Unsupported enum member: %s", dtt.name()));
    }

    private static Pair<StringToDateTimeNodeSettings, NodeSettingsRO>
        performMigration(final MigrationNode migrationNode) throws MigrationException {
        final NodeSettingsRO origVariableSettings = migrationNode.getOriginalNodeVariableSettings();
        final NodeSettingsRO origModelSettings = migrationNode.getOriginalNodeModelSettings();

        String oldVariableKey = null;
        NodeSettingsRO usedVariableSettings = null;
        String newVariableKey = null;
        final var newSettings = new StringToDateTimeNodeSettings();
        try {
            if (origVariableSettings.children().hasMoreElements()) {
                throw new MigrationException("Cannot migrate settings since variable migration is not implemented");
            }

            newSettings.m_locale = origModelSettings.getString(LOCALE_CFG_KEY).replace("_", "-");
            newSettings.m_format = new TemporalFormat(origModelSettings.getString(DATE_FORMAT_CFG_KEY),
                convertDateTimeType(origModelSettings.getString(TYPE_ENUM_CFG_KEY)));
            newSettings.m_outputColumnSuffix = origModelSettings.getString(SUFFIX_CFG_KEY);
            newSettings.m_onError = origModelSettings.getBoolean(CANCEL_ON_FAIL_CFG_KEY) ? ActionIfExtractionFails.FAIL
                : ActionIfExtractionFails.SET_MISSING;
            newSettings.m_appendOrReplace = new ReplaceOrAppend.Persistor().load(origModelSettings);
            newSettings.m_columnFilter = new ColumnFilterPersistor().load(origModelSettings);

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

}
