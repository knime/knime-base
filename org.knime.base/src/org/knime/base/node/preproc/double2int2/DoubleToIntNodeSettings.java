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
 *   Jan 20, 2023 (jonasklotz): created
 */
package org.knime.base.node.preproc.double2int2;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.webui.node.dialog.impl.ChoicesProvider;
import org.knime.core.webui.node.dialog.impl.ColumnFilter;
import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.impl.Schema;
import org.knime.core.webui.node.dialog.persistence.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.persistence.field.Persist;

/**
 * Settings for the Web UI dialog of the Double to Int node. Double check backwards compatible loading if this class is
 * ever used in the NodeModel.
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin
 */
@SuppressWarnings("restriction")
final class DoubleToIntNodeSettings implements DefaultNodeSettings {

    @Persist(configKey = DoubleToIntNodeModel.CFG_INCLUDED_COLUMNS, settingsModel = SettingsModelColumnFilter2.class)
    @Schema(title = "Column Selection", description = "Move the columns of interest into the &quot;Includes&quot; list",
        choices = NumericalColumns.class, withTypes = false)
    ColumnFilter m_inclCols;

    @Persist(configKey = DoubleToIntNodeModel.CFG_LONG, settingsModel = SettingsModelBoolean.class)
    @Schema(title = "Create long values",
        description = "Use this option to generate 64bit long values instead of 32bit integer values. "
            + "This is useful if double values in the input are too big to fit into an integer.")
    boolean m_prodLong = false; //NOSONAR being explicit is desired here

    @Persist(customPersistor = RoundingOptionsPersistor.class)
    @Schema(title = "Rounding type", description = "The type of rounding applied to the selected double cells. "
        + "(Round: standard rounding, Floor: next smaller integer, Ceil: next bigger integer")
    RoundingOptions m_calctype;

    enum RoundingOptions {

            @Schema(title = "Round")
            ROUND,

            @Schema(title = "Floor")
            FLOOR,

            @Schema(title = "Ceil")
            CEIL;

    }

    private static final class NumericalColumns implements ChoicesProvider {

        @Override
        public String[] choices(final SettingsCreationContext context) {
            var spec = context.getDataTableSpecs()[0];
            if (spec != null) {
                return spec.stream()//
                    .filter(c -> include(c.getType()))//
                    .map(DataColumnSpec::getName)//
                    .toArray(String[]::new);
            }
            return new String[0];
        }

        private static boolean include(final DataType type) {
            return (type.isCompatible(DoubleValue.class) && !type.isCompatible(IntValue.class));
        }

    }

    private static final class RoundingOptionsPersistor implements NodeSettingsPersistor<RoundingOptions> {

        @Override
        public RoundingOptions load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var roundingOption = settings.getString(DoubleToIntNodeModel.CFG_TYPE_OF_ROUND);
            return RoundingOptions.valueOf(roundingOption.toUpperCase());
        }

        @Override
        public void save(final RoundingOptions obj, final NodeSettingsWO settings) {
            settings.addString(DoubleToIntNodeModel.CFG_TYPE_OF_ROUND, obj.name().toLowerCase());

        }

    }

}
