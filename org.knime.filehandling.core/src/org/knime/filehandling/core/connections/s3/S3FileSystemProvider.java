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
 *   20.08.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.s3;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knime.filehandling.core.connections.FSPath;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.S3Object;

/**
 * File system provider for {@link S3FileSystem}s
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class S3FileSystemProvider extends FileSystemProvider {

    /**  */
    public static final String CONNECTION_INFORMATION = "ConnectionInformation";

    HashMap<URI, FileSystem> m_fileSystems = new HashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public String getScheme() {
        return "s3";
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("resource")
    @Override
    public FileSystem newFileSystem(final URI uri, final Map<String, ?> env) throws IOException {
        S3CloudConnectionInformation connInfo = null;
        if (env.containsKey(CONNECTION_INFORMATION)) {
            connInfo = (S3CloudConnectionInformation)env.get(CONNECTION_INFORMATION);
        }
        if (!m_fileSystems.containsKey(uri)) {
            m_fileSystems.put(uri, new S3FileSystem(this, uri, env, connInfo));
        }

        return m_fileSystems.get(uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystem getFileSystem(final URI uri) {
        if (!m_fileSystems.containsKey(uri)) {
            throw new FileSystemNotFoundException(String.format("No filesystem for uri %s", uri));
        }
        return m_fileSystems.get(uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getPath(final URI uri) {
        return getFileSystem(uri).getPath(uri.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SeekableByteChannel newByteChannel(final Path path, final Set<? extends OpenOption> options,
        final FileAttribute<?>... attrs) throws IOException {
        return new S3SeekableByteChannel(toS3Path(path), options);
    }

    @Override
    @SuppressWarnings("resource")
    public InputStream newInputStream(final Path path, final OpenOption... options) throws IOException {

        final S3Path s3path = toS3Path(path);
        final S3Object object = s3path.getFileSystem().getClient().getObject(s3path.getBucketName(), s3path.getKey());

        final InputStream inputStream = object.getObjectContent();

        if (inputStream == null) {
            object.close();
            throw new IOException(String.format("Could not read path %s", s3path));
        }

        return inputStream;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DirectoryStream<Path> newDirectoryStream(final Path dir, final Filter<? super Path> filter)
        throws IOException {
        final S3Path path = toS3Path(dir);
        return new S3DirectoryStream(path, filter);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDirectory(final Path dir, final FileAttribute<?>... attrs) throws IOException {
        final S3Path s3Path = toS3Path(dir);

        if (exists(s3Path)) {
            throw new FileAlreadyExistsException(String.format("Already exists: %s", s3Path));
        }

        final Bucket bucket = s3Path.getBucket();

        final String bucketName = s3Path.getBucketName();
        if (bucket == null) {
            s3Path.getFileSystem().getClient().createBucket(bucketName);
        }

        // create empty object
        final ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);
        final String objectKey = s3Path.getKey().endsWith("/") ? s3Path.getKey() : s3Path.getKey() + "/";
        s3Path.getFileSystem().getClient().putObject(bucketName, objectKey, new ByteArrayInputStream(new byte[0]),
            metadata);

    }

    boolean exists(final S3Path path) {
        if (path.getBucketName() == null) {
            //This is the fake S3 root.
            return true;
        }
        final AmazonS3 client = path.getFileSystem().getClient();

        if (!path.getKey().isEmpty()) {
            return client.doesObjectExist(path.getBucketName(), path.getKey());
        } else {
            return client.doesBucketExistV2(path.getBucketName());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final Path path) throws IOException {
        final S3Path s3Path = toS3Path(path);
        final AmazonS3 client = s3Path.getFileSystem().getClient();
        try {
            if (!s3Path.getKey().isEmpty()) {
                client.deleteObject(s3Path.getBucketName(), s3Path.getKey());
            } else {
                client.deleteBucket(s3Path.getBucketName());
            }
        } catch (final Exception ex) {
            throw new IOException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copy(final Path source, final Path target, final CopyOption... options) throws IOException {
        final S3Path sourceS3Path = toS3Path(source);
        final S3Path targetS3Path = (S3Path)target;
        final AmazonS3 client = sourceS3Path.getFileSystem().getClient();
        try {
            client.copyObject(sourceS3Path.getBucketName(), sourceS3Path.getKey(), targetS3Path.getBucketName(),
                targetS3Path.getKey());
        } catch (final Exception ex) {
            throw new IOException(ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void move(final Path source, final Path target, final CopyOption... options) throws IOException {
        final S3Path sourceS3Path = toS3Path(source);
        final S3Path targetS3Path = (S3Path)target;
        final AmazonS3 client = sourceS3Path.getFileSystem().getClient();

        try {
            client.copyObject(sourceS3Path.getBucketName(), sourceS3Path.getKey(), targetS3Path.getBucketName(),
                targetS3Path.getKey());
        } catch (final Exception ex) {
            throw new IOException(ex);
        }

        delete(sourceS3Path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSameFile(final Path path, final Path path2) throws IOException {
        return path.equals(path2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isHidden(final Path path) throws IOException {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileStore getFileStore(final Path path) throws IOException {

        return new S3FileStore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkAccess(final Path path, final AccessMode... modes) throws IOException {
        final S3Path s3Path = toS3Path(path);
        if (s3Path.isVirtualRoot()) {
            return;
        }
        if (!exists(s3Path)) {
            throw new AccessDeniedException(s3Path.getFullPath());
        }

        AccessControlList acl;
        try {
            acl = s3Path.getFileSystem().getClient().getObjectAcl(s3Path.getBucketName(), s3Path.getKey());
        } catch (final AmazonServiceException ex) {
            throw new AccessDeniedException(ex.getMessage());
        }
        for (final AccessMode mode : modes) {
            switch (mode) {
                case EXECUTE:
                    throw new AccessDeniedException(s3Path.getFullPath());
                case READ:
                    if (!containsPermission(EnumSet.of(Permission.FullControl, Permission.Read), acl)) {
                        throw new AccessDeniedException(s3Path.getFullPath(), null, "file is not readable");
                    }
                    break;
                case WRITE:
                    if (!containsPermission(EnumSet.of(Permission.FullControl, Permission.Write), acl)) {
                        throw new AccessDeniedException(s3Path.getFullPath(), null, "file is not readable");
                    }
                    break;
            }
        }

    }

    private static boolean containsPermission(final EnumSet<Permission> permissions, final AccessControlList acl) {
        for (final Grant grant : acl.getGrantsAsList()) {
            if (grant.getGrantee().getIdentifier().equals(acl.getOwner().getId())
                && permissions.contains(grant.getPermission())) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(final Path path, final Class<V> type,
        final LinkOption... options) {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <A extends BasicFileAttributes> A readAttributes(final Path path, final Class<A> type,
        final LinkOption... options) throws IOException {

        final FSPath fsPath = (FSPath)path;
        if (type == BasicFileAttributes.class || type == PosixFileAttributes.class) {
            return (A)fsPath.getFileAttributes(type);
        }

        throw new UnsupportedOperationException(String.format("only %s supported", BasicFileAttributes.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> readAttributes(final Path path, final String attributes, final LinkOption... options)
        throws IOException {
        // TODO implement
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAttribute(final Path path, final String attribute, final Object value, final LinkOption... options)
        throws IOException {
        // TODO implement
        throw new UnsupportedOperationException();

    }

    /**
     * Returns whether the file system for the given URI is open
     *
     * @param uri the URI to the file system
     * @return whether the file system for the given URI is open
     */
    public boolean isOpen(final URI uri) {
        return m_fileSystems.containsKey(uri);
    }

    /**
     * Removes the file system for the given URI from the list of file systems
     *
     * @param uri the URI to the file system
     */
    public void removeFileSystem(final URI uri) {
        m_fileSystems.remove(uri);
    }

    private static S3Path toS3Path(final Path path) {
        if (!(path instanceof S3Path)) {
            throw new IllegalArgumentException(
                String.format("Path has to be an instance of %s", S3Path.class.getName()));
        }
        return (S3Path)path;
    }
}
