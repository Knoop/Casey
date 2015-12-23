package com.knoop.casa;

import com.yarmis.core.Yarmis;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Maurice on 22-12-2015.
 */
public class Casey {

    private Yarmis yarmis;

    private Set<OnStateChangeListener> listeners = new HashSet<>();


    /**
     * Start a new Casey instance with the given parameters.
     *
     * @param args The parameters to start the instance with.
     */
    private Casey(String[] args) {

        Exception failure = null;

        try {
            this.start();
        } catch (Exception e) {
            failure = e;
        }

        if (failure == null)
            this.notifyOnStart();
        else
            this.notifyOnFailedStart(failure);

    }


    private void start() throws Exception {
        this.yarmis = Yarmis.initialize().build();
        this.yarmis.connection().startHosting();

    }

    /**
     * Notifies all listeners that the start was successful
     */
    private void notifyOnStart() {
        for (OnStateChangeListener listener : this.listeners)
            listener.onStart();
    }

    /**
     * Notifies all listeners that the start failed due to the given exception.
     *
     * @param e The exception that occured during startup.
     */
    private void notifyOnFailedStart(Exception e) {
        for (OnStateChangeListener listener : this.listeners)
            listener.onFailedStart(e);
    }


    /**
     * Stops the applications. Fully exits it after having stopped yarmis. Will notify all listeners that it is stopping
     * before actually closing anything down.
     */
    protected void stop() {

        this.notifyOnStop();
        try {
            this.yarmis.connection().stopHosting();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    /**
     * Notifies all listeners that the application is going to stop.
     */
    private void notifyOnStop() {
        for (OnStateChangeListener listener : this.listeners)
            listener.onStop();
    }


    public static void main(String[] args) {
        if (instance == null)
            Casey.instance = new Casey(args);
        else
            instance.command(args);
    }

    /**
     * Perform the command that is indicated in the given parameters.
     *
     * @param args The command to execute.
     */
    private void command(String[] args) {


    }

    public static class OnStateChangeListener {

        /**
         * Called when the application is successfully started.
         */
        protected void onStart() {

        }

        /**
         * Called when the application couldn't be started due to an exception that occured during the startup. The
         * exception that occured is provided.
         *
         * @param e The Exception that caused the startup to fail.
         */
        private void onFailedStart(Exception e) {

        }

        /**
         * Called when the application is stopped.
         */
        protected void onStop() {

        }

    }

    static Casey instance;


}
