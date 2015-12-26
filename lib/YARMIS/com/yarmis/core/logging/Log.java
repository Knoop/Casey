package com.yarmis.core.logging;

import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Created by Maurice on 17-11-2015. Functionality based on Android's logging.
 */
public class Log {

    /**
     * The queue containing all logging entries that were logged bot not yet written.
     */
    private static final LinkedList<LogEntry> queue = new LinkedList<>();

    /**
     * The Thread that runs the logrunner.
     */
    private static Thread runner;

    /**
     * All LogWriters that are registered to write logging info obtained through this logging.
     */
    private static Set<LogWriter> writers = new HashSet<LogWriter>();

    /**
     * Makes sure that the logrunner is currently running.
     */
    private static void ensureLogging() {
        if (queue.size() > 0 && writers.size() > 0 && runner == null) {
            runner = new Thread(Log.logRunner);
            runner.start();
        }
    }

    /**
     * Register the given writer as a LogWriter for this logging.
     *
     * @param writer The writer to use for logging.
     */
    public static void registerWriter(LogWriter writer) {
        writers.add(writer);
        Log.ensureLogging();

    }

    /**
     * Remove the given writer as a LogWriter for this logging.
     *
     * @param writer The writer to stop using for logging.
     */
    public static void removeWriter(LogWriter writer) {
        writers.remove(writer);
    }

    /**
     * Remove all writers for this logging.
     */
    public static void removeAll() {
        writers.clear();
    }

    /**
     * Adds a logging entry to the queue, containing the given attributes. It ensures that the given entry will be
     * logged.
     *
     * @param label   The label of the entry to logging
     * @param message The message of the entry to logging
     * @param level   The level of the entry to logging
     */
    private static synchronized void log(String label, String message, Level level) {
        Log.queue.addLast(new LogEntry(level, label, message, Calendar.getInstance()));
        Log.ensureLogging();
    }


    public static void d(String label, String message) {
        Log.debug(label, message);
    }

    public static void debug(String label, String message) {
        Log.log(label, message, Level.DEBUG);
    }

    public static void i(String label, String message) {
        Log.info(label, message);
    }

    public static void info(String label, String message) {
        Log.log(label, message, Level.INFO);
    }

    public static void e(String label, String message) {
        Log.error(label, message);
    }

    public static void error(String label, String message) {
        Log.log(label, message, Level.ERROR);
    }

    public static void w(String label, String message) {
        Log.warn(label, message);
    }

    public static void warn(String label, String message) {
        Log.log(label, message, Level.WARN);
    }

    public static void v(String label, String message) {
        Log.verbose(label, message);
    }

    public static void verbose(String label, String message) {
        Log.log(label, message, Level.VERBOSE);
    }

    public static void e(String label, Throwable exception) {
        Log.error(label, exception);
    }

    public static void error(String label, Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString());
        for (StackTraceElement element : throwable.getStackTrace())
            sb.append('\n').append(element.toString());
        Log.error(label, sb.toString());
    }

    /**
     * A single entry to be logged. Contains the content, the label and the importance.
     */
    static class LogEntry {

        public LogEntry(Level level, String label, String message, Calendar timestamp) {
            this.level = level;
            this.label = label;
            this.message = message;
            this.timestamp = timestamp;
        }

        final String label;
        final String message;
        final Level level;
        final Calendar timestamp;

    }

    private static final Runnable logRunner = new Runnable() {
        @Override
        public void run() {
            // While there is something to logging, do it. Otherwise stop.
            LogEntry temp;
            while (Log.queue.size() > 0 && writers.size() > 0) {
                temp = Log.queue.pollFirst();
                for (LogWriter writer : Log.writers)
                    writer.writeIfNeeded(temp);
            }
            Log.runner = null;
        }
    };

    public enum Level {

        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR,

    }

}
