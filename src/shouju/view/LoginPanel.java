package shouju.view;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import shouju.controller.*;
import shouju.model.*;

public class LoginPanel extends JPanel {
    private JFrame parentFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox rememberMeCheckBox;
    private JWindow historyPopup;
    private JList<String> historyList;
    private DefaultListModel<String> historyModel;
    private Map<String, UserLoginInfo> loginHistory = new LinkedHashMap<>();
    private static final String HISTORY_FILE = "login_history.dat";

    public LoginPanel(JFrame frame) {
        this.parentFrame = frame;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 初始化历史记录弹出窗口
        initHistoryPopup();

        // 初始化组件
        initializeComponents();

        // 添加组件
        addComponents(gbc);

        // 加载历史记录
        loadLoginHistory();
        updateHistoryList();

        frame.getContentPane().add(this);
    }

    private void initHistoryPopup() {
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    selectHistoryItem();
                }
            }
        });

        historyPopup = new JWindow();
        historyPopup.getContentPane().add(new JScrollPane(historyList));
        historyPopup.setSize(200, 150);
        historyPopup.setFocusableWindowState(false);
    }

    private void initializeComponents() {
        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        rememberMeCheckBox = new JCheckBox("记住密码");

        // 用户名框鼠标事件
        usernameField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                showHistoryPopup(usernameField);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // 延迟隐藏，避免无法选择
                Timer timer = new Timer(300, ev -> {
                    Point mousePos = MouseInfo.getPointerInfo().getLocation();
                    Point popupPos = historyPopup.getLocationOnScreen();
                    Rectangle popupBounds = new Rectangle(popupPos, historyPopup.getSize());

                    if (!popupBounds.contains(mousePos)) {
                        historyPopup.setVisible(false);
                    }
                });
                timer.setRepeats(false);
                timer.start();
            }
        });

        // 添加焦点监听器
        usernameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // 当用户名框失去焦点时隐藏下拉框
                historyPopup.setVisible(false);
            }
        });

        // 添加键盘监听器
        usernameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                // 当用户开始输入时隐藏下拉框
                historyPopup.setVisible(false);
            }
        });

        // 密码框获取焦点时也隐藏下拉框
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                historyPopup.setVisible(false);
            }
        });
    }

    private void showHistoryPopup(JComponent component) {
        if (historyModel.isEmpty()) return;

        Point loc = component.getLocationOnScreen();
        historyPopup.setLocation(loc.x, loc.y + component.getHeight());
        historyPopup.setVisible(true);
        historyPopup.toFront();
    }

    private void selectHistoryItem() {
        String selected = historyList.getSelectedValue();
        if (selected != null) {
            UserLoginInfo info = loginHistory.get(selected);
            if (info != null) {
                usernameField.setText(info.getUsername());
                passwordField.setText(info.getPassword());
                rememberMeCheckBox.setSelected(true);
            }
        }
        historyPopup.setVisible(false);
        passwordField.requestFocus(); // 自动跳转到密码框
    }

    private void updateHistoryList() {
        historyModel.clear();
        for (String username : loginHistory.keySet()) {
            historyModel.addElement(username);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadLoginHistory() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(HISTORY_FILE))) {
            loginHistory = (Map<String, UserLoginInfo>) ois.readObject();
        } catch (Exception e) {
            // 文件不存在或其他错误，忽略
        }
    }

    private void saveLoginHistory() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HISTORY_FILE))) {
            oos.writeObject(loginHistory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (AuthController.login(username, password)) {
            if (rememberMeCheckBox.isSelected()) {
                loginHistory.put(username, new UserLoginInfo(username, password));
                saveLoginHistory();
                updateHistoryList();
            } else {
                loginHistory.remove(username);
                saveLoginHistory();
                updateHistoryList();
            }

            // 登录成功后显示主界面
            SwingUtilities.invokeLater(() -> {
                parentFrame.dispose(); // 关闭登录窗口

                // 创建主界面
                Main mainFrame = new Main();
                mainFrame.setVisible(true);

            });
        } else {
            JOptionPane.showMessageDialog(this, "用户名或密码错误",
                    "登录失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 添加组件
    private void addComponents(GridBagConstraints gbc) {
        // 标题
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel titleLabel = new JLabel("收据管理系统登录", JLabel.CENTER);
        titleLabel.setFont(new Font("微软雅黑", Font.BOLD, 18));
        add(titleLabel, gbc);

        // 用户名
        gbc.gridy++;
        gbc.gridwidth = 1;
        add(new JLabel("用户名:"), gbc);
        gbc.gridx = 1;
        add(usernameField, gbc);

        // 密码
        gbc.gridx = 0;
        gbc.gridy++;
        add(new JLabel("密码:"), gbc);
        gbc.gridx = 1;
        add(passwordField, gbc);

        // 记住密码
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        add(rememberMeCheckBox, gbc);

        // 登录按钮
        gbc.gridy++;
        JButton loginButton = new JButton("登录");
        loginButton.addActionListener(this::handleLogin);
        add(loginButton, gbc);

        // 注册和忘记密码链接
        gbc.gridy++;
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        JButton registerLink = createLinkButton("注册账号", e -> {
            parentFrame.getContentPane().removeAll();
            new RegisterPanel(parentFrame);
            parentFrame.revalidate();
        });

        JButton forgotLink = createLinkButton("忘记密码", e -> {
            parentFrame.getContentPane().removeAll();
            new ForgotPasswordPanel(parentFrame);
            parentFrame.revalidate();
        });

        linkPanel.add(registerLink);
        linkPanel.add(forgotLink);
        add(linkPanel, gbc);
    }

    private JButton createLinkButton(String text, ActionListener listener) {
        JButton button = new JButton(text);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setForeground(Color.BLUE);
        button.addActionListener(listener);
        return button;
    }
}