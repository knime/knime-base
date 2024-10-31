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
 *   Nov 5, 2024 (Tobias Kampmann): created
 */
package org.knime.time.util;

import java.util.Arrays;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;

/**
 *
 * @author Tobias Kampmann
 */
@SuppressWarnings("restriction")
public enum AppendOrReplace {

        @Label(value = "Replace", description = "The selected columns will be replaced by the new columns.")
        REPLACE("Replace selected columns"), //
        @Label(value = "Append with Suffix", //
            description = "The selected columns will be appended to the input table, "
                + "with a new name that is the previous name plus the provided suffix.")
        APPEND("Append selected columns"); //

    private final String m_oldConfigValue;

    AppendOrReplace(final String oldConfigValue) {
        this.m_oldConfigValue = oldConfigValue;
    }

    static AppendOrReplace getByOldConfigValue(final String oldValue) throws InvalidSettingsException {
        return Arrays.stream(values()) //
            .filter(v -> v.m_oldConfigValue.equals(oldValue)) //
            .findFirst() //
            .orElseThrow(() -> new InvalidSettingsException(
                String.format("Invalid value '%s'. Possible values: %s", oldValue, getOldConfigValues())));
    }

    static String[] getOldConfigValues() {
        return Arrays.stream(values()).map(v -> v.m_oldConfigValue).toArray(String[]::new);
    }

    /**
     * Defining the Reference for the AppendOrReplace enum.
     * Dont use AppendOrReplace more than once. The predicates will only work for the last one.
     */
    public interface ValueRef extends Reference<AppendOrReplace> {
    }

    /**
     * Predicate to check if the selected value is Append.
     */
    public static final class IsAppend implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ValueRef.class).isOneOf(AppendOrReplace.APPEND);
        }
    }

    /**
     * Predicate to check if the selected value is Replace.
     */
    public static final class IsReplace implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ValueRef.class).isOneOf(AppendOrReplace.REPLACE);
        }
    }

    /**
     *  used to persist the value of the enum in the node settings
     */
    public static final class Persistor implements FieldNodeSettingsPersistor<AppendOrReplace> {

        private static final String CONFIG_KEY = "replace_or_append";

        @Override
        public AppendOrReplace load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return AppendOrReplace.getByOldConfigValue(settings.getString(CONFIG_KEY));
        }

        @Override
        public void save(final AppendOrReplace obj, final NodeSettingsWO settings) {
            settings.addString(CONFIG_KEY, obj.m_oldConfigValue);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{CONFIG_KEY};
        }
    }
}
