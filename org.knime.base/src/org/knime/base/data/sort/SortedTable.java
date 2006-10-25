/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 * History
 *   23.10.2006 (sieb): created
 */
package org.knime.base.data.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.DataContainer;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;

/**
 * A data table that sorts a given data table according to the passed sorting
 * parameters.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class SortedTable implements DataTable {

    /**
     * Number of rows for each container.
     */
    private static final int CONTAINERSIZE = 2000;

    private BufferedDataTable m_sortedTable;

    /**
     * Hold the table spec to be used by the comparator inner class.
     */
    private DataTableSpec m_spec;

    /**
     * The included column indices.
     */
    private int[] m_indices;

    /**
     * The RowComparator to compare two DataRows (inner class).
     */
    private final RowComparator m_rowComparator = new RowComparator();

    /**
     * Array containing information about the sort order for each column. true:
     * ascending false: descending
     */
    private boolean[] m_sortOrder;

    /**
     * Creates a sorted table from the given table and the sorting parameters.
     * The table is sorted in the constructor.
     * 
     * @param dataTable the buffered data table to sort
     * @param inclList the list with the columns to sort; the first column name
     *            represents the first sort criteria, the second the second
     *            criteria and so on.
     * 
     * @param sortOrder the sort order; each field corresponds to the column in
     *            the list of included columns
     * 
     * @param exec the execution context used to create the the buffered data
     *            table and indicate the progress
     * 
     * @throws Exception if the parameters are not specified correctly
     */
    public SortedTable(final BufferedDataTable dataTable,
            final List<String> inclList, final boolean[] sortOrder,
            final ExecutionContext exec) throws Exception {

        m_spec = dataTable.getDataTableSpec();
        // get the column indices of the columns that will be sorted
        // also make sure that m_inclList and m_sortOrder both exist
        if (inclList == null) {
            throw new Exception("List of colums to include (incllist) is "
                    + "not set in the model");
        } else {
            m_indices = new int[inclList.size()];
        }

        m_sortOrder = sortOrder;
        if (m_sortOrder == null) {
            throw new Exception("Sortorder array is " + "not set in the model");
        }

        Vector<DataContainer> containerVector = new Vector<DataContainer>();
        int pos = -1;
        for (int i = 0; i < inclList.size(); i++) {
            String dc = inclList.get(i);
            pos = m_spec.findColumnIndex(dc);
            if (pos == -1) {
                throw new Exception("Could not find column name:"
                        + dc.toString());
            }
            m_indices[i] = pos;
        }
        // Initialize RowIterator
        RowIterator rowIt = dataTable.iterator();
        int nrRows = dataTable.getRowCount();
        int currentRowNr = 0;
        // wrap all DataRows in Containers of size containerSize
        // sort each container before it is'stored'.
        BufferedDataContainer newContainer =
                exec.createDataContainer(m_spec, false);
        int nrRowsinContainer = 0;
        ArrayList<DataRow> containerrowlist = new ArrayList<DataRow>();
        ExecutionMonitor subexec = exec.createSubProgress(.5);
        while (rowIt.hasNext()) {
            subexec.setProgress((double)currentRowNr / (double)nrRows,
                    "Reading in data... ");
            exec.checkCanceled();
            if (newContainer.isClosed()) {
                newContainer = exec.createDataContainer(m_spec, false);
                nrRowsinContainer = 0;
                containerrowlist = new ArrayList<DataRow>();
            }
            DataRow row = rowIt.next();
            currentRowNr++;
            nrRowsinContainer++;
            containerrowlist.add(row);
            if (nrRowsinContainer == CONTAINERSIZE) {
                exec.checkCanceled();
                // sort list
                DataRow[] temparray = new DataRow[containerrowlist.size()];
                temparray = containerrowlist.toArray(temparray);
                subexec.setMessage("Presorting Container");
                Arrays.sort(temparray, 0, temparray.length, m_rowComparator);
                // write in container
                for (int i = 0; i < temparray.length; i++) {
                    newContainer.addRowToTable(temparray[i]);
                }
                newContainer.close();
                containerVector.add(newContainer);
            }
        }
        if (nrRowsinContainer % CONTAINERSIZE != 0) {
            exec.checkCanceled();
            // sort list
            DataRow[] temparray = new DataRow[containerrowlist.size()];
            temparray = containerrowlist.toArray(temparray);
            Arrays.sort(temparray, 0, temparray.length, m_rowComparator);
            // write in container
            for (int i = 0; i < temparray.length; i++) {
                newContainer.addRowToTable(temparray[i]);
            }
            newContainer.close();
            containerVector.add(newContainer);
        }

        // merge all sorted containers together
        BufferedDataContainer mergeContainer =
                exec.createDataContainer(m_spec, false);

        // an array of RowIterators gives access to all (sorted) containers
        RowIterator[] currentRowIterators =
                new RowIterator[containerVector.size()];
        DataRow[] currentRowValues = new DataRow[containerVector.size()];

        // Initialize both arrays
        for (int c = 0; c < containerVector.size(); c++) {
            DataContainer tempContainer = containerVector.get(c);
            DataTable tempTable = tempContainer.getTable();
            currentRowIterators[c] = tempTable.iterator();
        }
        for (int c = 0; c < containerVector.size(); c++) {
            currentRowValues[c] = currentRowIterators[c].next();
        }
        int position = -1;

        // find the smallest/biggest element of all, put it in
        // mergeContainer
        ExecutionMonitor subexec2 = exec.createSubProgress(.5);
        for (int i = 0; i < currentRowNr; i++) {
            subexec2.setProgress((double)i / (double)currentRowNr, "Merging");
            exec.checkCanceled();
            position = findNext(currentRowValues);
            mergeContainer.addRowToTable(currentRowValues[position]);
            if (currentRowIterators[position].hasNext()) {
                currentRowValues[position] =
                        currentRowIterators[position].next();
            } else {
                currentRowIterators[position] = null;
                currentRowValues[position] = null;
            }
        }
        // Everything should be written out in the MergeContainer
        for (int i = 0; i < currentRowIterators.length; i++) {
            assert (currentRowValues[i] == null);
            assert (currentRowIterators[i] == null);
        }
        mergeContainer.close();
        BufferedDataTable dt = mergeContainer.getTable();
        assert (dt != null);
        m_sortedTable = dt;
    }

    /**
     * This method finds the next DataRow (position) that should be inserted in
     * the MergeContainer.
     */
    private int findNext(final DataRow[] currentValues) {
        int min = 0;
        while (currentValues[min] == null) {
            min++;
        }

        for (int i = min + 1; i < currentValues.length; i++) {
            if (currentValues[i] != null) {
                if (m_rowComparator.compare(currentValues[i],
                        currentValues[min]) < 0) {
                    min = i;
                }
            }
        }
        return min;
    }

    /**
     * @see org.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {

        return m_sortedTable.getDataTableSpec();
    }

    /**
     * @see org.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {

        return m_sortedTable.iterator();
    }

    /**
     * The private class RowComparator is used to compare two DataRows. It
     * implements the Comparator-interface, so we can use the Arrays.sort method
     * to sort an array of DataRows. If both DataRows are null they are
     * considered as equal. A null DataRow is considered as 'less than' an
     * initialized DataRow. On each position, the DataCells of the two DataRows
     * are compared with their compareTo-method.
     * 
     * @author Nicolas Cebron, University of Konstanz
     */
    private class RowComparator implements Comparator<DataRow> {

        /**
         * This method compares two DataRows based on a comparison for each
         * DataCell and the sorting order (m_sortOrder) for each column.
         * 
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         * @param dr1 one data row
         * @param dr2 another datarow to be compared with dr1
         * @return -1 if dr1 < dr2, 0 if dr1 == dr2 and 1 if dr1 > dr2
         */
        public int compare(final DataRow dr1, final DataRow dr2) {

            if (dr1 == dr2) {
                return 0;
            }
            if (dr1 == null) {
                return 1;
            }
            if (dr2 == null) {
                return -1;
            }

            assert (dr1.getNumCells() == dr2.getNumCells());

            for (int i = 0; i < m_indices.length; i++) {
                // only if the cell is in the includeList
                // same column means that they have the same type
                DataValueComparator comp =
                        m_spec.getColumnSpec(m_indices[i]).getType()
                                .getComparator();
                int cellComparison =
                        comp.compare(dr1.getCell(m_indices[i]), dr2
                                .getCell(m_indices[i]));

                if (cellComparison != 0) {
                    return (m_sortOrder[i] ? cellComparison : -cellComparison);
                }
            }
            return 0; // all cells in the DataRow have the same value
        }
    }

    /**
     * @return the sorted table as buffered data table
     */
    public BufferedDataTable getBufferedDataTable() {
        return m_sortedTable;
    }

    /**
     * @return the number of rows of this table
     */
    public int getRowCount() {
        return m_sortedTable.getRowCount();
    }
}
