package net;

import config.GameSettings;
import config.PlayMode;
import controller.GameController;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import util.Log;

public class NetGame {
    public GameController gameController;
    private final int port = 14723;
    private final GameSettings settings;
    Socket sock;
    private Thread handler;
    public NetGame(GameSettings settings){
        this.settings = settings;
    }

    /** 断开这一局的连接线程。 */
    public void stopHandler() {
        if (handler != null) handler.interrupt();
    }
    public void serverHost() {
        JFrame waitFrame = new JFrame("Waiting for Player...");
        waitFrame.setSize(400, 100);
        waitFrame.setLocationRelativeTo(null);
        waitFrame.setVisible(true);
        Log.info("OnlineGame Host: Waiting for player.");

        try (ServerSocket ss = new ServerSocket(port)) {
            waitFrame.setTitle("Waiting for player: " + getPublicIP());
            sock = ss.accept();
            Log.info("Connected from " + sock.getRemoteSocketAddress());
        } catch (IOException _) {
            JOptionPane.showMessageDialog(gameController.getGameFrame(), "Failed to establish connection.");
            gameController.getGameFrame().returnToTitle();
            return;
        } finally {
            waitFrame.dispose();
        }

        handler = new Handler(sock, gameController, settings.playMode());
        handler.start();
    }
    public void connectHost(){
        String host = JOptionPane.showInputDialog(null,"Enter host","Connect to Host",JOptionPane.PLAIN_MESSAGE);
        if (host == null || host.isEmpty()) {
            JOptionPane.showMessageDialog(gameController.getGameFrame(), "Invalid host address.");
            return;
        }

        try {
            sock = new Socket(host, port);
            handler = new Handler(sock, gameController, settings.playMode());
            handler.start();
        } catch (IOException _) {
            JOptionPane.showMessageDialog(gameController.getGameFrame(),"Unable to connect to the server.\nPlease ensure the host is online.");
            gameController.getGameFrame().returnToTitle();
        }
    }
    public void registerController(GameController gameController) {
        this.gameController = gameController;
    }
    public static String getPublicIP(){
        String ip = null;
        try {
            Socket socket = new Socket("www.baidu.com", 80);
            InetAddress inetAddress = socket.getLocalAddress();
            ip = inetAddress.getHostAddress();
            Log.info("Local IP: " + ip);
            socket.close();
        } catch (Exception _) {
            Log.warn("Exception occurred on get local IP");
        }
        return ip;
    }
}
class Handler extends Thread {
    private final Socket sock;
    private final GameController gameController;
    private final PlayMode playMode;
    private static final AtomicBoolean running = new AtomicBoolean(true);

    Handler(Socket sock, GameController gameController, PlayMode playMode) {
        this.sock = sock;
        this.gameController = gameController;
        this.playMode = playMode;
    }
    @Override
    public void run() {
        try (InputStream input = sock.getInputStream(); OutputStream output = sock.getOutputStream()) {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

            handleCommunication(writer, reader);
        } catch (Exception e) {
            Log.error("Connection lost: " + e.getMessage());
            gameController.onlineGameTerminate(true);
        } finally {
            try {
                sock.close();
            } catch (IOException ioe) {
                Log.warn("Error closing socket: " + ioe.getMessage());
            }
        }
    }

    private void handleCommunication(BufferedWriter writer, BufferedReader reader) throws IOException {
        String s;
        while (running.get()) {
            if (playMode == PlayMode.HOST) {
                initializeGameAsHost(writer, reader);
            } else if (playMode == PlayMode.JOIN) {
                joinGame(writer, reader);
            }

            gameController.resetTimeLeft();
            for (;;) {
                s = reader.readLine();
                if (s.equals("clientDone")) {
                    gameController.onlineGameTerminate(reader.readLine().equals("2"));
                    break;
                }
                if (!gameController.isAlive() || isInterrupted()){
                    writer.write("clientDone\n");
                    writer.write(gameController.getVictoryMode());
                    writer.flush();
                    break;
                }
                if (s.equals("synchronization")){
                    gameController.setTimeLeft(Integer.parseInt(reader.readLine()));
                }
                if (gameController.timeLeft%10==0){
                    writer.write("synchronization\n");
                    writer.write(String.valueOf(gameController.timeLeft));
                    writer.flush();
                }
            }
        }
    }

    private void initializeGameAsHost(BufferedWriter writer, BufferedReader reader) throws IOException {
        synchronized (gameController) {
            gameController.awaitBoardReady();
            do {
                writer.write("InitializeGame\n");
                writer.flush();
                writer.write(gameController.gameStateText());
                writer.write("endOfGame\n");
                writer.flush();
            } while (!"receiveInitializedGame".equals(reader.readLine()));
            Log.info("Host Initialized.");
        }
    }

    private void joinGame(BufferedWriter writer, BufferedReader reader) throws IOException {
        String s;
        while ((s = reader.readLine()) != null && !"InitializeGame".equals(s)) ;

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null && !"endOfGame".equals(line)) {
            sb.append(line).append('\n');
        }
        gameController.loadFromString(sb.toString());
        writer.write("receiveInitializedGame");
        writer.flush();
    }
}