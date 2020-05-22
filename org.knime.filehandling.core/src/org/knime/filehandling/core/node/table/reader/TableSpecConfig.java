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
 *   May 12, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.util.SerializeUtil;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.mapping.TypeMapping;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Configuration storing all the information needed to create a {@link DataTableSpec}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class TableSpecConfig {

    private static final String CFG_INDIVIDUAL_SPECS = "individual_specs" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_INDIVIDUAL_SPEC = "individual_spec_";

    private static final String CFG_ROOT_PATH = "root_path" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_FILE_PATHS = "file_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PRODUCTION_PATHS = "production_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_NUM_PRODUCTION_PATHS = "num_production_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_DATATABLE_SPEC = "datatable_spec" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PRODUCTION_PATH = "production_path_";

    private final String m_rootPath;

    private final Map<String, ReaderTableSpec<?>> m_individualSpecs;

    private final ProductionPath[] m_prodPaths;

    private final DataTableSpec m_dataTableSpec;

    /**
     * Constructor.
     *
     * @param rootPath if it represents a folder then all keys in the <b>individualSpecs<b> must be contained in this
     *            folder, otherwise the <b>rootPath</b> equals the {@link Path#toString()} version of the
     *            <b>individualSpecs<b> key and <b>individualSpecs<b> contains only a single element.
     * @param outputSpec the {@link DataTableSpec} resulting from merging the <b>individualSpecs</b> and applying the
     *            {@link TypeMapping}
     * @param individualSpecs a map from the path/file to its individual {@link ReaderTableSpec}
     * @param paths the production paths
     */
    public TableSpecConfig(final String rootPath, final DataTableSpec outputSpec,
        final Map<Path, ? extends ReaderTableSpec<?>> individualSpecs, final ProductionPath[] paths) {
        // check for nulls
        CheckUtils.checkNotNull(rootPath, "The rootPath cannot be null");
        CheckUtils.checkNotNull(individualSpecs, "The individual specs cannot be null");
        CheckUtils.checkNotNull(outputSpec, "The outputSpec cannot be null");
        CheckUtils.checkNotNull(paths, "The paths cannot be null");

        // check for size
        CheckUtils.checkArgument(!rootPath.trim().isEmpty(), "The rootPath cannot be empty");
        CheckUtils.checkArgument(!individualSpecs.isEmpty(), "The individual specs cannot be empty");
        CheckUtils.checkArgument(paths.length > 0, "The paths cannot be empty");

        m_rootPath = rootPath;
        m_dataTableSpec = outputSpec;
        m_individualSpecs = individualSpecs.entrySet().stream()//
            .collect(Collectors.toMap(//
                e -> e.getKey().toString()//
                , Map.Entry::getValue//
                , (x, y) -> y//
                , LinkedHashMap::new));
        m_prodPaths = paths.clone();
    }

    /**
     * Constructor.
     *
     * @param rootPath if it represents a folder then all <b>paths<b> must be contained in this folder, otherwise the
     *            <b>rootPath</b> equals the <b>paths[0]<b> and <b>paths<b> contains only a single element.
     * @param outputSpec the {@link DataTableSpec} resulting from merging the <b>individualSpecs</b> and applying the
     *            {@link TypeMapping}
     * @param paths the string representation of the paths associated with each individual spec
     * @param individualSpecs the individual {@link ReaderTableSpec ReaderTableSpecs}
     * @param productionPaths the {@link ProductionPath ProductionPaths}
     */
    private TableSpecConfig(final String rootPath, final DataTableSpec outputSpec, final String[] paths,
        final ReaderTableSpec<?>[] individualSpecs, final ProductionPath[] productionPaths) {
        m_rootPath = rootPath;
        m_dataTableSpec = outputSpec;
        m_individualSpecs = IntStream.range(0, paths.length)//
            .boxed()//
            .collect(Collectors.toMap(//
                i -> paths[i], //
                i -> individualSpecs[i], //
                (x, y) -> y, //
                LinkedHashMap::new));
        m_prodPaths = productionPaths;
    }

    /**
     * Returns {@code true} if this {@link TableSpecConfig} has been created using the provided <b>rootPath</b>,
     * {@code false} otherwise.
     *
     * @param rootPath the path to test if it has been used to create this {@link TableSpecConfig}
     * @return {@code true} if the {@link TableSpecConfig} has been created using the provded <b>rootPath</b>,
     *         {@code false} otherwise
     */
    boolean isConfiguredWith(final String rootPath) {
        return m_rootPath.equals(rootPath);
    }

    /**
     * Returns {@code true} if this {@link TableSpecConfig} has been created using the provided <b>paths</b>,
     * {@code false} otherwise.
     *
     * @param paths the paths to test if they have been used to create this {@link TableSpecConfig}
     * @return {@code true} if the {@link TableSpecConfig} has been created using the provded <b>paths</b>,
     *         {@code false} otherwise
     */
    boolean isConfiguredWith(final List<Path> paths) {
        return m_individualSpecs.size() == paths.size() //
            && paths.stream()//
                .map(Path::toString)//
                .allMatch(m_individualSpecs::containsKey);
    }

    /**
     * Returns the {@link DataTableSpec}.
     *
     * @return the {@link DataTableSpec}
     */
    DataTableSpec getDataTableSpec() {
        return m_dataTableSpec;
    }

    /**
     * Returns the {@link String} representation of the paths associated with each of the individual specs.
     *
     * @return the {@link String} representation of the paths to be read
     */
    List<String> getPaths() {
        return Collections.unmodifiableList(new ArrayList<>(m_individualSpecs.keySet()));
    }

    /**
     * Returns the {@link ReaderTableSpec} associated with the given path.
     *
     * @param path the path identifying the {@link ReaderTableSpec}
     * @return the associated {@link ReaderTableSpec}
     */
    ReaderTableSpec<?> getSpec(final String path) {
        return m_individualSpecs.get(path);
    }

    /**
     * Returns the {@link ProductionPath ProductionPaths} used to map the individual columns to their corresponding
     * {@link DataType DataTypes}.
     *
     * @return the {@link ProductionPath ProductionPaths} used for the type mapping
     */
    public ProductionPath[] getProductionPaths() {
        return m_prodPaths;
    }

    /**
     * Checks that this configuration can be loaded from the provided settings.
     *
     * @param settings to validate
     * @param registry the {@link ProducerRegistry}
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static void validate(final NodeSettingsRO settings, final ProducerRegistry<?, ?> registry)
        throws InvalidSettingsException {
        settings.getString(CFG_ROOT_PATH);
        DataTableSpec.load(settings.getNodeSettings(CFG_DATATABLE_SPEC));
        final int numIndivialPaths = settings.getStringArray(CFG_FILE_PATHS).length;
        validateIndividualSpecs(settings.getNodeSettings(CFG_INDIVIDUAL_SPECS), numIndivialPaths);
        validateProductionPaths(settings.getNodeSettings(CFG_PRODUCTION_PATHS), registry);
    }

    private static void validateIndividualSpecs(final NodeSettingsRO settings, final int numIndividualPaths)
        throws InvalidSettingsException {
        for (int i = 0; i < numIndividualPaths; i++) {
            settings.getStringArray(CFG_INDIVIDUAL_SPEC + i);
        }
    }

    private static void validateProductionPaths(final NodeSettingsRO settings, final ProducerRegistry<?, ?> registry)
        throws InvalidSettingsException {
        final int numProductionPaths = settings.getInt(CFG_NUM_PRODUCTION_PATHS);
        for (int i = 0; i < numProductionPaths; i++) {
            SerializeUtil.loadProductionPath(settings, registry, CFG_PRODUCTION_PATH + i);
        }
    }

    /**
     * Saves the configuration to settings.
     *
     * @param settings to save to
     */
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFG_ROOT_PATH, m_rootPath);
        m_dataTableSpec.save(settings.addNodeSettings(CFG_DATATABLE_SPEC));
        settings.addStringArray(CFG_FILE_PATHS, //
            m_individualSpecs.keySet().stream()//
                .toArray(String[]::new));
        saveIndivualSpecs(settings.addNodeSettings(CFG_INDIVIDUAL_SPECS));
        saveProductionPaths(settings.addNodeSettings(CFG_PRODUCTION_PATHS));
    }

    private void saveProductionPaths(final NodeSettingsWO settings) {
        int i = 0;
        for (final ProductionPath pP : m_prodPaths) {
            SerializeUtil.storeProductionPath(pP, settings, CFG_PRODUCTION_PATH + i++);
        }
        settings.addInt(CFG_NUM_PRODUCTION_PATHS, i);
    }

    private void saveIndivualSpecs(final NodeSettingsWO settings) {
        int i = 0;
        for (final ReaderTableSpec<?> readerTableSpec : m_individualSpecs.values()) {
            settings.addStringArray(CFG_INDIVIDUAL_SPEC + i++//
                , readerTableSpec.stream()//
                    .map(spec -> MultiTableUtils.getNameAfterInit(spec))//
                    .toArray(String[]::new)//
            );
        }
    }

    /**
     * De-serializes the {@link TableSpecConfig} previously written to the given settings.
     *
     * @param settings containing the serialized {@link TableSpecConfig}
     * @param registry the {@link ProducerRegistry}
     * @return the de-serialized {@link TableSpecConfig}
     * @throws InvalidSettingsException - if the settings do not exists / cannot be loaded
     */
    public static TableSpecConfig load(final NodeSettingsRO settings, final ProducerRegistry<?, ?> registry)
        throws InvalidSettingsException {
        final String rootPath = settings.getString(CFG_ROOT_PATH);
        final DataTableSpec dataTableSpec = DataTableSpec.load(settings.getConfig(CFG_DATATABLE_SPEC));
        final String[] paths = settings.getStringArray(CFG_FILE_PATHS);
        final ReaderTableSpec<?>[] individualSpecs =
            loadIndividualSpecs(settings.getNodeSettings(CFG_INDIVIDUAL_SPECS), paths.length);
        final ProductionPath[] prodPaths =
            loadProductionPaths(settings.getNodeSettings(CFG_PRODUCTION_PATHS), registry);
        return new TableSpecConfig(rootPath, dataTableSpec, paths, individualSpecs, prodPaths);
    }

    private static ReaderTableSpec<?>[] loadIndividualSpecs(final NodeSettingsRO nodeSettings,
        final int numIndividualPaths) throws InvalidSettingsException {
        final ReaderTableSpec<?>[] individualSpecs = new ReaderTableSpec[numIndividualPaths];
        for (int i = 0; i < numIndividualPaths; i++) {
            individualSpecs[i] = ReaderTableSpec
                .createReaderTableSpec(Arrays.asList(nodeSettings.getStringArray(CFG_INDIVIDUAL_SPEC + i)));
        }
        return individualSpecs;
    }

    private static ProductionPath[] loadProductionPaths(final NodeSettingsRO settings,
        final ProducerRegistry<?, ?> registry) throws InvalidSettingsException {
        final ProductionPath[] prodPaths = new ProductionPath[settings.getInt(CFG_NUM_PRODUCTION_PATHS)];
        for (int i = 0; i < prodPaths.length; i++) {
            final int idx = i;
            prodPaths[i] = SerializeUtil.loadProductionPath(settings, registry, CFG_PRODUCTION_PATH + i)
                .orElseThrow(() -> new InvalidSettingsException(
                    String.format("No production path associated with key <%s>", CFG_PRODUCTION_PATH + idx)));
        }
        return prodPaths;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_dataTableSpec == null) ? 0 : m_dataTableSpec.hashCode());
        result = prime * result + ((m_individualSpecs == null) ? 0 : m_individualSpecs.hashCode());
        result = prime * result + Arrays.hashCode(m_prodPaths);
        result = prime * result + ((m_rootPath == null) ? 0 : m_rootPath.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TableSpecConfig other = (TableSpecConfig)obj;
        if (!m_dataTableSpec.equals(other.m_dataTableSpec)) {
            return false;
        }
        if (!m_individualSpecs.equals(other.m_individualSpecs)) {
            return false;
        }
        if (!Arrays.equals(m_prodPaths, other.m_prodPaths)) {
            return false;
        }
        if (!m_rootPath.equals(other.m_rootPath)) {
            return false;
        }
        return true;
    }

}
