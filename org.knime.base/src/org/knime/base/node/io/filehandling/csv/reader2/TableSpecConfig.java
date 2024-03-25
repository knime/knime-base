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
 *   Mar 25, 2024 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.util.function.Supplier;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings.DefaultNodeSettingsContext;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidgetElementId;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ElementTitleProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueRef;

/**
 *
 * @author Paul Bärnreuther
 */
public class TableSpecConfig implements WidgetGroup {

    /**
     * E.g. for columns age, name, working_class
     *
     *
      uischema:
          "tableSpecConfig": {
            "options": {
              "format": "fixedArrayLayout",
              "items": [
                { "id": "age", "text": "age" },
                { "id": "name", "text": "name" },
                { "id": "working_class", "text": "working_class" }
              ],
              "detail": {
                "age": [
                  { "type": "Control", "scope": "#/properties/hide" },
                  { "type": "Control", "scope": "#/properties/outputName" },
                  {
                    "type": "Control",
                    "scope": "#/properties/outputType"
                    // Possible values for an integer column
                  }
                ],
                "name": [
                  { "type": "Control", "scope": "#/properties/hide" },
                  { "type": "Control", "scope": "#/properties/outputName" },
                  {
                    "type": "Control",
                    "scope": "#/properties/outputType"
                    // Possible values for a string column
                  }
                ],
                "working_class": [
                  { "type": "Control", "scope": "#/properties/hide" },
                  { "type": "Control", "scope": "#/properties/outputName" },
                  {
                    "type": "Control",
                    "scope": "#/properties/outputType"
                    // Possible values for a string column
                  }
                ]
              }
            }
          }
        }

     */

    static final class IdAsElementTitleProvider implements ElementTitleProvider {

        private Supplier<String> m_elementCSVHeader;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_elementCSVHeader = initializer.computeFromValueSupplier(TransformationTableEntry.CSVHeader.class);
        }

        @Override
        public String computeState(final DefaultNodeSettingsContext context) {
            return m_elementCSVHeader.get();
        }

    }

    class IndexReference implements ValueRef<Integer> {

    }

    @ArrayWidget(elementTitleProvider = IdAsElementTitleProvider.class)
    TransformationTableEntry[] m_transformationTable;


    static final class TransformationTableEntry implements WidgetGroup {

        static final class CSVHeader implements ValueRef<String> {

        }

        @ArrayWidgetElementId(valueRef = CSVHeader.class)
        String m_csvHeader;

        @Widget(title = "Include in Output", description = "TODO")
        boolean m_includeInOutput;

        @Widget(title = "New name", description = "TODO")
        String m_outputColumnName;

    }

}
