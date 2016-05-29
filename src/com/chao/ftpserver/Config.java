package com.chao.ftpserver;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chao on 2016/5/24.
 */
public class Config {
    public int port;
    public int maxConnection;
    public List<String> blackIPList = new ArrayList<>();
    public List<User> userList = new ArrayList<>();

    public Config(String configPath) {
        initServerConfig(configPath);
    }

    private void initServerConfig(String configPath) {
        try {
            FileReader fileReader = new FileReader(configPath);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String tempLine;
            while ((tempLine = bufferedReader.readLine()) != null) {
                if (tempLine.startsWith("port")) {
                    port = Integer.parseInt(tempLine.substring(tempLine.indexOf(" ") + 1));
                } else if (tempLine.startsWith("maxConnection")) {
                    maxConnection = Integer.parseInt(tempLine.substring(tempLine.indexOf(" ") + 1));
                } else if (tempLine.startsWith("blackIP")) {
                    String[] tempStrings = tempLine.substring(tempLine.indexOf(" ") + 1).split("\\|");
                    for (String temp : tempStrings) {
                        blackIPList.add(temp);
                    }
                } else if (tempLine.startsWith("user")) {
                    String[] tempUser = tempLine.substring(tempLine.indexOf(" ") + 1).split("\\|");
                    User user = new User(tempUser[0], tempUser[1]);
                    userList.add(user);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class User {
        String username;
        String password;

        User(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public String toString() {
            return username + "," + password;
        }
    }
}
