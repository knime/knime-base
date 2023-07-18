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
 *   10 Jan 2023 (ivan.prigarin): created
 */
package org.knime.base.node.preproc.transpose;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Currently only used for the node dialogue, backwards compatible loading is ensured by the node model. If this is ever
 * used for the node model, backwards compatible loading will need to be implemented.
 *
 * @author Ivan Prigarin, KNIME GbmH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class TransposeTableNodeSettings implements DefaultNodeSettings {

    private static final String CHUNKING_MODE_KEY = "guess_or_fixed";

    @Persist(customPersistor = ChunkingModePersistor.class)
    @Widget(title = "Chunk size configuration",
        description = "Select how the node should handle chunking while processing the input table:<ul>"
            + "<li><b>Automatic:</b> Use a dynamic chunk size that adapts to the "
            + "current memory available. The number of columns read will be maximized for performance.</li>"
            + "<li><b>Manual:</b> Manually specify the number of columns read "
            + "during one iteration over the table. Larger chunk sizes lead to more "
            + "memory consumption, but yield faster execution time.</li></ul>")
    ChunkingMode m_chunkingMode = ChunkingMode.GUESS_SIZE;

    @Persist(configKey = "chunk_size")
    @Widget(title = "Columns per chunk",
        description = "The number of columns read during one iteration over the table. "
            + "Increasing this value yields faster execution time, but also increases memory consumption.")
    int m_chunkSize;

    enum ChunkingMode {
            @Label("Automatic")
            GUESS_SIZE,

            @Label("Manual")
            SPECIFY_SIZE;
    }

    private static final class ChunkingModePersistor implements FieldNodeSettingsPersistor<ChunkingMode> {

        @Override
        public ChunkingMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(CHUNKING_MODE_KEY)
                && settings.getString(CHUNKING_MODE_KEY).equals(TransposeTableNodeDialogPane.OPTION_FIXED_CHUNK_SIZE)) {
                return ChunkingMode.SPECIFY_SIZE;
            } else {
                return ChunkingMode.GUESS_SIZE;
            }

        }

        @Override
        public void save(final ChunkingMode obj, final NodeSettingsWO settings) {
            if (obj == ChunkingMode.GUESS_SIZE) {
                settings.addString(CHUNKING_MODE_KEY, TransposeTableNodeDialogPane.OPTION_GUESS_CHUNK_SIZE);
            } else {
                settings.addString(CHUNKING_MODE_KEY, TransposeTableNodeDialogPane.OPTION_FIXED_CHUNK_SIZE);
            }

        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{CHUNKING_MODE_KEY};
        }
    }
}
