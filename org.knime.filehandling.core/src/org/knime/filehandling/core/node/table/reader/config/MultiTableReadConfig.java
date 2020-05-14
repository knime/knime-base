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
 *   Feb 3, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.spec.TableSpecConfig;

/**
 * Configuration for the table readers that can jointly read tables from multiple sources.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of the {@link ReaderSpecificConfig}
 */
public interface MultiTableReadConfig<C extends ReaderSpecificConfig<C>> {

    /**
     * Returns the configuration for reading an individual table.
     *
     * @return the configuration for an individual table
     */
    TableReadConfig<C> getTableReadConfig();

    /**
     * Returns the merge mode to use for merging multiple table specs.
     *
     * @return the {@link SpecMergeMode} to use (intersection, union or fail)
     */
    SpecMergeMode getSpecMergeMode();

    /**
     * Sets the mode for merging specs of multiple sources.
     *
     * @param mode the mode to set
     */
    void setSpecMergeMode(SpecMergeMode mode);

    /**
     * Indicates whether a table spec is already provided, or has to be computed.
     *
     * @return <code>true</code> if the {@link TableSpecConfig} is available, {@code false} otherwise
     */
    boolean hasTableSpec();

    /**
     * Returns the {@link TableSpecConfig}. This method should only be invoked if {@link #hasTableSpec()} returned
     * {@code true}
     *
     * @return the {@link TableSpecConfig}
     */
    TableSpecConfig getTableSpecConfig();

    /**
     * Sets the {@link TableSpecConfig}
     *
     * @param config the {@link TableSpecConfig} to set
     */
    public void setTableSpecConfig(TableSpecConfig config);

    /**
     * Loads the configuration in the dialog.
     *
     * @param settings to load from
     * @param registry the {@link ProducerRegistry}
     */
    void loadInDialog(NodeSettingsRO settings, final ProducerRegistry<?, ?> registry);

    /**
     * Loads the configuration in the node model.
     *
     * @param settings to load from
     * @param registry the {@link ProducerRegistry}
     * @throws InvalidSettingsException if the settings are invalid or can't be loaded
     */
    void loadInModel(NodeSettingsRO settings, final ProducerRegistry<?, ?> registry) throws InvalidSettingsException;

    /**
     * Checks that this configuration can be loaded from the provided settings.
     *
     * @param settings to validate
     * @param registry the {@link ProducerRegistry}
     * @throws InvalidSettingsException if the settings are invalid
     */
    void validate(NodeSettingsRO settings, ProducerRegistry<?, ?> registry) throws InvalidSettingsException;

    /**
     * Saves the configuration to settings.
     *
     * @param settings to save to
     */
    void save(NodeSettingsWO settings);

}
