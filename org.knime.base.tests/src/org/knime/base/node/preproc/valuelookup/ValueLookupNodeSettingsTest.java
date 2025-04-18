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
 *   8 Mar 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.valuelookup;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;

/**
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // WebUI* classes
class ValueLookupNodeSettingsTest {

    @Test
    void testColumnChoicesProvider() {
        final var dataTableSpec =
            new DataTableSpecCreator().addColumns(new DataColumnSpecCreator("Int1", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Double1", DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Bool1", BooleanCell.TYPE).createSpec(),
                new DataColumnSpecCreator("String1", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("Int2", IntCell.TYPE).createSpec()).createSpec();

        final var dictionaryTableSpec =
            new DataTableSpecCreator().addColumns(new DataColumnSpecCreator("DictInt1", IntCell.TYPE).createSpec(),
                new DataColumnSpecCreator("DictDouble1", DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator("DictBool1", BooleanCell.TYPE).createSpec(),
                new DataColumnSpecCreator("DictString1", StringCell.TYPE).createSpec(),
                new DataColumnSpecCreator("DictInt2", IntCell.TYPE).createSpec()).createSpec();

        final var ctx = DefaultNodeSettings
            .createDefaultNodeSettingsContext(new DataTableSpec[]{dataTableSpec, dictionaryTableSpec});

        // should pick all from data table (port 0)
        final var dataTableChoices = new ValueLookupNodeSettings.DataTableChoices().columnChoices(ctx).stream()
            .map(DataColumnSpec::getName).toArray(String[]::new);
        assertArrayEquals(dataTableSpec.getColumnNames(), dataTableChoices, "Wrong \"data table\" column choices.");

        // should pick all from dictionary table (port 1)
        final var dictTableChoices = new ValueLookupNodeSettings.DictionaryTableChoices().columnChoices(ctx).stream()
            .map(DataColumnSpec::getName).toArray(String[]::new);
        assertArrayEquals(dictionaryTableSpec.getColumnNames(), dictTableChoices,
            "Wrong \"dictionary table\" column choices.");
    }

}
