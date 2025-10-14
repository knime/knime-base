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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.util.extracttablespec;

import org.knime.core.data.util.DataTableSpecExtractor.TypeNameFormat;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.impl.defaultfield.EnumFieldPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.DefaultProvider;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;

/**
 * Node parameters for Extract Table Spec.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
class ExtractTableSpecNodeParameters implements NodeParameters {

    @Persist(configKey = ExtractTableSpecConfigKeys.EXTRACT_PROPERTY_HANDLERS)
    @Widget(title = "Extract property handlers", description = """
            If checked the information which of the columns have property
            handlers e.g. color handler associated is extracted, otherwise not.
            """)
    boolean m_extractPropertyHandlers = ExtractTableSpecNodeModel.DEF_EXTRACT_PROPERTY_HANDLERS;

    @Persist(configKey = ExtractTableSpecConfigKeys.POSSIBBLE_VALUES_AS_COLLECTION)
    @Widget(title = "Possible values as collection", description = """
            If checked, the possible values of each columns are extracted as
            possible values, otherwise not.
            """)
    boolean m_possibleValuesAsCollection = ExtractTableSpecNodeModel.DEF_POSSIBLE_VALUES_AS_COLLECTION;

    @Persistor(TypeNameFormatPersistor.class)
    @Widget(title = "Format of Type Names", description = """
            Determines how the types of columns are represented in the output table.
            """)
    @RadioButtonsWidget
    @Migration(value = TypeNameFormatMigration.class)
    TypeNameFormatEnum m_typeNameFormat = TypeNameFormatEnum.IDENTIFIER;

    static final class TypeNameFormatMigration implements DefaultProvider<TypeNameFormatEnum> {

        @Override
        public TypeNameFormatEnum getDefault() {
            return TypeNameFormatEnum.LEGACY_DISPLAY_NAME;
        }

    }

    static final class TypeNameFormatPersistor implements NodeParametersPersistor<TypeNameFormatEnum> {

        static final EnumFieldPersistor<TypeNameFormat> INSTANCE =
            new EnumFieldPersistor<>(ExtractTableSpecConfigKeys.TYPE_NAME_FORMAT, TypeNameFormat.class, false);

        @Override
        public TypeNameFormatEnum load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var originalFormat = INSTANCE.load(settings);
            return switch (originalFormat) {
                case IDENTIFIER -> TypeNameFormatEnum.IDENTIFIER;
                case LEGACY_DISPLAY_NAME -> TypeNameFormatEnum.LEGACY_DISPLAY_NAME;
                case DISPLAY_NAME -> TypeNameFormatEnum.DISPLAY_NAME;
            };
        }

        @Override
        public void save(final TypeNameFormatEnum obj, final NodeSettingsWO settings) {
            final var originalFormat = switch (obj) {
                case IDENTIFIER -> TypeNameFormat.IDENTIFIER;
                case LEGACY_DISPLAY_NAME -> TypeNameFormat.LEGACY_DISPLAY_NAME;
                case DISPLAY_NAME -> TypeNameFormat.DISPLAY_NAME;
            };
            INSTANCE.save(originalFormat, settings);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{ExtractTableSpecConfigKeys.TYPE_NAME_FORMAT}};
        }
    }

    enum TypeNameFormatEnum {
        @Label(value = "Identifier", description = """
                The type names are represented with a unique identifier. This is the recommended option.
                """)
        IDENTIFIER,
        @Label(value = "Legacy Display Name", description = """
                The type names are represented by their old display name. This option is intended to be
                used purely for backwards-compatibility for workflows created in version 5.4 or before.
                """, disabled = true)
        LEGACY_DISPLAY_NAME,
        @Label(value = "Display Name", description = """
                The type names are represented by their display name. This name may change at any point
                with an update, so it is highly discouraged to match against this output in any way.
                """)
        DISPLAY_NAME
    }

}
