package com.yarmis.core.logging;

import java.util.Calendar;

/**
 * Created by Maurice on 19-11-2015.
 */
public class SystemLogWriter extends FilteredLogWriter {


    public SystemLogWriter() {
        this.showAbove(null, true);
    }

    public void show(Log.Level level, boolean show) {
        super.filter[level.ordinal()] = show;
    }

    /**
     * Shows or hides all output that is above the given level.
     *
     * @param level THe level that you want to show everything above. This is not inclusive. To indicate that all levels
     *              must be shown, provide {@code null}.
     * @param show  Whether or not to show any level that is above the given level.
     */
    public void showAbove(Log.Level level, boolean show) {
        for (Log.Level value : Log.Level.values())
            if (level == null || value.ordinal() > level.ordinal())
                this.show(value, show);
    }


    private String timeIndent, customIndent;


    private static String makeIndent(String match) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < match.length(); ++i)
            sb.append(' ');
        return sb.toString();
    }

    @Override
    protected synchronized void write(Calendar timestamp, Log.Level level, String label, String message) {

        String[] lines = message.split("\n");
        String time = "[" + this.formatTimestamp(timestamp) + "] ";
        if (this.timeIndent == null || this.timeIndent.length() != time.length())
            this.timeIndent = makeIndent(time).substring(2);

        String custom = "[" + this.ANSIColorForLevel(level) + label + ANSI_RESET + "] | ";

        if (this.customIndent == null || this.customIndent.length() != custom.length())
            this.customIndent = makeIndent(custom);

        System.out.println(time + custom + (level == Log.Level.ERROR ? (ANSI_RED + lines[0] + ANSI_RESET) : lines[0]));

        for (int i = 1; i < lines.length; ++i)
            System.out.println(" ."+timeIndent + customIndent + (level == Log.Level.ERROR ? (ANSI_RED + lines[i] + ANSI_RESET) : lines[i]));
    }

    private String ANSIColorForLevel(Log.Level level) {
        switch (level) {
            case VERBOSE:
                return ANSI_GREEN;
            case DEBUG:
                return ANSI_WHITE;
            case INFO:
                return ANSI_BLUE;
            case WARN:
                return ANSI_YELLOW;
            case ERROR:
                return ANSI_RED;
            default:
                return ANSI_WHITE;
        }
    }

    protected static final String ANSI_RESET = "\u001B[0m";
    protected static final String ANSI_BLACK = "\u001B[30m";
    protected static final String ANSI_RED = "\u001B[31m";
    protected static final String ANSI_GREEN = "\u001B[32m";
    protected static final String ANSI_YELLOW = "\u001B[33m";
    protected static final String ANSI_BLUE = "\u001B[34m";
    protected static final String ANSI_PURPLE = "\u001B[35m";
    protected static final String ANSI_CYAN = "\u001B[36m";
    protected static final String ANSI_WHITE = "\u001B[37m";
}
