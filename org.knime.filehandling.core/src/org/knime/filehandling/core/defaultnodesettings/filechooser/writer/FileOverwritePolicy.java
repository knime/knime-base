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
 *   Jun 4, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser.writer;

import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;

import org.knime.core.node.util.ButtonGroupEnumInterface;

/**
 * Policy how to proceed when output file exists (overwrite, fail, append).
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public enum FileOverwritePolicy implements ButtonGroupEnumInterface {

        /** Overwrite existing file. */
        OVERWRITE(new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING}, "overwrite"),

        /** Append table content to existing file. */
        APPEND(new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.APPEND}, "append"),

        /** Ignore existing files i.e. don't overwrite or append but also don't fail. */
        IGNORE(new OpenOption[]{StandardOpenOption.CREATE_NEW}, "ignore"),

        /** Fail during execution. Neither overwrite nor append. */
        FAIL(new OpenOption[]{StandardOpenOption.CREATE_NEW}, "fail");

    private final OpenOption[] m_openOption;

    private final String m_description;

    private FileOverwritePolicy(final OpenOption[] openOption, final String description) {
        m_openOption = openOption;
        m_description = description;
    }

    /**
     * @return an array of {@link OpenOption} used for opening an {@link OutputStream}
     */
    public OpenOption[] getOpenOptions() {
        return m_openOption;
    }

    @Override
    public String getText() {
        return m_description;
    }

    @Override
    public String getActionCommand() {
        return name();
    }

    @Override
    public String getToolTip() {
        return m_description;
    }

    @Override
    public boolean isDefault() {
        return this == FAIL;
    }

}
