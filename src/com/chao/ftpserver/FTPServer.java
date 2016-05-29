package com.chao.ftpserver;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by chao on 2016/5/24.
 */
public class FTPServer {
    Config config;
    String workDirectory = "H:/";

    int connectionCount = 0;

    public FTPServer() {
        config = new Config();
        try {
            ServerSocket serverSocket = new ServerSocket(config.port);
            System.out.println(serverSocket.getInetAddress().getHostAddress()); // 0.0.0.0
            System.out.println(serverSocket.getLocalSocketAddress()); //0.0.0.0/0.0.0.0:21
            while (true) {
                // accept是阻塞方法
                Socket mSocket = serverSocket.accept();
                System.out.println(mSocket.getLocalSocketAddress()); // /192.168.1.104:21
                System.out.println(mSocket.getLocalAddress().getHostAddress()); // 192.168.1.104
                System.out.println(mSocket.getInetAddress().getHostAddress()); // 192.168.1.101

                String remoteAddress = mSocket.getRemoteSocketAddress().toString();
                String realClientAddress = remoteAddress.substring(1, remoteAddress.lastIndexOf(":"));

                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
                // 黑名单
                for (String blackIP : config.blackIPList) {
                    if (realClientAddress.equals(blackIP)) {
                        out.write("430 server refuse your connection for you ip is in black list\n");
                        out.flush();
                        out.close();
                        mSocket.close();
                    }
                }
                // 最大连接数
                if (connectionCount >= config.maxConnection) {
                    out.write("430 server refuse your connection for max connection\n");
                    out.flush();
                    out.close();
                    mSocket.close();
                }

                // 黑名单会断开mSocket
                if (!mSocket.isClosed()) {
                    connectionCount++;
                    System.out.println(remoteAddress + " connected. Now connection count is " + connectionCount);
                    new FTPThread(mSocket).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class FTPThread extends Thread {
        private Socket mSocket; // master socket
        private Socket sSocket; // slave socket

        BufferedWriter cmdOut; // command out
        BufferedReader cmdIn; // command in

        // 被动模式下开启ServerSocket，等待client连接，accept以后返回sSocket传输数据
        private ServerSocket pasvServerSocket;

        String cmd;
        String params;

        String response;

        String username;

        String remoteAddress;
        int remotePort; // 主动连接时client开放的端口，不是mSocket的client端口

        TransferType transferType = TransferType.binary;
        TranferMode tranferMode = TranferMode.pasv;

        public FTPThread(Socket mSocket) {
            this.mSocket = mSocket;
            String tempAddress = mSocket.getRemoteSocketAddress().toString();
            remoteAddress = tempAddress.substring(1, tempAddress.lastIndexOf(":"));
        }

        @Override
        public void run() {
            try {
                // outputStream只能读byte，OutputStreamWriter，bufferedWriter加上buffer
                cmdOut = new BufferedWriter(new OutputStreamWriter(mSocket.getOutputStream()));
                cmdIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));

                cmdOut.write("220 FTP Server is ready.\n");
                cmdOut.flush();

                String input;
                while ((input = cmdIn.readLine()) != null) {
                    System.out.println("client request: " + input);

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
                        case "QUIT":
                            handleQUIT();
                            break;
                        case "TYPE":
                            handleTYPE();
                            break;
                        case "PASV":
                            handlePASV();
                            break;
                        case "PORT":
                            handlePORT();
                            break;
                        case "RETR":
                            handleRETR();
                            break;
                        case "STOR":
                            handleSTOR();
                            break;
                        case "DELE":
                            break;
                        case "LIST":
                            break;
                        case "PWD":
                            break;
                        case "CWD":
                            break;
                        default:
                            response = "501 Syntax error in parameters or arguments.";
                            break;
                    }

                    System.out.println("server response: " + response);
                    cmdOut.write(response + "\n");
                    cmdOut.flush();
                }
                cmdOut.close();
                cmdIn.close();
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void handleUSER() {
            username = params;
            response = "331 User " + username + " accepted, provide password.";
        }

        void handlePASS() {
            for (Config.User user : config.userList) {
                if (username.equals(user.username)) {
                    if (params.equals(user.password)) {
                        response = "230 User " + username + " logged in.";
                        return;
                    }
                }
            }
            response = "530 Not logged in.";
        }

        void handleQUIT() {
            response = "221 Service closing control connection.";
            connectionCount--;
        }

        void handleTYPE() {
            if (params.equals("A")) {
                transferType = TransferType.ascii;
                response = "200 Command okay change to ASCII mode.";
            } else if (params.equals("I")) {
                transferType = TransferType.binary;
                response = "200 Command okay change to BINARY mode.";
            } else {
                response = "504 error in parameter.";
            }
        }

        void handlePASV() {
            String ipAddress = "";
            try {
//                byte[] address = InetAddress.getLocalHost().getAddress(); // 返回的是byte型的0.0.0.0
                byte[] address = mSocket.getLocalAddress().getAddress();

                for (int i = 0; i < 4; i++) {
                    // 获得10进制的地址
                    ipAddress += ((address[i] & 0xff) + ",");
                }

                pasvServerSocket = new ServerSocket(0);

                System.out.println("change to pasv mode. listen on " + ipAddress + pasvServerSocket.getLocalPort());

                int port1 = pasvServerSocket.getLocalPort() >> 8;
                int port2 = pasvServerSocket.getLocalPort() & 0xff;
                // 227 entering passive mode (127,0,0,1,4,18)
                response = "227 entering passive mode (" + ipAddress + port1 + "," +port2 + ")";
                // 进入被动模式
                tranferMode = TranferMode.pasv;
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void handlePORT() {
            // PORT 192,168,1,100,224,250
            // TODO remote address

            String[] tempAddress = params.split(",");
            int port1 = Integer.parseInt(tempAddress[4]);
            int port2 = Integer.parseInt(tempAddress[5]);
            remotePort = (port1 << 8) + port2;

            response = "227 entering positive mode.";
            tranferMode = TranferMode.port;
        }

        void handleRETR() {
            File file = new File(params);
            if (!file.exists()) {
                response = "550 File not found.";
                return;
            }

            try {
                if (tranferMode == TranferMode.port) {
                    // 主动模式下直接bind到client
                    sSocket = new Socket(remoteAddress, remotePort,
                            InetAddress.getByName(mSocket.getLocalAddress().getHostAddress()), 20);
                } else if (tranferMode == TranferMode.pasv) {
                    // 被动模式下server等待client连接
                    sSocket = pasvServerSocket.accept();
                }

                int downloadSize = 0; // 下载文件流量

                if (transferType == TransferType.ascii) {
                    // 必须要有这条150返回
                    cmdOut.write("150 Opening ASCII mode data connection for "+ params + "\n");
                    cmdOut.flush();

                    // reader跟writer是字符流
                    Reader in = new FileReader(file);
                    Writer out = new OutputStreamWriter(sSocket.getOutputStream(), "UTF-8");

                    int readChar;
                    while ((readChar = in.read()) != -1) {
                        // TODO 统计下载流量
                        out.write(readChar);
                        downloadSize++; // TODO 为何不是2B？读一个字符应该是2B
                    }
                    in.close();
                    out.close();
                } else if (transferType == TransferType.binary) {
                    cmdOut.write("150 Opening Binary mode data connection for "+ params + "\n");
                    cmdOut.flush();
                    // stream的都是字节流

                    // file输入流
                    InputStream in = new FileInputStream(file);
                    // socket输出流
                    OutputStream out = sSocket.getOutputStream();

                    int readByte;
//                    byte[] readByte = new byte[1]; // 1B
                    while ((readByte = in.read()) != -1) {
                        // TODO 统计下载流量
                        downloadSize++;
                        out.write(readByte);
                    }
                    in.close();
                    out.close();
                }

                System.out.println("download file size: " + downloadSize + "B");
                sSocket.close();
                response = "226 Transfer complete!";
            } catch (IOException e) {
                response = "451 Requested action aborted: local error in processing.";
                e.printStackTrace();
            }
        }

        void handleSTOR() {
            File file = new File(params);
            try {
                if (!file.createNewFile()) {
                    response = "550 create new file failed.";
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                if (tranferMode == TranferMode.port) {
                    // 主动模式下直接bind到client
                    sSocket = new Socket(remoteAddress, remotePort,
                            InetAddress.getByName(mSocket.getLocalAddress().getHostAddress()), 20);
                } else if (tranferMode == TranferMode.pasv) {
                    // 被动模式下server等待client连接
                    sSocket = pasvServerSocket.accept();
                }

                int uploadSize = 0;

                if (transferType == TransferType.ascii) {
                    cmdOut.write("150 Opening ASCII mode data connection for "+ params + "\n");
                    cmdOut.flush();

                    Reader in = new InputStreamReader(sSocket.getInputStream(), "UTF-8");
                    Writer out = new FileWriter(file);

                    int readChar;
//                    char[] readChar = new char[1024];
                    while ((readChar = in.read()) != -1) {
                        // TODO 统计上传流量
                        out.write(readChar);
                        uploadSize++;
                    }
                    in.close();
                    out.close();
                } else if (transferType == TransferType.binary) {
                    cmdOut.write("150 Opening Binary mode data connection for "+ params + "\n");
                    cmdOut.flush();

                    InputStream in = sSocket.getInputStream();
                    OutputStream out = new FileOutputStream(file);

                    int readByte;
//                    byte[] readByte = new byte[1024];
                    while ((readByte = in.read()) != -1) {
                        // TODO 统计上传流量
                        out.write(readByte);
                        uploadSize++;
                    }
                    System.out.println("upload file size: " + uploadSize + "B");
                    in.close();
                    out.close();
                }
                sSocket.close();
                response = "226 Transfer complete!";
            } catch (IOException e) {
                response = "451 Requested action aborted: local error in processing.";
                e.printStackTrace();
            }
        }

        void handleDELE() {

        }
    }

    enum TransferType {
        ascii,
        binary
    }

    enum TranferMode {
        pasv,
        port
    }

}

