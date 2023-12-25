package net;

import controller.GameController;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class NetGame {
    public GameController gameController;
    private int port = 14723;
    Socket sock;
    public void startOnlineGame(int port,boolean isHost){
        if (port<=0||port>65535){
            System.err.println("Illegal Port!");
            return;
        }
        this.port = port;
        if (isHost) serverHost();
        else connectHost();
    }
    public void serverHost() {
        JDialog jd;
        try (ServerSocket ss = new ServerSocket(port)){
            try {
                jd = new JDialog(new JFrame("Oh no"), "Wait for player");
                jd.setVisible(true);
                sock = ss.accept();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        jd.dispose();
        System.out.println("connected from " + sock.getRemoteSocketAddress());
        Thread t = new Handler(sock,true,gameController);
        t.start();
    }
    public void connectHost(){
        try {
            sock = new Socket("localhost", port);
            Thread t = new Handler(sock,false,gameController);
            t.start();
        }catch (IOException ioe){
            System.err.println("ioe");
        }
    }
    public void callHostTerminate(){

    }

    public void registerController(GameController gameController) {
        this.gameController = gameController;
    }

}
class Handler extends Thread {
    Socket sock;
    boolean isHost;
    GameController gameController;
    public Handler(Socket sock,boolean isHost,GameController gameController) {
        this.sock = sock;
        this.isHost = isHost;
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
        if (isHost) {
            writer.write(gameController.getDifficulty().getDifficultyInfo());
        }
        writer.flush();
        for (;;) {
            String s = reader.readLine();
            if (s.equals("clientDone")) {
                gameController.onlineGameTerminate(false);
                break;
            }
            writer.flush();
        }
    }
}