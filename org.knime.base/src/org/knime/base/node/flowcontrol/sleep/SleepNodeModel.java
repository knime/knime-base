/*
 * ------------------------------------------------------------------------
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
 *   Sept 17 2008 (mb): created (from wiswedel's TableToVariableNode)
 */
package org.knime.base.node.flowcontrol.sleep;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.util.FileUtil;
import org.knime.core.util.valueformat.NumberFormatter;
import org.knime.node.parameters.widget.choices.Label;

import com.google.common.base.Objects;

/**
 * A simple node which waits for an amount of time or a (file-based) condition.
 *
 * @author M. Berthold, University of Konstanz
 * @author Leonard Wörteler, KNIME GmbH, Konstanz, Germany
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
public class SleepNodeModel extends NodeModel {

    enum WaitMode {
            @Label(value = "For duration",
                description = "The node waits for the entered duration (hour, minute, second, millisecond).")
            WAIT_FOR_TIME,

            @Label(value = "To time of day",
                description = "The node waits until the entered time of day is reached (hour, minute, second).")
            WAIT_UNTIL_TIME,

            @Label(value = "For file change", description = """
                    The node waits until the selected file (or URL that
                    points to a local file) either gets created, modified or deleted.
                    """)
            WAIT_FILE
    }

    enum FileEvent {
            @Label(value = "Modified", description = "Waits until the specified file is modified.")
            MODIFICATION("Modification"),

            @Label(value = "Created", description = "Waits until the specified file is created.")
            CREATION("Creation"),

            @Label(value = "Deleted", description = "Waits until the specified file is deleted.")
            DELETION("Deletion");

        private String m_description;

        FileEvent(final String description) {
            m_description = description;
        }

        String description() {
            return m_description;
        }
    }

    /** Wait for, wait to or wait for file. */
    public static final String CFGKEY_WAITOPTION = "wait_option";

    /** Hours to wait for. */
    static final String CFGKEY_FORHOURS = "for_hours";

    /** Minutes to wait for. */
    static final String CFGKEY_FORMINUTES = "for_minutes";

    /** Seconds to wait for. */
    static final String CFGKEY_FORSECONDS = "for_seconds";

    /** Milliseconds to wait for. */
    static final String CFGKEY_FORMILLISECONDS = "for_milliseconds";

    /** Hours to wait to. */
    public static final String CFGKEY_TOHOURS = "to_hours";

    /** Minutes to wait to. */
    public static final String CFGKEY_TOMINUTES = "to_min";

    /** Seconds to wait to. */
    public static final String CFGKEY_TOSECONDS = "to_seconds";

    /** Path to file to wait for. */
    public static final String CFGKEY_FILEPATH = "path_to_file";

    /** File event to observe. */
    public static final String CFGKEY_FILESTATUS = "file_status_to_observe";

    /** Number of milliseconds to sleep between progress updates. Trade-off between progress frequency and CPU load. */
    private static final long PROGRESS_SLEEP_MS = 100;

    private WaitMode m_selection = WaitMode.WAIT_FOR_TIME; // initialized with default value

    private FileEvent m_fileEvent;

    private String m_filePath;

    private int m_toHours;

    private int m_toMin;

    private int m_toSec;

    private int m_forHours;

    private int m_forMin;

    private int m_forSec = 1;

    private int m_forMillis;

    /** One input, one output. */
    protected SleepNodeModel() {
        super(new PortType[]{FlowVariablePortObject.TYPE_OPTIONAL}, new PortType[]{FlowVariablePortObject.TYPE});
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return inSpecs;
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
        KNIMEConstants.GLOBAL_THREAD_POOL.runInvisible(() -> {
            switch (m_selection) {
                case WAIT_FOR_TIME -> waitForTime(exec);
                case WAIT_UNTIL_TIME -> waitUntilTime(exec);
                case WAIT_FILE -> waitFile(exec);
            }
            return null;
        });
        return inData[0] != null ? inData : new PortObject[]{FlowVariablePortObject.INSTANCE};
    }

    /**
     * Wait for a specified amount of time.
     */
    private void waitForTime(final ExecutionMonitor exec)
        throws InvalidSettingsException, InterruptedException, CanceledExecutionException {
        // wait for
        final var numFormat = NumberFormatter.builder().setGroupSeparator(",") //
            .setMinimumDecimals(1) //
            .setMaximumDecimals(1) //
            .setAlwaysShowDecimalSeparator(true) //
            .build();

        final var ticker = new AtomicLong();
        final var targetTime = OffsetDateTime.now().plusHours(m_forHours).plusMinutes(m_forMin).plusSeconds(m_forSec)
            .plusNanos(m_forMillis * 1_000_000L);
        final var sleepTime = targetTime.toInstant().toEpochMilli() - System.currentTimeMillis();
        final var padder = paddedSeconds(numFormat, sleepTime / 1000.0);
        final var total = numFormat.format(sleepTime / 1000.0);
        exec.setMessage(() -> padder.apply(ticker.get() / 1000.0, new StringBuilder("Waited ")) //
            .append('/').append(total).append(" seconds").toString());

        waitFor(sleepTime, exec, waited -> {
            ticker.set(waited);
            exec.setProgress(Math.min(1.0 * waited / sleepTime, 1.0));
        });
    }

    /**
     * Wait until a specified point in time is reached.
     */
    private void waitUntilTime(final ExecutionMonitor exec) throws InterruptedException, CanceledExecutionException {
        // wait until time: H + M + S = H' : M' : S'
        final var now = OffsetDateTime.now();
        final var today = OffsetDateTime.of(now.toLocalDate(), LocalTime.MIDNIGHT, now.getOffset());
        var targetTime =
            today.plusHours(m_toHours).plusMinutes(m_toMin).plusSeconds(m_toSec);
        if (targetTime.compareTo(now) < 0) {
            // assume that the next day is meant
            targetTime = targetTime.plusDays(1);
        }

        final var displayTime = targetTime.truncatedTo(ChronoUnit.SECONDS);
        exec.setMessage(() -> "Waiting until " + DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(displayTime));

        final long sleepTime = targetTime.toInstant().toEpochMilli() - System.currentTimeMillis();
        waitFor(sleepTime, exec, waited -> exec.setProgress(Math.min(1.0 * waited / sleepTime, 1.0)));
    }

    private static void waitFor(final long delay, final ExecutionMonitor exec, final LongConsumer waitedCallback)
        throws InterruptedException, CanceledExecutionException {
        final var t0 = System.currentTimeMillis();
        var waited = 0L;
        while (waited < delay) {
            Thread.sleep(Math.min(delay - waited, PROGRESS_SLEEP_MS));
            exec.checkCanceled();
            waited = System.currentTimeMillis() - t0;
            waitedCallback.accept(waited);
        }
    }

    /**
     * Wait until a specific event (created/deleted/modified) is registered for a given file.
     */
    private void waitFile(final ExecutionMonitor exec) // NOSONAR complex, but still OK
        throws IOException, URISyntaxException, InterruptedException, CanceledExecutionException {
        final var path = FileUtil.resolveToPath(FileUtil.toURL(m_filePath));
        if (path == null) {
            throw new IllegalArgumentException("File location '" + m_filePath + "' is not a local file.");
        }

        // if we check for creation and the file already exists, we can return right away, else we need to wait
        if (m_fileEvent == FileEvent.CREATION && Files.exists(path)) {
            return;
        }

        // `FileSystems.getDefault()` throws `UnsupportedOperationException` when closed on Windows!
        try (@SuppressWarnings("resource")
        final var ws = FileSystems.getDefault().newWatchService()) {

            final var elapsed = new AtomicLong();
            final Supplier<String> message = () -> "Waiting for " + m_fileEvent.name().toLowerCase(Locale.US)
                + " of file '" + path + "', for " + DurationFormatUtils.formatDurationHMS(elapsed.get());
            exec.setMessage(message);

            final var eventKind = switch (m_fileEvent) {
                case CREATION -> StandardWatchEventKinds.ENTRY_CREATE;
                case MODIFICATION -> StandardWatchEventKinds.ENTRY_MODIFY;
                case DELETION -> StandardWatchEventKinds.ENTRY_DELETE;
            };
            path.getParent().register(ws, eventKind);

            final var fileName = path.getFileName();
            final var t0 = System.currentTimeMillis();
            while (true) {
                exec.checkCanceled();
                elapsed.set(System.currentTimeMillis() - t0);
                exec.setMessage(message); // notify the progress monitor of changes

                WatchKey key = ws.poll(PROGRESS_SLEEP_MS, TimeUnit.MILLISECONDS);
                if (key != null && (key.pollEvents().stream().anyMatch(e -> Objects.equal(e.context(), fileName))
                    || !key.reset())) {
                    // the even we've been waiting for has fired for the file we were watching
                    break;
                }
            }
        }
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // ignore
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var selection = getWaitMode(settings);

        /*
         * Verify the presence of these settings values. Note: we do not - and never did - require the hours, minutes,
         * and seconds to be in a canonical range, e.g. 0-59 for seconds.
         */
        if (selection == WaitMode.WAIT_FOR_TIME) {
            settings.getInt(CFGKEY_FORHOURS);
            settings.getInt(CFGKEY_FORMINUTES);
            settings.getInt(CFGKEY_FORSECONDS);
            // milliseconds were only introduced with 5.8, so we don't check for CFGKEY_FORMILLISECONDS here
        } else if (selection == WaitMode.WAIT_UNTIL_TIME) {
            settings.getInt(CFGKEY_TOHOURS);
            settings.getInt(CFGKEY_TOMINUTES);
            settings.getInt(CFGKEY_TOSECONDS);
        }

        if (selection == WaitMode.WAIT_FILE) {
            final var sms = new SettingsModelString(CFGKEY_FILESTATUS, null);
            sms.loadSettingsFrom(settings);
        }
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_selection = getWaitMode(settings);

        if (m_selection == WaitMode.WAIT_FOR_TIME) {
            m_forHours = settings.getInt(CFGKEY_FORHOURS);
            m_forMin = settings.getInt(CFGKEY_FORMINUTES);
            m_forSec = settings.getInt(CFGKEY_FORSECONDS);
            m_forMillis = settings.getInt(CFGKEY_FORMILLISECONDS, 0); // introduced with 5.8
        } else if (m_selection == WaitMode.WAIT_UNTIL_TIME) {
            m_toHours = settings.getInt(CFGKEY_TOHOURS);
            m_toMin = settings.getInt(CFGKEY_TOMINUTES);
            m_toSec = settings.getInt(CFGKEY_TOSECONDS);
        } else if (m_selection == WaitMode.WAIT_FILE) {
            m_filePath = settings.getString(CFGKEY_FILEPATH);
            final var sms = new SettingsModelString(CFGKEY_FILESTATUS, null);
            sms.loadSettingsFrom(settings);
            final var fileEventDesc = sms.getStringValue();
            m_fileEvent = Arrays.stream(FileEvent.values()) //
                .filter(evt -> evt.description().equals(fileEventDesc)) //
                .findAny() //
                .orElseThrow(() -> new InvalidSettingsException("Unknown file event: '" + fileEventDesc + "'"));
        }
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // ignore -> no view
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addInt(CFGKEY_WAITOPTION, m_selection.ordinal());

        settings.addInt(CFGKEY_FORHOURS, m_forHours);
        settings.addInt(CFGKEY_FORMINUTES, m_forMin);
        settings.addInt(CFGKEY_FORSECONDS, m_forSec);
        settings.addInt(CFGKEY_FORMILLISECONDS, m_forMillis);

        settings.addInt(CFGKEY_TOHOURS, m_toHours);
        settings.addInt(CFGKEY_TOMINUTES, m_toMin);
        settings.addInt(CFGKEY_TOSECONDS, m_toSec);

        settings.addString(CFGKEY_FILEPATH, m_filePath);
        final var sms = new SettingsModelString(CFGKEY_FILESTATUS, FileEvent.MODIFICATION.description());
        sms.setStringValue(m_fileEvent == null ? null : m_fileEvent.description());
        sms.saveSettingsTo(settings);
    }

    static WaitMode getWaitMode(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var waitOption = settings.getInt(CFGKEY_WAITOPTION);
        final var modes = WaitMode.values();
        if (waitOption < 0 || waitOption >= modes.length) {
            throw new InvalidSettingsException(
                "Invalid wait mode " + waitOption + ", must be between 0 and " + (modes.length - 1));
        }
        return modes[waitOption];
    }

    private static BiFunction<Double, StringBuilder, StringBuilder> paddedSeconds(final NumberFormatter formatter,
        final double total) {
        final var totalStr = formatter.format(total);
        final var paddingStr = totalStr.replaceAll("\\d", "\u2007").replace(',', ' ').replace('.', ' '); // NOSONAR
        return (secs, sb) -> {
            // computed every time a progress message is requested
            final var currentStr = formatter.format(secs);
            final var padding = paddingStr.substring(0, Math.max(totalStr.length() - currentStr.length(), 0));
            return sb.append(padding).append(currentStr);
        };
    }
}
