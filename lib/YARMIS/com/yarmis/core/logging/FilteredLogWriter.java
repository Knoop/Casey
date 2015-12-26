package com.yarmis.core.logging;

/**
 * Created by Maurice on 17-11-2015.
 */
public abstract class FilteredLogWriter extends LogWriter {

    protected boolean[] filter = new boolean[Log.Level.values().length];

    @Override
    protected boolean needToWrite(Log.Level level) {
        return level != null && this.filter[level.ordinal()];
    }

}
