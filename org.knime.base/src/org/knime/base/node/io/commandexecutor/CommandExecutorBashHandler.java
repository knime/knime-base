package org.knime.base.node.io.commandexecutor;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.knime.core.node.NodeLogger;

final class CommandExecutorBashHandler {

    // 5MB limit in characters (5 * 1024 * 1024 / 2)
    private static final int MAX_CHARS = 2621440;

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