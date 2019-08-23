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
 *   21.08.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.s3;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.connections.attributes.FSBasicAttributes;
import org.knime.filehandling.core.connections.attributes.FSFileAttributes;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectMetadata;

/**
 * {@link Path} implementation for {@link S3FileSystem}
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class S3Path implements FSPath {

    /** Path separator for {@link S3FileSystem} */
    public static final String PATH_SEPARATOR = "/";

    private final String m_fullPath;

    private final ArrayList<String> m_blobParts;

    private final S3FileSystem m_fileSystem;

    private boolean m_isAbsolute;


    /**
     * Creates an S3Path from the given path string
     *
     * @param fileSystem the file system
     * @param pathString the string representing the S3 path
     */
    public S3Path(final S3FileSystem fileSystem, final String pathString) {

        m_fileSystem = fileSystem;
        m_fullPath = pathString;
        m_isAbsolute = m_fullPath.startsWith(PATH_SEPARATOR);
        m_blobParts = getPathSplits(m_fullPath);
    }

    /**
     * Creates an S3Path from the given bucket name and object key
     *
     * @param fileSystem the file system
     * @param bucketName the bucket name
     * @param key the object key
     */
    public S3Path(final S3FileSystem fileSystem, final String bucketName, final String key) {
        m_fileSystem = fileSystem;
        m_fullPath = bucketName + PATH_SEPARATOR + key;
        m_isAbsolute = true;
        m_blobParts = getPathSplits(m_fullPath);
    }

    private ArrayList<String> getPathSplits(final String pathString) {


        //Prepare path string for splitting
        String path = pathString;
        if (isAbsolute()) {
            path = path.substring(1);
        }


        if (path.endsWith(PATH_SEPARATOR)) {
            path = path.substring(0, path.length()-1);
        }



        ArrayList<String> splitList = new ArrayList<>();
        if(path.isEmpty()) {
            return splitList;
        }

        int index = 0;
        while(index != -1) {
            int  secondOccurence = path.indexOf(PATH_SEPARATOR, index+1);
            if(secondOccurence == -1) {
                splitList.add(path.substring(index));
            } else {
                splitList.add(path.substring(index, secondOccurence+1));
            }
            index = secondOccurence;
        }



        return splitList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public S3FileSystem getFileSystem() {
        return m_fileSystem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAbsolute() {
        return m_isAbsolute;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getRoot() {
        if (isAbsolute()) {
            return new S3Path(m_fileSystem, m_blobParts.get(0) + PATH_SEPARATOR);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getFileName() {
        if (m_blobParts.isEmpty()) {
            return null;
        }

        return new S3Path(m_fileSystem, m_blobParts.get(m_blobParts.size() - 1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getParent() {
        if (m_blobParts.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder(PATH_SEPARATOR);
        Iterator<String> iter = m_blobParts.iterator();
        for ( String part = iter.next(); iter.hasNext(); part = iter.next()) {
           sb.append(part);
           sb.append(PATH_SEPARATOR);
        }

        return new S3Path(m_fileSystem, sb.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNameCount() {
        return m_blobParts.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getName(final int index) {
        if (index < 0 || index >= m_blobParts.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < index; i++) {
            sb.append(m_blobParts.get(i));
        }

        return new S3Path(m_fileSystem, sb.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path subpath(final int beginIndex, final int endIndex) {
        if(beginIndex > endIndex) {
            throw new IllegalArgumentException("Begin index must not be greater than end index");
        }
        if (beginIndex < 0 || beginIndex >= m_blobParts.size() ||
                endIndex < 0 || endIndex >= m_blobParts.size()) {
            throw new ArrayIndexOutOfBoundsException();
        }

        StringBuilder sb = new StringBuilder();
        for (int i = beginIndex; i < endIndex; i++) {
            sb.append(m_blobParts.get(i));
        }

        return new S3Path(m_fileSystem, sb.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startsWith(final Path other) {
        if (!(other instanceof S3Path)) {
            return false;
        }

        if (other.getNameCount() > getNameCount()) {
            return false;
        }

        if (other.isAbsolute() && !isAbsolute()) {
            return false;
        }

        if (!other.getRoot().equals(getRoot())) {
            return false;
        }

        for (int i = 0; i < other.getNameCount(); i++) {
            if (!other.getName(i).equals(getName(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean startsWith(final String other) {
        final S3Path path = new S3Path(m_fileSystem, other);
        return startsWith(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean endsWith(final Path other) {
        return m_fullPath.endsWith(other.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean endsWith(final String other) {
       return m_fullPath.endsWith(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path normalize() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(final Path other) {
        S3Path otherPath = toS3Path(other);
        if(otherPath.m_isAbsolute) {
            return other;
        }
        if(otherPath.getNameCount() == 0) {
            return this;
        }

        String resolvedPath = m_fullPath + otherPath.getFullPath();

        return new S3Path(m_fileSystem, resolvedPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolve(final String other) {
        return resolve(new S3Path(m_fileSystem, other));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolveSibling(final Path other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path resolveSibling(final String other) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path relativize(final Path other) {
        S3Path path = toS3Path(other);
        if(!this.m_isAbsolute) {
            return this;
        }
        if(!path.isAbsolute()) {
            throw new IllegalArgumentException("Input path must be absolut");
        }
        URI relative = toUri().relativize(other.toUri());

        return new S3Path(m_fileSystem, relative.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI toUri() {
        //FIXME ACCESS STRING
        return URI.create("s3://" + m_fullPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toAbsolutePath() {
        if(isAbsolute()) {
            return this;
        }
        throw new IllegalStateException(String.format("Realtive path %s cannot be made absolut.", this));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path toRealPath(final LinkOption... options) throws IOException {
        return toAbsolutePath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File toFile() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>[] events, final Modifier... modifiers)
        throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchKey register(final WatchService watcher, final Kind<?>... events) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Path> iterator() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final Path other) {
        S3Path otherPath = toS3Path(other);
        return m_fullPath.compareTo(otherPath.getFullPath());
    }

    private static S3Path toS3Path(final Path other) {
        if(!(other instanceof S3Path)) {
            throw new IllegalArgumentException("Input path must be an S3 Path");
        }
        return (S3Path) other;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof S3Path)) {
            return false;
        }

        final S3Path path = (S3Path)other;

        if (!m_fullPath.equals(path.m_fullPath)) {
            return false;
        }
        return true;
    }

    /**
     * @return the bucket name
     */
    public String getBucketName() {
        if(m_blobParts.isEmpty()) {
            return null;
        }
        return m_blobParts.get(0);
    }

    /**
     * @return the key of the object i.e. the path without the bucket name.
     */
    public String getKey() {
        int start = 0;
        if(isAbsolute()) {
            start = 1;
        }
        if(m_blobParts.size() == 1) {
            return "";
        }
        return m_fullPath.substring(m_fullPath.indexOf(PATH_SEPARATOR,start+1)+1);
    }

    @Override
    public String toString() {
        return m_fullPath;
    }

    /**
     * @return the full path as String
     */
    public String getFullPath() {
        return m_fullPath;
    }

    /**
     * @return a {@link Bucket} if a bucket with that name exists in S3, null otherwise.
     */
    public Bucket getBucket() {

        for (final Bucket buck : m_fileSystem.getClient().listBuckets()) {
            if (buck.getName().equals(getBucketName())) {
                return buck;
            }
        }
        return null;
    }

    /**
     * @return whether the path is a directory
     */
    public boolean isDirectory() {
        return m_fullPath.endsWith(PATH_SEPARATOR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FSFileAttributes getFileAttributes(final Class<?> type) {
        if (type == BasicFileAttributes.class) {
            return new FSFileAttributes(!isDirectory(), this, p -> {

                FileTime lastmod = FileTime.fromMillis(0L);
                long size = 0;

                S3Path s3Path = (S3Path) p;
                    try {
                        final ObjectMetadata objectMetadata = s3Path.getFileSystem().getClient()
                            .getObjectMetadata(s3Path.getBucketName(), s3Path.getKey());

                        final Date metaDataLastMod = objectMetadata.getLastModified();

                        lastmod = metaDataLastMod != null ? FileTime.from(metaDataLastMod.toInstant())
                            : FileTime.from(s3Path.getBucket().getCreationDate().toInstant());
                        size = objectMetadata.getContentLength();

                    } catch (final Exception e) {
                        // If we do not have metadata we use fall back values
                    }


                return new FSBasicAttributes(lastmod, lastmod, lastmod, size, false, false);
            });
    }
        throw new UnsupportedOperationException(String.format("only %s supported", BasicFileAttributes.class));
    }


    /**
     * @return whether this is the virtual S3 root "/"
     */
    public boolean isVirtualRoot() {
        return m_blobParts.isEmpty();
                }
}
