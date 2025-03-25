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
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

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
        @Label("Rows per chunk")
        RowsPerChunk,
        /** Limit no of chunks. */
        @Label("Number of chunks")
        NrOfChunks
    }

    interface ModeRef extends Reference<Mode> {
    }

    static final class IsRowPerChunk implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ModeRef.class).isOneOf(Mode.RowsPerChunk);
        }
    }

    @Widget(title ="Mode", description = "Select if the chunking is based on a fixed number of rows per chunk or a fixed number of chunks.")
    @ValueSwitchWidget
    @ValueReference(ModeRef.class)
    Mode m_mode = Mode.RowsPerChunk;

    @Widget(title = "Rows per chunk", description = """
            Set the number of rows per chunk. The number of iterations is the row count of the input table divided by this value. To implement a streaming approach with one row at a time, set this value to 1.
            """)
    @NumberInputWidget(validation = IsPositiveIntegerValidation.class)
    @Effect(type = EffectType.SHOW, predicate = IsRowPerChunk.class)
    int m_nrRowsPerChunk = 1;


    @Widget(title = "Number of chunks", description = """
            Set the number of chunks. The number of rows per chunk is the row count of the input table divided by this value.
            """)
    @Effect(type = EffectType.HIDE, predicate = IsRowPerChunk.class)
    @NumberInputWidget(validation = IsPositiveIntegerValidation.class)
    int m_nrOfChunks = 1;
}
