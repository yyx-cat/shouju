package shouju;

import shouju.view.LoginPanel;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("收据管理系统");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//设置窗口可关闭
            frame.setSize(400, 350);//设置窗口大小
            frame.setLocationRelativeTo(null);//设置窗口居中

            // 初始化登录面板
            new LoginPanel(frame);

            frame.setResizable(false);//设置窗口大小不可变
            frame.setVisible(true);//设置窗口可见
        });
    }
}
