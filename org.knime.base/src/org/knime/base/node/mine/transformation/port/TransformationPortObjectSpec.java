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
 *   May 7, 2019 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.mine.transformation.port;

import java.util.Optional;

import org.apache.commons.math3.linear.RealVector;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.Node;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;

/**
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @since 4.0
 */
public final class TransformationPortObjectSpec extends AbstractSimplePortObjectSpec {

    /**
     * The transformation type.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     * @since 4.0
     */
    public enum TransformationType {
            /** PCA */
            PCA("PCA", (byte)0)

            /** LDA */
            ,LDA("LDA", (byte)1);

        /** Configuration key for the transformation type. */
        private static final String TRANS_TYPE_KEY = "transformation_type";

        private final String m_colPrefix;

        /** The byte used to store the option. */
        private final byte m_persistByte;

        private TransformationType(final String colPrefix, final byte persistByte) {
            m_colPrefix = colPrefix + " dimension ";
            m_persistByte = persistByte;
        }

        /**
         * Returns the column prefix.
         *
         * @return the column prefix
         */
        public String getColPrefix() {
            return m_colPrefix;
        }

        /**
         * Save the selected transformation type.
         *
         * @param model the model to save to
         */
        void saveSettingsTo(final ModelContentWO model) {
            model.addByte(TRANS_TYPE_KEY, m_persistByte);
        }

        /**
         * Loads the {@link TransformationType}.
         *
         * @param model the model to load the {@code TransformationType} from
         * @return the loaded {@code TransformationType}
         * @throws InvalidSettingsException if the {@code TransformationType} couldn't be loaded
         */
        static TransformationType loadSettingsFrom(final ModelContentRO model) throws InvalidSettingsException {
            final byte persistByte = model.getByte(TRANS_TYPE_KEY);
            for (final TransformationType strategy : values()) {
                if (persistByte == strategy.m_persistByte) {
                    return strategy;
                }
            }
            throw new InvalidSettingsException("The transformation type could not be loaded.");
        }
    }

    /**
     * @noreference This class is not intended to be referenced by clients.
     */
    public static final class Serializer extends AbstractSimplePortObjectSpecSerializer<TransformationPortObjectSpec> {
    }

    private static final String INPUT_COL_NAMES_KEY = "input_column_names";

    private static final String MAX_DIM_KEY = "max_dim_to_reduce_to";

    private TransformationType m_transType;

    private String[] m_inputColNames;

    private int m_maxDimToReduceTo;

    private RealVector m_eigenValues = null;

    /**
     * Empty constructor.
     *
     * @noreference This constructor is not intended to be referenced by clients.
     */
    public TransformationPortObjectSpec() {

    }

    /**
     * Constructor.
     *
     * @param transType the {code TransformationType}
     * @param inputColNames the input column names
     * @param maxDimToReduceTo the maximum number of dimension to reduce to
     */
    public TransformationPortObjectSpec(final TransformationType transType, final String[] inputColNames,
        final int maxDimToReduceTo) {
        m_transType = transType;
        m_inputColNames = inputColNames;
        m_maxDimToReduceTo = maxDimToReduceTo;
    }

    /**
     * Returns the non-increasingly sorted eigenvalues. Can only return an empty {@link Optional} if this spec is not
     * associated with a {@link TransformationPortObject}, e.g., after a {@link Node#invokeNodeModelConfigure} call.
     *
     * @return the sorted eigenvalues
     */
    public Optional<RealVector> getEigenValues() {
        return Optional.ofNullable(m_eigenValues);
    }

    void setEigenValues(final RealVector eigenValues) {
        m_eigenValues = eigenValues;
    }

    /**
     * Returns the {link {@link TransformationType}
     *
     * @return the {@code TransformationType}
     */
    public TransformationType getTransformationType() {
        return m_transType;
    }

    /**
     * Returns the maximum number of dimensions to reduce to.
     *
     * @return the maximum number of dimensions to reduce to
     */
    public int getMaxDimToReduceTo() {
        return m_maxDimToReduceTo;
    }

    /**
     * Returns the input column names.
     *
     * @return the input column names
     */
    public String[] getInputColumnNames() {
        return m_inputColNames;
    }

    @Override
    protected void save(final ModelContentWO model) {
        m_transType.saveSettingsTo(model);
        model.addStringArray(INPUT_COL_NAMES_KEY, m_inputColNames);
        model.addInt(MAX_DIM_KEY, m_maxDimToReduceTo);
    }

    @Override
    protected void load(final ModelContentRO model) throws InvalidSettingsException {
        m_transType = TransformationType.loadSettingsFrom(model);
        m_inputColNames = model.getStringArray(INPUT_COL_NAMES_KEY);
        m_maxDimToReduceTo = model.getInt(MAX_DIM_KEY);
    }

}
