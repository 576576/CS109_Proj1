package net;

import controller.GameController;

import javax.swing.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class NetGame {
    private GameController gameController;
    protected static int port = 14723;
    Socket socket;
    //TODO:finish the net package
    public void serverHost() throws IOException {
        try (ServerSocket ss = new ServerSocket(port)) {
            JDialog jd = new JDialog(new JFrame("Oh no"), "Wait for player");
            socket = ss.accept();
        }
        var adrdress = socket.getRemoteSocketAddress();
        var inputStream = socket.getInputStream();
    }
    public void connectHost() throws IOException {
        try (ServerSocket ss = new ServerSocket(port)) {
            socket = ss.accept();
        }
    }

    public static int getPort() {
        return port;
    }

    public void registerController(GameController gameController) {
        this.gameController = gameController;
    }
}
