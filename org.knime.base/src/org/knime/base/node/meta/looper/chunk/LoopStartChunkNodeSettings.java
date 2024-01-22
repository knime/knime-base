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
 *   Jan 22, 2024 (wiswedel): created
 */
package org.knime.base.node.meta.looper.chunk;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Settings for node.
 *
 * @noreference This class is not intended to be referenced by clients.
 * @author wiswedel
 * @since 5.3
 */
@SuppressWarnings("restriction")
public final class LoopStartChunkNodeSettings implements DefaultNodeSettings {

    /** Policy how to do the chunking. */
    @SuppressWarnings("java:S115") // naming (backward compatible in settings.xml)
    enum Mode {
        /** Limit no of rows per chunk. */
        @Label("Rows per Chunk")
        RowsPerChunk,
        /** Limit no of chunks. */
        @Label("Number of Chunks")
        NrOfChunks
    }

    private interface ModeSignals {
        class RowPerChunkCondition extends OneOfEnumCondition<Mode> {
            @Override
            public Mode[] oneOf() {
                return new Mode[] {Mode.RowsPerChunk};
            }
        }
    }

    @ValueSwitchWidget
    @Signal(id = ModeSignals.class, condition = ModeSignals.RowPerChunkCondition.class)
    Mode m_mode = Mode.RowsPerChunk;

    @Widget(title = "Rows per chunk", description = """
            Set the number of rows per iteration/chunk. The number of iterations
                    is calculated as the row count of the input table divided by this value.
                    Set the value to 1 in order to implement a streaming approach, that is,
                    one row at a time.
            """)
    @Effect(type = EffectType.SHOW, signals = ModeSignals.class)
    int m_nrRowsPerChunk = 100;


    @Widget(title = "Number of chunks", description = """
            Set the number of iterations/chunks. The number of rows per chunk
                    is calculated as the the row count of the input table divided by
                    this value.d
            """)
    @Effect(type = EffectType.HIDE, signals = ModeSignals.class)
    int m_nrOfChunks = 1;
}
