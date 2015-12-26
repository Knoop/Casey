package com.knoop.casey;

import com.knoop.casey.exceptions.FailedCommandExecution;
import com.yarmis.core.exceptions.FailedExecutionException;
import com.yarmis.core.logging.Log;

import java.io.File;
import java.io.IOException;

/**
 * Created by Maurice on 24-12-2015.
 */
public class Driver {

    /**
     * Starts a new process in which TestServer is executed.
     *
     * @return The exit value of running the command
     * @throws IOException
     * @throws InterruptedException
     */
    public static void runCommand(Command command) {

        File location = new File("lib/lights");
        Log.d("Driver", "Running command: " + command.format());
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", location.getPath() + "/" + command.format());
        builder.inheritIO();
        builder.redirectErrorStream(true);
        int result = -1;
        try{
            result = builder.start().waitFor();
        } catch (InterruptedException | IOException e) {
            throw (FailedCommandExecution) new FailedCommandExecution().initCause(e);
        }

        if(result != 0)
            throw new FailedCommandExecution(result);
    }

    public static class Command {

        private final String address, channel;
        private final Socket.Protocol protocol;
        private final Socket.State state;


        public Command(Socket socket, Socket.State state) {
            this.protocol = socket.getProtocol();
            this.address = socket.getAddress();
            this.channel = socket.getChannel();
            this.state = state;
        }


        public String format() {

            return this.protocol.name().toLowerCase()
                    + " " + this.channel.toUpperCase()
                    + " " + this.address.toUpperCase()
                    + " " + this.state.name().toLowerCase();
        }

    }

}
