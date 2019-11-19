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
 *   6 Nov 2019 (Alexander): created
 */
package org.knime.base.node.mine.cluster.assign;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.base.node.mine.cluster.PMMLClusterTranslator;
import org.knime.base.node.mine.cluster.PMMLClusterTranslator.ComparisonMeasure;
import org.knime.base.predict.PMMLTablePredictor;
import org.knime.base.predict.PredictorContext;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.pmml.PMMLModelType;
import org.w3c.dom.Node;

/**
 * Class containing the predictor logic for cluster assignment.
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public final class PMMLClusterAssigner implements PMMLTablePredictor {

    private static final String NO_MODEL_FOUND_MSG = "No Clustering Model found.";

    private PMMLClusterAssignerOptions m_options;

    /**
     * Creates a new cluster assigner predictor with the given options.
     * @param options options controlling the output of the predictor
     */
    public PMMLClusterAssigner(final PMMLClusterAssignerOptions options) {
        m_options = options;
    }

    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(PMMLClusterAssigner.class);

    private ColumnRearranger createColumnRearranger(final PMMLPortObject port,
        final DataTableSpec inSpec) throws InvalidSettingsException {

        List<Node> models = port.getPMMLValue().getModels(PMMLModelType.ClusteringModel);
        if (models.isEmpty()) {
            LOGGER.error(NO_MODEL_FOUND_MSG);
            throw new RuntimeException(NO_MODEL_FOUND_MSG);
        }
        PMMLClusterTranslator trans = new PMMLClusterTranslator();
        port.initializeModelTranslator(trans);

        ComparisonMeasure measure = trans.getComparisonMeasure();
        List<Prototype> prototypes = new ArrayList<Prototype>();
        String[] labels = trans.getLabels();
        double[][] protos = trans.getPrototypes();
        for (int i = 0; i < protos.length; i++) {
            double[] prototype = protos[i];
            prototypes.add(new Prototype(prototype, new StringCell(labels[i])));
        }
        ColumnRearranger colre = new ColumnRearranger(inSpec);
        colre.append(new ClusterAssignFactory(measure, prototypes, createOutColumnSpec(inSpec, m_options),
            findLearnedColumnIndices(inSpec, trans.getUsedColumns())));
        return colre;
    }

    private static int[] findLearnedColumnIndices(final DataTableSpec ospec, final Set<String> learnedCols)
        throws InvalidSettingsException {
        int[] colIndices = new int[learnedCols.size()];
        int idx = 0;
        for (String s : learnedCols) {
            int i = ospec.findColumnIndex(s);
            if (i < 0) {
                throw new InvalidSettingsException(String.format("Column \"%s\" not found in data input spec.", s));
            }
            colIndices[idx++] = i;
        }
        return colIndices;
    }

    private static DataColumnSpec createOutColumnSpec(
        final DataTableSpec inSpec, final PMMLClusterAssignerOptions options) {
        String newColName = DataTableSpec.getUniqueColumnName(inSpec, options.getClusterColumnName());
        return new DataColumnSpecCreator(newColName, StringCell.TYPE)
            .createSpec();
    }

    private static class ClusterAssignFactory extends SingleCellFactory {
        private final ComparisonMeasure m_measure;
        private final List<Prototype> m_prototypes;
        private final int[] m_colIndices;

        /**
         * Constructor.
         * @param measure comparison measure
         * @param prototypes list of prototypes
         * @param newColspec the DataColumnSpec of the appended column
         * @param learnedCols columns used for training
         */
        ClusterAssignFactory(final ComparisonMeasure measure,
                final List<Prototype> prototypes,
                final DataColumnSpec newColspec,
                final int[] learnedCols) {
            super(newColspec);
            m_measure = measure;
            m_prototypes = prototypes;
            m_colIndices = learnedCols;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DataCell getCell(final DataRow row) {
            double mindistance = Double.MAX_VALUE;
            DataCell winnercell = DataType.getMissingCell();
            for (Prototype proto : m_prototypes) {
                double dist;
                // TODO: if prototypes are normalized
                // we have to normalize input data here
                if (m_measure.equals(ComparisonMeasure.squaredEuclidean)) {
                    dist = proto.getSquaredEuclideanDistance(row, m_colIndices);
                } else {
                    dist = proto.getDistance(row, m_colIndices);
                }
                if (dist < mindistance) {
                    mindistance = dist;
                    if (mindistance >= 0.0) {
                        winnercell = proto.getLabel();
                    }
                }
            }
            return winnercell;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BufferedDataTable predict(final BufferedDataTable input, final PMMLPortObject model,
        final PredictorContext ctx) throws Exception {
        ColumnRearranger colre = createColumnRearranger(model, input.getDataTableSpec());
        BufferedDataTable bdt = ctx.getExecutionContext().createColumnRearrangeTable(
            input, colre, ctx.getExecutionContext());
        return bdt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataTableSpec getOutputSpec(final DataTableSpec inputSpec, final PMMLPortObjectSpec modelSpec,
        final PredictorContext ctx) throws InvalidSettingsException {
        String newColName = DataTableSpec.getUniqueColumnName(inputSpec,
                m_options.getClusterColumnName());
        ColumnRearranger colre = new ColumnRearranger(inputSpec);

        colre.append(new ClusterAssignFactory(
                null, null,
                new DataColumnSpecCreator(newColName, StringCell.TYPE).createSpec(),
                findLearnedColumnIndices(inputSpec,
                        new HashSet<String>(modelSpec.getLearningFields()))));

        DataTableSpec out = colre.createSpec();
        return out;
    }

    /**
     * Options for the {@code PMMLClusterAssigner} predictor.
     * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
     */
    public static final class PMMLClusterAssignerOptions {

        private static final String DEFAULT_CLUSTER_COLUMN_NAME = "Cluster";

        private String m_clusterColumnName;

        /**
         * Creates a new {@code PMMLClusterAssignerOptions} instance with the default name for the cluster column.
         */
        public PMMLClusterAssignerOptions() {
            this(DEFAULT_CLUSTER_COLUMN_NAME);
        }

        /**
         * Creates a new options instance with the given name for the cluster column.
         * @param clusterColumnName the name for the column holding the calculated clusters
         * or null if default ("Cluster") should be used
         */
        public PMMLClusterAssignerOptions(final String clusterColumnName) {
            m_clusterColumnName = clusterColumnName == null ? DEFAULT_CLUSTER_COLUMN_NAME : clusterColumnName;
        }

        /**
         * @return the name for the column holding the calculated clusters
         */
        public String getClusterColumnName() {
            return m_clusterColumnName;
        }

    }
}
