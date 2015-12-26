package com.yarmis.core;

import com.yarmis.core.exceptions.NoHostException;
import com.yarmis.core.exceptions.NoSuchConnectionException;
import com.yarmis.core.logging.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Maurice on 7-11-2015.
 */
public class ConnectionManager extends Manager {

    /**
     * The {@code ConnectivityPlugin} providing network connection capabilities.
     */
    private final ConnectivityPlugin connectivity = new ConnectivityPlugin();

    /**
     * All connections that are currently active.
     */
    private final Map<InetAddress, Connection> connections = new HashMap<>();
    ;

    /**
     * The hosting device. Its InetAddress should be contained by {@code connections}
     */
    private Device host;

    ConnectionManager(Yarmis yarmis) {
        super(yarmis);
    }

    /**
     * Register the given connection as an active connection.
     *
     * @param connection The connection to register as being active.
     */
    public void register(Connection connection) {

        if (this.isConnectedBy(connection))
            throw new IllegalArgumentException(
                    "A Connection is already registered for device: "
                            + connection.getDevice());
        Log.i("CommunicationManager", "Storing device: " + connection.getDevice());
        this.connections.put(connection.getDevice().address, connection);
    }


    /**
     * Register the given {@code Connection} as the connection to the host. The given connection must already be used.
     * If it is not in use, perhaps due to being closed, then the {@code Connection} won't be used.
     *
     * @param connection The {@code Connection} to use as the connection to the host.
     * @return {@code true} if the connection was set as the connection to the host, {@code false} if that failed.
     */
    public boolean useAsHost(Connection connection) {
        if (!connection.isClosed() && this.isConnectedBy(connection)) {
            this.host = connection.getDevice();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the {@code Connection} that was registered for the given {@code Device}
     *
     * @param device The {@code Device} for which to obtain the {@code Connection}.
     * @return The {@code Connection} that was stored, or null if there was no such {@code Connection}.
     */
    public Connection get(Device device) {
        return this.connections.get(device.address);
    }

    /**
     * Get the {@code Connection} that was registered for the host {@code Device}. Do note that the connection to the
     * Host may be replaced by another connection.
     *
     * @return The {@code Connection} that was stored, or null if there was no such {@code Connection}.
     * @throws NoHostException If there is no host set.
     */
    public Connection getForHost() throws NoHostException {

        return this.get(this.getHost());
    }

    /**
     * Get the {@code Device} that was registered as the host.
     *
     * @return The {@code Device} that was stored as the host.
     * @throws NoHostException If there is no host set.
     */
    public Device getHost() throws NoHostException {
        if (!this.hasHost())
            throw new NoHostException();

        return this.host;
    }

    /**
     * Get all connections for the given devices.
     *
     * @param devices All devices for which to get the connection
     * @return An array of all Connections that connect to the devices. The ordering is preserved. If any of the devices
     * is not currently connected to, the accompanying value in the connection array will be null.
     */
    public Connection[] getAll(Device[] devices) {
        Connection[] connections = new Connection[devices.length];

        for (int i = 0; i < devices.length; ++i)
            connections[i] = this.get(devices[i]);
        return connections;
    }

    /**
     * Connects to the given {@code Device}. This requires that the CommunicationManager is <b>not</b> hosting.
     *
     * @param device The {@code Device} to connect to
     * @throws IOException
     */
    public void connect(Device device) throws IOException {

        if (this.connectivity.isHosting()) {
            throw new IllegalStateException(
                    "Can't connect to a device when hosting.");
        } else {

            Connection connection = connectivity.connectTo(device);
            this.useAsHost(connection);
            // if the connection failed, an exception is thrown before reaching
            // this point.
            // The connection is already registered
            this.host = device;

        }

    }


    /**
     * Unregister the given {@code Connection}. This will check whether the given Connection was in use. If so, it is
     * unregistered and closed.
     *
     * @param connection The {@code Connection} to unregister
     * @return Whether the connection was in use and has been succesfully closed.
     */
    public boolean disconnect(Connection connection) {
        if (!this.isConnectedTo(connection.getDevice()) && this.get(connection.getDevice()).equals(connection))
            throw new NoSuchConnectionException(connection.getDevice());

        return this.disconnect(connection.getDevice());

    }

    /**
     * Disconnect the given {@code Device}. This will check whether the given {@code Device} was connected to. If so, it
     * is unregistered and closed.
     *
     * @param device The {@code Device} to disconnect
     * @return Whether the connection was in use and has been successfully closed.
     */
    public boolean disconnect(Device device) {

        if (!this.isConnectedTo(device))
            return false;

        Log.i("CommunicationManager", "dropping device: " + device);

        // Remove as host
        if (this.isHost(device))
            this.host = null;

        // Remove the connection
        try {
            this.removeConnection(device).close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, e);
            return false;
        }

    }

    /**
     * Disconnect the Host. This will check whether the host was connected to. If so, the connection to it is
     * unregistered and closed.
     *
     * @return Whether the host has successfully been disconnected.
     */
    public boolean disconnectHost() {
        return this.hasHost() && this.disconnect(host);
    }

    /**
     * Removes the {@code Connection} associated with the given {@code Device}. This does not perform any operations on
     * the {@code Connection}.
     *
     * @param device The {@code Device} for which to remove the {@code Connection}
     * @return The removed {@code Connection}
     */
    private Connection removeConnection(Device device) {
        return this.connections.remove(device.address);
    }

    /**
     * Indicates whether the given {@code Connection} is used as a connection for this ConnectionManager.
     *
     * @param connection The {@code Connection} for which to verify that is used as a connection.
     * @return
     */
    public boolean isConnectedBy(Connection connection) {
        return this.isConnectedTo(connection.getDevice()) && this.get(connection.getDevice()).equals(connection);
    }

    /**
     * Checks whether a {@code Device} is connected to by this ConnectionManager.
     *
     * @param device The {@code Device} for which to see whether a {@code Connection} exists.
     * @return {@code true} if a {@code Connection} exists, {@code false} otherwise.
     */
    public boolean isConnectedTo(Device device) {
        return this.connections.containsKey(device.address);
    }

    /**
     * Indicates whether the {@code ConnectionManager} is currently connected to a host.
     *
     * @return Whether the {@code ConnectionManager} is currently connected to a host.
     */
    public boolean hasHost() {
        return this.host != null && this.isConnectedTo(this.host);
    }

    /**
     * Indicates whether a the given {@code Device} has been set as the host.
     *
     * @return Whether or not the given device is the host.
     */
    public boolean isHost(Device device) {
        return this.hasHost() && this.host.equals(device);
    }

    /**
     * Get a list of all devices that are connected to this device. The list is constructed for every instance. Changing
     * the list has no influence on the connected devices.
     *
     * @return A list containing all devices that are connected.
     */
    public List<Device> connectedDevices() {
        LinkedList<Device> devices = new LinkedList<>();
        for (Connection connection : this.connections.values())
            devices.add(connection.getDevice());
        return devices;
    }

    /**
     * Indicate that the {@code ConnectionManager} should start accepting outside connections. This will allow outside
     * devices to connect. No It will however not close the connections that have been opened while it was hosting.
     *
     * @throws IOException If the socket couldn't be closed.
     */
    public void startHosting() throws IOException {

        this.connectivity.startHosting();
    }

    /**
     * Indicate that the {@code ConnectionManager} should no longer accept outside connections. This will keep new
     * devices from connecting. It will however not close the connections that have been opened while it was hosting.
     *
     * @throws IOException If the socket couldn't be closed.
     */
    public void stopHosting() throws IOException {
        this.connectivity.stopHosting();
    }


    /**
     * Clears the current connections. This will disconnect all currently open connections. This may or may not include
     * the connection to the host, depending on the value of {@code includeHost}
     *
     * @param includeHost Whether the current host should also be included.
     * @return Whether any of the connections failed to disconnect.
     */
    public boolean clearConnections(boolean includeHost) {

        // Speed up: if there is no host, we can "include host", because we need to remove all connections.
        // The first check of the loop below then always evaluates to true.
        if (!this.hasHost())
            includeHost = true;

        boolean failures = false;
        for (Connection connection : this.connections.values())
            if (includeHost || !this.isHost(connection.getDevice()))
                failures |= this.disconnect(connection);

        return failures;
    }

    /**
     * Get the local address of this device.
     *
     * @return The local address of this device.
     */
    public String localAddress() {
        return connectivity.localAddress();
    }

    public boolean isHosting() {
        return this.connectivity.isHosting();
    }


    private class ConnectivityPlugin {

        private ServerSocket server = null;

        /**
         * @throws IOException
         */
        private void startHosting() throws IOException {
            this.server = new ServerSocket(yarmis.settings.COMMUNICATION_PORT);
            this.startAccepting();
        }

        private void startAccepting() {
            (new Thread(new Runnable() {

                @Override
                public void run() {

                    try {
                        while (true) {
                            Socket socket = ConnectivityPlugin.this.server.accept();
                            new Connection(yarmis, socket.getInetAddress(), socket.getInputStream(), socket.getOutputStream());
                        }
                    } catch (SocketException e) {
                        // Socket is closed
                        Log.i("CommunicationManager", "Closed socket.");
                    } catch (IOException e) {
                        Log.e(TAG, e);

                    }
                }

            })).start();

        }

        /**
         * @throws IOException
         */
        private void stopHosting() throws IOException {
            this.server.close();
        }

        /**
         * Directly connects to the given {@code Device}. This should also call {@code registerConnection(Connection)}
         * if the connection was made.
         *
         * @param device The device to connect to.
         * @return The created Connection.
         * @throws IOException
         */
        protected Connection connectTo(Device device) throws IOException {

            Socket socket = new Socket(device.address, yarmis.settings.COMMUNICATION_PORT);
            return new Connection(yarmis, device, socket.getInputStream(), socket.getOutputStream());
        }

        /**
         * Indicates whether the {@code ConnectivityPlugin} is currently hosting
         *
         * @return true if the {@code ConnectivityPlugin} is hosting, false otherwise.
         */
        protected boolean isHosting() {
            return this.server != null && !this.server.isClosed();
        }

        /**
         * Get the localaddress, given as a String. This is the address where other devices should connect to to reach
         * this server.
         *
         * @return The local address of this device. This is the address where other devices should connect to to reach
         * this server.
         */
        public String localAddress() {
            // TODO Auto-generated method stub
            return null;
        }

    }


}
