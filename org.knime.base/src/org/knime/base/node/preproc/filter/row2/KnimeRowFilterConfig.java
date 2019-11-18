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
 *   Jul 17, 2019 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row2;

import org.knime.base.data.filter.row.dialog.component.RowFilterConfig;
import org.knime.base.data.filter.row.dialog.model.GroupType;
import org.knime.base.data.filter.row.dialog.registry.OperatorRegistry;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Configuration for the KNIME Row Filter node.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class KnimeRowFilterConfig extends RowFilterConfig {

    static final String QUERY_DEFINES_INCLUDE = "queryDefinesInclude";

    private boolean m_queryDefinesInclude = true;

    /**
     * @return the queryDefinesInclude which shows whether the include option is selected or not.
     */
    boolean isQueryDefinesInclude() {
        return m_queryDefinesInclude;
    }

    /**
     * @param queryDefinesInclude is set based on the user choice in the dialog. In case include is selected then it is
     *            set to true, otherwise it is set to false.
     */
    void setQueryDefinesInclude(final boolean queryDefinesInclude) {
        m_queryDefinesInclude = queryDefinesInclude;
    }

    /**
     * @param configName keeps the configuration name of the panel.
     * @param groupTypes stores the possible group types: AND and OR in this case.
     */
    public KnimeRowFilterConfig(final String configName, final GroupType[] groupTypes) {
        super(configName, groupTypes);
    }

    /**
     * {@inheritDoc} Saves the settings defines in the RowFilterConfig to the settings file. It also saves the settings
     * for the added include radio button.
     */
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        settings.addBoolean(QUERY_DEFINES_INCLUDE, m_queryDefinesInclude);
    }

    /**
     * {@inheritDoc} Loads the settings from the settings file after validation.
     */
    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_queryDefinesInclude = settings.getBoolean(QUERY_DEFINES_INCLUDE);
    }

    /**
     * {@inheritDoc} Validates the settings saved in the settings file.
     */
    @Override
    public void validateSettings(final NodeSettingsRO settings, final DataTableSpec tableSpec,
        final OperatorRegistry<?> operatorRegistry) throws InvalidSettingsException {
        super.validateSettings(settings, tableSpec, operatorRegistry);
        settings.getBoolean(QUERY_DEFINES_INCLUDE);
    }
}
