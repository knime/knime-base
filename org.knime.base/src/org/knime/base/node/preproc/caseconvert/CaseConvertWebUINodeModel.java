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
 * -------------------------------------------------------------------
 */
package org.knime.base.node.preproc.caseconvert;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

@SuppressWarnings("restriction")
final class CaseConvertWebUINodeModel extends WebUINodeModel<CaseConvertNodeSettings> {

    CaseConvertWebUINodeModel(final WebUINodeConfiguration config,
        final Class<CaseConvertNodeSettings> modelSettingsClass) {
        super(config, modelSettingsClass);
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
        final CaseConvertNodeSettings s) throws Exception {
        final var spec = inData[0].getDataTableSpec();
        final var rearranger = createColumnRearranger(spec, s);
        final var replaced = exec.createColumnRearrangeTable(inData[0], rearranger, exec);
        return new BufferedDataTable[]{replaced};
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final CaseConvertNodeSettings s)
        throws InvalidSettingsException {
        if (s.m_columns == null || s.m_columns.length == 0) {
            setWarningMessage("No columns selected");
            return new DataTableSpec[]{inSpecs[0]};
        }
        // Validate columns exist
        final var missing = new java.util.ArrayList<String>();
        for (final var name : s.m_columns) {
            if (inSpecs[0].findColumnIndex(name) < 0) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            throw new InvalidSettingsException("The input table has changed. Some columns are missing: "
                + org.knime.core.node.util.ConvenienceMethods.getShortStringFrom(missing, 3));
        }
        return new DataTableSpec[]{createColumnRearranger(inSpecs[0], s).createSpec()};
    }

    private ColumnRearranger createColumnRearranger(final DataTableSpec inSpec, final CaseConvertNodeSettings s)
        throws InvalidSettingsException {
        final var indices = new int[s.m_columns.length];
        for (int i = 0; i < indices.length; i++) {
            final int colIdx = inSpec.findColumnIndex(s.m_columns[i]);
            if (colIdx < 0) {
                throw new InvalidSettingsException("Column index for " + s.m_columns[i] + " not found.");
            }
            indices[i] = colIdx;
        }
        final var colre = new ColumnRearranger(inSpec);
        colre.replace(new ConverterFactory(indices, inSpec, s.m_uppercase), indices);
        return colre;
    }

    private static final class ConverterFactory implements CellFactory {
        private final int[] m_colindices;
        private final DataTableSpec m_spec;
        private final Locale m_locale = Locale.getDefault();
        private final boolean m_uppercase;

        ConverterFactory(final int[] colindices, final DataTableSpec spec, final boolean uppercase) {
            m_colindices = colindices;
            m_spec = spec;
            m_uppercase = uppercase;
        }

        @Override
        public DataCell[] getCells(final DataRow row) {
            final var newcells = new DataCell[m_colindices.length];
            for (int i = 0; i < newcells.length; i++) {
                final var dc = row.getCell(m_colindices[i]);
                if (!dc.isMissing()) {
                    final var str = ((StringValue)dc).getStringValue();
                    final var newstring = m_uppercase ? str.toUpperCase(m_locale) : str.toLowerCase(m_locale);
                    newcells[i] = new StringCell(newstring);
                } else {
                    newcells[i] = DataType.getMissingCell();
                }
            }
            return newcells;
        }

        @Override
        public DataColumnSpec[] getColumnSpecs() {
            final var newcolspecs = new DataColumnSpec[m_colindices.length];
            for (int i = 0; i < newcolspecs.length; i++) {
                final var colspec = m_spec.getColumnSpec(m_colindices[i]);
                final var domain = colspec.getDomain();
                final var colspeccreator = new DataColumnSpecCreator(colspec);
                if (domain.hasValues() && colspec.getType().equals(StringCell.TYPE)) {
                    final Set<DataCell> newdomainvalues = new LinkedHashSet<>();
                    for (final var dc : domain.getValues()) {
                        final var val = ((StringValue)dc).getStringValue();
                        final var newstring = m_uppercase ? val.toUpperCase(m_locale) : val.toLowerCase(m_locale);
                        newdomainvalues.add(new StringCell(newstring));
                    }
                    final var domaincreator = new DataColumnDomainCreator();
                    domaincreator.setValues(newdomainvalues);
                    colspeccreator.setDomain(domaincreator.createDomain());
                }
                colspeccreator.setType(StringCell.TYPE);
                newcolspecs[i] = colspeccreator.createSpec();
            }
            return newcolspecs;
        }

        @Override
        public void setProgress(final int curRowNr, final int rowCount, final RowKey lastKey,
            final ExecutionMonitor exec) {
            exec.setProgress((double)curRowNr / (double)rowCount, "Converting");
        }
    }

    // Expose for tests if necessary
    void performSaveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
    }

    void performLoadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadValidatedSettingsFrom(settings);
    }

    BufferedDataTable[] performExecute(final BufferedDataTable[] inObjects, final ExecutionContext exec)
        throws Exception {
        return super.execute(inObjects, exec);
    }
}
