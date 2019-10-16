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
 *   28.08.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.attributes;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;
import java.util.function.Function;

/**
 * Class for file attributes that uses {@link Function} implementations to generate the necessary metadata lazy
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class FSFileAttributes implements PosixFileAttributes {

    private final boolean m_isRegularFile;

    private final Path m_fileKey;

    private FSBasicAttributes m_basicAttributes;

    private FSPosixAttributes m_posixAttributes;

    private final Function<Path, FSBasicAttributes> m_metaDataFunction;

    private final Function<Path, FSPosixAttributes> m_posixMetaDataFunction;

    /**
     * Constructor for the usage of only {@link BasicFileAttributes} methods. Usage of the {@link PosixFileAttributes}
     * methods will result in null values.
     *
     * @param isRegularFile whether this path represents a regular file
     * @param path the Path of the file
     * @param metaDataFunction the function that implements the generation of basic file attributes
     */
    public FSFileAttributes(final boolean isRegularFile, final Path path,
        final Function<Path, FSBasicAttributes> metaDataFunction) {

        this(isRegularFile, path, metaDataFunction, p -> new FSPosixAttributes(null, null, null));
    }

    /**
     * Constructor for usage as {@link PosixFileAttributes}.
     *
     * @param isRegularFile whether this path represents a regular file
     * @param path the Path of the file
     * @param metaDataFunction the function that implements the generation of basic file attributes
     * @param posixMetaDataFunction the function that implements the generation of POSIX file attributes
     */
    public FSFileAttributes(final boolean isRegularFile, final Path path,
        final Function<Path, FSBasicAttributes> metaDataFunction,
        final Function<Path, FSPosixAttributes> posixMetaDataFunction) {
        m_isRegularFile = isRegularFile;
        m_fileKey = path;
        m_metaDataFunction = metaDataFunction;
        m_posixMetaDataFunction = posixMetaDataFunction;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime lastModifiedTime() {
        generateBasicAttributes();
        return m_basicAttributes.lastModifiedTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime lastAccessTime() {
        generateBasicAttributes();
        return m_basicAttributes.lastAccessTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileTime creationTime() {
        generateBasicAttributes();
        return m_basicAttributes.creationTime();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRegularFile() {
        return m_isRegularFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDirectory() {
        return !m_isRegularFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSymbolicLink() {
        generateBasicAttributes();
        return m_basicAttributes.isSymbolicLink();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOther() {
        generateBasicAttributes();
        return m_basicAttributes.isOther();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long size() {
        generateBasicAttributes();
        return m_basicAttributes.size();
    }

    private void generateBasicAttributes() {
        if (m_basicAttributes == null) {
            m_basicAttributes = m_metaDataFunction.apply(m_fileKey);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object fileKey() {
        return m_fileKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserPrincipal owner() {
        generatePosixAttributes();
        return m_posixAttributes.owner();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GroupPrincipal group() {
        generatePosixAttributes();
        return m_posixAttributes.group();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<PosixFilePermission> permissions() {
        generatePosixAttributes();
        return m_posixAttributes.permissions();
    }

    private void generatePosixAttributes() {
        if (m_posixAttributes == null) {
            m_posixAttributes = m_posixMetaDataFunction.apply(m_fileKey);
        }
    }

}
