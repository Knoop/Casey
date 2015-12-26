package com.yarmis.core;

/**
 * Created by Maurice on 10-11-2015.
 */
public class Manager {

    protected final Yarmis yarmis;
    protected final String TAG;

    protected Manager(Yarmis yarmis) {
        this(yarmis, "");

    }

    protected Manager(Yarmis yarmis, String tag)
    {
        this.yarmis = yarmis;
        this.TAG = tag;
    }


}
