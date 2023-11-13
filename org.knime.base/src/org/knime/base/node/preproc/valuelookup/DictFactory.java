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
 *   9 Jan 2023 (jasper): created
 */
package org.knime.base.node.preproc.valuelookup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;

import org.knime.base.node.preproc.valuelookup.BinarySearchDict.SortingOrder;
import org.knime.base.node.preproc.valuelookup.UnsortedInputDict.IllegalLookupKeyException;
import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.MatchBehaviour;
import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.SearchDirection;
import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings.StringMatching;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.collection.CellCollection;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.message.Message;

/**
 * Factory class that, provided with the dictionary table and {@link ValueLookupNodeSettings} populates a suitable
 * {@code LookupDict} implementation and returns it.
 *
 * @author Jasper Krauter, KNIME GmbH, Konstanz, Germany
 */
final class DictFactory {

    private final ValueLookupNodeSettings m_settings;

    private final BufferedDataTable m_dictTable;

    private final Comparator<DataCell> m_comparator;

    private final ExecutionMonitor m_dictInitMon;

    private final long m_updateProgressPeriod;

    private final int[] m_dictOutputColIndices;

    private long m_processedDictRows;

    private DataRow m_currentRow;

    DictFactory(final ValueLookupNodeSettings settings, final BufferedDataTable dictTable,
            final int[] dictOutputColIndices, final Comparator<DataCell> comparator,
            final ExecutionMonitor dictInitMon) {
        m_settings = settings;
        m_dictTable = dictTable;
        m_dictOutputColIndices = dictOutputColIndices;
        m_comparator = comparator;
        m_dictInitMon = dictInitMon;
        m_updateProgressPeriod = m_dictTable.size() / 25 + 1; // This process will report ~25 discrete progress steps
    }

    LookupDict initialiseDict() throws CanceledExecutionException {
        final var dictSpec = m_dictTable.getSpec();
        final var dictKeyColIndex = dictSpec.findColumnIndex(m_settings.m_dictKeyCol);
        final var dictKeyColType = dictSpec.getColumnSpec(dictKeyColIndex).getType();

        ArrayList<DataCell> keyCache = new ArrayList<>();
        ArrayList<DataCell[]> valueCache = new ArrayList<>();
        try (var dictionaryIterator = m_dictTable.iterator()) {
            if (m_settings.m_stringMatchBehaviour == StringMatching.FULLSTRING && m_settings.m_caseSensitive
                && !dictKeyColType.isCollectionType()) {
                // Could try to do Binary Search
                var binSearchDict = tryToInitialiseBinarySearchDict(dictionaryIterator, dictKeyColIndex, keyCache,
                    valueCache);
                if (binSearchDict.isPresent()) {
                    return binSearchDict.get();
                }
            }

            // If we reach here, we don't want a binary search dict. We might have elements in out key-value caches that
            // need to be added to the new dictionary before we can continue iterating over the dictionaryIterator.
            var typeForDictImplementation =
                dictKeyColType.isCollectionType() ? dictKeyColType.getCollectionElementType() : dictKeyColType;
            var resultDict = getDictImplementation(typeForDictImplementation);

            try {
                // Add the elements from the cache to our new dictionary (will be empty if key column is collection)
                for (var i = 0; i < keyCache.size(); ++i) {
                    resultDict.insertSearchPair(keyCache.get(i), valueCache.get(i));
                }

                populateDictionary(resultDict, dictionaryIterator, dictKeyColIndex);
            } catch (IllegalLookupKeyException e) {
                // Most likely a PatternSyntaxException, or some other faulty data that couldn't be processed
                throw KNIMEException
                    .of(Message.fromRowIssue("Could not insert search pair in row '" + m_currentRow.getKey() + "'", 1,
                        m_processedDictRows, dictKeyColIndex, e.getMessage()), e)
                    .toUnchecked();
            }

            return resultDict;
        }
    }

    private void populateDictionary(final UnsortedInputDict resultDict, final CloseableRowIterator dictionaryIterator,
        final int dictKeyColIndex)
        throws CanceledExecutionException, IllegalLookupKeyException {
        while (dictionaryIterator.hasNext()) {
            m_currentRow = dictionaryIterator.next();
            DataCell input = m_currentRow.getCell(dictKeyColIndex);
            var outputs = Arrays.stream(m_dictOutputColIndices).mapToObj(m_currentRow::getCell)
                    .toArray(DataCell[]::new);
            if (input.isMissing()) {
                // Missing value is a collection type (it's compatible with all types), so handle that first
                resultDict.insertSearchPair(input, outputs);
            } else if (input.getType().isCollectionType()) {
                // Add an individual key-value pair for each entry in the collection type -- order doesn't matter
                var keys = (CellCollection)input;
                for (var key : keys) {
                    resultDict.insertSearchPair(key, outputs);
                }
            } else {
                resultDict.insertSearchPair(input, outputs);
            }
            reportProcessedDictRow();
        }
    }

