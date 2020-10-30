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
 *   Sep 2, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import java.nio.file.Path;
import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;

/**
 * Configuration storing all the information needed to create a {@link DataTableSpec}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface TableSpecConfig {

    /**
     * Returns the {@link TableTransformation} that allows to map the raw spec to the final KNIME spec.
     *
     * @param <T> the type used to identify external types
     * @return the {@link TableTransformation}
     */
    <T> TableTransformation<T> getTransformationModel();

    /**
     * Returns {@code true} if this config has been created with the provided <b>rootPath</b> and {@link List} of
     * {@link Path Paths}.
     *
     * @param rootPath string representation of the root path
     * @param paths the paths for which this {@link DefaultTableSpecConfig} has been configured
     * @return {@code true} if this config has been created with the provided parameters
     */
    boolean isConfiguredWith(String rootPath, List<Path> paths);

    /**
     * Returns {@code true} if this {@link DefaultTableSpecConfig} has been created using the provided <b>rootPath</b>,
     * {@code false} otherwise.
     *
     * @param rootPath the path to test if it has been used to create this {@link DefaultTableSpecConfig}
     * @return {@code true} if the {@link DefaultTableSpecConfig} has been created using the provded <b>rootPath</b>,
     *         {@code false} otherwise
     */
    boolean isConfiguredWith(String rootPath);

    /**
     * Returns {@code true} if this {@link DefaultTableSpecConfig} has been created using the provided <b>paths</b>,
     * {@code false} otherwise.
     *
     * @param paths the paths to test if they have been used to create this {@link DefaultTableSpecConfig}
     * @return {@code true} if the {@link DefaultTableSpecConfig} has been created using the provded <b>paths</b>,
     *         {@code false} otherwise
     */
    boolean isConfiguredWith(List<Path> paths);

    /**
     * Returns the {@link DataTableSpec}.
     *
     * @return the {@link DataTableSpec}
     */
    DataTableSpec getDataTableSpec();

    /**
     * Returns the {@link String} representation of the paths associated with each of the individual specs.
     *
     * @return the {@link String} representation of the paths to be read
     */
    List<String> getPaths();

    /**
     * Returns the {@link ReaderTableSpec} associated with the given path.
     *
     * @param path the path identifying the {@link ReaderTableSpec}
     * @return the associated {@link ReaderTableSpec}
     */
    ReaderTableSpec<?> getSpec(String path);//NOSONAR

    /**
     * Returns the {@link ProductionPath ProductionPaths} used to map the individual columns to their corresponding
     * {@link DataType DataTypes}.
     *
     * @return the {@link ProductionPath ProductionPaths} used for the type mapping
     */
    ProductionPath[] getProductionPaths();

    /**
     * Returns the configured {@link ColumnFilterMode}.
     *
     * @return the configured {@link ColumnFilterMode}
     */
    ColumnFilterMode getColumnFilterMode();

    /**
     * Saves the configuration to settings.
     *
     * @param settings to save to
     */
    void save(NodeSettingsWO settings);

}