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
 * -------------------------------------------------------------------
 *
 * History
 *   Aug 20, 2025 (AI migration helper): created for modern UI migration of Model Reader node
 */
package org.knime.base.node.io.filehandling.model.reader;

import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.LegacyReaderFileSelectionPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.widget.message.TextMessage.SimpleTextMessageProvider;

/**
 * Settings definition for the Model Reader node (modern UI). Only provides the file selection widget for choosing a
 * KNIME model file previously written by the Model Writer node. The legacy dialog stored its settings below the
 * {@code "model"} key; we keep the same config key to ensure backwards compatible persistence.
 */
@SuppressWarnings("restriction")
public final class ModelReaderNodeSettings implements NodeParameters {

    /**
     * Main settings group persisted under the legacy key "model". Uses the common reader base settings to expose a
     * single "Source" widget with file system / path selection and flow variable support.
     */
    @Persist(configKey = "model")
    Settings settings = new Settings();

    /**
     * Settings group extending the common reader base settings. We do not restrict extensions here because the
     * legacy implementation accepted any model file; filtering by extension is already provided by the node factory
     * (see MODEL_SUFFIXES there).
     */
    static final class Settings implements WidgetGroup, Persistable {

        /** Reference to access the chosen file selection. */
        static final class FileSelectionRef extends ReferenceStateProvider<FileSelection> {
        }

        /** Message shown if an (empty) FS port is connected (mirrors common reader behavior). */
        static final class FileSystemManagedByPortMessage implements SimpleTextMessageProvider {

            @Override
            public boolean showMessage(final NodeParametersInput context) {
                return FileSystemPortConnectionUtil.hasEmptyFileSystemPort(context);
            }

            @Override
            public String title() {
                return "File system managed by File System Input Port";
            }

            @Override
            public String description() {
                return "No file system is currently connected. To proceed, either connect a file system to the input"
                    + " port or remove the port.";
            }

            @Override
            public MessageType type() {
                return MessageType.INFO;
            }
        }

        /** Persistor writing to legacy key 'filechooser'. */
        static final class FileSelectionPersistor extends LegacyReaderFileSelectionPersistor {
            FileSelectionPersistor() { super("filechooser"); }
        }

        @TextMessage(value = FileSystemManagedByPortMessage.class)
        @Layout(CommonReaderLayout.File.Source.class)
    Void fsPortInfo;

        @Widget(title = "Source", description = CommonReaderLayout.File.Source.DESCRIPTION)
        @ValueReference(FileSelectionRef.class)
        @Layout(CommonReaderLayout.File.Source.class)
        @Persistor(FileSelectionPersistor.class)
        @FileReaderWidget
    FileSelection source = new FileSelection();

        /** Legacy internal settings block under key 'filechooser_Internals'. */
        @Persist(configKey = "filechooser_Internals")
        FileChooserInternals fileChooserInternals = new FileChooserInternals();

        static final class FileChooserInternals implements WidgetGroup, Persistable {
            @Persist(configKey = "SettingsModelID")
            String settingsModelID = "SMID_ReaderFileChooser";
            @Persist(configKey = "EnabledStatus")
            boolean enabledStatus = true;
        }
    }
}
