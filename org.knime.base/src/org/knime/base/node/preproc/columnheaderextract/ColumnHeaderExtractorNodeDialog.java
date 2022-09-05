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
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.columnheaderextract;

import java.util.Arrays;

import org.knime.base.node.preproc.columnheaderextract.ColumnHeaderExtractorNodeModel.ColType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

/**
 * <code>NodeDialog</code> for the "ColumnHeaderExtractor" Node.
 *
 * @author Bernd Wiswedel
 */
public final class ColumnHeaderExtractorNodeDialog extends DefaultNodeSettingsPane {

    /**
     * Output formats for the column headers.
     * The trailing spaces are only needed because of weirdly truncated labels in Java.
     */
    private static final String[] OUTPUT_FORMATS = { "Single Row", "Single Column  " };

    /**
     * New pane for configuring the ColumnHeaderExtractor node.
     */
    ColumnHeaderExtractorNodeDialog() {
        SettingsModelBoolean replaceColHeader = ColumnHeaderExtractorNodeModel.createReplaceColHeader();
        SettingsModelString unifyHeaderPrefix =
                ColumnHeaderExtractorNodeModel.createUnifyHeaderPrefix(replaceColHeader);
        SettingsModelString colTypeFilter = ColumnHeaderExtractorNodeModel.createColTypeFilter();
        SettingsModelBoolean transposeColHeader = ColumnHeaderExtractorNodeModel.createTransposeColHeader();

        createNewGroup("Output Column Names");
        addDialogComponent(new DialogComponentBoolean(replaceColHeader, "Use new output names "));
        addDialogComponent(new DialogComponentString(unifyHeaderPrefix, "Prefix"));
        closeCurrentGroup();

        createNewGroup("Output Format for Column Names");
        addDialogComponent(new DialogComponentButtonGroup(
            new SettingsModelBooleanAdapter(transposeColHeader), false, /*"Output Format"*/null, OUTPUT_FORMATS));
        closeCurrentGroup();

        createNewGroup("Restrain Columns");
        addDialogComponent(new DialogComponentStringSelection(colTypeFilter, "Selected column type",
            Arrays.stream(ColType.values()).map(ColType::displayString).toArray(String[]::new)));
        closeCurrentGroup();
    }

    /**
     * Adapter that disguises a {@link SettingsModelBoolean} as a {@link SettingsModelString} with two options.
     */
    private static final class SettingsModelBooleanAdapter extends SettingsModelString {

        /** Inner boolean model. */
        private final SettingsModelBoolean m_booleanModel;

        private SettingsModelBooleanAdapter(final SettingsModelBoolean booleanModel) {
            super(booleanModel.getConfigName(), OUTPUT_FORMATS[0]);
            m_booleanModel = booleanModel;
            addChangeListener(ev -> m_booleanModel.setBooleanValue(OUTPUT_FORMATS[1].equals(getStringValue())));
        }

        @Override
        protected SettingsModelString createClone() {
            final var settings = new NodeSettings("");
            saveSettingsForModel(settings);
            try {
                return new SettingsModelBooleanAdapter(m_booleanModel.createCloneWithValidatedValue(settings));
            } catch (InvalidSettingsException e) {
                throw new InternalError(e);
            }
        }

        @Override
        protected String getModelTypeID() {
            return "SMID_boolean";
        }

        @Override
        protected String getConfigName() {
            return m_booleanModel.getConfigName();
        }

        @Override
        protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
                throws NotConfigurableException {
            // use the current value, if no value is stored in the settings
            m_booleanModel.setBooleanValue(settings.getBoolean(m_booleanModel.getConfigName(),
                m_booleanModel.getBooleanValue()));
        }

        @Override
        protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
            saveSettingsForModel(settings);
        }

        @Override
        protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
            settings.getBoolean(m_booleanModel.getConfigName());
        }

        @Override
        protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
            // no default value, throw an exception instead
            m_booleanModel.setBooleanValue(settings.getBoolean(m_booleanModel.getConfigName()));
        }

        @Override
        protected void saveSettingsForModel(final NodeSettingsWO settings) {
            settings.addBoolean(m_booleanModel.getConfigName(), m_booleanModel.getBooleanValue());
        }
    }
}
