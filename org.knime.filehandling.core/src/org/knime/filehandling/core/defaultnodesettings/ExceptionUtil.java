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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.UnresolvedAddressException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

/**
 * FIXME: this code is copied from org.knime.kerberos and should be moved to org.knime.core so that it is usable
 * everywhere.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 */
public class ExceptionUtil {

    private ExceptionUtil() {
        // static utility class
    }

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

    /**
     * Returns an {@link Optional} with either the first {@link Throwable} that is evaluated as {@code true} by the
     * passed {@link Predicate} or empty if none is evaluated as {@code true}.
     *
     * @param t a throwable, possibly with cause chain.
     * @param predicate a predicate, used to test the throwable
     * @return optional with the first throwable accepted by the predicate or empty if none is accepted
     */
    public static Optional<Throwable> getFirstThrowable(final Throwable t, final Predicate<Throwable> predicate) {
        if (predicate.test(t)) {
            return Optional.of(t);
        } else if (t.getCause() == null) {
            return Optional.empty();
        } else {
            return getFirstThrowable(t.getCause(), predicate);
        }
    }

    /**
     * Returns an {@link Optional} with either the last {@link Throwable} (in the cause chain) that is evaluated as
     * {@code true} by the passed {@link Predicate}, or empty if none is evaluated as {@code true}.
     *
     * @param t a throwable, possibly with cause chain.
     * @param predicate a predicate, used to test the throwable
     * @return optional with the first throwable accepted by the predicate or empty if none is accepted
     */
    public static Optional<Throwable> getLastThrowable(final Throwable t, final Predicate<Throwable> predicate) {
        final var causes = new LinkedList<Throwable>();
        causes.add(t);

        Throwable currCause = t.getCause();
        while (currCause != null && !causes.contains(currCause)) {
            causes.add(0, currCause);
            currCause = currCause.getCause();
        }

        return causes.stream()//
            .filter(predicate)//
            .findFirst();
    }


    public static String getDeepestNIOErrorMessage(final Throwable t) {
        String deeperMsg = null;
        if (t.getCause() != null && t.getCause() != t) {
            deeperMsg = getDeepestNIOErrorMessage(t.getCause());
        }

        if (deeperMsg != null && deeperMsg.length() > 0) {
            return deeperMsg;
        } else if (t instanceof FormattedNIOException) {
            return t.getMessage();
        } else if (t instanceof FileSystemException) {
            return String.format("%s (%s)", t.getMessage(), t.getClass().getSimpleName());
        } else {
            return null;
        }
    }

    /**
     * Returns deepest exeption cause from the cause stack o fthe given exception.
     *
     * @param t A throwable, possibly with cause chain.
     * @return deepest The deepest exception cause.
     */
    public static Throwable getDeepestError(final Throwable t) {
        if (t.getCause() != null && t.getCause() != t) {
            return getDeepestError(t.getCause());
        } else {
            return t;
        }
    }

    /**
     * Unpacks the underlying exception from certain "wrapper" Exceptions, such as {@link ExecutionException} or
     * {@link UncheckedIOException}.
     *
     * @param e An exception that (possibly) just wraps another one.
     * @return the wrapped exception, or the original one.
     */
    public static Throwable unpack(final Exception e) {
        if (e instanceof ExecutionException) {
            return e.getCause();
        } else if (e instanceof UncheckedIOException) {
            return e.getCause();
        } else {
            return e;
        }
    }

    /**
     *
     * @param msg The message to limit in length.
     * @param maxLen Maximum requested length of the message.
     * @return the truncated messaged, with "..." as a replacement text if something has been truncated.
     */
    public static String limitMessageLength(final String msg, final int maxLen) {
        if (msg.length() > maxLen) {
            return msg.substring(0, maxLen) + "...";
        } else {
            return msg;
        }
    }

    /**
     * Wraps IO exceptions so that the {@link Exception#getMessage()} is easier to understand.
     *
     * @param e the {@link IOException} to wrap
     * @return the wrapped {@link IOException}
     */
    public static IOException wrapIOException(final IOException e) {
        if (e instanceof AccessDeniedException) {
            return new FormattedAccessDeniedException((AccessDeniedException)e);
        }
        return e;
    }

    /**
     * Looks at the given {@link Throwable} and wraps it in an {@link IOException}, unless it is already an IOException,
     * in which case it will just be returned.
     *
     * @param e the {@link Throwable} to wrap
     * @return a {@link IOException} that wraps (or is) the given {@link Throwable}.
     */
    public static IOException wrapAsIOException(final Throwable e) {
        if (e instanceof IOException ioe) {
            return ioe;
        } else if (e instanceof UnresolvedAddressException) {
            return new IOException("Unable to connect: The host is unknown.", e);
        } else if ((e instanceof TimeoutException) && (e.getMessage() == null)) {
            return new IOException("Unable to connect: Connection timed out.", e);
        } else {
            return new IOException(e.getMessage(), e);
        }
    }

    /**
     * Creates a formatted {@link AccessDeniedException}.
     *
     * @param path the path for which the {@link AccessDeniedException} occurred
     * @return the formatted {@link AccessDeniedException}
     */
    public static AccessDeniedException createAccessDeniedException(final Path path) {
        return new FormattedAccessDeniedException(path);
    }

    /**
     * Creates a formatted {@link NoSuchFileException}.
     *
     * @param e The original exception
     * @param fileType The file type, e.g. "File", or "Workflow". May be null.
     * @return the formatted {@link NoSuchFileException}
     */
    public static NoSuchFileException createFormattedNoSuchFileException(final NoSuchFileException e,
        final String fileType) {
        if (StringUtils.isBlank(fileType)) {
            return new FormattedNoSuchFileException(e, "File/Folder");
        } else {
            var trimmed = fileType.trim();
            return new FormattedNoSuchFileException(e,
                Character.toUpperCase(trimmed.charAt(0)) + fileType.substring(1));
        }
    }

    /**
     * Marker interface for formatted NIO exceptions.
     *
     * @author Bjoern Lohrmann, KNIME GmbH
     */
    private static interface FormattedNIOException {
    }

    /**
     * An {@link AccessDeniedException} with a more user-friendly error message.
     *
     * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
     */
    private static final class FormattedAccessDeniedException extends AccessDeniedException // NOSONAR ignore
        implements FormattedNIOException {

        private static final String MSG_PREFIX = "Unable to access";

        private static final long serialVersionUID = 1L;

        FormattedAccessDeniedException(final AccessDeniedException e) {
            super(e.getFile(), e.getOtherFile(), MSG_PREFIX);
            initCause(e.getCause());
        }

        /**
         * @param path
         */
        public FormattedAccessDeniedException(final Path path) {
            super(path.toString(), null, MSG_PREFIX);
        }

        @Override
        public String getMessage() {
            return getReason() + " " + getFile();
        }
    }

    /**
     * An {@link NoSuchFileException} with a more user-friendly error message.
     *
     * @author Bjoern Lohrmann, KNIME GmbH
     */
    private static final class FormattedNoSuchFileException extends NoSuchFileException // NOSONAR ignore
        implements FormattedNIOException {

        private static final String MSG_FORMAT = "%s does not exist:";

        private static final long serialVersionUID = 1L;

        FormattedNoSuchFileException(final NoSuchFileException e, final String fileType) {
            super(e.getFile(), e.getOtherFile(), String.format(MSG_FORMAT, fileType));
            initCause(e.getCause());
        }

        @Override
        public String getMessage() {
            return getReason() + " " + getFile();
        }
    }
}
