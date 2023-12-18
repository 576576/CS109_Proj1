package net;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(NetGame.getPort()); // 监听指定端口
        System.out.println("server is running...");
        Socket sock = ss.accept();
        System.out.println("connected from " + sock.getRemoteSocketAddress());
        Thread t = new Handler(sock);
        t.start();
    }
}
class Handler extends Thread {
    Socket sock;
    public Handler(Socket sock) {
        this.sock = sock;
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
            System.out.println("client disconnected.");
        }
    }

    private void handle(InputStream input, OutputStream output) throws IOException {
        var writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        var reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        writer.write("helloAsChessGame\n");
        writer.flush();
        for (;;) {
            String s = reader.readLine();
            if (s.equals("clientDone")) {

                break;
            }
            writer.write("ok me: " + s + "\n");
            writer.flush();
        }
    }
}