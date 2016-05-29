package com.chao.ftpserver;

public class Main {

    public static void main(String[] args) {
        // 配置文件路径
        String configPath = "init.conf";
        if (args.length == 1) {
            configPath = args[0];
        }
        FTPServer ftpServer = new FTPServer(configPath);
        ftpServer.startServer();
    }

}
