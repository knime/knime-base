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
 *  ECLIPSE with only the Eclipse Public License, Version 1.0 (EPL-1.0)
 *  in lieu of the GNU General Public License of Version 3.
 *
 *  You may obtain a copy of the EPL 1.0 at
 *  http://www.eclipse.org/legal/epl-v10.html.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.knime.base.node.io.filehandling.model.reader;

import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.persistence.Persist;

/**
 * Node settings for the Model Reader node.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class ModelReaderNodeSettings implements NodeParameters {

    @Layout(ModelReaderNodeSettings.FileSelectionLayout.class)
    interface FileSelectionLayout {
    }

    @Persist(configKey = "file_selection")
    @Widget(title = "File", description = "Select the model file to read. Supported formats are .model and .zip files.")
    @Layout(FileSelectionLayout.class)
    FSLocation m_fileLocation = new FSLocation(FSCategory.MOUNTPOINT, "knime://knime.workflow/");

    /**
     * Constructor for settings initialization.
     */
    public ModelReaderNodeSettings() {
        // Default constructor
    }

    /**
     * Constructor with context.
     */
    public ModelReaderNodeSettings(final NoClassDefFoundError context) {
        // Constructor with context for initialization
    }
}
