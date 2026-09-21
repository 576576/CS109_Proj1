package net;

import config.GameSettings;
import config.PlayMode;
import controller.GameController;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import util.Log;

public class NetGame {
    public GameController gameController;
    private final int port = 14723;
    private final GameSettings settings;
    private Socket sock;
    private ServerSocket serverSocket;
    private Thread handler;
    /** 建房被用户取消（点了取消/ESC），用来区分"取消"和"真的连不上"。 */
    private final AtomicBoolean hostCancelled = new AtomicBoolean(false);

    public NetGame(GameSettings settings) {
        this.settings = settings;
    }

    /** 建房并阻塞等待一个对手连入；成功返回 true，取消或失败返回 false。 */
    public boolean prepareHost() {
        hostCancelled.set(false);
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            Log.warn("Cannot open server socket: " + e);
            return false;
        }
        Log.info("OnlineGame Host: Waiting for player.");
        try {
            sock = serverSocket.accept();
            Log.info("Connected from " + sock.getRemoteSocketAddress());
            return true;
        } catch (IOException e) {
            if (hostCancelled.get()) Log.info("Host cancelled, socket closed");
            else Log.warn("Failed to establish connection: " + e);
            return false;
        }
    }

    /** 加入对手房间：连接成功返回 true，失败（地址空/非法/超时）返回 false。 */
    public boolean prepareJoin(String host) {
        if (host == null || host.isBlank()) return false;
        try {
            sock = new Socket();
            sock.connect(new InetSocketAddress(host.trim(), port), 5000);
            Log.info("Connected to " + host);
            return true;
        } catch (IOException e) {
            Log.warn("Failed to connect: " + e);
            return false;
        }
    }

    /** 连接已建立后起 Handler 收发；未连接则什么都不做。 */
    public void startHandler() {
        if (isConnected()) {
            handler = new Handler(sock, gameController, settings.playMode());
            handler.start();
        }
    }

    /** 取消建房：关掉监听套接字，让阻塞在 accept() 的线程退出。 */
    public void cancelHost() {
        hostCancelled.set(true);
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    /** 取消加入：关掉正在连接的套接字，让 connect() 立即失败。 */
    public void cancelJoin() {
        try {
            if (sock != null) sock.close();
        } catch (IOException ignored) {
        }
    }

    public boolean isConnected() {
        return sock != null && sock.isConnected();
    }

    public boolean hostCancelled() {
        return hostCancelled.get();
    }

    /** 断开这一局的连接线程。 */
    public void stopHandler() {
        if (handler != null) handler.interrupt();
    }

    public void registerController(GameController gameController) {
        this.gameController = gameController;
    }

    public static String getPublicIP() {
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
                if (!gameController.isAlive() || isInterrupted()) {
                    writer.write("clientDone\n");
                    writer.write(gameController.getVictoryMode());
                    writer.flush();
                    break;
                }
                if (s.equals("synchronization")) {
                    gameController.setTimeLeft(Integer.parseInt(reader.readLine()));
                }
                if (gameController.timeLeft % 10 == 0) {
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
