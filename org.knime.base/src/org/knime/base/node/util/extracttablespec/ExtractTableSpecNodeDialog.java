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
 *
 * History
 *   03.06.2012 (kilian): created
 */
package org.knime.base.node.util.extracttablespec;

import java.util.Arrays;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.util.DataTableSpecExtractor.TypeNameFormat;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The dialog of the extract table spec node, providing all of its dialog
 * components.
 *
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 */
class ExtractTableSpecNodeDialog extends DefaultNodeSettingsPane {

    /**
     * @return Creates and returns the settings model in which is specified
     * whether the property handlers will be extracted or not.
     */
    public static SettingsModelBoolean getExtractPropertyHandlersModel() {
        return new SettingsModelBoolean(
                ExtractTableSpecConfigKeys.EXTRACT_PROPERTY_HANDLERS,
                ExtractTableSpecNodeModel.DEF_EXTRACT_PROPERTY_HANDLERS);
    }

    /**
     * @return CReates and returns the settings model in which is specified
     * whether the possible values will be extracted as collection cell or not.
     */
    public static SettingsModelBoolean getPossibleValuesAsCollectionModel() {
        return new SettingsModelBoolean(
                ExtractTableSpecConfigKeys.POSSIBBLE_VALUES_AS_COLLECTION,
                ExtractTableSpecNodeModel.DEF_POSSIBLE_VALUES_AS_COLLECTION);
    }

    static SettingsModelString getTypeNameModel() {
        return new SettingsModelString(ExtractTableSpecConfigKeys.TYPE_NAME_FORMAT,
            ExtractTableSpecNodeModel.DEF_TYPE_NAME_FORMAT.toString());
    }

    /**
     * The settings model in which is specified how to output type names.
     */
    SettingsModelString m_typeNameModel = getTypeNameModel();

    /**
     * Constructor of the <code>ExtractTableSpecNodeDialog</code>.
     */
    ExtractTableSpecNodeDialog() {
        addDialogComponent(new DialogComponentBoolean(getExtractPropertyHandlersModel(), "Extract property handlers"));

        addDialogComponent(
            new DialogComponentBoolean(getPossibleValuesAsCollectionModel(), "Possible values as collection"));

        addDialogComponent(new DialogComponentStringSelection(m_typeNameModel, "Format of Type Names",
            Arrays.stream(TypeNameFormat.values()).map(TypeNameFormat::toString).toArray(String[]::new)));
    }

    @Override
    public void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final var fmtPrettyString = m_typeNameModel.getStringValue();
        final var fmt = TypeNameFormat.fromString(fmtPrettyString);
        settings.addString(m_typeNameModel.getKey(), fmt.name());
    }

    @Override
    public void loadAdditionalSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        final var fmtEnumString =
            settings.getString(m_typeNameModel.getKey(), ExtractTableSpecNodeModel.DEF_TYPE_NAME_FORMAT.name());
        final var fmt = TypeNameFormat.valueOf(fmtEnumString);
        m_typeNameModel.setStringValue(fmt.toString());
    }
}
