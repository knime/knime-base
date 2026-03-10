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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;

/**
*
* @author janniksemperowitsch
*/
final class CommandExecutorProcessHandler {

    private CommandExecutorProcessHandler() {}

    // claculation for 5MB in Chars = 5 * 1024 * 1024 / 2
    private static final int MAX_CHARS = 2621440;
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(CommandExecutorProcessHandler.class);

    /**
     * @param command
     * @param merge
     * @param cut
     * @param outContainer
     * @param errContainer
     * @throws Exception
     */
    static void commandHandler(final String[] command, final boolean merge, final boolean cut, final BufferedDataContainer outContainer,
        final BufferedDataContainer errContainer) throws Exception {
        LOGGER.infoWithFormat("%s was executed with %d argument%s", command[0], command.length-1, (command.length-1==1)? "":"s");

        //TODO Add Sandboxing to not allow any command
        //TODO Add Pipe support
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(merge);
            Process process = pb.start();

            var futOut = KNIMEConstants.GLOBAL_THREAD_POOL.submit(() -> readOut(process, cut, outContainer));
            if (!merge) {
                var futErr = KNIMEConstants.GLOBAL_THREAD_POOL.submit(() -> readErr(process, cut, errContainer));
                futErr.get();
            }
            futOut.get();

            process.waitFor();

        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            throw new Exception("Failed to execute command: " + e.getMessage(), e);
        }
    }
    /**
     * @param process
     * @param cut
     * @param outContainer
     */
    private static void readOut(final Process process, final boolean cut, final BufferedDataContainer outContainer) {
        AtomicInteger i = new AtomicInteger(0);
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            if (!cut) {
                reader.lines().forEach(line -> {
                    outContainer.addRowToTable(new DefaultRow("Row_" + i.getAndIncrement(), new StringCell(truncate(line))));
                });
                return;
            }
            final StringBuilder sb = new StringBuilder();
            reader.lines()
            .takeWhile(line -> sb.length() < MAX_CHARS-2)
            .forEach(line -> {
                truncate(sb.append(line).append(System.lineSeparator()));
            });
            outContainer.addRowToTable(new DefaultRow("Row_0", new StringCell(sb.toString())));
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * @param process
     * @param cut
     * @param errContainer
     */
    private static void readErr(final Process process, final boolean cut, final BufferedDataContainer errContainer) {
        AtomicInteger j = new AtomicInteger(0);
        try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            if (!cut) {
                reader.lines().forEach(line -> {
                    errContainer.addRowToTable(new DefaultRow("Row_" + j.getAndIncrement(), new StringCell(truncate(line))));
                });
                return;
            }
            final StringBuilder sb = new StringBuilder();
            reader.lines()
                .takeWhile(line -> sb.length() < MAX_CHARS-2)
                .forEach(line -> {
                    truncate(sb.append(line).append(System.lineSeparator()));
                });
            errContainer.addRowToTable(new DefaultRow("Row_0", new StringCell(sb.toString())));
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        }
    }

    private static StringBuilder truncate(final StringBuilder sb) { //TODO Possibly migrate to counting bytes instead of chars with break for calling method
        if (sb.length() > MAX_CHARS) {
            int end = Character.isHighSurrogate(sb.charAt(MAX_CHARS - 1))
                      ? MAX_CHARS - 1
                      : MAX_CHARS;
            sb.setLength(end);
            LOGGER.warn("Truncated");
        }
        return sb;
    }
    private static String truncate(final String line) {
        if (line == null || line.length() <= MAX_CHARS) {
            return line;
        }
        int end = Character.isHighSurrogate(line.charAt(MAX_CHARS - 1))
                  ? MAX_CHARS - 1
                  : MAX_CHARS;

        LOGGER.warn("Truncated line from {" + line.length() + "} to {" + end + "} characters");
        return line.substring(0, end);
    }
}