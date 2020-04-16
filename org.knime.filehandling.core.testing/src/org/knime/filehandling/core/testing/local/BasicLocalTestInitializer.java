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
 *   Dec 17, 2019 (Tobias Urhaug, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.testing.local;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.knime.filehandling.core.connections.local.LocalPath;
import org.knime.filehandling.core.testing.FSTestInitializer;

/**
 * Implementation of a test initializer using the local file system.
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
public abstract class BasicLocalTestInitializer implements FSTestInitializer {

	private final String m_rootFolder;
	private Path m_currTempFolder;

	protected BasicLocalTestInitializer(final String root) {
		m_rootFolder = root;
	}

	@Override
	public void beforeTestCase() throws IOException {
		m_currTempFolder = Files.createTempDirectory(Paths.get(m_rootFolder), null);
	}

	@Override
	public void afterTestCase() throws IOException {
		FileUtils.deleteDirectory(m_currTempFolder.toFile());
	}

	protected Path getTempFolder() {
		return m_currTempFolder;
	}

	protected Path createLocalFileWithContent(final String content, final String... pathComponents) {
		if (pathComponents == null || pathComponents.length == 0) {
			throw new IllegalArgumentException("path components can not be empty or null");
		}

		Path directories = m_currTempFolder;
		for (int i = 0; i < pathComponents.length - 1; i++) {
			directories = directories.resolve(pathComponents[i]);
		}

		final Path file = directories.resolve(pathComponents[pathComponents.length - 1]);
		try {
			Files.createDirectories(directories);
			Path createdPath = Files.createFile(file);
			try (BufferedWriter writer = Files.newBufferedWriter(createdPath)) {
				writer.write(content);
			}

			return createdPath;
		} catch (IOException e) {
			throw new UncheckedIOException("Exception while creating a file at ." + file.toString(), e);
		}
	}
}
