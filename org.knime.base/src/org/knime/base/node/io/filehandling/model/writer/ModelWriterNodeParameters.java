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

package org.knime.base.node.io.filehandling.model.writer;

import java.util.List;

import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification.WidgetGroupModifier;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;

/**
 * Node parameters for Model Writer.
 *
 * @author Jochen Reißinger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ModelWriterNodeParameters implements NodeParameters {

    @Persist(configKey = "filechooser")
    @Modification(FileChooserModifier.class)
    LegacyFileWriterWithOverwritePolicyOptions m_fileChooser = new LegacyFileWriterWithOverwritePolicyOptions();

    private static final class FileChooserModifier implements LegacyFileWriterWithOverwritePolicyOptions.Modifier {

        /**
         * {@inheritDoc}
         */
        @Override
        public void modify(final WidgetGroupModifier group) {
            var fileSelection = findFileSelection(group);
            fileSelection.modifyAnnotation(Widget.class).withProperty("title", "Output location")
                .withProperty("description", "Select a file to store the output model").modify();
            // TODO: UIEXT-3116: enable .zip as filtered file extension next to .model
            fileSelection.modifyAnnotation(FileWriterWidget.class).withProperty("fileExtension", "model").modify();

            final var overwritePolicy = findOverwritePolicy(group);
            overwritePolicy //
                .addAnnotation(ChoicesProvider.class) //
                .withProperty("value", ModelWriterOverwritePolicyChoicesProvider.class) //
                .modify();
            overwritePolicy //
                .modifyAnnotation(Widget.class) //
                .withProperty("description",
                    "Specify the behavior of the node in case the output file already exists. " + "<ul>"
                        + "<li><b>Fail</b>: Will issue an error during the node's execution "
                        + "(to prevent unintentional overwrite).</li>" //
                        + "<li><b>Overwrite</b>: Will replace any existing file.</li>" //
                        + "</ul>")
                .modify();
        }

    }

    private static final class ModelWriterOverwritePolicyChoicesProvider
        implements EnumChoicesProvider<LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy> {

        @Override
        public List<LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy>
            choices(final NodeParametersInput context) {
            return List.of(LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy.fail,
                LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy.overwrite);
        }
    }
}
