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
 *   22.07.2021 (loescher): created
 */
package org.knime.base.node.meta.looper.recursive;

import java.util.Arrays;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;

/**
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 */
final class RecursiveLoopEndDynamicNodeSettings {

    private final int m_nrRecusivePorts;
    private final int m_nrCollectionPorts;

    private int m_maxIterations = 100;

    private boolean m_endLoopDeprecated = false;

    private boolean m_useFlowVariable = false;

    private long[] m_minNumberOfRows;

    private boolean[] m_addIterationColumn;

    private boolean[] m_onlyLastData;

    private boolean m_propagateVariables = false;

    RecursiveLoopEndDynamicNodeSettings(final int recursivePorts, final int collectionPorts) {
        m_nrRecusivePorts = recursivePorts;
        m_nrCollectionPorts = collectionPorts;
        m_minNumberOfRows = new long[recursivePorts];
        Arrays.fill(m_minNumberOfRows, 1);
        m_addIterationColumn = new boolean[collectionPorts];
        m_onlyLastData = new boolean[collectionPorts];
    }

    /**
     * @return the maxIterations
     */
    int getMaxIterations() {
        return m_maxIterations;
    }

    /**
     * @param maxIterations the maxIterations to set
     */
    void setMaxIterations(final int maxIterations) {
        m_maxIterations = maxIterations;
    }

    /**
     * @return the endLoopDeprecated
     */
    boolean isEndLoopDeprecated() {
        return m_endLoopDeprecated;
    }

    /**
     * @param endLoopDeprecated the endLoopDeprecated to set
     */
    void setEndLoopDeprecated(final boolean endLoopDeprecated) {
        m_endLoopDeprecated = endLoopDeprecated;
    }

    /**
     * @return the useFlowVariable
     */
    boolean hasFlowVariable() {
        return m_useFlowVariable;
    }

    /**
     * @param useFlowVariable the useFlowVariable to set
     */
    void setUseFlowVariable(final boolean useFlowVariable) {
        m_useFlowVariable = useFlowVariable;
    }

    /**
     * @param index the index of the recursive table
     * @return the minNumberOfRows
     */
    long getMinNumberOfRows(final int index) {
        return m_minNumberOfRows[index];
    }

    /**
     * @param index the index of the recursive table
     * @param minNumberOfRows the minNumberOfRows to set
     */
    void setMinNumberOfRows(final int index, final long minNumberOfRows) {
        m_minNumberOfRows[index] = minNumberOfRows;
    }

    /**
     * @param index the index of the collection table
     * @return the addIterationColumn
     */
    boolean hasIterationColumn(final int index) {
        return m_addIterationColumn[index];
    }

    /**
     * @param index the index of the collection table
     * @param addIterationColumn the addIterationColumn to set
     */
    void setAddIterationColumn(final int index, final boolean addIterationColumn) {
        m_addIterationColumn[index] = addIterationColumn;
    }

    /**
     * @param index the index of the collection table
     * @return the onlyLastData
     */
    boolean hasOnlyLastData(final int index) {
        return m_onlyLastData[index];
    }

    /**
     * @param onlyLastData the onlyLastData to set
     */
    void setOnlyLastData(final int index, final boolean onlyLastData) {
        m_onlyLastData[index] = onlyLastData;
    }

    /**
     * @return the propagateVariables
     */
    public boolean isPropagateVariables() {
        return m_propagateVariables;
    }

    /**
     * @param propagateVariables the propagateVariables to set
     */
    public void setPropagateVariables(final boolean propagateVariables) {
        m_propagateVariables = propagateVariables;
    }

