/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.scorer.accuracy;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.CheckUtils;

/**
 * The hilite scorer node's model. The scoring is performed on two given columns set by the dialog. The row keys are
 * stored for later hiliting purpose.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz
 */
final class AccuracyScorer2NodeModel extends AbstractAccuracyScorerNodeModel {

    private static final DataColumnSpec[] QUALITY_MEASURES_SPECS =
        new DataColumnSpec[]{new DataColumnSpecCreator("TruePositives", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("FalsePositives", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("TrueNegatives", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("FalseNegatives", IntCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Recall", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Precision", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Sensitivity", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Specificity", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("F-measure", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Accuracy", DoubleCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Cohen's kappa", DoubleCell.TYPE).createSpec()};

    @Override
    DataColumnSpec[] getOutputSpecs() {
        return QUALITY_MEASURES_SPECS;
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        final DataTableSpec spec = inSpecs[INPORT];
        CheckUtils.checkSetting(!(spec.getNumColumns() < 2),
            "The input table must have at least two colums to compare");

        return super.configure(inSpecs);
    }
}
