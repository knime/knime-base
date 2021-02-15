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
 *   Feb 9, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import static java.util.Arrays.asList;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.PATH1;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.PATH2;
import static org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigTestingUtils.ROOT_PATH;

import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.util.SerializeUtil;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Contains static methods and objects for testing TableSpecConfigSerializers before 4.4.0.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class Pre44TableSpecConfigSerializerTestUtils {

    static final TypedReaderColumnSpec<String> COL1 = TypedReaderColumnSpec.createWithName("A", "X", true);

    static final TypedReaderColumnSpec<String> COL2 = TypedReaderColumnSpec.createWithName("B", "Y", true);

    static final TypedReaderColumnSpec<String> COL3 = TypedReaderColumnSpec.createWithName("C", "Z", true);

    static final TypedReaderTableSpec<String> SPEC1 = new TypedReaderTableSpec<>(asList(COL1, COL2));

    static final TypedReaderTableSpec<String> SPEC2 = new TypedReaderTableSpec<>(asList(COL2, COL3));

    static final TypedReaderTableSpec<String> INTERSECTION = new TypedReaderTableSpec<>(asList(COL2));

    static final Map<String, TypedReaderTableSpec<String>> INDIVIDUAL_SPECS = createIndividualSpecs();

    private static Map<String, TypedReaderTableSpec<String>> createIndividualSpecs() {
        final LinkedHashMap<String, TypedReaderTableSpec<String>> specs = new LinkedHashMap<>();
        specs.put(PATH1, SPEC1);
        specs.put(PATH2, SPEC2);
        return specs;
    }

    private Pre44TableSpecConfigSerializerTestUtils() {
        // static utility class
    }

    static NodeSettings createSettings(final DataTableSpec tableSpec, final ProductionPath[] productionPaths,
        final String[] originalNames, final int[] positions, final boolean[] keep,
        final ColumnFilterMode columnFilterMode, final Boolean keepUnknown, final Integer unknownColumnPosition,
        final Boolean enforceTypes) {
        final NodeSettings settings = new NodeSettings("test");
        settings.addString("root_path_Internals", ROOT_PATH);
        tableSpec.save(settings.addNodeSettings("datatable_spec_Internals"));
        settings.addStringArray("file_paths_Internals", INDIVIDUAL_SPECS.keySet().stream().toArray(String[]::new));
        saveIndividualSpecs(INDIVIDUAL_SPECS, settings.addNodeSettings("individual_specs_Internals"));
        saveProductionPaths(settings.addNodeSettings("production_paths_Internals"), productionPaths);
        if (originalNames != null) {
            settings.addStringArray("original_names_Internals", originalNames);
        }
        if (positions != null) {
            settings.addIntArray("positional_mapping_Internals", positions);
        }
        if (keep != null) {
            settings.addBooleanArray("keep_Internals", keep);
        }
        if (columnFilterMode != null) {
            settings.addString("column_filter_mode_Internals", columnFilterMode.name());
        }
        if (keepUnknown != null) {
            settings.addBoolean("include_unknown_columns_Internals", keepUnknown);
        }
        if (unknownColumnPosition != null) {
            settings.addInt("unknown_column_position_Internals", unknownColumnPosition);
        }
        if (enforceTypes != null) {
            settings.addBoolean("enforce_types", enforceTypes);
        }
        return settings;
    }

    private static void saveProductionPaths(final NodeSettingsWO settings, final ProductionPath[] productionPaths) {
        // we mock the ProductionPathSerializer, so it doesn't actually serialize anything
        settings.addInt("num_production_paths_Internals", productionPaths.length);
        for (int i = 0; i < productionPaths.length; i++) {
            SerializeUtil.storeProductionPath(productionPaths[i], settings, "production_path_" + i);
        }
    }

    private static void saveIndividualSpecs(final Map<String, TypedReaderTableSpec<String>> individualSpecs,
        final NodeSettingsWO settings) {
        int i = 0;
        for (final ReaderTableSpec<? extends ReaderColumnSpec> readerTableSpec : individualSpecs.values()) {
            settings.addStringArray("individual_spec_" + i//
                , readerTableSpec.stream()//
                    .map(MultiTableUtils::getNameAfterInit)//
                    .toArray(String[]::new)//
            );
            i++;
        }
    }
}
