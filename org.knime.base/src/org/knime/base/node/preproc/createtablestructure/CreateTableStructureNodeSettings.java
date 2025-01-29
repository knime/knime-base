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
 *   Jan 29, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.createtablestructure;

import java.util.Arrays;

import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * The settings for the "Table Structure Creator" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class CreateTableStructureNodeSettings implements DefaultNodeSettings {

    enum CreateMode {
            @Label(value = "Custom", description = "Select column name and type for each column separatly")
            CUSTOM, //
            @Label(value = "Basic", description = "Create a number of columns with the prefix name and the same type")
            BASIC
    }

    enum ColType {
            @Label("String")
            String(StringCell.TYPE), //
            @Label("Integer")
            Integer(IntCell.TYPE), //
            @Label("Double")
            Double(DoubleCell.TYPE); //

        private final DataType m_type;

        private ColType(final DataType type) {
            m_type = type;
        }

        public DataType getType() {
            return m_type;
        }
    }

    interface CreateModeRef extends Reference<CreateMode> {
    }

    @Section
    interface ModeSection {
    }

    @Section(title = "Settings")
    interface SettingsSection {
    }

    @Widget(title = "Mode of creating columns", description = """
            Select how to create columns:
            <ul>
                <li><strong>Custom</strong>: Select column name and type for each column separatly</li>
                <li><strong>Basic</strong>: Legacy mode: Create a number of columns with the prefix name and the \
                same type</li>
            </ul>
            """)
    @ValueSwitchWidget
    @ValueReference(CreateModeRef.class)
    @Layout(ModeSection.class)
    CreateMode m_createMode = CreateMode.BASIC;

    @Effect(predicate = IsBasicMode.class, type = EffectType.SHOW)
    @Layout(SettingsSection.class)
    BasicModeSettings m_basicModeSettings = new BasicModeSettings();

    @Effect(predicate = IsCustomMode.class, type = EffectType.SHOW)
    @Widget(title = "Columns", description = "")
    @Layout(SettingsSection.class)
    @ArrayWidget(addButtonText = "Add new column", elementTitle = "Column")
    CustomModeSettings[] m_customModeSettings = new CustomModeSettings[]{new CustomModeSettings()};

    static final class IsBasicMode implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(CreateModeRef.class).isOneOf(CreateMode.BASIC);
        }
    }

    static final class IsCustomMode implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(CreateModeRef.class).isOneOf(CreateMode.CUSTOM);
        }
    }

    static final class ColTypeChoiceProvider implements ChoicesProvider {

        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            return Arrays.stream(ColType.values()).map(Enum::name).toArray(String[]::new);
        }
    }

    static final class CustomModeSettings implements DefaultNodeSettings {

        CustomModeSettings() {
        }

        CustomModeSettings(final String columnName, final ColType colType){
            this.m_columnName = columnName;
            this.m_colType = colType;
        }

        @Widget(title = "Column name ", description = """
                Name of the column
                """)
        @TextInputWidget(minLength = 1)
        String m_columnName = "Column ";

        @Widget(title = "Data type", description = """
                Data type of the column
                """)
        @ChoicesWidget(choices = ColTypeChoiceProvider.class)
        ColType m_colType = ColType.String;
    }

    static final class BasicModeSettings implements DefaultNodeSettings {

        @Widget(title = "Number of columns", description = """
                Number of columns to create
                """)
        @NumberInputWidget(min = 1, max = 100000)
        int m_numberOfColumns = 5;

        @Widget(title = "Column name prefix", description = """
                Prefix used to create columns
                """)
        @TextInputWidget(minLength = 1)
        String m_columnPrefix = "Column ";

        @Widget(title = "Data type", description = """
                Data type of the columns
                """)
        @ChoicesWidget(choices = ColTypeChoiceProvider.class)
        ColType m_colType = ColType.String;
    }

}
