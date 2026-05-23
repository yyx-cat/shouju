package shouju.view;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class ReceiptHistoryDialog extends JDialog {
    private static final String DATA_FILE = "receipt_data.dat";
    private JTextArea historyArea;

    public ReceiptHistoryDialog(JFrame parent, ImagePanel imagePanel) {
        super(parent, "收据历史记录", true);
        setSize(600, 400); // 比主窗口小的固定尺寸
        setLocationRelativeTo(parent);
        setResizable(false);

        initComponents();
        loadHistoryData();
    }

    private void initComponents() {
        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("宋体", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(historyArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(scrollPane, BorderLayout.CENTER);

        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(e -> dispose());
        add(closeButton, BorderLayout.SOUTH);
    }

    private void loadHistoryData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            historyArea.setText("暂无历史收据数据");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> allReceipts = (Map<String, Map<String, String>>) ois.readObject();

            StringBuilder sb = new StringBuilder();
            allReceipts.forEach((id, receipt) -> {
                sb.append("收据ID: ").append(id).append("\n");
                receipt.forEach((field, value) -> {
                    sb.append(field).append(": ").append(value).append("\n");
                });
                sb.append("----------------------------\n");
            });

            historyArea.setText(sb.toString());
        } catch (Exception ex) {
            historyArea.setText("加载历史数据失败: " + ex.getMessage());
        }
    }
}