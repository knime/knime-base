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

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.knime.cloud.core.util.port.CloudConnectionInformation;
import org.knime.core.util.KnimeEncryption;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;

/**
 * The Amazon S3 implementation of the {@link FileSystem} interface.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class S3FileSystem extends FileSystem {

    private final S3FileSystemProvider m_provider;

    private final AmazonS3 m_client;

    private final URI m_uri;

    private final FileStore m_fileStore = new S3FileStore();

    private static final String PATH_SEPARATOR = S3Path.PATH_SEPARATOR;

    /**
     * Constructs an S3FileSystem for the given URI
     *
     * @param provider the {@link S3FileSystemProvider}
     * @param uri the URI for the file system
     * @param env the environment map
     * @param connectionInformation the {@link CloudConnectionInformation}
     */
    public S3FileSystem(final S3FileSystemProvider provider, final URI uri, final Map<String, ?> env,
        final S3CloudConnectionInformation connectionInformation) {
        m_provider = provider;
        try {
            if (connectionInformation.switchRole()) {
                m_client = getRoleAssumedS3Client(connectionInformation);
            } else {
                m_client = getS3Client(connectionInformation);
            }
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
        m_uri = uri;
    }

    private static AmazonS3 getS3Client(final S3CloudConnectionInformation connectionInformation) throws Exception {
        final ClientConfiguration clientConfig =
            new ClientConfiguration().withConnectionTimeout(connectionInformation.getTimeout());

        final AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard().withClientConfiguration(clientConfig)
            .withRegion(connectionInformation.getHost());

        if (!connectionInformation.useKeyChain()) {
            final AWSCredentials credentials = getCredentials(connectionInformation);
            builder.withCredentials(new AWSStaticCredentialsProvider(credentials));
        }

        return builder.build();
    }

    private static AmazonS3 getRoleAssumedS3Client(final S3CloudConnectionInformation connectionInformation)
        throws Exception {
        final AWSSecurityTokenServiceClientBuilder builder =
            AWSSecurityTokenServiceClientBuilder.standard().withRegion(connectionInformation.getHost());
        if (!connectionInformation.useKeyChain()) {
            final AWSCredentials credentials = getCredentials(connectionInformation);
            builder.withCredentials(new AWSStaticCredentialsProvider(credentials));
        }

        final AWSSecurityTokenService stsClient = builder.build();

        final AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest().withRoleArn(buildARN(connectionInformation))
            .withDurationSeconds(3600).withRoleSessionName("KNIME_S3_Connection");

        final AssumeRoleResult assumeResult = stsClient.assumeRole(assumeRoleRequest);

        final BasicSessionCredentials tempCredentials =
            new BasicSessionCredentials(assumeResult.getCredentials().getAccessKeyId(),
                assumeResult.getCredentials().getSecretAccessKey(), assumeResult.getCredentials().getSessionToken());

        return AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(tempCredentials))
            .withRegion(connectionInformation.getHost()).build();
    }

    private static AWSCredentials getCredentials(final S3CloudConnectionInformation connectionInformation)
        throws Exception {

        if (connectionInformation.isUseAnonymous()) {
            return new AnonymousAWSCredentials();
        }
        final String accessKeyId = connectionInformation.getUser();
        final String secretAccessKey = KnimeEncryption.decrypt(connectionInformation.getPassword());

        return new BasicAWSCredentials(accessKeyId, secretAccessKey);

    }

    private static String buildARN(final CloudConnectionInformation connectionInformation) {
        return "arn:aws:iam::" + connectionInformation.getSwitchRoleAccount() + ":role/"
            + connectionInformation.getSwitchRoleName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystemProvider provider() {

        return m_provider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        m_client.shutdown();
        m_provider.removeFileSystem(m_uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return m_provider.isOpen(m_uri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSeparator() {
        return PATH_SEPARATOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Path> getRootDirectories() {
        final List<Path> paths = new ArrayList<>();

        for (final Bucket bucket : getBuckets()) {
            paths.add(new S3Path(this, m_fileStore.name() + bucket.getName() + PATH_SEPARATOR));
        }
        if (paths.isEmpty()) {
            //In this case we just add the virtual root
            paths.add(new S3Path(this, m_fileStore.name()));
        }

        return Collections.unmodifiableList(paths);
    }

    private Iterable<Bucket> getBuckets() {
        try {
            return m_client.listBuckets();
        } catch (final SdkClientException e) {
            // In case of anonymous browsing listBuckets() will fail.
            return Collections.emptyList();
        }
    }

    /**
     * {@inheritDoc}
     */

    @Override
    public Iterable<FileStore> getFileStores() {
        final List<FileStore> fileStores = new ArrayList<>();

        fileStores.add(m_fileStore);

        return fileStores;
    }

    /**
     * @return the {@link AmazonS3} client for this file system
     */
    public AmazonS3 getClient() {
        return m_client;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> supportedFileAttributeViews() {
        final Set<String> set = new HashSet<>();
        set.add("basic");
        set.add("posix");
        return set;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Path getPath(final String first, final String... more) {
        if (more.length == 0) {
            return new S3Path(this, first);
        } else {
            final StringBuilder sb = new StringBuilder(first);
            for (final String subPath : more) {
                if (!(PATH_SEPARATOR.charAt(0) == sb.charAt(sb.length() - 1) || subPath.endsWith(PATH_SEPARATOR))) {
                    sb.append(PATH_SEPARATOR);
                }
                sb.append(subPath);
            }
            return new S3Path(this, sb.toString());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

}