    void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt("maxNrIterations", m_maxIterations);
        settings.addBoolean("endLoop_deprecated", m_endLoopDeprecated);
        settings.addBoolean("useFlowVariable", m_useFlowVariable);
        settings.addBoolean("propagateFlowVariables", m_propagateVariables);
        settings.addLongArray("minNumberOfRows", m_minNumberOfRows);
        settings.addBooleanArray("addIterationColumn", m_addIterationColumn);
        settings.addBooleanArray("onlyLastData", m_onlyLastData);
    }

    void loadSettingsForDialog(final NodeSettingsRO settings) {
        final var maxIterations = settings.getInt("maxNrIterations", 100);
        m_maxIterations = maxIterations > 0 ? maxIterations : 100;
        m_endLoopDeprecated = settings.getBoolean("endLoop_deprecated", false);
        m_useFlowVariable = settings.getBoolean("useFlowVariable", false);
        m_propagateVariables = settings.getBoolean("propagateFlowVariables", false);
        m_minNumberOfRows = readLongArrayWithDefaults(settings, "minNumberOfRows", 1, 0, Long.MAX_VALUE);
        m_addIterationColumn = readBooleanArrayWithDefaults(settings, "addIterationColumn", false);
        m_onlyLastData = readBooleanArrayWithDefaults(settings, "onlyLastData", false);
    }

    void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_maxIterations = settings.getInt("maxNrIterations");
        CheckUtils.checkSetting(m_maxIterations > 0, "Maximum number of iterations must be positive (> 0)");
        m_endLoopDeprecated = settings.getBoolean("endLoop_deprecated", false); // this is deprecated anyway so we allow it to be absent
        m_useFlowVariable = settings.getBoolean("useFlowVariable");
        m_propagateVariables = settings.getBoolean("propagateFlowVariables");
        m_minNumberOfRows = readLongArray(settings, "minNumberOfRows", 1, 0, Long.MAX_VALUE);
        m_addIterationColumn = readBooleanArray(settings, "addIterationColumn", false);
        m_onlyLastData = readBooleanArray(settings, "onlyLastData", false);
    }




    private long[] readLongArray(final NodeSettingsRO settings, final String key, final long fillWith, final long min, final long max)
        throws InvalidSettingsException {
        final var result = new long[m_nrRecusivePorts];
        Arrays.fill(result, fillWith);
        final var read = settings.getLongArray(key);
        for (final var l : read) {
            CheckUtils.checkSetting(l >= min, "“%s” contains too small values! %d < %d", key, l, min);
            CheckUtils.checkSetting(l <= max, "“%s” contains too big values! %d > %d", key, l, max);
        }
        System.arraycopy(read, 0, result, 0, Math.min(read.length, m_nrRecusivePorts));
        if (result.length > read.length && read.length > 0) {
            Arrays.fill(result, read.length, result.length, read[read.length - 1]);
        }
        return result;
    }

    private long[] readLongArrayWithDefaults(final NodeSettingsRO settings, final String key, final long def, final long min, final long max) {
        final var result = new long[m_nrRecusivePorts];
        Arrays.fill(result, def);
        final var read = settings.getLongArray(key, new long[0]); // NOSONAR: has to call the varargs
        for (var i = 0; i < read.length; i++) {
            final var l = read[i];
            if (l < min || l > max) {
                read[i] = def;
            }
        }
        System.arraycopy(read, 0, result, 0, Math.min(read.length, m_nrRecusivePorts));
        if (result.length > read.length && read.length > 0) {
            Arrays.fill(result, read.length, result.length, read[read.length - 1]);
        }
        return result;
    }

    private boolean[] readBooleanArray(final NodeSettingsRO settings, final String key, final boolean fillWith)
        throws InvalidSettingsException {
        final var result = new boolean[m_nrCollectionPorts];
        Arrays.fill(result, fillWith);
        final var read = settings.getBooleanArray(key);
        System.arraycopy(read, 0, result, 0, Math.min(read.length, m_nrCollectionPorts));
        if (result.length > read.length && read.length > 0) {
            Arrays.fill(result, read.length, result.length, read[read.length - 1]);
        }
        return result;
    }

    private boolean[] readBooleanArrayWithDefaults(final NodeSettingsRO settings, final String key, final boolean def) {
        final var result = new boolean[m_nrCollectionPorts];
        Arrays.fill(result, def);
        final var read = settings.getBooleanArray(key, new boolean[0]); // NOSONAR: has to call the varargs
        System.arraycopy(read, 0, result, 0, Math.min(read.length, m_nrCollectionPorts));
        if (result.length > read.length && read.length > 0) {
            Arrays.fill(result, read.length, result.length, read[read.length - 1]);
        }
        return result;
    }

}
