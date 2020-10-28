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
 *   Oct 28, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.util.compress.archiver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.ar.ArArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioArchiveEntry;
import org.apache.commons.compress.archivers.cpio.CpioConstants;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.knime.filehandling.core.connections.FSFiles;

/**
 * Factory to create {@link ArchiveEntryCreator}s.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class ArchiveEntryFactory {

    private ArchiveEntryFactory() {
    }

    /**
     * Returns the concrete instance of {@link ArchiveEntryCreator} associated with the given identifier.
     *
     * @param archiver the identifier of the {@link ArchiveEntryCreator} to create
     * @return the concrete instance of {@link ArchiveEntryCreator} associated with the given identifier
     */
    public static ArchiveEntryCreator getArchiveEntryCreator(final String archiver) {
        if (archiver.equalsIgnoreCase(ArchiveStreamFactory.AR)) {
            return new ArEntryCreator();
        }
        if (archiver.equalsIgnoreCase(ArchiveStreamFactory.CPIO)) {
            return new CPIOEntryCreator();
        }
        if (archiver.equalsIgnoreCase(ArchiveStreamFactory.JAR)) {
            return new JarEntryCreator();
        }
        if (archiver.equalsIgnoreCase(ArchiveStreamFactory.TAR)) {
            return new TarEntryCreator();
        }
        if (archiver.equalsIgnoreCase(ArchiveStreamFactory.ZIP)) {
            return new ZIPEntryCreator();
        }
        throw new IllegalArgumentException("Unsupported type: " + archiver);
    }

}

abstract class EntryCreator implements ArchiveEntryCreator {

    @Override
    public ArchiveEntry apply(final Path p, final String entryName) throws IOException {
        final String newEntryName;
        if (FSFiles.isDirectory(p)) {
            if (entryName.endsWith("/") || entryName.endsWith("\\")) {
                newEntryName = entryName;
            } else {
                newEntryName = entryName + "/";
            }
        } else {
            newEntryName = entryName;
        }
        return createEntry(p, newEntryName);
    }

    long getSize(final Path p) throws IOException {
        return Files.size(p);
    }

    abstract ArchiveEntry createEntry(Path p, String entryName) throws IOException;

}

final class ArEntryCreator extends EntryCreator {

    @Override
    ArArchiveEntry createEntry(final Path p, final String entryName) throws IOException {
        return new ArArchiveEntry(entryName, getSize(p));
    }

}

final class CPIOEntryCreator extends EntryCreator {

    @Override
    CpioArchiveEntry createEntry(final Path p, final String entryName) throws IOException {
        final long size;
        final int mode;
        if (FSFiles.isDirectory(p)) {
            size = 0;
            mode = CpioConstants.C_ISDIR;
        } else {
            size = getSize(p);
            mode = CpioConstants.C_ISREG;
        }
        CpioArchiveEntry cpioArchiveEntry = new CpioArchiveEntry(entryName, size);
        cpioArchiveEntry.setMode(mode);
        return cpioArchiveEntry;
    }

}

final class JarEntryCreator extends EntryCreator {

    @Override
    JarArchiveEntry createEntry(final Path p, final String entryName) throws IOException {
        return new JarArchiveEntry(entryName);
    }

}

final class TarEntryCreator extends EntryCreator {

    @Override
    TarArchiveEntry createEntry(final Path p, final String entryName) throws IOException {
        final TarArchiveEntry tarArchiveEntry = new TarArchiveEntry(entryName);
        tarArchiveEntry.setSize(getSize(p));
        return tarArchiveEntry;
    }

}

final class ZIPEntryCreator extends EntryCreator {

    @Override
    ZipArchiveEntry createEntry(final Path p, final String entryName) throws IOException {
        return new ZipArchiveEntry(entryName);
    }

}
