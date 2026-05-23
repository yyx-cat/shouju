package shouju.view;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.event.*;
import java.io.*;
import java.util.*;
import java.net.URL;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.prefs.Preferences;
import java.awt.Rectangle;

// iTextPDF 相关导入
import com.itextpdf.text.Document;
import com.itextpdf.text.pdf.PdfWriter;

import shouju.model.ChineseNumberConverter;

public class ImagePanel extends JPanel {
    private BufferedImage originalImage;
    private Image scaledImage;
    private final Map<String, JTextField> inputFields = new LinkedHashMap<>();
    private final List<Map<String, JTextField>> projectFieldsList = new ArrayList<>();
    private JButton addProjectButton;
    private JButton removeProjectButton;
    private int currentProjectCount = 1;
    private double scaleRatio = 1.0;
    private int imageX, imageY;
    private static final String SAVE_FILE = "receipt_data.dat";
    private boolean isModified = false;
    private boolean fieldsInitialized = false;

    // 修改：添加PDF格式常量
    private static final String FORMAT_PNG = "PNG";
    private static final String FORMAT_JPG = "JPG";
    private static final String FORMAT_PDF = "PDF";
    private static final String FORMAT_PNG_KEY = "png";
    private static final String FORMAT_JPG_KEY = "jpg";
    private static final String FORMAT_PDF_KEY = "pdf";

    // 修改：为每种格式单独记忆上次保存目录
    private static final String LAST_SAVE_DIR_PNG_KEY = "lastSaveDirectoryPNG";
    private static final String LAST_SAVE_DIR_JPG_KEY = "lastSaveDirectoryJPG";
    private static final String LAST_SAVE_DIR_PDF_KEY = "lastSaveDirectoryPDF";

    private static final Map<String, Rectangle> BASE_FIELD_REGIONS;
    private static final Map<String, Rectangle> PROJECT_FIELD_REGIONS;
    private static final int PROJECT_FIELD_HEIGHT = 40;
    private static final int PROJECT_ROW_HEIGHT = 50;

    private static final String RECEIPT_NUMBER_PLACEHOLDER = "等待系统分配";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy年MM月dd日");

    static {
        Map<String, Rectangle> baseMap = new LinkedHashMap<>();
        baseMap.put("票据号码", new Rectangle(1050, 165, 200, 40));
        baseMap.put("开票日期", new Rectangle(1050, 215, 200, 40));
        baseMap.put("缴款单位或个人", new Rectangle(260, 270, 500, 40));
        baseMap.put("合计金额(大写)", new Rectangle(430, 600, 300, 40));
        baseMap.put("合计金额(小写)", new Rectangle(1100, 600, 150, 40));
        baseMap.put("备注", new Rectangle(110, 660, 800, 40));
        baseMap.put("开票人", new Rectangle(150, 800, 150, 40));
        BASE_FIELD_REGIONS = Collections.unmodifiableMap(baseMap);

        Map<String, Rectangle> projectMap = new LinkedHashMap<>();
        projectMap.put("项目名称", new Rectangle(70, 365, 300, PROJECT_FIELD_HEIGHT));
        projectMap.put("规格", new Rectangle(430, 365, 120, PROJECT_FIELD_HEIGHT));
        projectMap.put("单位", new Rectangle(620, 365, 120, PROJECT_FIELD_HEIGHT));
        projectMap.put("数量", new Rectangle(790, 365, 100, PROJECT_FIELD_HEIGHT));
        projectMap.put("单价", new Rectangle(920, 365, 120, PROJECT_FIELD_HEIGHT));
        projectMap.put("金额", new Rectangle(1100, 365, 120, PROJECT_FIELD_HEIGHT));
        PROJECT_FIELD_REGIONS = Collections.unmodifiableMap(projectMap);
    }

    public ImagePanel() {
        setLayout(null);
        initializeButtons();
        loadImage();
    }

    private void initializeButtons() {
        addProjectButton = new JButton("添加项目");
        addProjectButton.addActionListener(e -> addProject());

        removeProjectButton = new JButton("移除项目");
        removeProjectButton.addActionListener(e -> removeProject());

        add(addProjectButton);
        add(removeProjectButton);
        updateButtonsState();
    }

