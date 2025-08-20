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
 *   Aug 20, 2025 (AI migration helper): created for modern UI migration of Model Writer node
 */
package org.knime.base.node.io.filehandling.model.writer;

import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileWriterWidget;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.WidgetGroup;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistable;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.widget.message.TextMessage.SimpleTextMessageProvider;

/** Modern UI settings for the Model Writer node with legacy-compatible persistence. */
@SuppressWarnings("restriction")
public final class ModelWriterNodeSettings implements NodeParameters {

    @Persist(configKey = "model")
    Settings settings = new Settings();

    static final class Settings implements WidgetGroup, Persistable {

        private static final String FILECHOOSER_KEY = "filechooser";
        private static final String CFG_CREATE_MISSING = "create_missing_folders";
        private static final String CFG_IF_PATH_EXISTS = "if_path_exists";

        static final class FileSelectionRef extends ReferenceStateProvider<FileSelection> { }

        static final class FileSystemManagedByPortMessage implements SimpleTextMessageProvider {
            @Override
            public boolean showMessage(final NodeParametersInput context) {
                return FileSystemPortConnectionUtil.hasEmptyFileSystemPort(context);
            }
            @Override public String title() { return "File system managed by File System Input Port"; }
            @Override public String description() { return "No file system is currently connected. To proceed, either connect a file system to the input port or remove the port."; }
            @Override public MessageType type() { return MessageType.INFO; }
        }

        static final class FileSelectionPersistor implements NodeParametersPersistor<FileSelection> {
            private static final NodeLogger LOGGER = NodeLogger.getLogger(FileSelectionPersistor.class);
            @Override
            public FileSelection load(final NodeSettingsRO settings) throws InvalidSettingsException {
                var sel = new FileSelection();
                var fs = settings.getNodeSettings(FILECHOOSER_KEY);
                sel.m_path = org.knime.filehandling.core.data.location.FSLocationSerializationUtils
                    .loadFSLocation(fs.getConfig("path"));
                return sel;
            }
            @Override
            public void save(FileSelection sel, final NodeSettingsWO settings) {
                if (sel == null) {
                    LOGGER.coding("FileSelection null - creating empty instance");
                    sel = new FileSelection();
                }
                var root = settings.addNodeSettings(FILECHOOSER_KEY);
                org.knime.filehandling.core.data.location.FSLocationSerializationUtils.saveFSLocation(
                    sel.getFSLocation(), root.addNodeSettings("path"));
                var filterModeInternals = root.addNodeSettings("filter_mode_Internals");
                filterModeInternals.addString("SettingsModelID", "SMID_FilterMode");
                filterModeInternals.addBoolean("EnabledStatus", true);
                var fsInternals = root.addNodeSettings("file_system_chooser__Internals");
                fsInternals.addBoolean("has_fs_port", false);
                fsInternals.addBoolean("overwritten_by_variable", false);
                fsInternals.addString("convenience_fs_category", "RELATIVE");
                fsInternals.addString("relative_to", "knime.workflow.data");
                fsInternals.addString("mountpoint", "LOCAL");
                fsInternals.addString("spaceId", "");
                fsInternals.addString("spaceName", "");
                fsInternals.addInt("custom_url_timeout", 1000);
                fsInternals.addBoolean("connected_fs", true);
            }
            @Override
            public String[][] getConfigPaths() { return new String[][] {{FILECHOOSER_KEY, "path"}}; }
        }

        static final class CreateMissingFoldersPersistor implements NodeParametersPersistor<Boolean> {
            @Override
            public Boolean load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return settings.getNodeSettings(FILECHOOSER_KEY).getBoolean(CFG_CREATE_MISSING);
            }
            @Override
            public void save(final Boolean value, final NodeSettingsWO settings) {
                settings.addNodeSettings(FILECHOOSER_KEY).addBoolean(CFG_CREATE_MISSING, value != null && value);
            }
            @Override
            public String[][] getConfigPaths() { return new String[][] {{FILECHOOSER_KEY, CFG_CREATE_MISSING}}; }
        }

        enum OverwritePolicy {
            FAIL("fail"),
            OVERWRITE("overwrite");
            private final String legacyValue;
            OverwritePolicy(final String legacy) { legacyValue = legacy; }
            String legacy() { return legacyValue; }
            static OverwritePolicy fromLegacy(final String v) {
                for (var p : values()) { if (p.legacyValue.equalsIgnoreCase(v)) { return p; } }
                return FAIL;
            }
        }

        static final class IfPathExistsPersistor implements NodeParametersPersistor<OverwritePolicy> {
            @Override
            public OverwritePolicy load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return OverwritePolicy.fromLegacy(
                    settings.getNodeSettings(FILECHOOSER_KEY).getString(CFG_IF_PATH_EXISTS));
            }
            @Override
            public void save(final OverwritePolicy value, final NodeSettingsWO settings) {
                settings.addNodeSettings(FILECHOOSER_KEY)
                    .addString(CFG_IF_PATH_EXISTS, (value == null ? OverwritePolicy.FAIL : value).legacy());
            }
            @Override
            public String[][] getConfigPaths() { return new String[][] {{FILECHOOSER_KEY, CFG_IF_PATH_EXISTS}}; }
        }

        @Persist(configKey = "filechooser_Internals")
        FileChooserInternals fileChooserInternals = new FileChooserInternals();

        static final class FileChooserInternals implements WidgetGroup, Persistable {
            @Persist(configKey = "SettingsModelID")
            String settingsModelID = "SMID_WriterFileChooser";
            @Persist(configKey = "EnabledStatus")
            boolean enabledStatus = true;
        }

        @TextMessage(value = FileSystemManagedByPortMessage.class)
        Void fsPortInfo;

        @Widget(title = "Destination", description = "Select where to write the model. Supports local, workflow-relative and connected file systems. Flow variables allowed.")
        @ValueReference(FileSelectionRef.class)
        @Persistor(FileSelectionPersistor.class)
        @FileWriterWidget
        FileSelection destination = new FileSelection();

        @Widget(title = "Create missing folders", description = "Create parent directories if they do not exist.")
        @Persistor(CreateMissingFoldersPersistor.class)
        boolean createMissingFolders = false;

        @Widget(title = "If path exists", description = "Fail the execution or Overwrite an existing target file.")
        @Persistor(IfPathExistsPersistor.class)
        OverwritePolicy ifPathExists = OverwritePolicy.FAIL;
    }
}
