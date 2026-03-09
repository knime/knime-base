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
 *   Mar 6, 2026 (janniksemperowitsch): created
 */
package org.knime.base.node.io.commandexecutor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.knime.core.node.NodeLogger;

/**
*
* @author janniksemperowitsch
*/
final class CommandExecutorBashHandler {

    private CommandExecutorBashHandler() {}

    // 5MB limit in characters (5 * 1024 * 1024 / 2)
    private static final int MAX_CHARS = 2621440;

    /**
     * HAndles the command execution
     * @param command
     * @param merge
     * @param cut
     * @param outputPointerStdout
     * @param outputPointerStderr
     * @param LOGGER
     */
    public static void commandHandler(
        final String[] command, final boolean merge, final boolean cut,
        final StringBuilder outputPointerStdout, final StringBuilder outputPointerStderr, final NodeLogger LOGGER) {
        LOGGER.info(Arrays.toString(command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(merge);
            Process process = pb.start();

            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                reader.lines().forEach(line -> {
                    synchronized (outputPointerStdout) {
                        outputPointerStdout.append(line).append(System.lineSeparator());
                    }
                });
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }

            if (!merge) {
                try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    reader.lines().forEach(line -> {
                        synchronized (outputPointerStderr) {
                            outputPointerStderr.append(line).append(System.lineSeparator());

                        }
                    });
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }

            if (cut) {
                truncate(outputPointerStdout, LOGGER);
                truncate(outputPointerStderr, LOGGER);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                LOGGER.info(exitCode);
            }

        } catch (Exception e) {
            synchronized (outputPointerStderr) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    private static void truncate(final StringBuilder sb, final NodeLogger LOGGER) {
        synchronized (sb) {
            if (sb.length() > MAX_CHARS) {
                // Ensure we don't split a Unicode surrogate pair - Gemini
                int end = Character.isHighSurrogate(sb.charAt(MAX_CHARS - 1))
                          ? MAX_CHARS - 1
                          : MAX_CHARS;
                sb.setLength(end);
                LOGGER.warn("Truncated");
            }
        }
    }
}