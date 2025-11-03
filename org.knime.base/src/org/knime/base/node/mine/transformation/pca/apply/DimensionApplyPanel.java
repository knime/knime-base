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
 *   May 9, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.mine.transformation.pca.apply;

import java.text.DecimalFormat;

import org.apache.commons.math3.linear.RealVector;
import org.knime.base.node.mine.transformation.pca.perform.DimensionSelectionPanel;
import org.knime.base.node.mine.transformation.pca.settings.PCAApplySettings;
import org.knime.base.node.mine.transformation.port.TransformationPortObjectSpec;
import org.knime.base.node.mine.transformation.util.TransformationUtils;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.CheckUtils;

/**
 * The dimensions panel informing about the information preservation based on the number of selected columns and vice
 * versa.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
final class DimensionApplyPanel extends DimensionSelectionPanel {

    /** The unknown information preservation label. */
    static final String UNKNOWN_INFORMATION_PRESERVATION = "unknown information preservation";

    /** The unknown target dimensionality label. */
    static final String UNKNOWN_TARGET_DIMENSIONALITY = "unknown target dimensionality";

    /** The index of the {@link TransformationPortObjectSpec} port. */
    private final int m_transPortIdx;

    /** Decimal formatter. */
    private final DecimalFormat m_formater;

    /** The sorted eigenvalues. */
    private RealVector m_eigenVals;

    /** Flag used to enable/disable spinner correction. */
    private boolean m_replaceInvalidDimByMax;

    /**
     * Constructor.
     *
     * @param applySettings the {@code PCAApplySettings}
     * @param transPortIdx the index of the {@code TransformationPortObjectSpec} port.
     */
    DimensionApplyPanel(final PCAApplySettings applySettings, final int transPortIdx) {
        super(applySettings);
        CheckUtils.checkArgument(transPortIdx >= 0, "The transformation port index cannot be smaller than 0");
        m_formater = new DecimalFormat(".0");
        m_transPortIdx = transPortIdx;
        m_replaceInvalidDimByMax = false;
    }

    @Override
    protected void updateErrors() {
        if (m_replaceInvalidDimByMax && getDimToReduceTo() > getMaxDimToReduceTo()) {
            setDimToReduceTo(getMaxDimToReduceTo());
        }
        super.updateErrors();
        updateLbls();
    }

    private void setEigenVals(final RealVector eigenValues) {
        m_eigenVals = eigenValues;
        updateLbls();
    }

    private void updateLbls() {
        if (m_eigenVals == null) {
            setDimensionLblText(UNKNOWN_INFORMATION_PRESERVATION);
            setInfPreservationLblText(UNKNOWN_TARGET_DIMENSIONALITY);
        } else {
            updateDimensionLbl();
            updateInfPreservationLbl();
        }
    }

    private void updateDimensionLbl() {
        final int tgtDim = getDimToReduceTo();
        double norm = 0;
        double sum = 0;
        for (int i = 0; i < getMaxDimToReduceTo(); i++) {
            norm += m_eigenVals.getEntry(i);
            if (i < tgtDim) {
                sum = norm;
            }
        }
        setDimensionLblText(m_formater.format(100d * sum / norm) //NOSONAR eigenvalues > 0 and tgtDim > 0
            + "% information preservation");
    }

    private void updateInfPreservationLbl() {
        final int tgtDim = TransformationUtils.calcDimForGivenInfPreservation(m_eigenVals, getMaxDimToReduceTo(),
            getInfPreservationValue());
        setInfPreservationLblText(tgtDim + (tgtDim == 1 ? " dimension " : " dimensions ") + "in output");
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_replaceInvalidDimByMax = false;
        super.loadSettingsFrom(settings, specs);
        if (specs.length <= m_transPortIdx || !(specs[m_transPortIdx] instanceof TransformationPortObjectSpec)) {
            throw new NotConfigurableException("There is no TransformationPortObjectSpec at the defined index");
        }
        final TransformationPortObjectSpec transSpec = (TransformationPortObjectSpec)specs[m_transPortIdx];
        updateMaxDim(transSpec.getMaxDimToReduceTo());
        setEigenVals(transSpec.getEigenValues().orElse(null));
        m_replaceInvalidDimByMax = true;
    }

}
