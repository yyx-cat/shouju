package shouju.view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class ReceiptSelectionDialog extends JDialog {
    private final ImagePanel imagePanel;
    private final boolean isForModify;

    public ReceiptSelectionDialog(JFrame parent, ImagePanel imagePanel, boolean isForModify) {
        super(parent, isForModify ? "选择要修改的收据" : "历史收据查询", true);
        this.imagePanel = imagePanel;
        this.isForModify = isForModify;

        setSize(700, 500);
        setLocationRelativeTo(parent);
        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 表格显示收据列表
        JTable receiptTable = createReceiptTable();
        JScrollPane scrollPane = new JScrollPane(receiptTable);

        // 底部按钮
        JPanel buttonPanel = new JPanel();
        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    private JTable createReceiptTable() {
        // 从文件加载数据
        Map<String, Map<String, String>> receipts = loadReceipts();

        // 创建表格模型
        String[] columnNames = {"票据号码", "开票日期", "缴款单位", "金额"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // 填充数据
        receipts.values().forEach(receipt -> {
            Object[] rowData = {
                    receipt.get("票据号码"),
                    receipt.get("开票日期"),
                    receipt.get("缴款单位或个人"),
                    receipt.get("金额")
            };
            model.addRow(rowData);
        });

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        if (isForModify) {
            table.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        int row = table.getSelectedRow();
                        if (row >= 0) {
                            String receiptNumber = (String) table.getValueAt(row, 0);
                            confirmAndLoadReceipt(receiptNumber);
                        }
                    }
                }
            });
        }

        return table;
    }

    private void confirmAndLoadReceipt(String receiptNumber) {
        int choice = JOptionPane.showConfirmDialog(
                this,
                "确定要修改票据 " + receiptNumber + " 吗？",
                "确认修改",
                JOptionPane.YES_NO_OPTION
        );

        if (choice == JOptionPane.YES_OPTION) {
            Map<String, Map<String, String>> receipts = loadReceipts();
            Map<String, String> selectedReceipt = receipts.values().stream()
                    .filter(r -> r.get("票据号码").equals(receiptNumber))
                    .findFirst()
                    .orElse(null);

            if (selectedReceipt != null) {
                imagePanel.loadReceiptToForm(selectedReceipt);
                dispose();
            }
        }
    }

    private Map<String, Map<String, String>> loadReceipts() {
        File file = new File("receipt_data.dat");
        if (!file.exists()) return new HashMap<>();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> receipts = (Map<String, Map<String, String>>) ois.readObject();
            return receipts;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "加载收据失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return new HashMap<>();
        }
    }
}