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
 *   Nov 24, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.webui.reader2;

import java.net.URL;

import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.connections.FSLocationUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.widget.message.TextMessage.SimpleTextMessageProvider;

/**
 * Parameters for single file reader source selection.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
public final class SingleFileReaderParameters implements NodeParameters {

    /**
     * Constructor with URL for initialization.
     *
     * @param url the URL to initialize from
     */
    public SingleFileReaderParameters(final URL url) {
        m_source = new FileSelection(FSLocationUtil.createFromURL(url.toString()));
    }

    /**
     * Default constructor.
     */
    public SingleFileReaderParameters() {
        // default constructor
    }

    /**
     * Reference for the file selection.
     */
    public static final class FileSelectionRef extends ReferenceStateProvider<FileSelection>
        implements Modification.Reference {
    }

    /**
     * Set the file extensions for the file reader widget using {@link Modification} on the implementation of this class
     * or the field where it is used.
     */
    public abstract static class SetFileReaderWidgetExtensions implements Modification.Modifier {
        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            group.find(FileSelectionRef.class).modifyAnnotation(FileReaderWidget.class)
                .withProperty("fileExtensions", getExtensions()).modify();
        }

        /**
         * @return the valid extensions by which the browsable files should be filtered
         */
        protected abstract String[] getExtensions();
    }

    static final class FileSystemManagedByPortNotAvailableMessage implements SimpleTextMessageProvider {

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

    @TextMessage(value = FileSystemManagedByPortNotAvailableMessage.class)
    @Layout(ReaderLayout.File.Source.class)
    Void m_fileSystemFromPortNotAvailableMessage;

    // TODO NOSONAR will be updated in UIEXT-1764
    private static final String SOURCE_DESCRIPTION = """
            Select a file location which stores the data you want to read. When clicking on the browse button,
            there are two default file system options to choose from:
            <br/>
            <ul>
                <li><b>The current Hub space</b>: Allows to select a file relative to the Hub space on which the
                    workflow is run.</li>
                <li><b>URL</b>: Allows to specify a URL (e.g. file://, http:// or knime:// protocol).</li>
            </ul>
            """;

    @Widget(title = "Source", description = SOURCE_DESCRIPTION)
    @ValueReference(FileSelectionRef.class)
    @Layout(ReaderLayout.File.Source.class)
    @Modification.WidgetReference(FileSelectionRef.class)
    @FileReaderWidget()
    public FileSelection m_source = new FileSelection();

    /**
     * Save the source to the given settings.
     *
     * @param sourceSettings the settings to save to
     */
    public void saveToSource(final FileSelectionPath sourceSettings) {
        sourceSettings.setLocation(m_source);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        m_source.validate();
    }
}
