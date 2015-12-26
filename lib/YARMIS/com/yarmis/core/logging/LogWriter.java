package com.yarmis.core.logging;

import java.util.Calendar;

/**
 * A LogWriter is called asynchronously to write a single logging entry.
 */
public abstract class LogWriter {

    protected boolean needToWrite(Log.Level level) {
        return true;
    }

    void writeIfNeeded(Log.LogEntry entry) {
        if (needToWrite(entry.level))
            this.write(entry.timestamp, entry.level, entry.label, entry.message);
    }

    /**
     * Write the given entry. This is only called if the entry was allowed according to the filter
     *
     * @param timestamp The timestamp at which the entry was created.
     * @param level     The debug level of the entry to write.
     * @param label     The label of the entry to write.
     * @param message   The message of the entry to write.
     */
    protected abstract void write(Calendar timestamp, Log.Level level, String label, String message);

    /**
     * Formats the timestamp. This is done by calling {@code formatDate(timestamp)} and {@code formatTime(timestamp)}, and combining the two results.
     *
     * @param timestamp
     * @return
     */
    public String formatTimestamp(Calendar timestamp) {
        StringBuilder sb = new StringBuilder();
        formatDate(sb, timestamp);
        sb.append(' ');
        formatTime(sb, timestamp);
        return sb.toString();
    }

    /**
     * Formats the date. This is done as {@code DD-MM-YYYY}.
     *
     * @param timestamp The timestamp for which to format the date
     * @return The formatted version of the date of the timestamp
     */
    public void formatDate(StringBuilder sb, Calendar timestamp) {
        this.format(sb, Calendar.YEAR, timestamp, 4);
        sb.append('-');
        this.format(sb, Calendar.MONTH, timestamp, 2);
        sb.append('-');
        this.format(sb, Calendar.DAY_OF_MONTH, timestamp, 2);
    }

    /**
     * Formats the time. This is done as {@code HH:MM:SS.mmm}, where the hours are displayed on a range from 0 to 23.
     *
     * @param timestamp The timestamp for which to format the time
     * @return The formatted version of the time of the timestamp
     */
    public void formatTime(StringBuilder sb, Calendar timestamp) {
        this.format(sb, Calendar.HOUR_OF_DAY, timestamp, 2);
        sb.append(':');
        this.format(sb, Calendar.MINUTE, timestamp, 2);
        sb.append(':');
        this.format(sb, Calendar.SECOND, timestamp, 2);
        sb.append('.');
        this.format(sb, Calendar.MILLISECOND, timestamp, 3);

    }

    /**
     * Formats a single property from the calendar to have a mimimum of the given amount of zeroes.
     * For example the integer value "3" would be formatted as "
     *
     * @param builder   The builder to which to append the formatted property
     * @param property  The integer encoding for the property
     * @param timestamp The Calendar from which the property should be retrieved
     * @param zeroes    The number of digits that are required for the value. Any required zeroes are prepended.
     */
    private void format(StringBuilder builder, int property, Calendar timestamp, int zeroes) {
        builder.append(String.format("%0" + zeroes + "d", timestamp.get(property)));
    }

}
