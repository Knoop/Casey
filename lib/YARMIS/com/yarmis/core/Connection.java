package com.yarmis.core;

import com.yarmis.core.exceptions.ConnectionNotAllowedException;
import com.yarmis.core.logging.Log;
import com.yarmis.core.messages.Message;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;

/**
 * Placeholder for the later coming connectivity facility
 *
 * @author Maurice
 */
public class Connection {

    /**
     * Indicates that this Connection has been closed. No new messages can be received if it is closed. Once the
     * Connection is closed it can not be reopened. If the connection is closed it is still possible to send messages.
     */
    boolean isClosed = false;

    /**
     * The YARMIS instance that uses this Connection.
     */
    private final Yarmis yarmis;

    private final InputStream inputStream;

    private final OutputStream outputStream;

    private BufferedWriter writer;

    private static final char SEPARATOR = ':';

    /**
     * The identifier for the device to which this is a Connection.
     */
    private final Device device;

    public Connection(Yarmis yarmis, InetAddress address, InputStream inputStream,
                      OutputStream outputStream) throws ConnectionNotAllowedException {
        this(yarmis, DeviceManager.createDevice(address), inputStream, outputStream);
    }

    public Connection(Yarmis yarmis, Device device, InputStream inputStream,
                      OutputStream outputStream) throws ConnectionNotAllowedException {

        this.yarmis = yarmis;
        this.device = device;
        this.inputStream = inputStream;
        this.outputStream = outputStream;

        // Register this connection to validate that it is allowed.
        // This is done after all given properties are set, but before it is possible to read or write with this Connection.
        this.yarmis.connection.register(this);

        this.setup();
    }

    /**
     * Perform the setup such that the Connection can read and write.
     */
    private final void setup() {

        this.writer = new BufferedWriter(new OutputStreamWriter(this.outputStream));
        (new Thread(new Reader())).start();
    }

    public final Device getDevice() {
        return this.device;
    }

    public final void close() throws IOException {

        if (this.isClosed)
            return;

        this.inputStream.close();
        this.outputStream.close();

        this.isClosed = true;

        this.yarmis.connection.disconnect(this);
    }


    /**
     * Called when the reader detects that the {@Code InputStream} was closed. This can either occur because {@code
     * Connection.close()} was called, or because it was called on the other {@code Device}
     */
    private void handleClosedConnection() {

        try {
            this.close();
        } catch (IOException e) {
            Log.e("Connection", e);
        }

    }

    /**
     * Indicates whether this Connection is closed. It is not possible to reopen a closed Connection.
     *
     * @return
     */
    public final boolean isClosed() {
        return this.isClosed;
    }

    /**
     * Returns the fingerprint of the public key that is associated with this connection, or @code{null} if no public
     * key is associated with this connection.
     *
     * @return [description]
     */
    public String getKeyFingerprint() {
        return null;
    }

    /**
     * Sends a JSONObject over the connection.
     *
     * @param message The message to send
     * @throws IOException
     */
    protected void send(Message message) throws IOException, JSONException {
        String jsonString = message.translate(yarmis.communication.communication).toString();
        // Determine the number of characters
        // NOTE THAT THIS IS DIFFERENT FROM THE NUMBER OF BYTES
        int l = jsonString.length();
        // Prepend the number of characters
        writer.write(Integer.toString(l) + Character.toString(SEPARATOR));
        writer.write(jsonString);
        writer.flush();
    }

    protected void receive(JSONObject message) {
        yarmis.communication.handleMessage(message, this);
    }

    /**
     * Reads the incoming messages over this connection.
     *
     * @author Maurice
     */
    private final class Reader implements Runnable {

        public void run() {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    Connection.this.inputStream));

            try {

                int i = reader.read();
                while (i != -1) {
                    char c = (char) i;
                    int length = 0;
                    do {
                        length *= 10;
                        length += Character.getNumericValue(c);
                        i = reader.read();
                    } while (i != -1 && (c = (char) i) != SEPARATOR);
                    if (i != -1) {
                        // We expect `length` characters of data
                        // NOTE THAT THIS IS DIFFERENT FROM THE NUMBER OF BYTES
                        char[] jsonArray = new char[length];
                        int read = 0;
                        do {
                            i = reader.read(jsonArray, read, length - read);
                            read += i;
                        } while (i != -1 && read < length);
                        // Break if end of stream has been reached,
                        // or if # of read chars differs from what
                        // we've expected
                        if (i == -1 || read != length) {
                            Log.warn("Connection", "Connection closed unexpectedly after reading " + i + " characters where we expected " + length + " characters.");
                            break;
                        } else {
                            String jsonString = new String(jsonArray);
                            Connection.this.receive(new JSONObject(jsonString));
                            i = reader.read();
                        }
                    }
                }

            } catch (Exception e) {
                Log.e("Connection", e);
            } finally {
                // Nothing left to read: inputstream is closed
                Connection.this.handleClosedConnection();
            }
        }
    }
}
