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

package org.knime.base.node.io.filehandling.pmml.writer;

import static org.knime.base.node.io.filehandling.pmml.writer.PMMLWriterNodeConfig2.CFG_VALIDATE_PMML;

import java.util.List;

import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy;

/**
 * Node parameters for PMML Writer.
 *
 * @author Rupert Ettrich, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class PMMLWriterNodeParameters implements NodeParameters {

    @Persist(configKey = "filechooser")
    @Modification(FileChooserModifier.class)
    LegacyFileWriterWithOverwritePolicyOptions m_fileChooser = new LegacyFileWriterWithOverwritePolicyOptions();

    @Persist(configKey = CFG_VALIDATE_PMML)
    @Widget(title = "Validate PMML before export",
        description = "Select to validate the PMML document that should be written. "
            + "If it is not compliant with the schema, the node will fail before actually writing.")
    boolean m_validatePMML = true;

    private static final class FileChooserModifier implements LegacyFileWriterWithOverwritePolicyOptions.Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            var fileSelection = findFileSelection(group);
            fileSelection.modifyAnnotation(Widget.class) //
                .withProperty("title", "Output location") //
                .withProperty("description", "Select a file to store the PMML model.") //
                .modify();
            fileSelection.modifyAnnotation(FileWriterWidget.class) //
                .withProperty("fileExtension", "pmml") //
                .modify();

            var createMissingFolders = findCreateMissingFolders(group);
            createMissingFolders.modifyAnnotation(Widget.class) //
                .withProperty("description",
                    "Select if the folders of the selected output location should be created if they do not "
                        + "already exist. If this option is unchecked, the node will fail if a folder does not exist.")
                .modify();

            restrictOverwritePolicyOptions(group, PMMLWriterOverwritePolicyChoicesProvider.class);
        }
    }

    private static final class PMMLWriterOverwritePolicyChoicesProvider
        extends LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicyChoicesProvider {

        @Override
        protected List<OverwritePolicy> getChoices() {
            return List.of(OverwritePolicy.fail, OverwritePolicy.overwrite);
        }
    }
}
