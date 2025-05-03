package net;

import controller.GameController;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static controller.GameController.isNewGameInitialized;
import static view.MenuFrame.startPlayMode;

public class NetGame {
    public GameController gameController;
    private final int port = 14723;
    Socket sock;
    public static Thread t;
    public NetGame(){
    }
    public void serverHost() {
        JFrame waitFrame = new JFrame("Waiting for Player...");
        waitFrame.setSize(400, 100);
        waitFrame.setLocationRelativeTo(null);
        waitFrame.setVisible(true);
        System.out.println("OnlineGame Host: Waiting for player.");

        try (ServerSocket ss = new ServerSocket(port)) {
            waitFrame.setTitle("Waiting for player: " + getPublicIP());
            sock = ss.accept();
            System.out.println("Connected from " + sock.getRemoteSocketAddress());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(gameController.getChessGameFrame(), "Failed to establish connection.");
            gameController.getChessGameFrame().returnToTitle();
            return;
        } finally {
            waitFrame.dispose();
        }

        t = new Handler(sock, gameController);
        t.start();
    }
    public void connectHost(){
        String host = JOptionPane.showInputDialog(null,"Enter host","Connect to Host",JOptionPane.PLAIN_MESSAGE);
        if (host == null || host.isEmpty()) {
            JOptionPane.showMessageDialog(gameController.getChessGameFrame(), "Invalid host address.");
            return;
        }

        try {
            sock = new Socket(host, port);
            t = new Handler(sock,gameController);
            t.start();
        }catch (IOException ioe){
            JOptionPane.showMessageDialog(gameController.getChessGameFrame(),"Unable to connect to the server.\nPlease ensure the host is online.");
            gameController.getChessGameFrame().returnToTitle();
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
            System.out.println("Local IP: " + ip);
            socket.close();
        } catch (Exception e) {
            Logger.getLogger("Exception occurred on get local IP");
        }
        return ip;
    }
}
class Handler extends Thread {
    private final Socket sock;
    private final GameController gameController;
    private static final AtomicBoolean running = new AtomicBoolean(true);

    public Handler(Socket sock,GameController gameController) {
        this.sock = sock;
        this.gameController=gameController;
    }
    @Override
    public void run() {
        try (InputStream input = sock.getInputStream(); OutputStream output = sock.getOutputStream()) {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));

            handleCommunication(writer, reader);
        } catch (Exception e) {
            Logger.getLogger(Handler.class.getName()).severe("Connection lost: " + e.getMessage());
            gameController.onlineGameTerminate(true);
        } finally {
            try {
                sock.close();
            } catch (IOException ioe) {
                Logger.getLogger(Handler.class.getName()).warning("Error closing socket: " + ioe.getMessage());
            }
        }
    }

    private void handleCommunication(BufferedWriter writer, BufferedReader reader) throws IOException {
        String s;
        while (running.get()) {
            if (startPlayMode == 3) {
                initializeGameAsHost(writer, reader);
            } else if (startPlayMode == 4) {
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
            while (!isNewGameInitialized) {
                try {
                    gameController.wait(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            do {
                writer.write("InitializeGame\n");
                writer.flush();
                writer.write(gameController.ConvertToString());
                writer.flush();
            } while (!"receiveInitializedGame".equals(reader.readLine()));
            System.out.println("Host Initialized.");
        }
    }

    private void joinGame(BufferedWriter writer, BufferedReader reader) throws IOException {
        String s;
        while ((s = reader.readLine()) != null && !"InitializeGame".equals(s)) ;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            sb.append(reader.readLine());
        }
        gameController.loadFromString(sb.toString());
        writer.write("receiveInitializedGame");
        writer.flush();
    }
}