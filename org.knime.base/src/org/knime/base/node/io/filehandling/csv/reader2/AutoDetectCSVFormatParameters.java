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
package org.knime.base.node.io.filehandling.csv.reader2;

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.Icon;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Parameters for auto-detecting CSV format.
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction")
public final class AutoDetectCSVFormatParameters implements NodeParameters {

    /**
     * Reference for the auto-detect button.
     */
    public static final class AutoDetectButtonRef implements ButtonReference {
    }

    @Widget(title = "Autodetect format", description = """
            By pressing this button, the format of the file will be guessed automatically. It is not guaranteed that
            the correct values are being detected.
            """)
    @Layout(CSVTableReaderLayoutAdditions.FileFormat.AutodetectFormat.class)
    @SimpleButtonWidget(ref = AutoDetectButtonRef.class, icon = Icon.RELOAD)
    @Effect(predicate = FileSystemPortConnectionUtil.ConnectedWithoutFileSystemSpec.class, type = EffectType.DISABLE)
    Void m_autoDetectButton;

    static final class BufferSizeRef implements ParameterReference<Integer> {
    }

    @Widget(title = "Number of characters for autodetection", description = """
            Specifies on how many characters of the selected file should be used for autodetection. The
            autodetection by default is based on the first 1024 * 1024 characters.
            """, advanced = true)
    @ValueReference(BufferSizeRef.class)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Layout(CSVTableReaderLayoutAdditions.FileFormat.AutodetectFormat.class)
    int m_numberOfCharactersForAutodetection = CSVTableReaderConfig.DEFAULT_AUTODETECTION_BUFFER_SIZE;

    /**
     * Save the settings to the given config.
     *
     * @param csvConfig the config to save to
     */
    public void saveToConfig(final CSVTableReaderConfig csvConfig) {
        csvConfig.setAutoDetectionBufferSize(m_numberOfCharactersForAutodetection);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_numberOfCharactersForAutodetection <= 0) {
            throw new InvalidSettingsException("The number of characters for autodetection must be positive.");
        }
    }
}
