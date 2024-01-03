package net;

import controller.GameController;

import javax.swing.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
        JFrame waitFrame = new JFrame("Wait For Player ");
        waitFrame.setSize(400,0);
        waitFrame.setLocationRelativeTo(null);
        waitFrame.setVisible(true);
        System.out.println("OnlineGame Host: Wait for player");
        try (ServerSocket ss = new ServerSocket(port)){
            waitFrame.setVisible(true);
            waitFrame.setTitle("Wait for player: "+getPublicIP());
            try {
                sock = ss.accept();
            } catch (IOException e) {
                System.err.println("Fail: Connection fail");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(gameController.getChessGameFrame(),"Host open Error");
            gameController.getChessGameFrame().returnToTitle();
        }finally {
            waitFrame.dispose();
        }
        System.out.println("connected from " + sock.getRemoteSocketAddress());
        t = new Handler(sock,gameController);
        t.start();
    }
    public void connectHost(){
        System.out.println("OnlineGame Joiner: Wait for player");
        String host;
        host = JOptionPane.showInputDialog(null,"Input host","Connect to host",JOptionPane.PLAIN_MESSAGE);
        try {
            sock = new Socket(host, port);
            t = new Handler(sock,gameController);
            t.start();
        }catch (IOException ioe){
            System.err.println("Fail: Server Not Found");
            JOptionPane.showMessageDialog(gameController.getChessGameFrame(),"Please open host first!");
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
    Socket sock;
    GameController gameController;
    public Handler(Socket sock,GameController gameController) {
        this.sock = sock;
        this.gameController=gameController;
    }
    @Override
    public void run() {
        try (InputStream input = this.sock.getInputStream()) {
            try (OutputStream output = this.sock.getOutputStream()) {
                handle(input, output);
            }
        } catch (Exception e) {
            try {
                this.sock.close();
            } catch (IOException ioe) {
                System.err.println("Connection failed.");
            }
            System.out.println("Disconnected.");
        }
    }

    private void handle(InputStream input, OutputStream output){
        var writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String s;
        try {
            if (startPlayMode==3) { //To host a game: send the same level to joiner
                for (;;) {
                    if (isNewGameInitialized) {
                        do {
                            writer.write("InitializeGame\n");
                            writer.flush();
                            writer.write(gameController.ConvertToString());
                            writer.flush();
                            s = reader.readLine();
                        } while (!s.equals("receiveInitializedGame"));
                        System.out.println("Host Initialized");
                        break;
                    }
                    System.out.print("");
                }
                writer.flush();

                do {
                    s = reader.readLine();
                    System.out.println(s);
                } while (!s.equals("receiveInitializedGame"));
            }
            if (startPlayMode==4) { //As joiner: receive the level
                for (;;){
                    s =reader.readLine();
                    if (s.equals("InitializeGame")){
                        StringBuilder sb = new StringBuilder();
                        for (int i=0;i<9;i++){
                            s =reader.readLine();
                            sb.append(s);
                        }
                        gameController.loadFromString(sb.toString());
                        writer.write("receiveInitializedGame");
                        writer.flush();
                        break;
                    }
                    System.out.print("");
                }
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
                    writer.write(gameController.timeLeft);
                    writer.flush();
                }
            }
        } catch (IOException e) {
            System.err.println("Disconnected.");
            gameController.onlineGameTerminate(true);
        }
    }
}