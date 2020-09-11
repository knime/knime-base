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
 *   Jul 31, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.util;

import java.nio.file.Path;
import java.util.Collection;

import org.knime.filehandling.core.node.table.reader.selector.TransformationModel;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * Represents the raw state of a multi table read i.e. before type mapping, renaming, filtering or reordering.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type representing external types
 */
public interface StagedMultiTableRead<T> {

    /**
     * Creates a {@link MultiTableRead} that uses the default settings i.e. the default type mapping, no filtering, no
     * renaming and no reordering.
     *
     * @return a {@link MultiTableRead} that uses the defaults
     */
    MultiTableRead withoutTransformation();

    /**
     * Creates a {@link MultiTableRead} using the given {@link TransformationModel}.
     *
     * @param selectorModel specifies the type mapping, column renaming, filtering and reordering
     * @return a {@link MultiTableRead} using the provided {@link TransformationModel}
     */
    MultiTableRead withTransformation(final TransformationModel<T> selectorModel);

    /**
     * Returns the raw {@link ReaderTableSpec} consisting of {@link TypedReaderColumnSpec}. Raw means before any type
     * mapping, column filtering or reordering. To be used to make the mentioned operations configurable.
     *
     * @return the raw {@link ReaderTableSpec} i.e. before type mapping, column filtering or reordering
     */
    TypedReaderTableSpec<T> getRawSpec();

    /**
     * Checks if the provided <b>paths</b> match the paths used to instantiate this MultiTableRead.
     *
     * @param paths to read from
     * @return {@code true} if the provided <b>paths</b> are valid
     */
    boolean isValidFor(final Collection<Path> paths);

}
