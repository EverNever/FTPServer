package com.chao.ftp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FTPClient {
    private static FTPClient client = null;
    public static final String TAG = "FTPClient";

    public String host;
    private Socket cmdSocket;
    private Socket dataSocket;

    private PrintWriter cmdOut;
    private BufferedReader cmdIn;

    private ServerSocket serverSocket;

    private FTPClient() {

    }

    public static FTPClient getInstance() {
        if (client == null) {
            client = new FTPClient();
        }
        return client;
    }

    public static void clearInstance() {
        client = null;
    }

    public boolean login(String host, int port, String username, String password) {
        this.host = host;
        try {
            cmdSocket = new Socket(host, port);
            cmdIn = new BufferedReader(new InputStreamReader(
                    cmdSocket.getInputStream()));
            cmdOut = new PrintWriter(cmdSocket.getOutputStream());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // 账号
            cmdOut.println("user " + username);
            cmdOut.flush();
            System.out.println(cmdIn.readLine());
            // 密码
            cmdOut.println("pass " + password);
            cmdOut.flush();
            System.out.println(cmdIn.readLine());
            System.out.println(cmdIn.readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public boolean mkdir(String dirName) {
        try {
            cmdOut.println("mkd " + dirName);
            cmdOut.flush();
            System.out.println(cmdIn.readLine());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean download(String localPath, String remotePath, Mode mode) {
        // 主动模式或者被动模式
        if (mode == Mode.port) {
            port();
        } else if (mode == Mode.pasv) {
            pasv();
        } else {
            return false;
        }
        try {
            File dir = new File(localPath.substring(0, localPath.lastIndexOf("/")));
            File file = new File(localPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOut = new FileOutputStream(file);

            cmdOut.println("retr " + remotePath);
            cmdOut.flush();

            if (serverSocket != null) {
                //主动模式
                dataSocket = serverSocket.accept();
            }

            byte[] buffer = new byte[1000];
            BufferedInputStream dataInput = new BufferedInputStream(
                    dataSocket.getInputStream());

            int length;
            while ((length = dataInput.read(buffer)) > 0) {
                fileOut.write(buffer, 0, length);
            }
            fileOut.close();
            dataInput.close();
            System.out.println(cmdIn.readLine());
            dataSocket.close();
            System.out.println(cmdIn.readLine());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean upload(String localPath, String remotePath, Mode mode) {
        if (mode == Mode.pasv) {
            pasv();
        } else if (mode == Mode.port) {
            port();
        } else {
            return false;
        }
        try {
            //先在远端创建一个文件夹
            mkdir(remotePath.substring(0, remotePath.lastIndexOf("/")));

            File file = new File(localPath);
            FileInputStream fileIn = new FileInputStream(file);
            byte[] readByte = new byte[1024];
            cmdOut.println("stor " + remotePath);
            cmdOut.flush();

            System.out.println(cmdIn.readLine());
            if (serverSocket != null) {
                //主动模式
                dataSocket = serverSocket.accept();
            }
            OutputStream out = dataSocket.getOutputStream();

            while (fileIn.read(readByte) > 0) {
                out.write(readByte);
            }

            fileIn.close();
            out.close();

            System.out.println(cmdIn.readLine());

            dataSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void port() {
        String cmd = "port ";
        try {
            byte[] address = InetAddress.getLocalHost().getAddress();
            serverSocket = new ServerSocket(0);

//TODO 最后改回来
            for (int i = 0; i < 4; ++i) {
                cmd = cmd + (address[i] & 0xff) + ",";
            }

//			cmd = cmd + "127,0,0,1,";

            int port1 = serverSocket.getLocalPort() >> 8;
            int port2 = serverSocket.getLocalPort() & 0xff;

            cmdOut.println(cmd + port1 + "," + port2);
            cmdOut.flush();
            System.out.println(cmdIn.readLine());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void pasv() {
        cmdOut.println("pasv");
        cmdOut.flush();
        String tempString = "";
        try {
            tempString = cmdIn.readLine();
            System.out.println(tempString);
            Pattern pattern = Pattern
                    .compile(".+\\(\\d+,\\d+,\\d+,\\d+,(\\d+),(\\d+)\\)");
            Matcher matcher = pattern.matcher(tempString);
            int port1 = 0;
            int port2 = 0;
            if (matcher.find()) {
                port1 = Integer.parseInt(matcher.group(1));
                port2 = Integer.parseInt(matcher.group(2));
            }

            int port = port1 * 256 + port2;

            dataSocket = new Socket(host, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public enum Mode {
        port, pasv
    }

}
