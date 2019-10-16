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
 *   Sep 6, 2019 (bjoern): created
 */
package org.knime.filehandling.core.defaultnodesettings;

/**
* FIXME: this code is copied from org.knime.kerberos and should be moved to org.knime.core so that
* it is usable everywhere.
*
* @author Bjoern Lohrmann, KNIME GmbH
*/
public class ExceptionUtil {

   /**
    * Returns deepest non empty error message from the given exception and its cause stack.
    *
    * @param t A throwable, possibly with cause chain.
    * @param appendType Whether to append the type of the deepest exception with non-empty error message to the
    *            returned string.
    * @return deepest non empty error message or null.
    */
   public static String getDeepestErrorMessage(final Throwable t, final boolean appendType) {
       String deeperMsg = null;
       if (t.getCause() != null) {
           deeperMsg = getDeepestErrorMessage(t.getCause(), appendType);
       }

       if (deeperMsg != null && deeperMsg.length() > 0) {
           return deeperMsg;
       } else if (t.getMessage() != null && t.getMessage().length() > 0) {
           if (appendType) {
               return String.format("%s (%s)", t.getMessage(), t.getClass().getSimpleName());
           } else {
               return t.getMessage();
           }
       } else {
           return null;
       }
   }
}

