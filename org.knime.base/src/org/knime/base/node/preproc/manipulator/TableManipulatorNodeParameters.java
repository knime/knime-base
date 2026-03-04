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

package org.knime.base.node.preproc.manipulator;

import org.knime.base.node.io.filehandling.webui.reader2.AbstractConfigIDSaver;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.additionalsave.SaveAdditional;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.updates.ValueReference;

/**
 * Node parameters for Table Manipulator.
 * 
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
class TableManipulatorNodeParameters implements NodeParameters {

    @Section(title = "RowID Handling")
    interface RowIdHandlingSection {
    }

    @Section(title = "Table Transformation")
    @After(RowIdHandlingSection.class)
    interface TableTransformationSection {
    }

    static final class TableManipulatorParametersRef
        implements org.knime.node.parameters.updates.ParameterReference<TableManipulatorParameters> {
    }

    @SaveAdditional(ConfigIDSaver.class)
    @ValueReference(TableManipulatorParametersRef.class)
    @PersistWithin.PersistEmbedded
    TableManipulatorParameters m_parameters = new TableManipulatorParameters();

    @Layout(TableTransformationSection.class)
    @Migration(TableManipulatorMigration.TransformationParametersMigration.class)
    TableManipulatorTransformationParameters m_transformationParameters =
        new TableManipulatorTransformationParameters();

    static final class ConfigIDSaver extends AbstractConfigIDSaver<TableManipulatorParameters> {

        @Override
        protected ConfigID createConfigID(final TableManipulatorParameters param) {
            return param.saveToConfig(new TableManipulatorMultiTableReadConfig());
        }
    }

    void saveToConfig(final TableManipulatorMultiTableReadConfig config, final ConfigID existingConfigId) {
        m_parameters.saveToConfig(config);
        m_transformationParameters.saveToConfig(config, existingConfigId,
            m_parameters.getColumnFilterMode().toColumnFilterMode());
    }

    @Override
    public void validate() throws InvalidSettingsException {
        m_parameters.validate();
        m_transformationParameters.validate();
    }
}
