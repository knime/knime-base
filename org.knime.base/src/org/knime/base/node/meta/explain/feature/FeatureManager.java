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
 *   Apr 4, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.ListDataValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.util.CheckUtils;

/**
 * Handles the mapping from columns to features. The need for this class arises because a single column can represent an
 * arbitrary number of features e.g. in the case of list or vector cells.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class FeatureManager {

    /**
     * The current feature spec
     */
    private DataTableSpec m_featureSpec;

    private final List<FeatureHandlerFactory> m_featureHandlerFactories = new ArrayList<>();

    private final boolean m_treatAllAsSingleFeature;

    private final boolean m_dontUseElementNames;

    private boolean m_namesFullyInitialized;

    private int m_numFeatures;

    private int[] m_numFeaturesPerCol;

    private final List<List<String>> m_names = new ArrayList<>();

    /**
     * @param treatCollectionsAsSingleFeature set to true if a collection corresponds to a single feature
     * @param dontUseElementNames set to true if the feature names of collection features should not use the element
     *            names
     */
    public FeatureManager(final boolean treatCollectionsAsSingleFeature, final boolean dontUseElementNames) {
        m_treatAllAsSingleFeature = treatCollectionsAsSingleFeature;
        m_dontUseElementNames = dontUseElementNames;
    }

    /**
     * In case of collection/vector columns it is not guaranteed that the number of features are known during the
     * configuration phase. This is reflected by this method which may return an empty optional if the number of
     * features is not known before execution. Note that it is also possible that the names change for collection
     * columns e.g. if a collection column turns out to contain more elements than claimed during configuration.
     *
     * @return an optional list of features
     */
    public Optional<List<String>> getFeatureNames() {
        checkInitialized();
        return m_namesFullyInitialized ? Optional.of(flattenNames()) : Optional.empty();
    }

    private List<String> flattenNames() {
        return m_names.stream().flatMap(c -> c.stream()).collect(Collectors.toList());
    }

    /**
     * Note that the number of features can change between configuration and execution because a collection column could
     * contain more (or less) features than claimed during configuration.
     *
     * @return the number of features if it is known
     * @see FeatureManager#getFeatureNames()
     */
    public OptionalInt getNumFeatures() {
        return m_namesFullyInitialized ? OptionalInt.of(m_numFeatures) : OptionalInt.empty();
    }

    /**
     * @return true if the feature columns contain any collection/vector columns
     */
    public boolean containsCollection() {
        return m_featureHandlerFactories.stream().anyMatch(FeatureHandlerFactory::handlesCollections);
    }

    /**
     * Intended for the use during configuration. Sets up the feature manager with the provided featureTableSpec
     *
     * @param featureTableSpec {@link DataTableSpec} containing only feature columns
     */
    public void updateWithSpec(final DataTableSpec featureTableSpec) {
        CheckUtils.checkNotNull(featureTableSpec);
        m_featureSpec = featureTableSpec;
        setupHandlers(featureTableSpec);
        tryToInitializeFeatureNames(featureTableSpec);
    }

    private void checkInitialized() {
        CheckUtils.checkState(!m_featureHandlerFactories.isEmpty(),
            "The FeatureManager has not been initialized, yet.");
    }

    /**
     * The number of features can change between configuration and execution (for collection/vector columns).
     *
     * @return an array containing the number of features per column
     */
    public int[] getNumberOfFeaturesPerColumn() {
        CheckUtils.checkState(m_namesFullyInitialized, "The number of features is currently unknown.");
        return m_numFeaturesPerCol.clone();
    }

    /**
     * @return a unmodifiable list of factories that can be used e.g. for perturbartion.
     */
    public List<FeatureHandlerFactory> getFactories() {
        return Collections.unmodifiableList(m_featureHandlerFactories);
    }

    /**
     * Intended for the use during execution.
     * Updates all information based on the actual from of rows.
     * This includes the verification and possible update of the feature names and counts for collection
     * columns.
     *
     * @param row a row from the input table
     * @return true if the configuration matches the execution state (i.e. same number of features)
     */
    public boolean updateWithRow(final DataRow row) {
        checkInitialized();
        CheckUtils.checkArgument(m_featureHandlerFactories.size() == row.getNumCells(),
            "The provided row %s has the wrong number of cells. Expected %s cells but row has %s cells.", row,
            m_featureHandlerFactories.size(), row.getNumCells());

        // TODO maybe we can make the FeatureHandlers more efficient by telling them the exact number of features to expect
        return updateNumFeatures(row);
    }

    /**
     * @param row containing features
     * @return true if the number of features in each cell of <b>row</b> matches the number of features expected
     */
    public boolean hasSameNumberOfFeatures(final DataRow row) {
        final int[] numFeaturesPerCell = countFeatures(row);
        return Arrays.equals(numFeaturesPerCell, m_numFeaturesPerCol);
    }

    /**
     * Updates the number of features based on {@link DataRow row} and returns whether the number of features calculated
     * during configuration matches the number of features contained in {@link DataRow row}.
     *
     * @param row any row from the feature table (usually the first one)
     * @return true if the number of features in <b> row matches the number of features calculated during configuration
     */
    private boolean updateNumFeatures(final DataRow row) {
        int[] numFeatures = countFeatures(row);
        if (!Arrays.equals(numFeatures, m_numFeaturesPerCol)) {
            m_numFeatures = Arrays.stream(numFeatures).sum();
            m_numFeaturesPerCol = numFeatures;
            updateFeatureNamesForInvalidColumns(numFeatures);
            return false;
        }
        return true;
    }

    private void updateFeatureNamesForInvalidColumns(final int[] numFeaturesPerCol) {
        for (int i = 0; i < getNumColumns(); i++) {
            final int actualFeatureCount = numFeaturesPerCol[i];
            final int numNames = m_names.get(i).size();
            if (actualFeatureCount != numNames) {
                // can only happen for collection columns
                final String columnName = m_featureSpec.getColumnSpec(i).getName();
                final List<String> featureNames = createCollectionNames(columnName, actualFeatureCount);
                m_names.set(i, featureNames);
            }
        }
        m_namesFullyInitialized = true;
    }

    private static List<String> createCollectionNames(final String columnName, final int numFeaturesInColumn) {
        return IntStream.range(0, numFeaturesInColumn).mapToObj(i -> columnName + "[" + i + "]")
            .collect(Collectors.toList());
    }

    /**
     * @param row
     * @return
     */
    private int[] countFeatures(final DataRow row) {
        final int numFactories = m_featureHandlerFactories.size();
        assert numFactories == row.getNumCells();
        final int[] numFeatures = new int[numFactories];
        try {
            for (int i = 0; i < m_featureHandlerFactories.size(); i++) {
                final DataCell cell = row.getCell(i);
                final FeatureHandlerFactory factory = m_featureHandlerFactories.get(i);
                numFeatures[i] = factory.numFeatures(cell);
            }
            return numFeatures;
        } catch (MissingValueException mve) {
            throw new IllegalArgumentException("Missing value in row " + row.getKey() + " detected.");
        }
    }

    private int getNumColumns() {
        return m_featureHandlerFactories.size();
    }

    private void tryToInitializeFeatureNames(final DataTableSpec featureTableSpec) {
        m_names.clear();
        m_numFeatures = 0;
        int numColumns = getNumColumns();
        m_numFeaturesPerCol = new int[numColumns];
        boolean namesFullyInitialized = true;
        for (int i = 0; i < numColumns; i++) {
            final DataColumnSpec colSpec = featureTableSpec.getColumnSpec(i);
            final FeatureHandlerFactory factory = m_featureHandlerFactories.get(i);
            List<String> names = factory.getFeatureNames(colSpec);
            if (names.isEmpty()) {
                namesFullyInitialized = false;
            } else if (m_dontUseElementNames && names.size() > 1) {
                names = createCollectionNames(colSpec.getName(), names.size());
            }
            m_names.add(names);
            m_numFeatures += names.size();
            m_numFeaturesPerCol[i] = names.size();
        }
        m_namesFullyInitialized = namesFullyInitialized;
    }

    private void setupHandlers(final DataTableSpec featureTableSpec) {
        m_featureHandlerFactories.clear();
        for (final DataColumnSpec colSpec : featureTableSpec) {
            final FeatureHandlerFactory featureHandlerFactory = getFeatureHandlerFactory(colSpec.getType());
            m_featureHandlerFactories.add(featureHandlerFactory);
        }
    }

    // TODO this could be handled via an extension point
    private FeatureHandlerFactory getFeatureHandlerFactory(final DataType colType) {
        if (!m_treatAllAsSingleFeature) {
            if (colType.isCompatible(BitVectorValue.class)) {
                return new BitVectorFeatureHandlerFactory();
            } else if (colType.isCompatible(ByteVectorValue.class)) {
                return new ByteVectorFeatureHandlerFactory();
            } else if (colType.isCompatible(ListDataValue.class)) {
                return new ListFeatureHandlerFactory();
            }
        }
        // we can treat any column as single feature
        return new SingleFeatureHandlerFactory();
    }

}
