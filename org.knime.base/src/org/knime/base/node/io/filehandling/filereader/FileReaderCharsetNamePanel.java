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
 *   21.08.2007 (ohl): created
 */
package org.knime.base.node.io.filehandling.filereader;

import java.nio.charset.Charset;
import java.util.Objects;

import org.knime.filehandling.core.encoding.CharsetNamePanel;

/**
 * Implements the tab panel for the character set settings (in the advanced settings dialog).
 *
 * @author Peter Ohl, University of Konstanz
 */
final class FileReaderCharsetNamePanel extends CharsetNamePanel {

    private static final long serialVersionUID = 1390868853449848650L;

    /**
     * Creates a panel to select the character set name and initializes it from the passed object.
     *
     * @param charset the {@link Charset} name
     */
    FileReaderCharsetNamePanel(final String charset) {
        super(charset);
    }

    /**
     * Writes the current settings of the panel into the passed settings object.
     *
     * @param settings the object to write settings in
     * @return true if the new settings are different from the one passed in.
     */
    boolean overrideSettings(final FileReaderNodeSettings settings) {
        String oldCSN = settings.getCharsetName();
        String newCSN = getSelectedCharsetName().orElse(null);

        settings.setCharsetName(newCSN);

        boolean changed = !Objects.equals(oldCSN, newCSN);

        if (changed) {
            settings.setCharsetUserSet(true);
        }

        return changed;
    }

}
