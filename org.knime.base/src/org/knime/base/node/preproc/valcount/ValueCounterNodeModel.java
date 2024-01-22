/*
 * ------------------------------------------------------------------------
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
package org.knime.base.node.preproc.valcount;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.util.MutableInteger;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * This is the model for the value counter node that does all the work.
 *
 * @author Thorsten Meinl, University of Konstanz
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
final class ValueCounterNodeModel extends WebUINodeModel<ValueCounterNodeSettings> {

    private static final DataColumnSpec COL_SPEC = new DataColumnSpecCreator("count", IntCell.TYPE).createSpec();

    private static final DataTableSpec TABLE_SPEC = new DataTableSpec(COL_SPEC);

    private final HiLiteTranslator m_translator = new HiLiteTranslator();

    /**
     * Creates a new value counter model.
     *
     * @param config
     * @since 5.3
     */
    public ValueCounterNodeModel(final WebUINodeConfiguration config) {
        super(config, ValueCounterNodeSettings.class);
    }

    /**
     * @since 5.3
     */
    @Override
    protected void validateSettings(final ValueCounterNodeSettings settings) throws InvalidSettingsException {
        if (settings.m_columnName == null) {
            throw new InvalidSettingsException("No column selected.");
        }
    }

    /**
     * @throws InvalidSettingsException
     * @since 5.3
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final ValueCounterNodeSettings settings)
        throws InvalidSettingsException {
        validateSettings(settings);
        return new DataTableSpec[]{TABLE_SPEC};
    }

    /**
     * @since 5.3
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final ValueCounterNodeSettings settings) throws Exception {
        validateSettings(settings);
        final int colIndex = inData[0].getDataTableSpec().findColumnIndex(settings.m_columnName);
        final double max = inData[0].size();
        var rowCount = 0;
        final var hlMap = new HashMap<DataCell, Set<RowKey>>();
        final var countMap = new HashMap<DataCell, MutableInteger>();

        for (DataRow row : inData[0]) {
            exec.checkCanceled();
            exec.setProgress(rowCount / max, countMap.size() + " different values found");
            rowCount++;
            DataCell cell = row.getCell(colIndex);

            MutableInteger count = countMap.get(cell);
            if (count == null) {
                count = new MutableInteger(0);
                countMap.put(cell, count);
            }
            count.inc();

            if (settings.m_hiliting) {
                Set<RowKey> s = hlMap.get(cell);
                if (s == null) {
                    s = new HashSet<>();
                    hlMap.put(cell, s);
                }
                s.add(row.getKey());
            }
        }

        final DataValueComparator comp = inData[0].getDataTableSpec().getColumnSpec(colIndex).getType().getComparator();

        var sorted = new ArrayList<Map.Entry<DataCell, MutableInteger>>(countMap.entrySet());
        Collections.sort(sorted, (o1, o2) -> comp.compare(o1.getKey(), o2.getKey()));

        BufferedDataContainer cont = exec.createDataContainer(TABLE_SPEC);
        for (Map.Entry<DataCell, MutableInteger> entry : sorted) {
            final var newKey = new RowKey(entry.getKey().toString());
            cont.addRowToTable(new DefaultRow(newKey, entry.getValue().intValue()));
        }
        cont.close();

        if (settings.m_hiliting) {
            final var temp = new HashMap<RowKey, Set<RowKey>>();
            for (Map.Entry<DataCell, Set<RowKey>> entry : hlMap.entrySet()) {
                final var newKey = new RowKey(entry.getKey().toString());
                temp.put(newKey, entry.getValue());
            }
            m_translator.setMapper(new DefaultHiLiteMapper(temp));
        }
        return new BufferedDataTable[]{cont.getTable()};
    }

    /**
     * @since 5.3
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        final var f = new File(nodeInternDir, "Hiliting.conf.gz");
        if (f.exists() && f.canRead()) {
            try (final var in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(f)))) {
                NodeSettingsRO s = NodeSettings.loadFromXML(in);
                m_translator.setMapper(DefaultHiLiteMapper.load(s));
            } catch (InvalidSettingsException ex) {
                throw new IOException(ex);
            }
        }
    }

    /**
     * @since 5.3
     */
    @Override
    protected void reset() {
        m_translator.setMapper(null);
    }

    /**
     * @since 5.3
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        if (getSettings().map(s -> s.m_hiliting).orElse(false)) {
            final var s = new NodeSettings("Hiliting");
            ((DefaultHiLiteMapper)m_translator.getMapper()).save(s);
            final var f = new File(nodeInternDir, "Hiliting.conf.gz");
            try (final var out = new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
                s.saveToXML(out);
            }
        }
    }

    @Override
    protected void setInHiLiteHandler(final int inIndex, final HiLiteHandler hiLiteHdl) {
        m_translator.removeAllToHiliteHandlers();
        m_translator.addToHiLiteHandler(hiLiteHdl);
    }

    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_translator.getFromHiLiteHandler();
    }
}
