package com.yarmis.core;

import com.yarmis.core.exceptions.InvalidRequestException;
import com.yarmis.core.logging.Log;
import com.yarmis.core.messages.Response;

import java.util.*;

/**
 * Created by Maurice on 7-11-2015.
 */
public class ResultHandler {

    /**
     * The Timeout after which results are simply
     */
    public static final long TIMEOUT = 10000;

    /**
     * The amount of milliseconds that a Timeout may occur earlier then intended. If the next timeout should occur
     * within this amount of time, then the next timeout is performed, even if its time hasn't completely run out.
     */
    private static final long TIME_OUT_OFFSET = 10;

    /**
     * The scheduler that will timeout results when they are ought to be timed out, or when they are within the by
     * {@code TIME_OUT_OFFSET} defined amount of milliseconds from timing out.
     */
    private Scheduler scheduler = new Scheduler();

    /**
     * A Mapping of identifiers to the accompanying result objects.
     */
    private Map<String, Result> results = new HashMap<String, Result>();

    ResultHandler() {

    }


    /**
     * Create a new result for the given identifier, with the default timeout value.
     *
     * @param identifier The identifier for which to create a Result
     * @return The created {@code Result}.
     */
    public Result create(String identifier) {
        return this.create(identifier, TIMEOUT);
    }

    /**
     * Create a new result for the given identifier, with the default timeout value.
     *
     * @param identifier The identifier for which to create a Result
     * @return The created {@code Result}.
     */
    public Result create(String identifier, long timeout) {

        Result result = new Result();
        this.results.put(identifier, result);
        this.scheduler.schedule(timeout, identifier);
        return result;
    }

    /**
     * Applies the given {@code Response} to the relevant {@code Result}. This will release the relevant {@code
     * Result}.
     *
     * @param response A {@code Response} containing information with which a {@code Result} can be released.
     * @throws InvalidRequestException If the response was for a request that was not expected.
     */
    public void release(Response response) throws InvalidRequestException {

        if (this.results.containsKey(response.getIdentifier())) {
            this.scheduler.unschedule(response.getIdentifier());
            this.results.get(response.getIdentifier()).set(response);
        } else
            throw new InvalidRequestException("Request " + response.getIdentifier()
                    + " is not known as an outstanding request. This can be caused by a timeout.");
    }

    private class Scheduler {
        /**
         * A {@code TreeMap} containing for every moment in time when a timeout is to occur the list of identifiers of
         * {@code Results} that must then be timed out.
         */
        private TreeMap<Long, LinkedList<String>> ordered_timeouts;

        /**
         * A mapping of identifiers of {@code Result}s to the moment that it is to timeout.
         */
        private Map<String, Long> timeouts;

        /**
         * The {@code Timer} that will perform the timed task of timing out all required {@code Result}s.
         */
        private Timer timer;

        /**
         * The time when the next timeout will occur, or null if there is no timeout scheduled.
         */
        private Long next;


        private Scheduler() {
            this.ordered_timeouts = new TreeMap<Long, LinkedList<String>>();
            this.timeouts = new HashMap<String, Long>();
            this.timer = new Timer();
            this.next = null;

        }

        /**
         * Schedule a timeout for the given identifier. Scheduling a timeout is synchronized with {@code unschedule} and
         * the actual timing out of identifiers.
         *
         * @param delay      The timeout of time until the timeout should occur. This is expressed in milliseconds.
         * @param identifier The identifier for which to timeout after the given amount of milliseconds,
         */
        private void schedule(long delay, String identifier) {
            synchronized (this) {
                if (delay <= 0)
                    throw new IllegalArgumentException("Delay must be an amount of milliseconds greater than 0");

                if (this.timeouts.containsKey(identifier))
                    return;

                long time = System.currentTimeMillis() + delay;

                // indicate that the identifier will be done at given time
                this.timeouts.put(identifier, time);

                // add the given identifier to the list of identifiers waiting for that moment.
                LinkedList<String> identifiers = this.ordered_timeouts.get(time);

                // If nothing was waiting yet, create a new list
                if (identifiers == null) {
                    identifiers = new LinkedList<String>();
                    this.ordered_timeouts.put(time, identifiers);
                }

                // If the identifier was not yet contained, add it
                if (!identifiers.contains(identifier))
                    identifiers.add(identifier);

                // update the timer to make sure that the timer will fire for the earliest timeout
                updateTimer();
            }
        }

        /**
         * Unschedule the given identifier. The timeout will then no longer occur for the identifier at the time that
         * was set for that identifier. Unscheduling a timeout is synchronized with {@code schedule} and the actual
         * timing out of identifiers.
         *
         * @param identifier The identifier for which to unschedule the timeout.
         */
        private void unschedule(String identifier) {
            synchronized (this) {
                // Get the time when the identifier would fire
                Long time = timeouts.remove(identifier);

                // Not registered
                if (time == null)
                    return;

                // Get all identifiers firing at that time.
                LinkedList<String> identifiers = this.ordered_timeouts.get(time);

                // Remove it, this should always be the case.
                if (identifiers != null)
                    identifiers.remove(identifier);

                // If no identifiers are left waiting for that time, remove it
                if (identifiers.size() == 0)
                    this.ordered_timeouts.remove(time);

                // Make sure that the timer won't fire for a non-existing identifier
                updateTimer();
            }
        }

        /**
         * Updates the timer to run {@code timeoutTask} for the first upcoming timeout. This handles times that are in
         * the past, as to make sure that all timeouts will be send out eventually.
         */
        private synchronized void updateTimer() {
            // Get the first timeout or null if there is none
            Long first = ordered_timeouts.size() > 0 ? ordered_timeouts.firstKey() : null;

            // If there is a timeout scheduled, but it is known as the next, nothing needs to be done
            if (first != null && first.equals(next))
                return;

            // Next is no longer the actual next timer. Need to change timer, but only if it was set
            if (this.next != null) {
                timer.cancel();
                timer = null;
            }

            // First contains the next timeout
            this.next = first;

            // If there are timeouts to happen, schedule (if required, schedule it as now).
            if (next != null) {
                timer = new Timer();
                timer.schedule(new TimeoutTask(), new Date(first < System.currentTimeMillis() ? System.currentTimeMillis() + TIME_OUT_OFFSET : first));
            }
        }

        /**
         * The TimerTask that is executed to cause timeouts for all identifiers that were scheduled to timeout at that
         * point. This removes the identifiers from the ordering of timeouts. All timeouts that will occur within {@code
         * TIME_OUT_OFFSET} milliseconds are also timed out. After all required identifiers are timed out, the timer
         * will be updated to be able to handle the next entry.
         * <p/>
         * Obtaining the next list of identifiers to fire is synchronized with {@code schedule} and {@code unschedule}.
         */
        private class TimeoutTask extends TimerTask {

            @Override
            public void run() {

                Map.Entry<Long, LinkedList<String>> entry;
                do {
                    // obtain everything to timeout
                    synchronized (Scheduler.this) {
                        entry = Scheduler.this.ordered_timeouts.pollFirstEntry();
                    }

                    // timeout
                    for (String identifier : entry.getValue()) {
                        Log.e("ResultHandler", "Timeout " + identifier + " @ " + System.currentTimeMillis());
                        ResultHandler.this.results.get(identifier).timeout();
                    }
                }
                while (Scheduler.this.ordered_timeouts.size() > 0 && entry.getKey() + TIME_OUT_OFFSET < System.currentTimeMillis());
                Scheduler.this.updateTimer();
            }
        }
    }
}