    public void generateReceiptImage() {
        calculateAmounts();

        String receiptNumber = inputFields.get("票据号码").getText().trim();
        if (receiptNumber.isEmpty() || receiptNumber.equals(RECEIPT_NUMBER_PLACEHOLDER)) {
            receiptNumber = getNextReceiptNumber();
            inputFields.get("票据号码").setText(receiptNumber);
        }

        boolean hasValidProject = false;
        for (int i = 1; i <= currentProjectCount; i++) {
            if (!inputFields.get("项目名称" + i).getText().trim().isEmpty()) {
                hasValidProject = true;
                break;
            }
        }

        if (!hasValidProject) {
            JOptionPane.showMessageDialog(this, "至少需要填写一个项目！", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (isModified) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "当前收据有未保存的修改，是否先保存？",
                    "未保存的修改",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (choice == JOptionPane.YES_OPTION && !saveReceiptData()) {
                JOptionPane.showMessageDialog(this, "生成收据失败：保存不成功", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            // 步骤1：让用户选择保存格式
            String selectedFormat = showFormatSelectionDialog();
            if (selectedFormat == null) {
                return; // 用户取消
            }

            // 步骤2：创建文件选择器
            JFileChooser fileChooser = createFileChooser(receiptNumber, selectedFormat);

            // 步骤3：显示保存对话框
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File outputFile = ensureCorrectExtension(fileChooser.getSelectedFile(), selectedFormat);

                boolean saved;
                if (FORMAT_PDF.equals(selectedFormat)) {
                    // PDF：先生成高清图片，再转为PDF
                    BufferedImage highQualityImage = createHighQualityReceiptImage();
                    saved = saveImageAsPDF(highQualityImage, outputFile);
                } else if (FORMAT_PNG.equals(selectedFormat)) {
                    // PNG：使用普通质量的图片
                    BufferedImage normalImage = createReceiptImage();
                    saved = saveAsImage(normalImage, outputFile, "png");
                } else {
                    // JPG：使用普通质量的图片
                    BufferedImage normalImage = createReceiptImage();
                    saved = saveAsImage(normalImage, outputFile, "jpg");
                }

                if (saved) {
                    // 保存本次选择的文件夹路径
                    saveLastSaveDirectory(outputFile.getParentFile(), selectedFormat);

                    // 显示成功消息
                    showSuccessMessage(outputFile, selectedFormat);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "生成失败: " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    /**
     * 创建高清收据图片（专门用于PDF）
     */
    public BufferedImage createHighQualityReceiptImage() {
        // 使用更高的缩放因子，比如4倍，确保清晰
        int scaleFactor = 4;

        // 获取原始图片尺寸
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 计算高清尺寸
        int highResWidth = originalWidth * scaleFactor;
        int highResHeight = originalHeight * scaleFactor;

        // 创建高清图像
        BufferedImage highResImage = new BufferedImage(
                highResWidth, highResHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = highResImage.createGraphics();

        // 设置最高质量的渲染
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);

        // 绘制白色背景（防止透明背景变黑）
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, highResWidth, highResHeight);

        // 绘制背景图片（高质量缩放）
        Image scaledImage = originalImage.getScaledInstance(
                highResWidth, highResHeight, Image.SCALE_SMOOTH);
        g2d.drawImage(scaledImage, 0, 0, null);

        // 设置高清字体
        java.awt.Font highResFont = new java.awt.Font("宋体", java.awt.Font.PLAIN, 22 * scaleFactor);
        g2d.setFont(highResFont);
        g2d.setColor(Color.BLACK);

        // 绘制所有字段（坐标也需要按比例放大）
        drawAllFields(g2d, scaleFactor);

        g2d.dispose();
        return highResImage;
    }

    /**
     * 绘制所有字段到高清图片
     */
    private void drawAllFields(Graphics2D g2d, int scaleFactor) {
        // 绘制基础字段
        for (Map.Entry<String, Rectangle> entry : BASE_FIELD_REGIONS.entrySet()) {
            String fieldName = entry.getKey();
            Rectangle rect = entry.getValue();
            String text = inputFields.get(fieldName).getText();

            if (text != null && !text.trim().isEmpty()) {
                int x = (int)(rect.x * scaleFactor);
                int y = (int)(rect.y * scaleFactor) + (int)(30 * scaleFactor);
                g2d.drawString(text, x, y);
            }
        }

        // 绘制项目字段
        for (int i = 0; i < currentProjectCount; i++) {
            Map<String, JTextField> projectFields = projectFieldsList.get(i);
            for (Map.Entry<String, Rectangle> entry : PROJECT_FIELD_REGIONS.entrySet()) {
                String fieldName = entry.getKey();
                Rectangle rect = entry.getValue();
                String text = projectFields.get(fieldName).getText();

                if (text != null && !text.trim().isEmpty()) {
                    int x = (int)(rect.x * scaleFactor);
                    int y = (int)(rect.y * scaleFactor) + (int)(30 * scaleFactor) +
                            i * (int)(PROJECT_ROW_HEIGHT * scaleFactor);
                    g2d.drawString(text, x, y);
                }
            }
        }
    }
    /**
     * 显示格式选择对话框（支持PNG、JPG、PDF）
     * @return 用户选择的格式，如果取消则返回null
     */
    private String showFormatSelectionDialog() {
        Object[] options = {FORMAT_PNG, FORMAT_JPG, FORMAT_PDF, "取消"};
        String message = "请选择要保存的格式：\n" +
                "• PNG - 图片格式，无损质量\n" +
                "• JPG - 图片格式，文件较小\n" +
                "• PDF - 文档格式，适合打印和存档";

        int choice = JOptionPane.showOptionDialog(
                this,
                message,
                "选择保存格式",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]  // 默认选中PDF
        );

        switch (choice) {
            case 0:
                return FORMAT_PNG;
            case 1:
                return FORMAT_JPG;
            case 2:
                return FORMAT_PDF;
            default:
                return null; // 取消
        }
    }

    /**
     * 创建文件选择器
     * @param receiptNumber 收据号码
     * @param format 保存格式
     * @return 配置好的文件选择器
     */
    private JFileChooser createFileChooser(String receiptNumber, String format) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("保存收据 - " + format + "格式");

        // 设置默认文件名
        String defaultFileName = "收据_" + receiptNumber;
        if (FORMAT_PDF.equals(format)) {
            defaultFileName += ".pdf";
        } else if (FORMAT_PNG.equals(format)) {
            defaultFileName += ".png";
        } else {
            defaultFileName += ".jpg";
        }
        fileChooser.setSelectedFile(new File(defaultFileName));

        // 设置文件过滤器
        if (FORMAT_PDF.equals(format)) {
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "PDF 文档 (*.pdf)", "pdf"));
        } else if (FORMAT_PNG.equals(format)) {
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "PNG 图片 (*.png)", "png"));
        } else {
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "JPEG 图片 (*.jpg, *.jpeg)", "jpg", "jpeg"));
        }

        // 设置上次保存的文件夹
        String lastSaveDir = getLastSaveDirectory(format);
        if (lastSaveDir != null && !lastSaveDir.isEmpty()) {
            File lastDir = new File(lastSaveDir);
            if (lastDir.exists() && lastDir.isDirectory()) {
                fileChooser.setCurrentDirectory(lastDir);
            }
        }

        return fileChooser;
    }

    /**
     * 确保文件有正确的扩展名
     * @param file 原始文件
     * @param format 保存格式
     * @return 带有正确扩展名的文件
     */
    private File ensureCorrectExtension(File file, String format) {
        String fileName = file.getName().toLowerCase();

        if (FORMAT_PDF.equals(format)) {
            if (!fileName.endsWith(".pdf")) {
                return new File(file.getParentFile(), file.getName() + ".pdf");
            }
        } else if (FORMAT_PNG.equals(format)) {
            if (!fileName.endsWith(".png")) {
                return new File(file.getParentFile(), file.getName() + ".png");
            }
        } else {
            // JPG格式
            if (!fileName.endsWith(".jpg") && !fileName.endsWith(".jpeg")) {
                return new File(file.getParentFile(), file.getName() + ".jpg");
            }
        }

        return file;
    }

    /**
     * 将图片直接转为PDF（图片大小，无空白边距）
     */
    private boolean saveImageAsPDF(BufferedImage image, File file) {
        try {
            // 1. 创建PDF文档，使用图片的实际尺寸
            float imageWidth = image.getWidth();
            float imageHeight = image.getHeight();

            // 转换像素为点（points），通常1像素=0.75点（根据DPI）
            // 使用更精确的转换：72DPI下，1点=1/72英寸，通常假设96DPI的屏幕
            float pointsPerPixel = 72.0f / 96.0f; // 96DPI是标准屏幕分辨率
            float pdfWidth = imageWidth * pointsPerPixel;
            float pdfHeight = imageHeight * pointsPerPixel;

            // 创建自定义大小的文档
            Document document = new Document(new com.itextpdf.text.Rectangle(pdfWidth, pdfHeight), 0, 0, 0, 0);

            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // 2. 将BufferedImage转换为iText的Image
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            com.itextpdf.text.Image pdfImage = com.itextpdf.text.Image.getInstance(baos.toByteArray());

            // 3. 设置图片填充整个PDF页面（无边距）
            pdfImage.setAbsolutePosition(0, 0);
            pdfImage.scaleAbsolute(pdfWidth, pdfHeight);

            // 4. 添加到PDF
            document.add(pdfImage);
            document.close();

            return true;

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "生成PDF失败: " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * 保存为图片格式
     */
    private boolean saveAsImage(BufferedImage image, File file, String format) {
        try {
            if ("jpg".equals(format)) {
                // JPG格式需要特殊处理
                BufferedImage rgbImage = new BufferedImage(
                        image.getWidth(),
                        image.getHeight(),
                        BufferedImage.TYPE_INT_RGB
                );
                Graphics2D g2d = rgbImage.createGraphics();
                g2d.drawImage(image, 0, 0, null);
                g2d.dispose();
                ImageIO.write(rgbImage, "jpg", file);
            } else {
                ImageIO.write(image, format, file);
            }
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "保存图片失败: " + ex.getMessage(),
                    "保存错误",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * 获取上次保存的文件夹路径（按格式）
     * @param format 保存格式
     * @return 上次保存的文件夹路径
     */
    private String getLastSaveDirectory(String format) {
        try {
            Preferences prefs = Preferences.userNodeForPackage(ImagePanel.class);
            String key;
            if (FORMAT_PDF.equals(format)) {
                key = LAST_SAVE_DIR_PDF_KEY;
            } else if (FORMAT_PNG.equals(format)) {
                key = LAST_SAVE_DIR_PNG_KEY;
            } else {
                key = LAST_SAVE_DIR_JPG_KEY;
            }
            return prefs.get(key, null);
        } catch (Exception e) {
            return getLastSaveDirectoryFromProperties(format);
        }
    }

    /**
     * 保存上次保存的文件夹路径（按格式）
     * @param directory 文件夹
     * @param format 保存格式
     */
    private void saveLastSaveDirectory(File directory, String format) {
        if (directory != null) {
            try {
                Preferences prefs = Preferences.userNodeForPackage(ImagePanel.class);
                String key;
                if (FORMAT_PDF.equals(format)) {
                    key = LAST_SAVE_DIR_PDF_KEY;
                } else if (FORMAT_PNG.equals(format)) {
                    key = LAST_SAVE_DIR_PNG_KEY;
                } else {
                    key = LAST_SAVE_DIR_JPG_KEY;
                }
                prefs.put(key, directory.getAbsolutePath());
                prefs.flush();
            } catch (Exception e) {
                saveLastSaveDirectoryToProperties(directory.getAbsolutePath(), format);
            }
        }
    }

    /**
     * 从属性文件获取上次保存的文件夹路径（按格式）
     */
    private String getLastSaveDirectoryFromProperties(String format) {
        Properties props = new Properties();
        File propFile = new File("receipt_system.properties");

        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                props.load(fis);
                String key = FORMAT_PNG.equals(format) ? LAST_SAVE_DIR_PNG_KEY : LAST_SAVE_DIR_JPG_KEY;
                return props.getProperty(key);
            } catch (Exception e) {
                // 忽略错误
            }
        }
        return null;
    }

    /**
     * 保存上次保存的文件夹路径到属性文件（按格式）
     */
    private void saveLastSaveDirectoryToProperties(String directoryPath, String format) {
        Properties props = new Properties();
        File propFile = new File("receipt_system.properties");

        // 先读取现有属性
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                props.load(fis);
            } catch (Exception e) {
                // 忽略错误
            }
        }

        // 设置新的文件夹路径
        String key = FORMAT_PNG.equals(format) ? LAST_SAVE_DIR_PNG_KEY : LAST_SAVE_DIR_JPG_KEY;
        props.setProperty(key, directoryPath);

        // 保存到文件
        try (FileOutputStream fos = new FileOutputStream(propFile)) {
            props.store(fos, "Receipt System Preferences");
        } catch (Exception e) {
            // 忽略错误
        }
    }

    /**
     * 显示保存成功消息
     * @param file 保存的文件
     * @param format 保存格式
     */
    private void showSuccessMessage(File file, String format) {
        String formatInfo;
        if (FORMAT_PDF.equals(format)) {
            formatInfo = "PDF格式（矢量文档，适合打印和存档）";
        } else if (FORMAT_PNG.equals(format)) {
            formatInfo = "PNG格式（无损压缩，支持透明背景）";
        } else {
            formatInfo = "JPG格式（有损压缩，文件较小）";
        }

        JOptionPane.showMessageDialog(this,
                "收据已保存成功！\n" +
                        "格式: " + formatInfo + "\n" +
                        "保存路径: " + file.getAbsolutePath(),
                "保存成功",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private BufferedImage createReceiptImage() {
        BufferedImage image = new BufferedImage(
                getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        if (scaledImage != null) {
            g2d.drawImage(scaledImage, imageX, imageY, this);
        }

        g2d.setColor(Color.BLACK);
        g2d.setFont(new  java.awt.Font("宋体", java.awt.Font.PLAIN, (int)(22 * scaleRatio)));  // 注意括号

        for (Map.Entry<String, Rectangle> entry : BASE_FIELD_REGIONS.entrySet()) {
            String fieldName = entry.getKey();
            Rectangle rect = entry.getValue();
            String text = inputFields.get(fieldName).getText();

            if (!text.isEmpty()) {
                int x = imageX + (int)(rect.x * scaleRatio);
                int y = imageY + (int)(rect.y * scaleRatio) + (int)(30 * scaleRatio);
                g2d.drawString(text, x, y);
            }
        }

        for (int i = 0; i < currentProjectCount; i++) {
            Map<String, JTextField> projectFields = projectFieldsList.get(i);
            for (Map.Entry<String, Rectangle> entry : PROJECT_FIELD_REGIONS.entrySet()) {
                String fieldName = entry.getKey();
                Rectangle rect = entry.getValue();
                String text = projectFields.get(fieldName).getText();

                if (!text.isEmpty()) {
                    int x = imageX + (int)(rect.x * scaleRatio);
                    int y = imageY + (int)(rect.y * scaleRatio) + (int)(30 * scaleRatio) + i * (int)(PROJECT_ROW_HEIGHT * scaleRatio);
                    g2d.drawString(text, x, y);
                }
            }
        }

        g2d.dispose();
        return image;
    }

    private void loadImage() {
        try {
            URL imageUrl = getClass().getResource("/resources/image.png");
            if (imageUrl != null) {
                originalImage = ImageIO.read(imageUrl);
                System.out.println("图片加载成功: " + imageUrl);
            } else {
                System.err.println("找不到图片资源: /resources/image.png");
                createFallbackImage();
            }
        } catch (IOException ex) {
            System.err.println("无法加载图片: " + ex.getMessage());
            createFallbackImage();
        }
    }

    private void createFallbackImage() {
        originalImage = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = originalImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, 600, 400);
        g2d.setColor(Color.RED);
        g2d.drawString("图片加载失败，请检查resources/image.png", 150, 200);
        g2d.dispose();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (originalImage == null) return;

        double widthRatio = (double) getWidth() / originalImage.getWidth();
        double heightRatio = (double) getHeight() / originalImage.getHeight();
        scaleRatio = Math.min(widthRatio, heightRatio);

        int scaledWidth = (int) (originalImage.getWidth() * scaleRatio);
        int scaledHeight = (int) (originalImage.getHeight() * scaleRatio);

        imageX = (getWidth() - scaledWidth) / 2;
        imageY = (getHeight() - scaledHeight) / 2;

        scaledImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
        g.drawImage(scaledImage, imageX, imageY, this);

        if (!fieldsInitialized) {
            createInputFields();
            fieldsInitialized = true;
        }

        updateButtonPositions();
    }

    /**
     * 添加一个新项目行（空的）
     */
    private void addNewProjectRow() {
        if (currentProjectCount >= 4) {
            JOptionPane.showMessageDialog(this,
                    "最多只能添加4个项目",
                    "已达上限",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int newRowIndex = currentProjectCount + 1; // 新行的索引
        Map<String, JTextField> projectFields = new LinkedHashMap<>();
        int yOffset = (newRowIndex - 1) * PROJECT_ROW_HEIGHT;

        for (Map.Entry<String, Rectangle> entry : PROJECT_FIELD_REGIONS.entrySet()) {
            String fieldName = entry.getKey();
            Rectangle rect = entry.getValue();
            Rectangle adjustedRect = new Rectangle(
                    rect.x,
                    rect.y + yOffset,
                    rect.width,
                    rect.height
            );
            JTextField field = createTextField(fieldName + newRowIndex, adjustedRect);
            projectFields.put(fieldName, field);
        }

        projectFieldsList.add(projectFields);
        currentProjectCount = projectFieldsList.size(); // 更新计数
        updateButtonsState();
        revalidate();
        repaint();
    }
    /**
     * 删除最后一个项目行（有内容时询问确认）
     */
    private void removeLastProjectRow() {
        if (currentProjectCount <= 1) {
            JOptionPane.showMessageDialog(this,
                    "至少需要保留一个项目",
                    "无法删除",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 安全检查：确保索引有效
        if (projectFieldsList.isEmpty() || projectFieldsList.size() <= currentProjectCount - 1) {
            // 如果列表为空或索引无效，重置计数并返回
            currentProjectCount = projectFieldsList.size();
            return;
        }

        // 检查最后一个项目是否有内容
        Map<String, JTextField> lastProject = projectFieldsList.get(currentProjectCount - 1);
        boolean hasContent = false;
        for (JTextField field : lastProject.values()) {
            if (field != null && !field.getText().trim().isEmpty()) {
                hasContent = true;
                break;
            }
        }

        if (hasContent) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "该项目中有内容，确定要删除吗？",
                    "确认删除",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (confirm != JOptionPane.YES_OPTION) {
                return; // 用户取消删除
            }
        }

        removeLastProjectRowWithoutConfirm();
    }
    /**
     * 不询问直接删除最后一个项目行（用于内部清理）
     */
    private void removeLastProjectRowWithoutConfirm() {
        if (currentProjectCount <= 1) return;

        // 安全检查：确保有元素可以移除
        if (projectFieldsList.isEmpty()) {
            currentProjectCount = 0;
            updateButtonsState();
            return;
        }

        int indexToRemove = currentProjectCount - 1;

        // 再次检查索引是否有效
        if (indexToRemove < 0 || indexToRemove >= projectFieldsList.size()) {
            // 如果索引无效，调整计数并返回
            currentProjectCount = projectFieldsList.size();
            updateButtonsState();
            return;
        }

        Map<String, JTextField> lastProject = projectFieldsList.remove(indexToRemove);

        // 从面板移除字段组件
        for (JTextField field : lastProject.values()) {
            if (field != null) {
                remove(field);
            }
        }

        // 从inputFields映射中移除
        for (String fieldName : PROJECT_FIELD_REGIONS.keySet()) {
            inputFields.remove(fieldName + (indexToRemove + 1));
        }

        currentProjectCount = projectFieldsList.size(); // 更新计数
        updateButtonsState();
        revalidate();
        repaint();
    }

    private void updateButtonPositions() {
        int buttonY = imageY + 50;
        addProjectButton.setBounds(imageX + 70, buttonY, 100, 30);
        removeProjectButton.setBounds(imageX + 180, buttonY, 100, 30);
    }

    private void createInputFields() {
        // 创建基础字段
        BASE_FIELD_REGIONS.forEach((fieldName, rect) -> {
            createTextField(fieldName, rect);
        });

        // 重置项目列表
        projectFieldsList.clear();
        currentProjectCount = 0;

        // 添加一个初始空项目
        addNewProjectRow();
    }

    private JTextField createTextField(String fieldName, Rectangle rect) {

        int x = imageX + (int)(rect.x * scaleRatio);
        int y = imageY + (int)(rect.y * scaleRatio);
        int width = (int)(rect.width * scaleRatio);
        int height = (int)(rect.height * scaleRatio);

        JTextField textField = new JTextField();
        textField.setBounds(x, y, width, height);
        textField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        textField.setOpaque(false);
        textField.setFont(new java.awt.Font("宋体", java.awt.Font.PLAIN, (int)(20 * scaleRatio)));

        inputFields.put(fieldName, textField);
        add(textField);

        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { markModified(); }
            public void insertUpdate(DocumentEvent e) { markModified(); }
            public void removeUpdate(DocumentEvent e) { markModified(); }

            private void markModified() {
                isModified = true;
            }
        });

        return textField;
    }

    private void addProject() {
        addNewProjectRow();
        isModified = true;
    }

    private void removeProject() {
        removeLastProjectRow();
    }

    private void updateButtonsState() {
        if (addProjectButton != null && removeProjectButton != null) {
            addProjectButton.setEnabled(currentProjectCount < 4);
            removeProjectButton.setEnabled(currentProjectCount > 1);
        }
    }

    private String getNextReceiptNumber() {
        // 获取开票日期
        JTextField dateField = inputFields.get("开票日期");
        String dateText = dateField.getText().trim();

        // 如果没有日期字段或者日期为空，使用当前日期
        String datePart;
        if (dateField == null || dateText.isEmpty()) {
            // 使用当前日期
            datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        } else {
            try {
                // 解析日期格式（yyyy年MM月dd日 -> yyyyMMdd）
                String cleanedDate = dateText.replace("年", "")
                        .replace("月", "")
                        .replace("日", "");
                datePart = cleanedDate;
            } catch (Exception e) {
                // 如果日期格式不对，使用当前日期
                datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
            }
        }

        // 确保datePart是8位数字
        if (datePart.length() != 8) {
            datePart = new SimpleDateFormat("yyyyMMdd").format(new Date());
        }

        int maxSequence = 0;
        File dataFile = new File(SAVE_FILE);

        if (dataFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile))) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, String>> allReceipts = (Map<String, Map<String, String>>) ois.readObject();

                // 查找相同日期下的最大序号
                for (String receiptNumber : allReceipts.keySet()) {
                    if (receiptNumber.startsWith(datePart)) {
                        try {
                            String sequenceStr = receiptNumber.substring(8); // 取后3位
                            int currentSequence = Integer.parseInt(sequenceStr);
                            if (currentSequence > maxSequence) {
                                maxSequence = currentSequence;
                            }
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 生成新的序号（001开始）
        int newSequence = maxSequence + 1;
        return String.format("%s%03d", datePart, newSequence);
    }

    public boolean createNewReceipt() {
        if (isModified) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "当前收据有未保存的修改，是否先保存？",
                    "未保存的修改",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            switch (choice) {
                case JOptionPane.YES_OPTION:
                    if (!saveReceiptData()) {
                        return false;
                    }
                    break;
                case JOptionPane.NO_OPTION:
                    break;
                case JOptionPane.CANCEL_OPTION:
                default:
                    return false;
            }
        }

        clearAllFields();

        // 自动设置当前日期作为默认值
        JTextField dateField = inputFields.get("开票日期");
        String currentDate = DATE_FORMAT.format(new Date());
        dateField.setText(currentDate);
        dateField.setForeground(Color.BLACK);
        dateField.setToolTipText("默认当前日期，可手动修改");

        // 自动生成票据号码
        String receiptNumber = getNextReceiptNumber();
        JTextField receiptNumberField = inputFields.get("票据号码");
        receiptNumberField.setText(receiptNumber);
        receiptNumberField.setForeground(Color.BLACK);

        JOptionPane.showMessageDialog(this, "已创建新收据", "新建成功", JOptionPane.INFORMATION_MESSAGE);
        return true;
    }

    private void clearAllFields() {
        // 清空所有字段的文本
        for (JTextField field : inputFields.values()) {
            if (field != null) {
                field.setText("");
            }
        }

        isModified = false;
    }

    public boolean isReceiptExists(String receiptNumber) {
        File file = new File(SAVE_FILE);
        if (!file.exists()) return false;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, String>> allReceipts = (Map<String, Map<String, String>>) ois.readObject();
            return allReceipts.containsKey(receiptNumber);
        } catch (Exception ex) {
            return false;
        }
    }

    public void loadReceiptToForm(Map<String, String> receiptData) {
        clearAllFields();

        // 加载基础字段
        BASE_FIELD_REGIONS.keySet().forEach(fieldName -> {
            if (receiptData.containsKey(fieldName)) {
                inputFields.get(fieldName).setText(receiptData.get(fieldName));
            }
        });

        // 统计项目数量
        int projectCount = 0;
        for (int i = 1; i <= 4; i++) {
            if (receiptData.containsKey("项目名称" + i)) {
                projectCount++;
            } else {
                break;
            }
        }

        if (projectCount > 0) {
            // 清空现有项目字段
            for (int i = 1; i <= currentProjectCount; i++) {
                for (String fieldName : PROJECT_FIELD_REGIONS.keySet()) {
                    String key = fieldName + i;
                    if (inputFields.containsKey(key)) {
                        inputFields.get(key).setText("");
                    }
                }
            }

            // 添加所需数量的项目
            while (currentProjectCount < projectCount) {
                addNewProjectRow();
            }

            // 加载项目数据
            for (int i = 0; i < projectCount; i++) {
                int projectIndex = i + 1;
                PROJECT_FIELD_REGIONS.keySet().forEach(fieldName -> {
                    String dataKey = fieldName + projectIndex;
                    String fieldKey = fieldName + projectIndex;
                    if (receiptData.containsKey(dataKey) && inputFields.containsKey(fieldKey)) {
                        inputFields.get(fieldKey).setText(receiptData.get(dataKey));
                    }
                });
            }
        }

        isModified = false;
    }

    public boolean saveReceiptData() {
        return saveReceiptData(false);
    }

    private String formatCurrency(double amount) {
        return String.format("¥%.2f", amount);
    }

    private String toChineseUpper(double amount) {
        return ChineseNumberConverter.toChineseUpper(amount);
    }

    private boolean isValidReceiptNumber(String number) {
        // 修改：只需要验证是否为11位数字
        if (number.length() != 11) {
            return false;
        }

        try {
            // 验证是否是11位数字
            Long.parseLong(number); // 使用Long.parseLong确保是数字
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isValidDateFormat(String date) {
        try {
            // 尝试解析日期，如果解析成功则格式正确
            DATE_FORMAT.parse(date);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean saveReceiptData(boolean isReplaceMode) {
        // 在验证前先确保日期有值
        JTextField dateField = inputFields.get("开票日期");
        String dateText = dateField.getText().trim();

        if (dateText.isEmpty()) {
            // 如果保存时日期为空，自动设置为当前日期
            String currentDate = DATE_FORMAT.format(new Date());
            dateField.setText(currentDate);
            JOptionPane.showMessageDialog(this,
                    "日期为空，已自动设置为当前日期",
                    "日期已设置",
                    JOptionPane.INFORMATION_MESSAGE);
        } else if (!isValidDateFormat(dateText)) {
            // 如果日期格式不正确，自动修正
            dateField.setText(DATE_FORMAT.format(new Date()));
            JOptionPane.showMessageDialog(this,
                    "日期格式不正确，已自动设置为当前日期",
                    "日期已修正",
                    JOptionPane.WARNING_MESSAGE);
        }

        if (!validateRequiredFields()) {
            return false;
        }

        calculateAmounts();

        JTextField receiptNumberField = inputFields.get("票据号码");
        String receiptNumber = receiptNumberField.getText().trim();

        // 验证票据号码格式
        // 验证票据号码格式
        if (!isValidReceiptNumber(receiptNumber)) {
            // 如果格式不正确，提示用户并自动生成
            JOptionPane.showMessageDialog(this,
                    "票据号码格式不正确！\n已自动生成新的票据号码。\n" +
                            "正确格式：必须是11位数字",
                    "票据号码已修正",
                    JOptionPane.WARNING_MESSAGE);

            receiptNumber = getNextReceiptNumber();
            receiptNumberField.setText(receiptNumber);
        }

        if (!isReplaceMode && isReceiptExists(receiptNumber)) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "票据号码 " + receiptNumber + " 已存在！\n是否替换现有票据？",
                    "重复票据",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                return saveReceiptData(true);
            } else {
                JOptionPane.showMessageDialog(this,
                        "请修改票据号码后重试",
                        "保存取消",
                        JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
        }

        Map<String, String> currentReceipt = new LinkedHashMap<>();

        BASE_FIELD_REGIONS.keySet().forEach(fieldName -> {
            currentReceipt.put(fieldName, inputFields.get(fieldName).getText());
        });

        int savedProjectCount = 0;
        for (int i = 1; i <= currentProjectCount; i++) {
            boolean hasContent = false;
            for (String fieldName : PROJECT_FIELD_REGIONS.keySet()) {
                if (!inputFields.get(fieldName + i).getText().trim().isEmpty()) {
                    hasContent = true;
                    break;
                }
            }

            if (hasContent) {
                savedProjectCount++;
                for (String fieldName : PROJECT_FIELD_REGIONS.keySet()) {
                    currentReceipt.put(fieldName + savedProjectCount,
                            inputFields.get(fieldName + i).getText());
                }
            }
        }

        try {
            File file = new File(SAVE_FILE);
            Map<String, Map<String, String>> allReceipts = new LinkedHashMap<>();

            if (file.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    Object data = ois.readObject();
                    if (data instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Map<String, String>> oldData =
                                (Map<String, Map<String, String>>) data;
                        allReceipts.putAll(oldData);
                    }
                }
            }

            if (isReplaceMode) {
                allReceipts.remove(receiptNumber);
            }

            allReceipts.put(receiptNumber, currentReceipt);

            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                oos.writeObject(allReceipts);
                isModified = false;
                JOptionPane.showMessageDialog(this, "保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "保存失败: " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
            return false;
        }
    }

    private void calculateAmounts() {
        double totalAmount = 0.0;

        for (int i = 1; i <= currentProjectCount; i++) {
            try {
                double quantity = Double.parseDouble(inputFields.get("数量" + i).getText().trim());
                double unitPrice = Double.parseDouble(inputFields.get("单价" + i).getText().trim());
                double amount = quantity * unitPrice;
                inputFields.get("金额" + i).setText(String.format("%.2f", amount));
                totalAmount += amount;
            } catch (NumberFormatException e) {
                // skip
            }
        }

        if (totalAmount > 0) {
            inputFields.get("合计金额(小写)").setText(formatCurrency(totalAmount));
            inputFields.get("合计金额(大写)").setText(toChineseUpper(totalAmount));
        }
    }

    private boolean validateRequiredFields() {
        // 检查并处理日期字段
        JTextField dateField = inputFields.get("开票日期");
        String dateText = dateField.getText().trim();

        if (dateText.isEmpty()) {
            // 如果日期为空，自动设置为当前日期
            String currentDate = DATE_FORMAT.format(new Date());
            dateField.setText(currentDate);
            dateField.setToolTipText("已自动设置为当前日期");
        } else if (!isValidDateFormat(dateText)) {
            // 如果日期格式不正确，提示用户并自动修正
            JOptionPane.showMessageDialog(this,
                    "日期格式不正确！已自动设置为当前日期\n请使用 yyyy年MM月dd日 格式",
                    "日期格式已修正",
                    JOptionPane.WARNING_MESSAGE);
            dateField.setText(DATE_FORMAT.format(new Date()));
            dateField.setToolTipText("已自动修正为当前日期");
        }


        // 验证基础字段（排除备注、金额和缴款单位或个人）
        for (Map.Entry<String, Rectangle> entry : BASE_FIELD_REGIONS.entrySet()) {
            String fieldName = entry.getKey();
            // 跳过备注、金额字段和缴款单位或个人字段
            if ("备注".equals(fieldName) ||
                    fieldName.contains("金额") ||
                    "缴款单位或个人".equals(fieldName)) {
                continue;
            }

            String value = inputFields.get(fieldName).getText().trim();
            if (value.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        fieldName + "不能为空！",
                        "错误",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        boolean hasValidProject = false;
        for (int i = 1; i <= currentProjectCount; i++) {
            boolean projectHasContent = false;

            // 检查项目是否有内容（排除规格字段，因为它可以为空）
            for (String fieldName : PROJECT_FIELD_REGIONS.keySet()) {
                if ("金额".equals(fieldName) || "规格".equals(fieldName)) continue;
                if (!inputFields.get(fieldName + i).getText().trim().isEmpty()) {
                    projectHasContent = true;
                    break;
                }
            }

            if (projectHasContent) {
                hasValidProject = true;
                // 验证项目字段（规格字段可以为空，不检查）
                for (String fieldName : PROJECT_FIELD_REGIONS.keySet()) {
                    if ("金额".equals(fieldName) || "规格".equals(fieldName)) continue;
                    String value = inputFields.get(fieldName + i).getText().trim();
                    if (value.isEmpty()) {
                        JOptionPane.showMessageDialog(this,
                                "第" + i + "个项目的" + fieldName + "不能为空！",
                                "错误",
                                JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                }

                try {
                    double quantity = Double.parseDouble(inputFields.get("数量" + i).getText().trim());
                    double unitPrice = Double.parseDouble(inputFields.get("单价" + i).getText().trim());
                    double amount = quantity * unitPrice;
                    inputFields.get("金额" + i).setText(String.format("%.2f", amount));
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this,
                            "第" + i + "个项目的数量或单价格式不正确！",
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }

        if (!hasValidProject) {
            JOptionPane.showMessageDialog(this,
                    "至少需要填写一个完整的项目！",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(900, 650);
    }
}