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
 *   May 2, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.mine.transformation.pca.settings;

import org.knime.base.node.mine.transformation.settings.TransformationApplySettings;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;

/**
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class PCAApplySettings extends TransformationApplySettings {

    /**
     * Configuration key for information preservation setting
     *
     * @since 5.9
     */
    public static final String INFORMATION_PRESERVATION_CFG = "information_preservation";

    /**
     * Configuration key for dimension selection setting
     *
     * @since 5.9
     */
    public static final String DIMENSION_SELECTED_CFG = "use_fixed_number_dimensions";

    private final SettingsModelDoubleBounded m_infPreservation =
        new SettingsModelDoubleBounded(INFORMATION_PRESERVATION_CFG, 100, 0.01, 100);

    private final SettingsModelBoolean m_useFixedDimensions = new SettingsModelBoolean(DIMENSION_SELECTED_CFG, true);

    /**
     * Returns the model storing the information preservation value.
     *
     * @return model storing the information preservation value
     */
    public SettingsModelDoubleBounded getInfPreservationModel() {
        return m_infPreservation;
    }

    /**
     * Returns the model storing the flag whether to use a defined (fixed) number of dimension or the information
     * preservation value.
     *
     * @return model storing the use fixed dimensions flag
     */
    public SettingsModelBoolean getUseFixedDimensionModel() {
        return m_useFixedDimensions;
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateSettings(settings);
        m_infPreservation.validateSettings(settings);
        m_useFixedDimensions.validateSettings(settings);
    }

    @Override
    public void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
        m_infPreservation.loadSettingsFrom(settings);
        m_useFixedDimensions.loadSettingsFrom(settings);
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_infPreservation.saveSettingsTo(settings);
        m_useFixedDimensions.saveSettingsTo(settings);
    }

}
