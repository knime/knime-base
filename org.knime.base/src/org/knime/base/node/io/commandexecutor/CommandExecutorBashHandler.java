package org.knime.base.node.io.commandexecutor;

import java.io.BufferedReader;
import java.io.InputStreamReader;

final class CommandExecutorBashHandler {

    public static void commandHandler(
        final String[] command, final boolean merge,
        final StringBuilder outputPointerStdout, final StringBuilder outputPointerStderr) {

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
                synchronized (outputPointerStderr) { outputPointerStderr.append("Stderr Read Error: ").append(e.getMessage()); }
            }
            if (!merge) {
                try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    reader.lines().forEach(line -> {
                        synchronized (outputPointerStderr) {
                            outputPointerStderr.append(line).append(System.lineSeparator());
                        }
                    });
                } catch (Exception e) {
                    synchronized (outputPointerStderr) { outputPointerStderr.append("Stderr Read Error: ").append(e.getMessage()); }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                synchronized (outputPointerStderr) {
                    outputPointerStderr.append("Process exited with code: ").append(exitCode);
                }
            }

        } catch (Exception e) {
            synchronized (outputPointerStderr) {
                outputPointerStderr.append("Execution Error: ").append(e.getMessage());
            }
        }
    }
}