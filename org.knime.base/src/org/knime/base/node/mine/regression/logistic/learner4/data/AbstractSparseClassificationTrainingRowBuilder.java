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
 *   Aug 30, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.data;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.util.CheckUtils;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
abstract class AbstractSparseClassificationTrainingRowBuilder <T extends DataValue>
    extends AbstractTrainingRowBuilder<ClassificationTrainingRow> {

    private final int m_targetIdx;

    private final Class<T> m_expectedType;

    AbstractSparseClassificationTrainingRowBuilder(final BufferedDataTable data,
        final PMMLPortObjectSpec pmmlSpec, final boolean sortFactorsCategories, final Class<T> expectedType) {
        super(data, pmmlSpec, sortFactorsCategories);
        final DataColumnSpec targetSpec = pmmlSpec.getTargetCols().get(0);
        m_expectedType = expectedType;
        m_targetIdx = data.getDataTableSpec().findColumnIndex(targetSpec.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final ClassificationTrainingRow createTrainingRow(final DataRow row, final int[] nonZeroFeatures,
        final float[] values, final int id) {
        final DataCell targetCell = row.getCell(m_targetIdx);
        final DataType type = targetCell.getType();
        CheckUtils.checkArgument(type.isCompatible(m_expectedType),
            "The DataCell %s is not of the expected type %s.", targetCell.toString(),
            m_expectedType.getSimpleName());
        @SuppressWarnings("unchecked") // explicitly checked above
        final T targetValue = (T)targetCell;

        CheckUtils.checkState(!targetCell.isMissing(), "The target column contains missing values in row with ID "
            + "\"%s\"; consider to use \"Missing Value\" node to fix it.", row.getKey().toString());
        return create(values, nonZeroFeatures, id, targetValue);
    }

    protected abstract ClassificationTrainingRow create(final float[] values, final int[] nonZeroFeatures, final int id,
        final T targetCell);

}
