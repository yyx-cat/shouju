package shouju.view;

import shouju.controller.AuthController;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class RegisterPanel extends JPanel{
    private JFrame parentFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField emailField;

    public RegisterPanel(JFrame frame) {
        this.parentFrame = frame;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 添加组件
        addComponents(gbc);

        frame.getContentPane().add(this);
    }

    private void addComponents(GridBagConstraints gbc){
        // 标题
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("注册新账号", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        add(titleLabel, gbc);

        // 用户名
        gbc.gridy++;
        gbc.gridwidth = 1;
        add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(15);
        add(usernameField, gbc);

        // 密码
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("密码:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        add(passwordField, gbc);

        // 确认密码
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("确认密码:"), gbc);
        gbc.gridx = 1;
        confirmPasswordField = new JPasswordField(15);
        add(confirmPasswordField, gbc);

        // 邮箱
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("邮箱:"), gbc);
        gbc.gridx = 1;
        emailField = new JTextField(15);
        add(emailField, gbc);

        // 注册按钮
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton registerButton = new JButton("注册");
        registerButton.addActionListener(this::handleRegister);
        add(registerButton, gbc);

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

    private void handleRegister(ActionEvent e){
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String email = emailField.getText().trim();

        if (AuthController.register(username, password, confirmPassword, email)) {
            JOptionPane.showMessageDialog(this, "注册成功，请登录",
                    "注册成功", JOptionPane.INFORMATION_MESSAGE);

            parentFrame.getContentPane().removeAll();
            new LoginPanel(parentFrame);
            parentFrame.revalidate();
        }
    }

}
