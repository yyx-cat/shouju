package shouju.model;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UserDatabase {
    //存储用户数据（使用文件进行存储）
    private static final String USER_DATA_FILE = "users.txt";//创建一个txt文件
    private static Map<String, User> users = new HashMap<>();

    //加载时读取文件
    static {//static保证线程安全，只加载一次，且会提前加载
        loadUsersFromFile();
    }

    //加载用户信息
    private static void loadUsersFromFile(){
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    users.put(parts[0], new User(parts[0], parts[1], parts[2]));
                }
            }
        } catch (FileNotFoundException e) {
            // 文件不存在是正常情况，首次运行时会创建
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // 保存用户数据到文件
    private static void saveUsersToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_DATA_FILE))) {
            for (User user : users.values()) {
                writer.write(user.getUsername() + "," + user.getPassword() + "," + user.getEmail());
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //添加用户
    public static boolean addUser(User user) {
        //用户名不能重复
        if (users.containsKey(user.getUsername())) {
            return false;
        }
        users.put(user.getUsername(), user);
        saveUsersToFile(); // 每次修改后保存
        return true;
    }

    //获取用户名
    public static User getUser(String username) {
        return users.get(username);
    }

    //判断用户是否存在，且密码是否正确
    public static boolean validateUser(String username, String password) {
        User user = getUser(username);
        return user != null && user.getPassword().equals(password);
    }

    //重置密码
    public static boolean resetPassword(String username, String email, String newPassword) {
        User user = getUser(username);
        //用户存在且用户邮箱正确，修改密码为新的密码
        if (user != null && user.getEmail().equals(email)) {
            user.setPassword(newPassword);
            saveUsersToFile(); // 每次修改后保存
            return true;
        }
        return false;
    }
}
