package org.knime.time.util;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;


/**
 * Type for an option to choose between failing with an error or setting the cell to missing
 *
 * @author David Hickey, TNG
 */
@SuppressWarnings("restriction")
public enum ActionIfExtractionFails {
            @Label(value = "Set to missing", description = """
                    Set the cell to missing if the string column cannot be converted to the specified \
                    type.
                    """)
            SET_MISSING, //
            @Label(value = "Fail", description = """
                    Fail with an error if the string column cannot be converted to the specified \
                    type.
                    """)
            FAIL;

        /**
         * Persistor for the {@link ActionIfExtractionFails} enum.
         */
        public static final class Persistor extends NodeSettingsPersistorWithConfigKey<ActionIfExtractionFails> {

            @Override
            public ActionIfExtractionFails load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return settings.getBoolean(getConfigKey()) //
                    ? FAIL //
                    : SET_MISSING;
            }

            @Override
            public void save(final ActionIfExtractionFails obj, final NodeSettingsWO settings) {
                settings.addBoolean(getConfigKey(), obj == FAIL);
            }
        }
    }