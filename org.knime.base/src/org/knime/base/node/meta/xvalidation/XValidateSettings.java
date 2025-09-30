/* Created on Jun 9, 2006 12:17:56 PM by thor
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
 * -------------------------------------------------------------------
 *
 */
package org.knime.base.node.meta.xvalidation;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Simple class for managing the cross validation settings.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class XValidateSettings {

    static final String CFG_VALIDATIONS = "validations";
    static final String CFG_RANDOM_SAMPLING = "randomSampling";
    static final String CFG_LEAVE_ONE_OUT = "leaveOneOut";
    static final String CFG_STRATIFIED_SAMPLING = "stratifiedSampling";
    static final String CFG_CLASS_COLUMN = "classColumn";
    static final String CFG_USE_RANDOM_SEED = "useRandomSeed";
    static final String CFG_RANDOM_SEED = "randomSeed";

    private short m_validations = 10;

    private boolean m_randomSampling = true;

    private boolean m_leaveOneOut = false;

    private boolean m_stratifiedSampling = false;

    private String m_classColumn;

    private long m_randomSeed;

    private boolean m_useRandomSeed;

    /**
     * Returns if leave-one-out cross validation should be performed.
     *
     * @return <code>true</code> if leave-one-out should be done,
     *         <code>false</code> otherwise
     */
    public boolean leaveOneOut() {
        return m_leaveOneOut;
    }

    /**
     * Sets if leave-one-out cross validation should be performed.
     *
     * @param b <code>true</code> if leave-one-out should be done,
     *            <code>false</code> otherwise
     */
    public void leaveOneOut(final boolean b) {
        m_leaveOneOut = b;
    }

    /**
     * Writes the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addShort(CFG_VALIDATIONS, m_validations);
        settings.addBoolean(CFG_RANDOM_SAMPLING, m_randomSampling);
        settings.addBoolean(CFG_LEAVE_ONE_OUT, m_leaveOneOut);
        settings.addBoolean(CFG_STRATIFIED_SAMPLING, m_stratifiedSampling);
        settings.addString(CFG_CLASS_COLUMN, m_classColumn);
        settings.addBoolean(CFG_USE_RANDOM_SEED, m_useRandomSeed);
        settings.addLong(CFG_RANDOM_SEED, m_randomSeed);
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_validations = settings.getShort(CFG_VALIDATIONS);
        m_randomSampling = settings.getBoolean(CFG_RANDOM_SAMPLING);
        m_leaveOneOut = settings.getBoolean(CFG_LEAVE_ONE_OUT);

        // added in v2.1
        m_stratifiedSampling = settings.getBoolean(CFG_STRATIFIED_SAMPLING, false);
        m_classColumn = settings.getString(CFG_CLASS_COLUMN, null);

        // added in 2.6
        m_useRandomSeed = settings.getBoolean(CFG_USE_RANDOM_SEED, false);
        m_randomSeed =
                settings.getLong(CFG_RANDOM_SEED, System.currentTimeMillis());
    }

    /**
     * Loads the settings from the node settings object using default values for
     * missing settings.
     *
     * @param settings a node settings object
     * @since 2.6
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) {
        m_validations = settings.getShort(CFG_VALIDATIONS, (short)10);
        m_randomSampling = settings.getBoolean(CFG_RANDOM_SAMPLING, true);
        m_leaveOneOut = settings.getBoolean(CFG_LEAVE_ONE_OUT, false);
        m_stratifiedSampling = settings.getBoolean(CFG_STRATIFIED_SAMPLING, false);
        m_classColumn = settings.getString(CFG_CLASS_COLUMN, null);
        m_useRandomSeed = settings.getBoolean(CFG_USE_RANDOM_SEED, false);
        m_randomSeed =
                settings.getLong(CFG_RANDOM_SEED, System.currentTimeMillis());
    }

    /**
     * Returns if the rows of the input table should be sampled randomly.
     *
     * @return <code>true</code> if the should be sampled randomly,
     *         <code>false</code> otherwise
     */
    public boolean randomSampling() {
        return m_randomSampling;
    }

    /**
     * Sets if the rows of the input table should be sampled randomly.
     *
     * @param randomSampling <code>true</code> if the should be sampled
     *            randomly, <code>false</code> otherwise
     */
    public void randomSampling(final boolean randomSampling) {
        m_randomSampling = randomSampling;
    }

    /**
     * Returns if the rows of the input table should be sampled stratified.
     *
     * @return <code>true</code> if the should be sampled stratified,
     *         <code>false</code> otherwise
     */
    public boolean stratifiedSampling() {
        return m_stratifiedSampling;
    }

    /**
     * Sets if the rows of the input table should be sampled stratified.
     *
     * @param stratifiedSampling <code>true</code> if the should be sampled
     *            stratified, <code>false</code> otherwise
     */
    public void stratifiedSampling(final boolean stratifiedSampling) {
        m_stratifiedSampling = stratifiedSampling;
    }

    /**
     * Returns the class column's name for stratified sampling.
     *
     * @return the class column's name
     */
    public String classColumn() {
        return m_classColumn;
    }

    /**
     * Sets the class column's name for stratified sampling.
     *
     * @param classColumn the class column's name
     */
    public void classColumn(final String classColumn) {
        m_classColumn = classColumn;
    }

    /**
     * Returns the number of validation runs that should be performed.
     *
     * @return the number of validation runs
     */
    public short validations() {
        return m_validations;
    }

    /**
     * Sets the number of validation runs that should be performed.
     *
     * @param validations the number of validation runs
     */
    public void validations(final short validations) {
        m_validations = validations;
    }

    /**
     * Returns if a pre-defined random seed should be used.
     *
     * @return <code>true</code> if a defined seed should be used,
     *         <code>false</code> if a random random seed should be use
     * @since 2.6
     */
    public boolean useRandomSeed() {
        return m_useRandomSeed;
    }

    /**
     * Sets if a pre-defined random seed should be used.
     *
     * @param b <code>true</code> if a defined seed should be used,
     *         <code>false</code> if a random random seed should be use
     * @since 2.6
     */
    public void useRandomSeed(final boolean b) {
        m_useRandomSeed = b;
    }


    /**
     * Returns the random seed used for random and stratified sampling.
     *
     * @return the seed
     * @since 2.6
     */
    public long randomSeed() {
        return m_randomSeed;
    }

    /**
     * Sets the random seed used for random and stratified sampling.
     *
     * @param value the seed
     * @since 2.6
     */
    public void randomSeed(final long value) {
        m_randomSeed = value;
    }
}
