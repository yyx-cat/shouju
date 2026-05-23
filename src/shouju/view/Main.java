package shouju.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class Main extends JFrame {
    private ImagePanel imagePanel;

    public Main() {
        setTitle("发票管理系统");
        setSize(1000, 800);
        imagePanel = new ImagePanel();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.NORTH);

        imagePanel = new ImagePanel();
        JScrollPane scrollPane = new JScrollPane(imagePanel);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] buttonLabels = {
                "新建收据", "保存收据", "修改收据", "历史查询", "生成收据", "退出系统"
        };

        for (String label : buttonLabels) {
            JButton button = new JButton(label);
            button.setPreferredSize(new Dimension(120, 30));
            button.addActionListener(this::handleButtonAction);
            panel.add(button);
        }

        return panel;
    }

    private void handleButtonAction(ActionEvent e) {
        String command = e.getActionCommand();
        switch (command) {
            case "修改收据":
                new ReceiptSelectionDialog(this, imagePanel, true).setVisible(true);
                break;
            case "新建收据":
                imagePanel.createNewReceipt();
                break;
            case "保存收据":
                imagePanel.saveReceiptData();
                break;
            case "历史查询":
                new ReceiptHistoryDialog(this, imagePanel).setVisible(true);
                break;
            case "生成收据":
                imagePanel.generateReceiptImage();
                break;
            case "退出系统":
                System.exit(0);
                break;
            default:
                JOptionPane.showMessageDialog(this, "功能开发中: " + command);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::new);
    }

    public ImagePanel getImagePanel() {
        return imagePanel; //
    }
}