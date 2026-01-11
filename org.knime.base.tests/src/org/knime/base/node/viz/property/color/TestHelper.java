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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.viz.property.color;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.property.ColorAttr;
import org.knime.core.data.property.ColorGradient;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModel;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.data.property.ColorModelRange2;
import org.knime.core.data.property.ColorModelRange2.SpecialColorType;
import org.knime.core.node.BufferedDataTable;
import org.knime.testing.util.TableTestUtil;
import org.knime.testing.util.TableTestUtil.TableBuilder;

/**
 * Shared test utilities for {@link ColorPaletteDesignerNodeFactory} tests.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 */
final class TestHelper {

    private TestHelper() {
        // Utility class
    }

    /**
     * Creates a test table spec with two columns containing nominal values.
     *
     * @return a test table spec
     */
    static DataTableSpec createTestTableSpec() {
        final var col0Spec = new DataColumnSpecCreator("Column 0", StringCell.TYPE).createSpec();
        // Create columns with domains for nominal values
        final var col1Spec = createColumnWithDomain("Column 1", StringCell.TYPE, List.of("A", "B", "C"));

        final var col2Spec = createColumnWithDomain("Column 2", StringCell.TYPE, List.of("D", "A", "C"));
        final var col3Spec = new DataColumnSpecCreator("Column 3", IntCell.TYPE).createSpec();

        return new DataTableSpec(new DataColumnSpec[]{col0Spec, col1Spec, col2Spec, col3Spec});
    }

    static final String[] COLUMNS_WITH_DOMAIN = new String[]{"Column 1", "Column 2"};

    /**
     * Creates a test table spec with numeric columns for gradient testing.
     *
     * @return a test table spec with numeric columns
     */
    static DataTableSpec createNumericTestTableSpec() {
        final var col0Spec = TableTestUtil.createColumnSpecWithDomain(0.0, 50.0, "Double Column 1");
        final var col1Spec = TableTestUtil.createColumnSpecWithDomain(-50.0, 25.0, "Double Column 2");
        final var col2Spec = new DataColumnSpecCreator("Double Column 3", DoubleCell.TYPE).createSpec();
        final var col3Spec = new DataColumnSpecCreator("String Column", StringCell.TYPE).createSpec();

        return new DataTableSpec(new DataColumnSpec[]{col0Spec, col1Spec, col2Spec, col3Spec});
    }

    static final String[] NUMERIC_COLUMNS_WITH_DOMAIN = new String[]{"Double Column 1", "Double Column 2"};

    static final String[] NUMERIC_COLUMNS_WITHOUT_DOMAIN = new String[]{"Double Column 3"};

    /**
     * Creates a column spec with a domain containing the specified values.
     *
     * @param name the column name
     * @param type the data type
     * @param values the domain values
     * @return a column spec with domain
     */
    static DataColumnSpec createColumnWithDomain(final String name, final DataType type, final List<String> values) {
        final var specCreator = new DataColumnSpecCreator(name, type);
        final var domainValues = values.stream().map(StringCell::new).collect(Collectors.toSet());
        final DataColumnDomain domain = new DataColumnDomainCreator(domainValues).createDomain();
        specCreator.setDomain(domain);
        return specCreator.createSpec();
    }

    static BufferedDataTable createTestTable(final DataColumnSpec[] colSpecs, final Object[][] rowValues) {
        final var tableBuilder = new TableBuilder(new DataTableSpec(colSpecs));
        for (final var rowValue : rowValues) {
            tableBuilder.addRow(rowValue);
        }
        return tableBuilder.buildDataTable();
    }

    static final Map<DataCell, ColorAttr> INITIIAL_COLOR_MAP = Map.<DataCell, ColorAttr> of( //
        new StringCell("A"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[0], //
        new StringCell("B"), ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr()[1], //
        new MissingCell(null), ColorPaletteDesignerNodeFactory.hexToColorAttr("#808080"));

    static DataTableSpec createModelSpecWithNominalColorHandler() {
        final var colorModel = new ColorModelNominal(INITIIAL_COLOR_MAP,
            ColorPaletteOption.BREWER_SET1_COLORS9.getPaletteAsColorAttr(), Set.of(new MissingCell(null)));
        return createModelSpec(colorModel);
    }

    static DataTableSpec createModelSpecWithNumericPercentageColorHandler() {
        final var colorModel = new ColorModelRange2(SPECIAL_COLORS, ColorGradient.PURPLE_GREEN_5);
        return createModelSpec(colorModel);
    }

    static DataTableSpec createModelSpecWithNumericAbsoluteColorHandler() {
        final var colorModel = new ColorModelRange2(SPECIAL_COLORS, ColorGradient.PURPLE_GREEN_5).applyToDomain(0, 1);
        return createModelSpec(colorModel);
    }

    private static DataTableSpec createModelSpec(final ColorModel colorModel) {
        final var colorHandler = new ColorHandler(colorModel);
        final var colSpec = new DataColumnSpecCreator("Color", StringCell.TYPE).createSpec();
        final var colSpecWithHandler = new DataColumnSpecCreator(colSpec);
        colSpecWithHandler.setColorHandler(colorHandler);
        return new DataTableSpec(colSpecWithHandler.createSpec());
    }

    static final Map<SpecialColorType, Color> SPECIAL_COLORS = Map.of(//
        SpecialColorType.MISSING, Color.BLACK, //
        SpecialColorType.NAN, Color.DARK_GRAY, //
        SpecialColorType.NEGATIVE_INFINITY, Color.WHITE, //
        SpecialColorType.BELOW_MIN, Color.LIGHT_GRAY, //
        SpecialColorType.ABOVE_MAX, Color.GRAY, //
        SpecialColorType.POSITIVE_INFINITY, Color.YELLOW);
}