    private UnsortedInputDict getDictImplementation(final DataType dictKeyColType) {
        if (m_settings.m_matchBehaviour == MatchBehaviour.EQUAL) {
            if (dictKeyColType.isCompatible(StringValue.class)) {
                switch (m_settings.m_stringMatchBehaviour) {
                    case FULLSTRING:
                        return new StringDict(m_settings);
                    case SUBSTRING:
                        return new SubstringDict(m_settings);
                    case WILDCARD:
                    case REGEX:
                        return new PatternDict(m_settings);
                    default:
                        throw new IllegalArgumentException(
                            "Unknown String Matching behaviour: " + m_settings.m_stringMatchBehaviour.toString());
                }
            } else {
                return new ExactDict(m_settings);
            }
        } else {
            return new ApproxDict(m_settings, m_comparator);
        }
    }

    /**
     * This method tries to initialise a Binary Search Dictionary, and while iterating over the input, writes all
     * entries to the key- and value-cache provided. If the input data is sorted either ascendingly or descendingly
     * (i.e. binary search is possible and meaningful), the resulting dictionary is returned. Otherwise,
     * {@code Optional.empty()} will be returned, and the iterator is now advanced by as many steps as there are now
     * elements in the cache.
     *
     * @param dictionaryIterator
     * @param dictKeyColIndex
     * @param dictKeyType
     * @param dictOutputColIndices
     * @param keyCache
     * @param valueCache
     * @param modelSettings
     * @return
     * @throws CanceledExecutionException
     */
    private Optional<BinarySearchDict> tryToInitialiseBinarySearchDict(final Iterator<DataRow> dictionaryIterator,
        final int dictKeyColIndex, final ArrayList<DataCell> keyCache,
        final ArrayList<DataCell[]> valueCache) throws CanceledExecutionException {
        var couldBeAscendinglySorted = true;
        var couldBeDescendinglySorted = true;

        DataCell lastKey = null;

        while (dictionaryIterator.hasNext()) {
            // Read the next row and key from the iterator, and (maybe) add it to the cache
            m_currentRow = dictionaryIterator.next();
            var key = m_currentRow.getCell(dictKeyColIndex);
            var values = Arrays.stream(m_dictOutputColIndices).mapToObj(m_currentRow::getCell).toArray(DataCell[]::new);

            // "compress" input data -- only add new entry if the key isn't already present
            // this prevents having to find the first / last item of a key later in the binary search
            var cmp = lastKey == null ? 0 : m_comparator.compare(lastKey, key);
            if (lastKey != null && cmp == 0) {
                // If search direction is forward, we don't want to add this item at all.
                // If backward, the key is the same so we only have to replace the values
                if (m_settings.m_searchDirection == SearchDirection.BACKWARD) {
                    valueCache.set(valueCache.size() - 1, values);
                }
            } else {
                // If the previous item has a different key, we definitely want to add it to our cache.
                keyCache.add(key);
                valueCache.add(values);
            }

            // Check which orderings are still possible
            couldBeAscendinglySorted &= cmp <= 0;
            couldBeDescendinglySorted &= cmp >= 0;
            if (!couldBeAscendinglySorted && !couldBeDescendinglySorted) {
                return Optional.empty(); // early return, input not sorted
            }

            reportProcessedDictRow();
            lastKey = key;
        }
        var sortingOrder = couldBeAscendinglySorted ? SortingOrder.ASC : SortingOrder.DESC;
        return Optional.of(new BinarySearchDict(m_settings, m_comparator, keyCache, valueCache, sortingOrder));
    }

    /**
     * Increments {@code m_processedDictRows} by one, updates the progress bar and checks for cancelled execution. See
     * {@link ExecutionMonitor#checkCanceled()}.
     *
     * @throws CanceledExecutionException
     */
    private void reportProcessedDictRow() throws CanceledExecutionException {
        ++m_processedDictRows;
        if (m_processedDictRows % m_updateProgressPeriod == 0) {
            m_dictInitMon.setProgress(m_processedDictRows / (double)m_dictTable.size(),
                "Reading dictionary into memory, row " + m_processedDictRows + " of " + m_dictTable.size());
            m_dictInitMon.checkCanceled(); // Might throw a CanceledExecutionException
        }
    }
}
