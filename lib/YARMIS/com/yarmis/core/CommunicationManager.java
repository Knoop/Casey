package com.yarmis.core;

import com.yarmis.core.Communication.CommunicationException;
import com.yarmis.core.exceptions.InvalidRequestException;
import com.yarmis.core.logging.Log;
import com.yarmis.core.messages.*;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommunicationManager extends Manager {


    /**
     * A Manager that keeps track of all results
     */
    private ResultHandler results = new ResultHandler();


    final Communication communication;


    CommunicationManager(Yarmis yarmis) {
        super(yarmis, "CommunicationManager");
        communication = new Communication(yarmis);
    }


    /**
     * Requests for the given method to be executed by the Host.
     *
     * @param m    The Method to execute on the Host.
     * @param args The arguments to provide to the Host
     * @return A Result object that can be used to retrieve the return value.
     */
    public Result request(String recipient, Method m, Object... args) {

        String identifier = communication.getIdentifier();
        Result result = results.create(identifier);
        this.sendMessage(communication.makeRequest(identifier, recipient, m, args), yarmis.connection.getHost());
        return result;

    }

    /**
     * Send the given message to all given devices
     *
     * @param message The message to send
     * @param devices The devices to send the message to.
     */
    private void sendMessage(Message message, Device... devices) {
        this.sendMessage(message, this.yarmis.connection.getAll(devices));
    }

    /**
     * The Message to send over all given connections
     *
     * @param message     The message to send
     * @param connections The connections to send the message over.
     */
    private void sendMessage(Message message, Connection... connections) {

        // Try to send it to every device
        for (Connection connection : connections) {
            try {
                connection.send(message);
            } catch (IOException e) {
                throw new CommunicationException(e);
            }
        }

    }

    /**
     * Notify all given devices that the given listener method was called.
     *
     * @param devices        All Devices that must be notified
     * @param listenerMethod The method that is part of a listener that was called.
     */
    public void notify(Module module, Method listenerMethod, Object[] args, Device... devices) {

        this.sendMessage(communication.makeNotification(module, listenerMethod, args), devices);
    }

    /**
     * Notify all given devices that this device wants to listen, or wants to stop listening, to events for the given
     * listener class on the given module.
     *
     * @param module        The module for which to register the listener
     * @param listenerClass The class which events this device is interested in
     * @param register      Whether the class wants to register to listen ({@code true}) or to stop listening ({@code
     *                      false}).
     * @param devices       All devices for which we want to listen to events on the given class
     */
    public Result hook(Module module, Class<?> listenerClass, boolean register, Device... devices) {

        String identifier = communication.getIdentifier();
        Result result = results.create(identifier);
        this.sendMessage(communication.makeHook(identifier, module, listenerClass, register), devices);
        return result;

    }


    /**
     * Sends a response to the given request, based on whether the request was executed successfully.
     *
     * @param message    The message for which the response must be send.
     * @param connection The connection to which the response needs to be send
     * @param value      The value that came from the execution of the given request
     * @param success    Indicates whether the execution of the given request was successful.
     */
    private void respond(IdentifyableMessage message,
                         Connection connection, Object value, boolean success) {
        this.sendMessage(communication.makeResponse(
                message.getIdentifier(), value, success), connection);
    }

    /**
     * Entry point for incoming messages. This needs to be called by a Connection to indicate that it received a
     * message. This checks the message, lets it be handled by the right instance, and then responds to the message if
     * necessary.
     *
     * @param message    The message that needs to be handled
     * @param connection The {@code Connection} that received the given message.
     */
    void handleMessage(JSONObject message, Connection connection) {
        // Non existing messages are of no use
        if (message != null)
            MessageHandler.handle(this, message, connection);
    }


    public Communication communication() {
        return communication;
    }


    /**
     * Indicates whether requests that are received are allowed to be executed on this device.
     *
     * @return true if Request can be executed on this device, false otherwise.
     */
    private boolean allowRequests() {
        return yarmis.connection.isHosting();
    }

    /**
     * Indicates whether hooks that are received are allowed to be added on this device. If a hook is added, the hooked
     * device receives notifications if the listener it is hooked for got called.
     *
     * @return true if Hooks can be added on this device, false otherwise.
     */
    private boolean allowHooks() {
        return true;
    }

    private static class MessageHandler implements Runnable {
        /**
         * The {@code CommunicationManager} that has created this handler.
         */
        private final CommunicationManager communicationManager;

        /**
         * The {@code JSONObject} version of the message that must be handled by this {@code MessageHandler}. The {@code
         * MessageHandler} should later convert this into an actual {@code Message}.
         */
        private final JSONObject message;

        /**
         * The {@code Connection} over which the {@code message} was received.
         */
        private final Connection receiver;

        private MessageHandler(CommunicationManager communicationManager, JSONObject message, Connection receiver) {
            this.message = message;
            this.receiver = receiver;
            this.communicationManager = communicationManager;
        }

        /**
         * Asynchronous part of handling messages. This first translates the received {@code JSONObject} to an instance
         * of {@code Message}. Then the translated {@code Message} is passed to one of three methods, one for each type
         * of {@code Message}.
         */
        public void run() {
            Message message;
            // Try to translate
            try {
                message = Message.from(this.message, communicationManager.communication);

                // Notifications
                if (message instanceof Notification) {
                    Log.v(communicationManager.TAG, "Received notification: " + message);
                    this.handleNotification((Notification) message);
                }
                // Response
                else if (message instanceof Response) {
                    Log.v(communicationManager.TAG, "Received response: " + message);
                    this.handleResponse((Response) message);
                }
                // Hook - May throw an Exception
                else if (message instanceof Hook) {
                    Log.v(communicationManager.TAG, "Received hook: " + message);
                    this.handleHook((Hook) message, this.receiver);
                }
                // Request - May throw an Exception
                else if (message instanceof Request) {
                    Log.v(communicationManager.TAG, "Received request: " + message);
                    this.handleRequest((Request) message, this.receiver);
                }
            } catch (Exception e) {
                Log.e(communicationManager.TAG, e);
            }
        }

        /**
         * Handle for dealing with notifications. This is currently only a placeholder.
         *
         * @param notification The notification that was received.
         */
        private void handleNotification(Notification notification) {

            this.communicationManager.yarmis.modules.getModule(notification.getModuleIdentifier()).notify(notification);
        }


        /**
         * Handle for dealing with responses. This will release the {@code Result} waiting for this response.
         *
         * @param response The response that was received.
         * @throws InvalidRequestException If the response is a response to a not existing request.
         */
        private void handleResponse(Response response) {
            this.communicationManager.results.release(response);
        }


        /**
         * Called when a Hook was received. This means that some device wants to listen to This registers or unregisters
         * the listener that is described in the hook.
         *
         * @param hook The Hook that needs to be handled.
         */
        private void handleHook(Hook hook, Connection connection) {
            Log.i("CommunicationManager", "Received hook " + hook);

            if (this.communicationManager.allowHooks()) {
                boolean success;
                Object outcome;
                try {
                    if (hook.adding)
                        outcome = this.communicationManager.yarmis.notifications.register(hook.listenerClass, this.receiver.getDevice());
                    else
                        outcome = this.communicationManager.yarmis.notifications.unregister(hook.listenerClass, this.receiver.getDevice());
                    success = true;
                } catch (Throwable throwable) {
                    outcome = throwable;
                    success = false;
                }
                this.communicationManager.respond(hook, connection, outcome, success);
            } else {
                // We didn't allow hooks, so we can't handle a hook
                throw new IllegalStateException("Hooks are not allowed");
            }

        }


        /**
         * Handle for dealing with requests. If this system isn't currently hosting an exception is thrown. Otherwise
         * the request is inspected and executed. A response to the requester is send both if the execution succeeded
         * and if it failed. However, if it wasn't hosting, then the connection isn't responded to.
         *
         * @param request    The Request to handle
         * @param connection The Connection from which the Request originated
         */
        private void handleRequest(Request request, Connection connection) {
            if (this.communicationManager.allowRequests()) {
                boolean success;
                Object outcome;
                try {
                    outcome = this.communicationManager.yarmis.modules.handleRequest(request, connection);
                    success = true;
                } catch (Throwable throwable) {
                    outcome = throwable;
                    success = false;
                }
                this.communicationManager.respond(request, connection, outcome, success);
            } else {
                // This could occur if the host goes rogue and sends requests to us.
                throw new IllegalStateException("Can't handle a request when not hosting");
            }
        }

        /**
         * An ExecutorService that will run all MessageHandlers.
         */
        private static ExecutorService messageExecutor = Executors.newCachedThreadPool();

        /**
         * Let the given message coming from the given connection be handled asynchronously.
         *
         * @param message    The message that was received, before it is converted to an instance of Message.
         * @param connection The connection that received the given message.
         */
        private static void handle(CommunicationManager communicationManager, JSONObject message, Connection connection) {
            MessageHandler.messageExecutor.execute(new MessageHandler(communicationManager, message, connection));
        }
    }


}