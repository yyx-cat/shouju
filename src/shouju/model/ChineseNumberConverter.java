package shouju.model;
import java.math.BigDecimal;

public class ChineseNumberConverter {
    private static final String[] CN_NUMBERS = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
    private static final String[] CN_INTEGER_UNITS = {"", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿"};
    private static final String[] CN_DECIMAL_UNITS = {"角", "分"};
    private static final String CN_FULL = "整";
    private static final String CN_NEGATIVE = "负";
    private static final String CN_ZERO = "零元" + CN_FULL;

    /**
     * 将数字金额转换为中文大写金额
     * @param amount 金额（支持正负、小数）
     * @return 中文大写金额字符串
     */
    public static String toChineseUpper(double amount) {
        if (amount == 0) {
            return CN_ZERO;
        }

        // 使用 BigDecimal 避免精度问题
        BigDecimal amountBig = BigDecimal.valueOf(amount);
        boolean isNegative = amountBig.compareTo(BigDecimal.ZERO) < 0;
        if (isNegative) {
            amountBig = amountBig.abs();
        }

        String result;
        long integerPart = amountBig.longValue();
        int decimalPart = amountBig.remainder(BigDecimal.ONE)
                .movePointRight(2)
                .abs()
                .setScale(0, BigDecimal.ROUND_HALF_UP)
                .intValue();

        // 处理整数部分
        String integerStr = convertIntegerPart(integerPart);

        // 处理小数部分
        String decimalStr = convertDecimalPart(decimalPart);

        // 拼接结果
        if (integerStr.isEmpty()) {
            result = decimalStr;
        } else {
            result = integerStr + "元" + (decimalStr.isEmpty() ? CN_FULL : decimalStr);
        }

        return (isNegative ? CN_NEGATIVE : "") + result;
    }

    private static String convertIntegerPart(long number) {
        if (number == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int unitIndex = 0;
        boolean hasNonZero = false;
        boolean needZero = false;

        while (number > 0) {
            int digit = (int) (number % 10);
            if (digit == 0) {
                if (hasNonZero) {
                    needZero = true;
                }
            } else {
                if (needZero) {
                    sb.insert(0, CN_NUMBERS[0]);
                    needZero = false;
                }
                sb.insert(0, CN_INTEGER_UNITS[unitIndex % 9]);
                sb.insert(0, CN_NUMBERS[digit]);
                hasNonZero = true;
            }
            number /= 10;
            unitIndex++;
        }

        return sb.toString();
    }

    private static String convertDecimalPart(int decimal) {
        if (decimal == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int jiao = decimal / 10;  // 角
        int fen = decimal % 10;  // 分

        if (jiao > 0) {
            sb.append(CN_NUMBERS[jiao]).append(CN_DECIMAL_UNITS[0]);
        }
        if (fen > 0) {
            sb.append(CN_NUMBERS[fen]).append(CN_DECIMAL_UNITS[1]);
        }

        return sb.toString();
    }
}
