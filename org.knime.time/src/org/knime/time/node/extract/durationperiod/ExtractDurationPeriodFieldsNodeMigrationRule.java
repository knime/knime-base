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
 *   Feb 19, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.time.node.extract.durationperiod;

import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.workflow.migration.MigrationException;
import org.knime.workflow.migration.MigrationNodeMatchResult;
import org.knime.workflow.migration.NodeMigrationAction;
import org.knime.workflow.migration.NodeMigrationRule;
import org.knime.workflow.migration.model.MigrationNode;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
public class ExtractDurationPeriodFieldsNodeMigrationRule extends NodeMigrationRule {

    @Override
    protected Class<? extends NodeFactory<?>> getReplacementNodeFactoryClass(final MigrationNode migrationNode,
        final MigrationNodeMatchResult matchResult) {
        return ExtractDurationPeriodFieldsNodeFactory2.class;
    }

    @Override
    protected MigrationNodeMatchResult match(final MigrationNode migrationNode) {
        if ("org.knime.time.node.extract.durationperiod.ExtractDurationPeriodFieldsNodeFactory"
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

    @SuppressWarnings("restriction")
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

    static final String COLUMN_SELECTION_KEY = "col_select";

    static final String SUBSECOND_KEY = "subsecond";

    static final String SUBSECOND_UNIT_KEY = "subsecond_units";

    private static Pair<ExtractDurationPeriodFieldsNodeSettings, NodeSettingsRO>
        performMigration(final MigrationNode migrationNode) throws MigrationException {
        final NodeSettingsRO origModelSettings = migrationNode.getOriginalNodeModelSettings();
        final var newSettings = new ExtractDurationPeriodFieldsNodeSettings();

        try {
            newSettings.m_selectedColumn = origModelSettings.getString(COLUMN_SELECTION_KEY);
            final var fields = Arrays.stream(ExtractableField.values())
                .filter(value -> value.getOldConfigValue().isPresent()
                    && origModelSettings.getBoolean(value.getOldConfigValue().get(), false))
                .map(value -> new ExtractFieldSettings(value, value.getOldConfigValue().get()))
                .collect(Collectors.toList());
            if (origModelSettings.getBoolean(SUBSECOND_KEY, false)) {
                final var field = ExtractableField.getByOldConfigValue(origModelSettings.getString(SUBSECOND_UNIT_KEY));
                fields.add(new ExtractFieldSettings(field, field.getOldConfigValue().get()));
            }
            newSettings.m_extractFields = fields.toArray(ExtractFieldSettings[]::new);

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
