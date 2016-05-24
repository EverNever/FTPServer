package com.chao.ftp;

/**
 * Created by chao on 2015/7/7.
 */public class Config {
    public interface FTPConfig {
        //假的数据
        String HOST = "123.123.123.123";
        int PORT = 21;
        String USERNAME = "ftpuser";
        String PASSWORD = "12345678";
    }
    public static final String KEY_IP = "key_ip";
    public static final String KEY_PORT = "key_port";
    public static final String KEY_USERNAME = "key_username";
    public static final String KEY_PASSWORD = "key_password";
}
