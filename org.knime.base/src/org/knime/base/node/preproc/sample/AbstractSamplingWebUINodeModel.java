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
 *   Apr 15, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.sample;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.node.preproc.filter.row.rowfilter.IRowFilter;
import org.knime.base.node.preproc.sample.AbstractSamplingNodeSettings.CountMode;
import org.knime.base.node.preproc.sample.AbstractSamplingNodeSettings.SamplingMode;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * Abstract WebUI node model for sampling rows of a table.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 * @param <T> The settings class
 * @since 5.5
 */
@SuppressWarnings("restriction")
public abstract class AbstractSamplingWebUINodeModel<T extends AbstractSamplingNodeSettings> extends WebUINodeModel<T> {

    /**
     * @param configuration
     * @param modelSettingsClass
     */
    protected AbstractSamplingWebUINodeModel(final WebUINodeConfiguration configuration,
        final Class<T> modelSettingsClass) {
        super(configuration, modelSettingsClass);
    }

    @Override
    protected void validateSettings(final T settings) throws InvalidSettingsException {
        final var countMode = settings.m_partitioningMode;
        switch (countMode) {
            case RELATIVE:
                validatePercentage(settings.m_percentage);
                break;
            case ABSOLUTE:
                validateCount(settings.m_rowCount);
                break;
            default:
                throw new InvalidSettingsException("Unknown method: " + countMode);
        }

        if (SamplingMode.STRATIFIED == settings.m_mode) {
            if (StringUtils.isBlank(settings.m_classColumn)) {
                throw new InvalidSettingsException("No class column for stratified sampling selected");
            }
            if (countMode == CountMode.ABSOLUTE && settings.m_rowCount > Integer.MAX_VALUE) {
                throw new InvalidSettingsException(
                    String.format("Stratified mode cannot be used with a row count bigger than %d", Integer.MAX_VALUE));
            }
        }
    }

    private static void validatePercentage(final double percentage) throws InvalidSettingsException {
        if (percentage < 0.0 || percentage > 100.0) {
            NumberFormat f = NumberFormat.getPercentInstance(Locale.US);
            String p = f.format(percentage);
            throw new InvalidSettingsException("Invalid percentage: " + p);
        }
    }

    //TODO Validation differs from Dialog validation, should be reevaluated with FEATKNAP-312
    private static void validateCount(final long count) throws InvalidSettingsException {
        if (count < 0) {
            throw new InvalidSettingsException("Invalid count: " + count);
        }
    }

    /**
     * Method to be used in the execute method to determine the row filter for the sampling.
     *
     * @param in the data table from the inport
     * @param exec the execution monitor to check for cancellation
     * @param settings the settings
     * @return a row filter for sampling according to current settings
     * @throws CanceledExecutionException if exec request canceling
     * @throws InvalidSettingsException if current settings are invalid
     */
    protected IRowFilter getSamplingRowFilter(final BufferedDataTable in, final ExecutionMonitor exec, final T settings)
        throws CanceledExecutionException, InvalidSettingsException {

        final var countMode = settings.m_partitioningMode;
        long rowCount = CountMode.RELATIVE == countMode ? //
            (long)((settings.m_percentage / 100.0) * in.size()) : //
            settings.m_rowCount;

        final var samplingMode = settings.m_mode;
        if (SamplingMode.RANDOM == samplingMode || SamplingMode.STRATIFIED == samplingMode) {
            Random rand;
            if (settings.m_seed.isPresent()) {
                rand = new Random(settings.m_seed.get());
            } else {
                //Creating a seed based on the current time and the hash code of the class.
                long seed = System.nanoTime() ^ (((long)hashCode() << 32) | (this.hashCode()));
                rand = new Random(seed);
                getLogger().debug("Using random seed " + seed);
            }
            if (SamplingMode.RANDOM == samplingMode) {
                return Sampler.createSampleFilter(in, rowCount, rand, exec);
            } else {
                if (rowCount > Integer.MAX_VALUE) {
                    throw new InvalidSettingsException(String
                        .format("Stratified mode cannot be used with a row count bigger than %d", Integer.MAX_VALUE));
                }
                return new StratifiedSamplingRowFilter(in, settings.m_classColumn, (int)rowCount, rand, exec);
            }
        }

        if (SamplingMode.LINEAR == samplingMode) {
            return new LinearSamplingRowFilter(in.size(), rowCount);
        }
        return Sampler.createRangeFilter(rowCount);

    }
}
