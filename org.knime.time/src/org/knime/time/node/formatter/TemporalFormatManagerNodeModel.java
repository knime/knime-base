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
 *   25 May 2023 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.time.node.formatter;

import java.util.Locale;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.property.format.ValueFormatHandler;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.format.ValueFormatModelDateTime;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui* classes
final class TemporalFormatManagerNodeModel extends WebUINodeModel<TemporalFormatManagerSettings> {

    TemporalFormatManagerNodeModel(final WebUINodeConfiguration config) {
        super(config, TemporalFormatManagerSettings.class);
    }

    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs, final TemporalFormatManagerSettings modelSettings)
        throws InvalidSettingsException {
        final var formatHandler = handlerFor(modelSettings);

        final var dts = inSpecs[0];
        final var selected = getSelectedColumns(dts, modelSettings);

        return new DataTableSpec[] { createOutputSpec(dts, selected, formatHandler) };
    }

    private static String[] getSelectedColumns(final DataTableSpec dts,
            final TemporalFormatManagerSettings modelSettings) {
        return modelSettings.m_columnFilter.getSelected(
            dts.stream()//
                .filter(TemporalFormatManagerNodeModel::isTemporal)//
                .map(DataColumnSpec::getName)//
                .toArray(String[]::new), dts);
    }

    private DataTableSpec createOutputSpec(final DataTableSpec in, final String[] selected,
            final ValueFormatHandler formatHandler) {
        final var creator = new DataTableSpecCreator(in);
        for (final String col : selected) {
            final var colSpecCreator = new DataColumnSpecCreator(in.getColumnSpec(col));
            colSpecCreator.setValueFormatHandler(formatHandler);
            final var outSpec = colSpecCreator.createSpec();
            creator.replaceColumn(in.findColumnIndex(col), outSpec);
        }
        return creator.createSpec();
    }

    static boolean isTemporal(final DataColumnSpec colSpec) {
        final var type = colSpec.getType();
        return type.isCompatible(LocalDateTimeValue.class)
                || type.isCompatible(LocalTimeValue.class)
                || type.isCompatible(LocalDateValue.class)
                || type.isCompatible(ZonedDateTimeValue.class)
                || type.isCompatible(DurationValue.class);
    }

    private ValueFormatHandler handlerFor(final TemporalFormatManagerSettings settings) {

        final var locale = Locale.forLanguageTag(settings.m_localeLanguageTag);
        return new ValueFormatHandler(new ValueFormatModelDateTime(locale, settings.m_pattern));
    }

    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec,
            final TemporalFormatManagerSettings modelSettings) throws Exception {
        final var in = inData[0];
        final var inSpec = in.getSpec();
        final var formatHandler = handlerFor(modelSettings);
        final var selected = getSelectedColumns(inSpec, modelSettings);
        final var outSpec = createOutputSpec(inSpec, selected, formatHandler);
        final var out = outSpec == inSpec ? in : exec.createSpecReplacerTable(in, outSpec);
        return new BufferedDataTable[] {out};
    }

}
