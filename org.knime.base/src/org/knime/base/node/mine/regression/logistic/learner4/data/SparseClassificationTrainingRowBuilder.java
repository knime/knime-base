/*
 * ------------------------------------------------------------------------
 *
f *  Copyright by KNIME AG, Zurich, Switzerland
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
 *   23.05.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.NominalValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.util.CheckUtils;

/**
 * Builder object that creates sparse {@link ClassificationTrainingRow}s.
 *
 * @author Adrian Nembach, KNIME.com
 */
public final class SparseClassificationTrainingRowBuilder
    extends AbstractSparseClassificationTrainingRowBuilder<NominalValue> {

    private final Map<DataCell, Integer> m_targetDomain;

    /**
     * @param data the {@link BufferedDataTable} containing the data to learn on
     * @param pmmlSpec the spec of the PMML portobject
     * @param targetReferenceCategory the category for which the probability is not modeled explicitly
     * @param sortTargetCategories flag that indicates whether the target categories should be sorted lexicographically
     * @param sortFactorsCategories flag that indicates whether factor (nominal) features should be sorted
     *            lexicographically
     * @throws InvalidSettingsException if the provided settings are invalid or inconsistent
     */
    public SparseClassificationTrainingRowBuilder(final BufferedDataTable data, final PMMLPortObjectSpec pmmlSpec,
        final DataCell targetReferenceCategory, final boolean sortTargetCategories, final boolean sortFactorsCategories)
        throws InvalidSettingsException {
        super(data, pmmlSpec, sortFactorsCategories, NominalValue.class);
        final DataColumnSpec targetSpec = pmmlSpec.getTargetCols().get(0);
        final DataColumnDomain domain = targetSpec.getDomain();
        final Set<DataCell> possibleClasses = domain.getValues();
        if (possibleClasses == null) {
            throw new IllegalStateException("Calculate the possible values of " + targetSpec.getName());
        }
        Stream<DataCell> valueStream = possibleClasses.stream();
        if (sortTargetCategories) {
            valueStream = valueStream.sorted(targetSpec.getType().getComparator());
        }
        if (targetReferenceCategory != null) {
            // targetReferenceCategory must be the last element
            valueStream = Stream.concat(valueStream.filter(c -> !c.equals(targetReferenceCategory)),
                Stream.of(targetReferenceCategory));
        }
        m_targetDomain = new LinkedHashMap<>();
        valueStream.forEach(c -> m_targetDomain.put(c, m_targetDomain.size()));
        CheckUtils.checkSetting(m_targetDomain.size() == possibleClasses.size(),
            "The target reference category (\"%s\") is not found in the target column", targetReferenceCategory);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTargetDimension() {
        return m_targetDomain.size() - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ClassificationTrainingRow create(final float[] values, final int[] nonZeroFeatures, final int id,
        final NominalValue targetCell) {
        @SuppressWarnings("unlikely-arg-type") // targetCell is actually a cell
        final Integer target = m_targetDomain.get(targetCell);
        if (target == null) {
            throw new IllegalStateException(
                "DataCell \"" + targetCell.toString() + "\" is not in the DataColumnDomain of target column. "
                    + "Please apply a " + "Domain Calculator on the target column.");
        }
        return new SparseClassificationTrainingRow(values, nonZeroFeatures, id, target.intValue());
    }

}
