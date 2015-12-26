package com.yarmis.core;

import com.yarmis.core.logging.Log;
import com.yarmis.core.logging.LogWriter;
import com.yarmis.core.logging.SystemLogWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class Yarmis {


    /**
     * The ModuleManager the keeps track of all modules. This is available to all managers and modules. This will be set
     * after {@code Settings}. No further guarantees on ordering are provided.
     */
    final ModuleManager modules;

    /**
     * The CommunicationManager that keeps track of all communication for YARMIS. This is available to all managers and
     * modules This will be set after {@code Settings}. No further guarantees on ordering are provided.
     */
    final ConnectionManager connection;

    /**
     * The CommunicationManager that keeps track of all communication for YARMIS. This is available to all managers and
     * modules This will be set after {@code Settings}. No further guarantees on ordering are provided.
     */
    final CommunicationManager communication;

    /**
     * The DeviceManager that keeps track of all known devices. This is available to all managers and modules This will
     * be set after {@code Settings}. No further guarantees on ordering are provided.
     */
    final DeviceManager devices;

    /**
     * The SecurityManager that keeps track of rights. This will be set after {@code Settings}. No further guarantees on
     * ordering are provided.
     */
    final SecurityManager security;


    /**
     * The NotificationManager that keeps track of notifications and the listeners that are listening to those
     * notifications.
     */
    final NotificationManager notifications;
    /**
     * The settings that are available to all managers and modules. This is the first property of YARMIS to be set, so
     * it can be used by other managers and modules.
     */
    final Settings settings;


    /**
     * Creates a new YARMIS instance
     */
    private Yarmis(Settings settings, List<LogWriter> writers) throws IOException {
        // First do the items for which ordering matters
        this.settings = settings;

        // Let all logwriters logging
        for (LogWriter writer : writers)
            Log.registerWriter(writer);

        // Then stop guaranteeing the order.
        // The following managers should be interchangeable in terms of ordering.
        this.modules = new ModuleManager(this);
        this.devices = new DeviceManager(this);
        this.connection = new ConnectionManager(this);
        this.communication = new CommunicationManager(this);

        this.notifications = new NotificationManager(this);
        this.security = new SecurityManager(this, new PermissionRepository(new File(settings.PERMISSION_CONFIG_FILE)));


    }


    public static <Functionality> void setAsState(Functionality functionality, State state) {

        switch (state) {
            case LOCAL:
                instance.modules.makeAccessible((Module) functionality);
                ((Module) functionality).setLocal();
                break;
            case REMOTE:
                instance.modules.makeInaccessible((Module) functionality);
                ((Module) functionality).setRemote();
                break;
        }
    }

    public void connectTo(String address) throws IOException {
        this.connectTo(DeviceManager.createDevice("test", address));
    }

    public void connectTo(Device device) throws IOException {
        this.connection.connect(device);
    }

    public void dropConnection() throws IOException {
        this.connection.disconnectHost();
    }

    public ModuleManager modules() {
        return this.modules;
    }

    public CommunicationManager communication() {
        return this.communication;
    }

    public ConnectionManager connection() {
        return this.connection;
    }

    public DeviceManager devices() {
        return this.devices;
    }

    public PermissionRepository permissions() {
        return this.security.getPermissions();
    }

    /**
     * The instance of YARMIS, this is made available for managers.
     */
    private static Yarmis instance;

    private static boolean builderCreated = false;

    public static Builder initialize() {
        if (Yarmis.instance != null)
            throw new IllegalStateException("A Yarmis instance already exists");
        if (builderCreated)
            throw new IllegalStateException("A builder for a new Yarmis instance already exists");

        return new Builder();

    }

    public static final class Builder {

        private boolean hasBuild = false;
        private List<LogWriter> writers = new LinkedList<LogWriter>();


        private Builder() {

        }

        public Builder addLogWriter(LogWriter logWriter) {
            this.writers.add(logWriter);
            return this;
        }

        /**
         * Create the yarmis instance
         *
         * @return The created instance of YARMIS
         */
        public Yarmis build() throws FileNotFoundException, IOException {

            if (hasBuild)
                throw new IllegalStateException("Can't build instance of YARMIS. An instance was already build.");

            // Make sure there is a LogWriter
            if (writers.size() == 0)
                writers.add(new SystemLogWriter());

            Yarmis.instance = new Yarmis(new Settings(), writers);
            this.hasBuild = true;
            return Yarmis.instance;
        }

    }

    public enum State {LOCAL, REMOTE}

}