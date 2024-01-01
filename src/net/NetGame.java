package net;

import controller.GameController;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static view.MenuFrame.startPlayMode;

public class NetGame {
    public GameController gameController;
    private final int port = 14723;
    Socket sock;
    static JDialog jd = new JDialog();
    public NetGame(){
        jd.setLocationRelativeTo(null);
        jd.setResizable(false);
    }
    public void serverHost() {
        jd = new JDialog(gameController.getChessGameFrame(), "Wait for player");
        jd.setVisible(true);
        System.out.println("OnlineGame: Wait for player");
        try (ServerSocket ss = new ServerSocket(port)){
            try {
                sock = ss.accept();
            } catch (IOException e) {
                System.err.println("Fail: Connection fail");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("connected from " + sock.getRemoteSocketAddress());
        Thread t = new Handler(sock,gameController);
        t.start();
    }
    public void connectHost(){
        jd = new JDialog(gameController.getChessGameFrame(), "Wait for player");
        jd.setVisible(true);
        System.out.println("OnlineGame: Wait for player");
        try {
            sock = new Socket("localhost", port);
            Thread t = new Handler(sock,gameController);
            t.start();
        }catch (IOException ioe){
            System.err.println("Fail: Connection fail");
            ioe.printStackTrace();
        }
    }
    public void registerController(GameController gameController) {
        this.gameController = gameController;
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

    private void handle(InputStream input, OutputStream output) throws IOException {
        var writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        if (startPlayMode==3) {
            writer.write("InitializeGame\n");
            writer.write(gameController.toString());
            writer.flush();
            for (;;){
                String s = reader.readLine();
                if (s.equals("receiveInitializedGame")) {
                    NetGame.jd.dispose();
                    break;
                }
            }
        }
        if (startPlayMode==4) {
            reader.readLine();
            for (;;){
                String s =reader.readLine();
                if (s.equals("InitializeGame")){
                    StringBuilder sb = new StringBuilder();
                    for (int i=0;i<9;i++) sb.append(reader.readLine());
                    gameController.loadFromString(sb.toString());
                    writer.write("receiveInitializedGame");
                    writer.flush();
                    NetGame.jd.dispose();
                    break;
                }
            }
        }
        for (;;) {
            String s = reader.readLine();
            if (s.equals("clientDone")) {
                gameController.onlineGameTerminate(reader.readLine().equals("2"));
                break;
            }
            if (!gameController.isAlive()){
                writer.write("clientDone\n");
                writer.write(gameController.getVictoryMode());
                writer.flush();
                break;
            }
        }
    }
}