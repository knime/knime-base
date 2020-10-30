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

import java.nio.file.Path;
import java.util.List;

import org.knime.filehandling.core.node.table.reader.SpecMergeMode;

/**
 * Configuration for the table readers that can jointly read tables from multiple sources.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of the {@link ReaderSpecificConfig}
 * @noreference non-public API
 * @noimplement non-public API
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
     * @deprecated only used as fallback if no {@link TableSpecConfig} is available
     */
    @Deprecated
    SpecMergeMode getSpecMergeMode();

    /**
     * Indicates whether the node should fail if the table specs differ.
     *
     * @return {@code true} if the node should fail on differing specs
     */
    boolean failOnDifferingSpecs();

    /**
     * Indicates whether a table spec is already provided, or has to be computed.
     *
     * @return <code>true</code> if the {@link DefaultTableSpecConfig} is available, {@code false} otherwise
     */
    boolean hasTableSpecConfig();

    /**
     * Returns the {@link DefaultTableSpecConfig}. This method should only be invoked if {@link #hasTableSpecConfig()}
     * returned {@code true}
     *
     * @return the {@link DefaultTableSpecConfig}
     */
    TableSpecConfig getTableSpecConfig();

    /**
     * Sets the {@link DefaultTableSpecConfig}
     *
     * @param config the {@link DefaultTableSpecConfig} to set
     */
    void setTableSpecConfig(TableSpecConfig config);

    /**
     * Returns {@code true} if the {@link DefaultTableSpecConfig} has been created with the provided <b>rootPath</b> and
     * {@link List} of {@link Path Paths}.
     *
     * @param rootPath string representation of the root path
     * @param paths the paths for which the {@link DefaultTableSpecConfig} has been configured
     * @return {@code true} if the {@link DefaultTableSpecConfig} is present and has been created with the provided
     *         parameters
     */
    default boolean isConfiguredWith(final String rootPath, final List<Path> paths) {
        return hasTableSpecConfig() && getTableSpecConfig().isConfiguredWith(rootPath, paths);
    }
}
