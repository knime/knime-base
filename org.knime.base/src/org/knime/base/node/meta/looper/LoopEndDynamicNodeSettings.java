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
 * ---------------------------------------------------------------------
 *
 * History
 *   15.05.2009 (meinl): created
 */
package org.knime.base.node.meta.looper;

import java.util.Arrays;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This class holds the settings for the generic loop end node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author based on {@link LoopEndNodeSettings} by Thorsten Meinl, University of Konstanz
 * @since 4.5
 */
final class LoopEndDynamicNodeSettings extends AbstractLoopEndNodeSettings {

    private boolean[] m_ignoreEmptyTables;

    private boolean[] m_tolerateColumnTypes;

    private boolean[] m_tolerateChangingSpecs;

    private final int m_numberOfPorts;


    LoopEndDynamicNodeSettings(final int numberOfPorts) {
        m_numberOfPorts = numberOfPorts;
        m_ignoreEmptyTables = new boolean[numberOfPorts];
        Arrays.fill(m_ignoreEmptyTables, true);
        m_tolerateColumnTypes = new boolean[numberOfPorts];
        m_tolerateChangingSpecs = new boolean[numberOfPorts];
    }



    /**
     * Sets if iterations with empty tables are ignored in the output.
     *
     * @param index the index of the table for which these settings should be set
     * @param ignore <code>true</code> empty tables will be ignored,
     *               <code>false</code> empty tables that have different specs will cause an exception
     */
    void ignoreEmptyTables(final int index, final boolean ignore) {
        m_ignoreEmptyTables[index] = ignore;
    }

    /**
     * Returns if iterations with empty tables are ignored in the output.
     *
     * @param index the index of the table for which these settings should be set
     * @return <code>true</code> empty tables will be ignored,
     *         <code>false</code> empty tables that have different specs will cause an exception
     */
    boolean ignoreEmptyTables(final int index) {
        return m_ignoreEmptyTables[index];
    }

    /**
     * Sets if column types in different tables are merged.
     * @param index the index of the table for which these settings should be set
     * @param tolerate <code>true</code> merge columns types,
     *                 <code>false</code> don't merge column types.
     */
    void tolerateColumnTypes(final int index, final boolean tolerate) {
        m_tolerateColumnTypes[index] = tolerate;
    }

    /**
     * Returns if column types in different tables are merged.
     * @param index the index of the table for which these settings should be set
     * @return tolerate <code>true</code> merge columns types,
     *                  <code>false</code> don't merge column types.
     */
    boolean tolerateColumnTypes(final int index) {
        return m_tolerateColumnTypes[index];
    }


    /**
     * Returns if changing tables specs are to be tolerated
     *
     * @param index the index of the table for which these settings should be set
     * @param tolerate <code>true</code> changes are tolerated and missing values are inserted for missing column in
     *            respective iterations <code>false</code> the node fails if table spec varies
     */
    void tolerateChangingTableSpecs(final int index, final boolean tolerate) {
        m_tolerateChangingSpecs[index] = tolerate;
    }

    /**
     * Returns if changing tables specs are to be tolerated
     *
     * @param index the index of the table for which these settings should be set
     * @return <code>true</code> changes are tolerated and missing values are inserted for missing column in respective
     *         iterations
     *         <code>false</code> the node fails if table spec varies
     */
    boolean tolerateChangingTableSpecs(final int index) {
        return m_tolerateChangingSpecs[index];
    }

    /**
     * Writes the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    @Override
    public void saveSettings(final NodeSettingsWO settings) {
        super.saveSettings(settings);
        settings.addBooleanArray("ignoreEmptyTables", m_ignoreEmptyTables);
        settings.addBooleanArray("tolerateColumnTypes", m_tolerateColumnTypes);
        settings.addBooleanArray("tolerateChangingSpecs", m_tolerateChangingSpecs);
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     */
    @Override
    public void loadSettings(final NodeSettingsRO settings) {
        super.loadSettings(settings);
        m_ignoreEmptyTables = readWithDefaults(settings, "ignoreEmptyTables");
        m_tolerateColumnTypes = readWithDefaults(settings, "tolerateColumnTypes");
        m_tolerateChangingSpecs = readWithDefaults(settings, "tolerateChangingSpecs");
    }

    private boolean[] readWithDefaults(final NodeSettingsRO settings, final String key) {
        final var result = new boolean[m_numberOfPorts];
        final var read = settings.getBooleanArray(key, new boolean[0]); // NOSONAR: has to call the varargs
        System.arraycopy(read, 0, result, 0, Math.min(read.length, m_numberOfPorts));
        if (result.length > read.length && read.length > 0) {
            Arrays.fill(result, read.length, result.length, read[read.length-1]);
        }
        return result;
    }
}
