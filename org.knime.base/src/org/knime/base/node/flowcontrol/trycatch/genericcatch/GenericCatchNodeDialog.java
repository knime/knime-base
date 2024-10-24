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
 *   02.12.2014 (adae): created
 */
package org.knime.base.node.flowcontrol.trycatch.genericcatch;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * The dialog for all catch nodes.
 *
 * @author Iris Adae, University of Konstanz
 * @since 2.11.1
 */
final class GenericCatchNodeDialog extends DefaultNodeSettingsPane {

    /**
     * Default constructor.
     */
    GenericCatchNodeDialog() {
        createNewGroup("Error Variables");
        final SettingsModelBoolean alwaysPopulate = GenericCatchNodeModel.getAlwaysPopulate();
        addDialogComponent(new DialogComponentBoolean(alwaysPopulate, "Always populate error variable"));
        // The variable name input.
        final SettingsModelString defaultVariable = GenericCatchNodeModel.getDefaultVariable(alwaysPopulate);
        addDialogComponent(new DialogComponentString(defaultVariable,
            "Default for \"%s\" variable:".formatted(GenericCatchNodeModel.VAR_FAILING_NAME)));
        // The error message input.
        final SettingsModelString defaultMessage = GenericCatchNodeModel.getDefaultMessage(alwaysPopulate);
        addDialogComponent(new DialogComponentString(defaultMessage,
            "Default for \"%s\" variable:".formatted(GenericCatchNodeModel.VAR_FAILING_MESSAGE)));
        // The error details input.
        final SettingsModelString defaultDetails = GenericCatchNodeModel.getDefaultMessage(alwaysPopulate);
        addDialogComponent(new DialogComponentString(defaultDetails,
            "Default for \"%s\" variable:".formatted(GenericCatchNodeModel.VAR_FAILING_DETAILS)));
        // The stacktrace input.
        final SettingsModelString defaultStackTrace = GenericCatchNodeModel.getDefaultStackTrace(alwaysPopulate);
        addDialogComponent(new DialogComponentString(defaultStackTrace,
            "Default for \"%s\" variable:".formatted(GenericCatchNodeModel.VAR_FAILING_STACKTRACE)));
        closeCurrentGroup();

        createNewGroup("Scope Variables");
        SettingsModelBoolean propagateVariablesModel = GenericCatchNodeModel.createPropagateVariablesModel();
        addDialogComponent(new DialogComponentBoolean(propagateVariablesModel, "Propagate variables"));
        closeCurrentGroup();
    }
}
