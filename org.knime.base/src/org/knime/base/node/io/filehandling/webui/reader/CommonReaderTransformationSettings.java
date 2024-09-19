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
 *   Sep 19, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.webui.reader;

import java.util.List;

import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.PersistorSettings.SetConfigIdSettingsValueRef.ConfigIdSettingsRef;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.WidgetModification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings({"javadoc", "restriction"})
public class CommonReaderTransformationSettings {

    public interface ConfigIdSettings<C extends ReaderSpecificConfig<C>> extends WidgetGroup, PersistableSettings {
        void applyToConfig(final DefaultTableReadConfig<C> config);
    }

    /**
     * @param <T> the type used to serialize external data types
     */
    public static final class ColumnSpecSettings<T> implements WidgetGroup, PersistableSettings {

        public String m_name;

        public T m_type;

        public ColumnSpecSettings(final String name, final T type) {
            m_name = name;
            m_type = type;
        }

        public ColumnSpecSettings() {
        }
    }

    // ??? from here on out, everything is copied from the CSV reader

    public static final class TableSpecSettings<T> implements WidgetGroup, PersistableSettings {

        public String m_sourceId;

        public List<ColumnSpecSettings<T>> m_spec;

        public TableSpecSettings(final String sourceId, final List<ColumnSpecSettings<T>> spec) {
            m_sourceId = sourceId;
            m_spec = spec;
        }

        public TableSpecSettings() {
        }
    }

    /**
     * TODO NOSONAR UIEXT-1946 These settings are sent to the frontend where they are not needed. They are merely held
     * here to be used in the CSVTransformationSettingsPersistor. We should look for an alternative mechanism to provide
     * these settings to the persistor. This would then also allow us to use non-serializable types like the
     * TypedReaderTableSpec instead of the TableSpecSettings, saving us the back-and-forth conversion.
     */
    public static abstract class PersistorSettings<C extends ReaderSpecificConfig<C>>
        implements WidgetGroup, PersistableSettings {

        public abstract class SetConfigIdSettingsValueRef implements WidgetModification.ImperativeWidgetModification {
            static class ConfigIdSettingsRef implements WidgetModification.Reference {
            }

            @Override
            public void modify(final WidgetGroupModifier group) {
                group.find(ConfigIdSettingsRef.class).addAnnotation(ValueReference.class)
                    .withProperty("value", getConfigIdSettingsValueRef()).build();
            }

            protected abstract Class<? extends Reference<ConfigIdSettings<C>>> getConfigIdSettingsValueRef();
        }

        @WidgetModification.WidgetReference(ConfigIdSettingsRef.class)
        ConfigIdSettings<C> m_configId;

        //        @ValueProvider(SourceIdProvider.class)
        //        String m_sourceId = "";
        //
        //        @ValueProvider(FSLocationsProvider.class)
        //        FSLocation[] m_fsLocations = new FSLocation[0];
        //
        //        static class TableSpecSettingsRef implements Reference<List<TableSpecSettings<String>>> {
        //        }
        //
        //        List<TableSpecSettings<T>> m_specs = List.of();
        //
        //        @ValueProvider(CommonReaderNodeSettings.AdvancedSettings.AppendPathColumnRef.class)
        //        boolean m_appendPathColumn;
        //
        //        @ValueProvider(CommonReaderNodeSettings.AdvancedSettings.FilePathColumnNameRef.class)
        //        String m_filePathColumnName = "File Path";
        //
        //        @ValueProvider(TakeColumnsFromProvider.class)
        //        ColumnFilterMode m_takeColumnsFrom = ColumnFilterMode.UNION;
    }

}
