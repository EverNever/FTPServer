package com.chao.ftpserver;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by chao on 2016/5/24.
 */
public class FTPServer {
    Config config;
    int connectionCount = 0;

    public FTPServer() {
        config = new Config();
        try {
            ServerSocket serverSocket = new ServerSocket(config.port);
            while (true) {
                // accept是阻塞方法
                Socket mSocket = serverSocket.accept();
                String remoteAddress = mSocket.getRemoteSocketAddress().toString();
                String realClientAddress = remoteAddress.substring(1, remoteAddress.lastIndexOf(":"));
                System.out.print(realClientAddress);
                for (String blackIP : config.blackIPList) {
                    if (realClientAddress.equals(blackIP)) {
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
                        out.write("430 server refuse your connection for you ip is in black list\n");
                        out.flush();
                        out.close();
                        mSocket.close();
                    }
                }
                // 黑名单中会断开mSocket
                if (!mSocket.isClosed()) {
                    connectionCount++;
                    new FTPThread(mSocket).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class FTPThread extends Thread {
        private Socket mSocket;

        String cmd;
        String params;

        public FTPThread(Socket mSocket) {
            this.mSocket = mSocket;
        }

        @Override
        public void run() {
            BufferedWriter out;
            BufferedReader in;
            try {
                // outputStream只能读byte，OutputStreamWriter，bufferedWriter加上buffer
                out = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
                in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                out.write("220 FTP Server is ready.\n");
                out.flush();

                while (true) {
                    String input = in.readLine();
                    if (!input.contains(" ")) {
                        // no params
                        cmd = input;
                    } else {
                        cmd = input.substring(0, input.indexOf(" "));
                        params = input.substring(input.indexOf(" ") + 1);
                    }
                    cmd = cmd.toUpperCase();

                    switch (cmd) {
                        case "USER":
                            handleUSER();
                            break;
                        case "PASS":
                            handlePASS();
                            break;
                        default:
                            // error
                            break;
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void handleUSER() {

        }

        void handlePASS() {

        }

        void handleQUIT() {
            connectionCount--;
        }
    }

}

