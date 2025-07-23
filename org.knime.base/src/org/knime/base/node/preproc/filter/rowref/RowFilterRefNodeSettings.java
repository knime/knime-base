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
 *   Jan 9, 2023 (benjamin): created
 */
package org.knime.base.node.preproc.filter.rowref;

import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Before;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * {@link NodeParameters} implementation for the Reference Row Filter to auto-generate a Web-UI based dialog. Note
 * that this class is only used for the dialog generation and not by the node model.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 5.1
 */
@SuppressWarnings("restriction")
public final class RowFilterRefNodeSettings extends AbstractRowFilterRefNodeSettings {

    @After(ColumnsLayout.class)
    @Before(UpdateDomainsLayout.class)
    interface IncludeOrExcludeRowsLayout {
    }

    @Migration(IncludeOrExcludeRowsMigration.class)
    @Persist(configKey = "inexcludeV2")
    @Widget(title = "Include or exclude rows from the reference table",
        description = "Includes or excludes all rows from the reference table in the resulting table from the first "
            + "input.")
    @ValueSwitchWidget
    @Layout(IncludeOrExcludeRowsLayout.class)
    IncludeOrExcludeRows m_inexclude = IncludeOrExcludeRows.INCLUDE;

    enum IncludeOrExcludeRows {
            @Label("Include")
            INCLUDE,

            @Label("Exclude")
            EXCLUDE;
    }

    static final class IncludeOrExcludeRowsMigration implements NodeParametersMigration<IncludeOrExcludeRows> {

        private static final String LEGACY_KEY_INCLUDE_EXCLUDE = "inexclude";

        /**
         * Value saved in settings to indicate we should exclude selected rows:
         *
         * "Exclude rows from reference table";
         */
        /** Value saved in settings to indicate we should include selected rows. */
        private static final String LEGACY_INCLUDE_VALUE = "Include rows from reference table";

        @Override
        public List<ConfigMigration<IncludeOrExcludeRows>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(IncludeOrExcludeRowsMigration::load)
                .withDeprecatedConfigPath(LEGACY_KEY_INCLUDE_EXCLUDE).build());
        }

        static IncludeOrExcludeRows load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return LEGACY_INCLUDE_VALUE.equals(settings.getString(LEGACY_KEY_INCLUDE_EXCLUDE)) //
                ? IncludeOrExcludeRows.INCLUDE //
                : IncludeOrExcludeRows.EXCLUDE;
        }

    }
}
