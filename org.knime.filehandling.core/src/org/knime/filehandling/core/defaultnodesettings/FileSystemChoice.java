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
 *   Sep 2, 2019 (Julian Bunzel): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Class encapsulating the four different types of file system choices.
 *
 * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
 */
public class FileSystemChoice {

    /**
     * Enum stating the four different types of file system choices.
     *
     * @author Julian Bunzel, KNIME GmbH, Berlin, Germany
     */
    public enum Choice {

            /** Local file system */
            LOCAL_FS,

            /** KNIME file system */
            KNIME_FS,

            /** Custom URL file system */
            CUSTOM_URL_FS,

            /** File system provided at the input port */
            CONNECTED_FS;
    }

    /** Instance of local file system choice */
    private static final FileSystemChoice LOCAL_FS_CHOICE = new FileSystemChoice(Choice.LOCAL_FS, "Local File System");

    /** Instance of KNIME file system choice */
    private static final FileSystemChoice KNIME_FS_CHOICE = new FileSystemChoice(Choice.KNIME_FS, "KNIME");

    /** Instance of Custom URL file system choice */
    private static final FileSystemChoice CUSTOM_URL_CHOICE = new FileSystemChoice(Choice.CUSTOM_URL_FS, "Custom URL");

    /** List of default file system choices */
    private static final List<FileSystemChoice> DEFAULT_CHOICES =
        Arrays.asList(LOCAL_FS_CHOICE, KNIME_FS_CHOICE, CUSTOM_URL_CHOICE);

    /** Identifier string */
    private final String m_id;

    /** Choice identifying the type of the file system */
    private final Choice m_type;

    /**
     * Creates a new instance of {@code FileSystemChoice}.
     *
     * @param type the choice
     * @param id the string identifier to be shown in dialog
     */
    private FileSystemChoice(final Choice type, final String id) {
        m_type = type;
        m_id = id;
    }

    /**
     * Returns the choice/type of file system
     *
     * @return the choice/type of file system
     */
    public final Choice getType() {
        return m_type;
    }

    /**
     * Returns the identifier
     *
     * @return the identifier
     */
    public final String getId() {
        return m_id;
    }

    @Override
    public final String toString() {
        // Overriding. Showing the identifer, since this is what we want to see in dialog components...
        return getId();
    }

    /**
     * Returns the default file system choices.
     *
     * @return the default file system choices
     */
    public static final List<FileSystemChoice> getDefaultChoices() {
        return Collections.unmodifiableList(DEFAULT_CHOICES);
    }

    /**
     * Returns the static instance for the local file system choice.
     *
     * @return instance for the local file system choice
     */
    public static final FileSystemChoice getLocalFsChoice() {
        return LOCAL_FS_CHOICE;
    }

    /**
     * Returns the static instance for the custom url file system choice.
     *
     * @return instance for the custom url file system choice
     */
    public static final FileSystemChoice getCustomFsUrlChoice() {
        return CUSTOM_URL_CHOICE;
    }

    /**
     * Returns the static instance for the KNIME file system choice.
     *
     * @return instance for the KNIME file system choice
     */
    public static final FileSystemChoice getKnimeFsChoice() {
        return KNIME_FS_CHOICE;
    }

    /**
     * Creates and returns a new flow variable FileSystemChoice with a given id
     *
     * @param id identifier for the FileSystemChoice
     * @return new flow variable FileSystemChoice with a given id
     */
    public static final FileSystemChoice createConnectedFileSystemChoice(final String id) {
        return new FileSystemChoice(Choice.CONNECTED_FS, id);
    }

    /**
     * Returns a FileSystemChoice identified by the given id. Either one one of the default choices is returned or a new
     * flow variable FileSystemChoice is created and returned.
     *
     * @param fileSystem file system identifier
     * @return A FileSystemChoice identified by the given id
     */
    public static FileSystemChoice getChoiceFromId(final String fileSystem) {
        Optional<FileSystemChoice> choice =
            DEFAULT_CHOICES.stream().filter(c -> c.getId().equals(fileSystem)).findFirst();
        if (choice.isPresent()) {
            return choice.get();
        }
        return createConnectedFileSystemChoice(fileSystem);
    }

    @Override
    public boolean equals(final Object other) {
        if(!(other instanceof FileSystemChoice)) {
            return false;
        }
        FileSystemChoice otherChoice = (FileSystemChoice) other;
        return m_id.equals(otherChoice.m_id) && m_type.equals(otherChoice.m_type);
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashBuiler = new HashCodeBuilder();
        hashBuiler.append(m_id);
        hashBuiler.append(m_type);
        return hashBuiler.build();
    }
}
