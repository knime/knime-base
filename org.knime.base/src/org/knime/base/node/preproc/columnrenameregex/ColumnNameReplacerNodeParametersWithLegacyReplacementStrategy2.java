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
 *   Sep 30, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.columnrenameregex;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.util.regex.RegexReplaceUtils.ReplacementResult;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;

/**
 * Before 5.5 renaming a column to an other existing one while also renaming that other one lead to this being treated
 * as a case where the first renaming needed to be adjusted although it does not lead to any collisions. E.g. columns
 *
 * "A" and "B" and renaming "A" to "B" and "B" to "C" lead to "B (#1)", "C" whereas since 5.5 the result would be "B"
 * and "C".
 *
 * @since 5.8
 * @author Paul Bärnreuther
 */
public class ColumnNameReplacerNodeParametersWithLegacyReplacementStrategy2 extends ColumnNameReplacerNodeSettings {

    @Migration(LoadFalseForOldNodes2.class)
    boolean m_respectRenamedColumnSourcesWhenResolvingNamingCollisions = true;

    /**
     * The flag was added with 5.8, so we need to load depending on a field added with 5.5 instead of using a
     * {@link DefaultProvider}.
     */
    static final class LoadFalseForOldNodes2 implements NodeParametersMigration<Boolean> {

        @Override
        public List<ConfigMigration<Boolean>> getConfigMigrations() {
            return List.of(ConfigMigration
                .builder(settings -> settings.getBoolean("properlySupportUnicodeCharacters", false)).build());
        }

    }

    @Override
    protected Set<String> getColumnsToCompareAgainstForCollisions(final Set<String> extantColumnNames,
        final Map<String, String> renames) {
        if (m_respectRenamedColumnSourcesWhenResolvingNamingCollisions) {
            return super.getColumnsToCompareAgainstForCollisions(extantColumnNames, renames);
        }
        return extantColumnNames;
    }

    @Override
    protected void addToRenamesMap(final Map<String, String> renames, final String oldName,
        final ReplacementResult replacement) {
        if (m_respectRenamedColumnSourcesWhenResolvingNamingCollisions) {
            super.addToRenamesMap(renames, oldName, replacement);
        } else {
            // Only add renamings that change the name, otherwise they are compared against themselves.
            if (replacement.wasReplaced() && !oldName.equals(replacement.result())) {
                renames.put(oldName, replacement.result());
            }
        }
    }

}
