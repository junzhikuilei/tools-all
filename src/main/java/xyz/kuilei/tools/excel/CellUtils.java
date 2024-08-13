package xyz.kuilei.tools.excel;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.cell.CellEditor;
import cn.hutool.poi.excel.cell.CellUtil;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.util.NumberToTextConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static cn.hutool.core.date.DatePattern.PURE_DATETIME_PATTERN;

/**
 * @author JiaKun Xu, 2023-09-07 10:57:12
 */
public class CellUtils {
    /**
     * NUMERIC
     *   a) cn.hutool.core.date.DateTime
     *   b) long
     *   c) double
     * BOOLEAN
     *   a) boolean
     * FORMULA
     *   a) 递归
     * BLANK
     *   a) ""
     * ERROR
     *   a) ""
     *   b) String
     * default
     *   a) String
     *
     * @see CellUtil#getCellValue(Cell, CellType, CellEditor)
     */
    @Nullable
    public static String getCellString(@Nullable Cell cell) {
        Object obj = getCellObject(cell);

        if (obj == null) {
            return null;
        } else if (obj instanceof DateTime) {
            // INFO: 默认系统时区
            return ((DateTime) obj).toString(PURE_DATETIME_PATTERN);
        } else {
            return obj.toString();
        }
    }

    @Nullable
    public static Object getCellObject(@Nullable Cell cell) {
        return getCellObject(cell, null);
    }

    /**
     * NOTE: 用这个把性能从 2872 行 400-500 ms -> 50-250 ms，其中 50-70 是大部分情况
     *
     * @see CellUtil#getCellValue(Cell, CellType, CellEditor)
     */
    @Nullable
    public static Object getCellObject(@Nullable Cell cell, @Nullable CellType cellType) {
        if (null == cell) {
            return null;
        }
        // 不可能集成 NullCell，因为咱没有使用 hutool 的获取 cell 的方法

        if (null == cellType) {
            cellType = cell.getCellType();
        }

        switch (cellType) {
            case NUMERIC:
                return getNumericValue(cell);  // DateTime, long, double
            case FORMULA:
                return getCellObject(cell, cell.getCachedFormulaResultType());
            case BLANK:
                return StrUtil.EMPTY;
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case ERROR:
                FormulaError error = FormulaError.forInt(cell.getErrorCellValue());
                return (null == error) ? StrUtil.EMPTY : error.getString();
            default:
                return cell.getStringCellValue();
        }
    }

    /**
     * @see CellUtil#getNumericValue(Cell)
     */
    @Nonnull
    private static Object getNumericValue(@Nonnull Cell cell) {
        double value = cell.getNumericCellValue();
        CellStyle style = cell.getCellStyle();
        if (null != style) {
            short formatIndex = style.getDataFormat();
            if (isDateType(cell, formatIndex)) {
                return DateUtil.date(cell.getDateCellValue());
            }

            String format = style.getDataFormatString();
            if (null != format && format.indexOf(46) < 0) {
                long longPart = (long) value;
                if ((double) longPart == value) {
                    return longPart;
                }
            }
        }

        return Double.parseDouble(NumberToTextConverter.toText(value));
    }

    /**
     * @see CellUtil#isDateType(Cell, int)
     */
    private static boolean isDateType(Cell cell, int formatIndex) {
        return formatIndex != 14 && formatIndex != 31 && formatIndex != 57 && formatIndex != 58 && formatIndex != 20 && formatIndex != 32 ? org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell) : true;
    }
}
