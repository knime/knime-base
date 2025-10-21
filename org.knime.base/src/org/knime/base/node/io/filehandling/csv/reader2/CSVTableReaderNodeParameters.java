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
 *   Nov 5, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.util.Optional;

import org.knime.base.node.io.filehandling.csv.reader.CSVMultiTableReadConfig;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.DataArea.FirstColumnContainsRowIds;
import org.knime.base.node.io.filehandling.csv.reader2.common.CommonTableReaderNodeParameters;
import org.knime.base.node.io.filehandling.csv.reader2.common.CommonTableReaderNodeParameters.UseExistingRowIdWidgetRef;
import org.knime.base.node.io.filehandling.csv.reader2.common.CommonTableReaderPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;

/**
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@Modification({CSVTableReaderNodeParameters.SetCSVExtensions.class,
    CSVTableReaderNodeParameters.SetTitleAndDescriptionForUseExistingRowIds.class})
public class CSVTableReaderNodeParameters implements NodeParameters {

    CSVTableReaderNodeParameters(final NodeParametersInput input) {
        this(input.getURLConfiguration());
    }

    CSVTableReaderNodeParameters(final NodeCreationConfiguration nodeCreationConfig) {
        this(nodeCreationConfig.getURLConfig());
    }

    private CSVTableReaderNodeParameters(final Optional<? extends URLConfiguration> urlConfig) {
        if (urlConfig.isPresent()) {
            final var url = urlConfig.get().getUrl();
            m_commonTableReaderNodeParameters = new CommonTableReaderNodeParameters(url);
            m_specificTableReaderNodeParameters = new CSVTableReaderSpecificNodeParameters(url);
        }
    }

    CSVTableReaderNodeParameters() {
        // default constructor
    }

    @Override
    public void validate() throws InvalidSettingsException {
        // TODO Auto-generated method stub
        NodeParameters.super.validate();
    }

    static final class SetCSVExtensions extends CommonTableReaderNodeParameters.SetFileReaderWidgetExtensions {
        @Override
        protected String[] getExtensions() {
            return new String[]{"csv", "tsv", "txt", "gz"};
        }
    }

    static final class SetTitleAndDescriptionForUseExistingRowIds implements Modification.Modifier {
        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            group.find(UseExistingRowIdWidgetRef.class).modifyAnnotation(Widget.class)
                .withProperty("title", FirstColumnContainsRowIds.TITLE)
                .withProperty("description", FirstColumnContainsRowIds.DESCRIPTION).modify();
        }
    }

    public static final class CommonTableReaderNodeParametersRef
        implements ParameterReference<CommonTableReaderNodeParameters> {
    }

    @ValueReference(CommonTableReaderNodeParametersRef.class)
    CommonTableReaderNodeParameters m_commonTableReaderNodeParameters = new CommonTableReaderNodeParameters();

    public static final class ReaderConfigNodeParametersRef implements ParameterReference<CSVTableReaderSpecificNodeParameters> {
    }

    @ValueReference(ReaderConfigNodeParametersRef.class)
    CSVTableReaderSpecificNodeParameters m_specificTableReaderNodeParameters =
        new CSVTableReaderSpecificNodeParameters();

    CSVTransformationParameters m_transformationParameters = new CSVTransformationParameters();

    public void saveToSource(final CommonTableReaderPath sourceSettings) {
        m_commonTableReaderNodeParameters.saveToSource(sourceSettings);
    }

    public void saveToConfig(final CSVMultiTableReadConfig config) {
        m_commonTableReaderNodeParameters.saveToConfig(config);
        m_specificTableReaderNodeParameters.saveToConfig(config);
        m_transformationParameters.saveToConfig(m_commonTableReaderNodeParameters, config);
    }

    public void loadFromConfig(final CommonTableReaderPath sourceSettings, final CSVMultiTableReadConfig config) {
        m_commonTableReaderNodeParameters.loadFromConfig(sourceSettings, config);
        m_specificTableReaderNodeParameters.loadFromConfig(config);
        m_transformationParameters.loadFromConfig(config);
    }

}
