package shouju.view;

import shouju.controller.AuthController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ForgotPasswordPanel extends JPanel {
    private JFrame parentFrame;
    private JTextField usernameField;
    private JTextField emailField;

    public ForgotPasswordPanel(JFrame frame) {
        this.parentFrame = frame;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 添加组件
        addComponents(gbc);

        frame.getContentPane().add(this);
    }

    private void addComponents(GridBagConstraints gbc) {
        // 标题
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("找回密码", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        add(titleLabel, gbc);

        // 用户名
        gbc.gridy++;
        gbc.gridwidth = 1;
        add(new JLabel("用户名:"), gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(15);
        add(usernameField, gbc);

        // 邮箱
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("注册邮箱:"), gbc);

        gbc.gridx = 1;
        emailField = new JTextField(15);
        add(emailField, gbc);

        // 重置密码按钮
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton resetButton = new JButton("重置密码");
        resetButton.addActionListener(this::handleResetPassword);
        add(resetButton, gbc);

        // 返回登录链接
        gbc.gridy++;
        JButton backLink = new JButton("返回登录");
        backLink.setBorderPainted(false);
        backLink.setContentAreaFilled(false);
        backLink.setForeground(Color.BLUE);
        backLink.addActionListener(e -> {
            parentFrame.getContentPane().removeAll();
            new LoginPanel(parentFrame);
            parentFrame.revalidate();
        });
        add(backLink, gbc);
    }

    private void handleResetPassword(ActionEvent e) {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();

        String newPassword = AuthController.resetPassword(username, email);
        if (newPassword != null) {
            JOptionPane.showMessageDialog(this,
                    "密码已重置为: " + newPassword + "\n请登录后及时修改密码",
                    "密码重置成功", JOptionPane.INFORMATION_MESSAGE);

            parentFrame.getContentPane().removeAll();
            new LoginPanel(parentFrame);
            parentFrame.revalidate();
        }
    }
}
