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

import org.knime.base.node.preproc.filter.row.rowfilter.IRowFilter;
import org.knime.base.util.prepoc.sample.SamplingUtil.CountMode;
import org.knime.base.util.prepoc.sample.SamplingUtil.SamplingMode;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 * @since 5.5
 */
@SuppressWarnings("restriction")
public abstract class AbstractSamplingWebUINodeModel<T extends DefaultNodeSettings> extends WebUINodeModel<T> {

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
        final var countMode = getCountMode(settings);
        if (CountMode.RELATIVE.equals(countMode)) {
            final var percentage = getPercentage(settings);
            if (percentage < 0.0 || percentage > 100.0) {
                NumberFormat f = NumberFormat.getPercentInstance(Locale.US);
                String p = f.format(percentage);
                throw new InvalidSettingsException("Invalid percentage: " + p);
            }
        } else if (CountMode.ABSOLUTE.equals(countMode)) {
            final var count = getCount(settings);
            if (count < 0) {
                throw new InvalidSettingsException("Invalid count: " + count);
            }
        } else {
            throw new InvalidSettingsException("Unknown method: " + countMode);
        }

        if (SamplingMode.STRATIFIED.equals(getSamplingMode(settings)) && (getClassColumn(settings) == null)) {
            throw new InvalidSettingsException("No class column for stratified sampling selected");
        }
    }

    /**
     * @param in
     * @param exec
     * @param settings
     * @return
     * @throws CanceledExecutionException
     * @throws InvalidSettingsException
     */
    protected IRowFilter getSamplingRowFilter(final BufferedDataTable in, final ExecutionMonitor exec, final T settings)
        throws CanceledExecutionException, InvalidSettingsException {
        Random rand;
        final var samplingMode = getSamplingMode(settings);
        if (SamplingMode.RANDOM.equals(samplingMode)
            || SamplingMode.STRATIFIED.equals(samplingMode)) {
            if (getSeed(settings) != null) {
                rand = new Random(getSeed(settings));
            } else {
                long seed = System.nanoTime() ^ ((hashCode() << 32) + (this.hashCode()));
                rand = new Random(seed);
                getLogger().debug("Using random seed " + seed);
            }
        } else {
            rand = null;
        }

        int rowCount;
        final var countMode = getCountMode(settings);
        if (CountMode.RELATIVE.equals(countMode)) {
            rowCount = (int)((getPercentage(settings)/100.0) * in.size());
        } else {
            rowCount = getCount(settings);
        }

        IRowFilter rowFilter;
        if (SamplingMode.RANDOM.equals(samplingMode)) {
            rowFilter = Sampler.createSampleFilter(in, rowCount, rand, exec);
        } else if (SamplingMode.STRATIFIED.equals(samplingMode)) {
            rowFilter = new StratifiedSamplingRowFilter(in, getClassColumn(settings), rowCount, rand, exec);
        } else if (SamplingMode.LINEAR.equals(samplingMode)) {
            rowFilter = new LinearSamplingRowFilter(in.size(), rowCount);
        } else {
            rowFilter = Sampler.createRangeFilter(rowCount);
        }
        return rowFilter;
    }

    /**
     * @param settings
     * @return the count mode
     */
    protected abstract CountMode getCountMode(T settings);

    /**
     * @param settings
     * @return
     */
    protected abstract double getPercentage(T settings);

    /**
     * @param settings
     * @return
     */
    protected abstract int getCount(T settings);

    /**
     * @param settings
     * @return
     */
    protected abstract SamplingMode getSamplingMode(T settings);

    /**
     * @param settings
     * @return
     */
    protected abstract String getClassColumn(T settings);

    /**
     * @param settings
     * @return
     */
    protected abstract Long getSeed(T settings);
}
