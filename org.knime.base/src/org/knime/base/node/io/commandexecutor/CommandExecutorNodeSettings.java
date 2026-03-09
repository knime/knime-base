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
 *   Mar 6, 2026 (janniksemperowitsch): created
 */
package org.knime.base.node.io.commandexecutor;

import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 *
 * @author janniksemperowitsch
 */
//@SuppressWarnings("restriction")
final class CommandExecutorNodeSettings implements NodeParameters {

  /**
   * Widget that is always present, taking a bash command to execute
   */
  @Widget(title = "Command",
      description = "ability to run shell commands and invoke external processes."
  )
  @TextInputWidget(placeholder = "Write Command")
  String m_command = "Command";


  /**
   * Widget to define behavior of Stdout and Stderr
   */
  @Widget(title = "Merge stderr into stdout",
      description = "Redirects stderr to stdout." //equivalent to 2>&1
  )
  boolean m_mergeErrorStream = false;

  /**
   * Widget to define output formatting
   */
  @Widget(title = "Single cell output",
          description = "returns all lines from stdout as a single line."
  )
  boolean m_singleOutputCell = false;


  /**
   * Widget that holds subwidgets of Arguments
   */
  @Widget(title = "Arguments",
          description = "ability to pass arguments to previous command."
  )
  @ArrayWidget(
      addButtonText = "Add Argument",
      elementTitle = "Argument"
  )
  NewArgumentSettings[] m_newArgumentSettings = new NewArgumentSettings[]{new NewArgumentSettings()};


  static final class NewArgumentSettings implements NodeParameters {

      NewArgumentSettings(){}

      /**
       * Subwidget of Argument Widget
       * It's only present, when needed, taking a bash argument to enhance the command from first Widget command to execute
       */
      @Widget(title = "New Argument", description = "An Argument to enhance the base commands doings.") //----------change "doings" to something better
      @TextInputWidget(placeholder = "New Argument")
      String m_argumentToAppend = "New Argument";

  }
  static final class NewArgumentSettingsArrayRef implements ParameterReference<NewArgumentSettings[]> {}
}

