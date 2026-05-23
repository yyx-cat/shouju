package shouju.controller;

import javax.swing.*;
import shouju.model.*;

public class AuthController {
    //用户登录
    public static boolean login(String username, String password){
        if (username.isEmpty() || password.isEmpty()) {
            showError("用户名和密码不能为空");
            return false;
        }
        if (!UserDatabase.validateUser(username, password)) {
            showError("用户名或密码不正确");
            return false;
        }
        return true;
    }

    //注册新用户
    public static boolean register(String username, String password, String confirmPassword, String email) {
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || email.isEmpty()) {
            showError("所有字段都必须填写");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            showError("两次输入的密码不一致");
            return false;
        }
        if (!email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            showError("邮箱格式不正确");
            return false;
        }
        if (!UserDatabase.addUser(new User(username, password, email))) {
            showError("用户名已存在");
            return false;
        }
        return true;
    }

    //重置密码
    public static String resetPassword(String username, String email) {
        if (username.isEmpty() || email.isEmpty()) {
            showError("请填写用户名和注册邮箱");
            return null;
        }

        String newPassword = generateRandomPassword();
        if (!UserDatabase.resetPassword(username, email, newPassword)) {
            showError("用户名或邮箱不匹配");
            return null;
        }

        return newPassword;
    }

    //随机生成一个新的密码
    private static String generateRandomPassword() {
        return Long.toHexString(Double.doubleToLongBits(Math.random()));
    }

    private static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "错误", JOptionPane.ERROR_MESSAGE);
    }
}
