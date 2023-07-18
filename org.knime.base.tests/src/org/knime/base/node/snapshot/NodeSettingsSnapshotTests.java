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
 *   18 Jul 2023 (Rupert Ettrich): created
 */
package org.knime.base.node.snapshot;

import static org.knime.base.node.snapshot.TestTableSpecUtil.createDefaultTestTableSpec;

import java.util.Map;

import org.knime.base.node.flowvariable.tablerowtovariable3.TableToVariable3NodeSettings;
import org.knime.base.node.preproc.append.row.AppendedRowsNodeSettings;
import org.knime.base.node.preproc.colconvert.numbertostring2.NumberToStringSettings;
import org.knime.base.node.preproc.colconvert.stringtonumber2.StringToNumber2NodeSettings;
import org.knime.base.node.preproc.column.renamer.ColumnRenamerSettings;
import org.knime.base.node.preproc.columnappend2.ColumnAppenderSettings;
import org.knime.base.node.preproc.columnheaderextract.ColumnHeaderExtractorNodeSettings;
import org.knime.base.node.preproc.columnheaderinsert.ColumnHeaderInsertSettings;
import org.knime.base.node.preproc.columnlag.LagColumnNodeSettings;
import org.knime.base.node.preproc.columnmerge.ColumnMergerNodeSettings;
import org.knime.base.node.preproc.double2int2.DoubleToIntNodeSettings;
import org.knime.base.node.preproc.duplicates.DuplicateRowFilterDialogSettings;
import org.knime.base.node.preproc.filter.column.ColumnFilterNodeSettings;
import org.knime.base.node.preproc.filter.columnref.ColumnFilterRefNodeSettings;
import org.knime.base.node.preproc.filter.rowref.RowFilterRefNodeSettings;
import org.knime.base.node.preproc.rowagg.RowAggregatorSettings;
import org.knime.base.node.preproc.rowtocolumnheader.RowToColumnHeaderSettings;
import org.knime.base.node.preproc.stringreplacer.StringReplacerNodeSettings;
import org.knime.base.node.preproc.stringreplacer.dict2.StringReplacerDictNodeSettings;
import org.knime.base.node.preproc.table.cellextractor.CellExtractorSettings;
import org.knime.base.node.preproc.table.cellupdater.CellUpdaterSettings;
import org.knime.base.node.preproc.table.cropper.TableCropperSettings;
import org.knime.base.node.preproc.table.splitter.TableSplitterNodeSettings;
import org.knime.base.node.preproc.table.updater.TableUpdaterNodeSettings;
import org.knime.base.node.preproc.transpose.TransposeTableNodeSettings;
import org.knime.base.node.preproc.unpivot2.Unpivot2NodeSettings;
import org.knime.base.node.preproc.valuelookup.ValueLookupNodeSettings;
import org.knime.base.node.viz.format.number.NumberFormatManagerNodeSettings;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;

/**
 *
 * @author Rupert Ettrich
 */
@SuppressWarnings("restriction")
class NodeSettingsSnapshotTests { // NOSONAR

    static class AppendedRowsSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected AppendedRowsSettingsTest() {
            super(Map.of(SettingsType.MODEL, AppendedRowsNodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class CellExtractorSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected CellExtractorSettingsTest() {
            super(Map.of(SettingsType.MODEL, CellExtractorSettings.class), createDefaultTestTableSpec());
        }
    }

    static class CellUpdaterSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected CellUpdaterSettingsTest() {
            super(Map.of(SettingsType.MODEL, CellUpdaterSettings.class), createDefaultTestTableSpec(),
                createDefaultTestTableSpec());
        }
    }

    static class ColumnAppenderSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected ColumnAppenderSettingsTest() {
            super(Map.of(SettingsType.MODEL, ColumnAppenderSettings.class), createDefaultTestTableSpec());
        }
    }

    static class ColumnFilterNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected ColumnFilterNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, ColumnFilterNodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class ColumnFilterRefNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected ColumnFilterRefNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, ColumnFilterRefNodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class ColumnHeaderExtractorNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected ColumnHeaderExtractorNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, ColumnHeaderExtractorNodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class ColumnHeaderInsertSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected ColumnHeaderInsertSettingsTest() {
            super(Map.of(SettingsType.MODEL, ColumnHeaderInsertSettings.class), createDefaultTestTableSpec(),
                createDefaultTestTableSpec());
        }
    }

    static class ColumnMergerNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected ColumnMergerNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, ColumnMergerNodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class ColumnRenamerSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected ColumnRenamerSettingsTest() {
            super(Map.of(SettingsType.MODEL, ColumnRenamerSettings.class), createDefaultTestTableSpec());
        }
    }

    static class DoubleToIntNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected DoubleToIntNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, DoubleToIntNodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class DuplicateRowFilterDialogSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected DuplicateRowFilterDialogSettingsTest() {
            super(Map.of(SettingsType.MODEL, DuplicateRowFilterDialogSettings.class), createDefaultTestTableSpec());
        }
    }

    static class LagColumnNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected LagColumnNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, LagColumnNodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class NumberFormatManagerNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected NumberFormatManagerNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, NumberFormatManagerNodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class NumberToStringSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected NumberToStringSettingsTest() {
            super(Map.of(SettingsType.MODEL, NumberToStringSettings.class), createDefaultTestTableSpec());
        }
    }

    static class RowAggregatorSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected RowAggregatorSettingsTest() {
            super(Map.of(SettingsType.MODEL, RowAggregatorSettings.class), createDefaultTestTableSpec());
        }
    }

    static class RowFilterRefNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected RowFilterRefNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, RowFilterRefNodeSettings.class), createDefaultTestTableSpec(),
                createDefaultTestTableSpec());
        }
    }

    static class RowToColumnHeaderSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected RowToColumnHeaderSettingsTest() {
            super(Map.of(SettingsType.MODEL, RowToColumnHeaderSettings.class), createDefaultTestTableSpec());
        }
    }

    static class StringReplacerDictNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected StringReplacerDictNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, StringReplacerDictNodeSettings.class), createDefaultTestTableSpec(),
                createDefaultTestTableSpec());
        }
    }

    static class StringReplacerNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected StringReplacerNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, StringReplacerNodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class StringToNumber2NodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected StringToNumber2NodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, StringToNumber2NodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class TableCropperSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected TableCropperSettingsTest() {
            super(Map.of(SettingsType.MODEL, TableCropperSettings.class), createDefaultTestTableSpec());
        }
    }

    static class TableSplitterNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected TableSplitterNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, TableSplitterNodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class TableToVariable3NodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected TableToVariable3NodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, TableToVariable3NodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class TableUpdaterNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected TableUpdaterNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, TableUpdaterNodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class TransposeTableNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected TransposeTableNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, TransposeTableNodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class Unpivot2NodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected Unpivot2NodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, Unpivot2NodeSettings.class), createDefaultTestTableSpec());
        }
    }

    static class ValueLookupNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
        protected ValueLookupNodeSettingsTest() {
            super(Map.of(SettingsType.MODEL, ValueLookupNodeSettings.class), createDefaultTestTableSpec(),
                createDefaultTestTableSpec());
        }
    }

}
