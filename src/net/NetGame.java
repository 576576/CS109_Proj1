package net;

import config.GameSettings;
import config.PlayMode;
import controller.GameController;

import javafx.application.Platform;
import javafx.scene.control.TextInputDialog;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import ui.Dialogs;
import ui.I18n;
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
    /** 建房。accept() 会一直阻塞：用阻塞式等待弹窗挡住界面，连上对手后弹窗自动关闭。 */
    public void serverHost() {
        Thread wait = new Thread(() -> {
            ServerSocket ss;
            try {
                ss = new ServerSocket(port);
            } catch (IOException e) {
                Log.warn("Cannot open server socket: " + e);
                Dialogs.warn(I18n.tr("msg.connFailed"));
                Platform.runLater(gameController::terminate);
                return;
            }
            Log.info("OnlineGame Host: Waiting for player.");
            java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
            Dialogs.waitWhile(
                    I18n.tr("dlg.waiting") + "\n" + I18n.tr("dlg.yourAddress") + getPublicIP(),
                    () -> {
                        try {
                            sock = ss.accept();
                            Log.info("Connected from " + sock.getRemoteSocketAddress());
                        } catch (IOException e) {
                            if (cancelled.get()) Log.info("Host cancelled, socket closed");
                            else Log.warn("Failed to establish connection: " + e);
                        }
                    },
                    () -> {
                        cancelled.set(true);
                        try {
                            ss.close();
                        } catch (IOException ignored) {
                        }
                    });
            if (sock != null && sock.isConnected()) {
                handler = new Handler(sock, gameController, settings.playMode());
                handler.start();
            } else if (!cancelled.get()) {
                Dialogs.warn(I18n.tr("msg.connFailed"));
                Platform.runLater(gameController::terminate);
            }
            // 取消：不启动游戏，也不 terminate，留在建房界面让用户重来
        }, "host-wait");
        wait.setDaemon(true);
        wait.start();
    }

    public void connectHost() {
        var dialog = new TextInputDialog();
        dialog.setTitle("Connect to Host");
        dialog.setHeaderText(null);
        dialog.setContentText("Enter host");
        var host = dialog.showAndWait();
        if (host.isEmpty() || host.get().isBlank()) {
            Dialogs.warn(I18n.tr("msg.invalidHost"));
            return;
        }

        try {
            sock = new Socket(host.get().trim(), port);
            handler = new Handler(sock, gameController, settings.playMode());
            handler.start();
        } catch (IOException e) {
            Dialogs.warn(I18n.tr("msg.noHost"));
            gameController.terminate();
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