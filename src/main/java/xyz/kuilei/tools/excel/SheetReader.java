package xyz.kuilei.tools.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.SheetUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author JiaKun Xu, 2023-09-08 09:07:32
 */
public class SheetReader {
    @Nonnull
    private final Iterator<Row> rowIterator;

    /**
     * 因为遍历 cell 是从上到下、从左到右，所以合并单元格也要如此。
     * <p>
     * null: not init
     * empty: no more regions
     * not empty: has regions
     */
    @Nonnull
    private final TreeMap<MergedRegionKey, MergedRegionVal> mergedRegionMap;

    public SheetReader(@Nonnull Sheet sheet) {
        this.rowIterator = sheet.rowIterator();
        this.mergedRegionMap = this.initMergedRegionMap(sheet);
    }

    @Nonnull
    private TreeMap<MergedRegionKey, MergedRegionVal> initMergedRegionMap(@Nonnull Sheet sheet) {
        TreeMap<MergedRegionKey, MergedRegionVal> map = new TreeMap<>();

        for (CellRangeAddress cra : sheet.getMergedRegions()) {
            map.put(new MergedRegionKey(cra), new MergedRegionVal(cra, sheet));
        }

        return map;
    }

    /**
     * @return null if no more lines
     */
    @Nullable
    public List<String> read() {
        Iterator<Row> it = this.rowIterator;

        if (it.hasNext()) {
            Row row = it.next();
            int size = row.getLastCellNum();  // WARN: 如果没有 cell，那么会返回 -1

            if (size <= 0) {
                return Collections.emptyList();
            }

            ArrayList<String> ret = new ArrayList<>(size);

            for (int i = 0; i < size; i++) {
                ret.add(this.obtainMergedCellString(row.getCell(i)));
            }

            return ret;
        }

        return null;
    }

    @Nullable
    private Cell obtainMergedCell(@Nullable Cell cell) {
        if (cell == null) {
            return null;
        }

        TreeMap<MergedRegionKey, MergedRegionVal> cellRangeMap = this.mergedRegionMap;

        if (cellRangeMap.isEmpty()) {
            return cell;
        }

        /*
         * WARN: 不能只判断 1 次，要用 while 循环判断，下面这种情况判断 1 次就处理不了了。
         * 假设第 1 列合并单元格为“合1”
         * 假设第 2 列合并单元格为“合2”
         * 在处理 v1 时，正常获取
         * 在处理 v2 时，访问的是“合2”，获取错误
         * 在处理 v3 时，正常获取
         * 在处理 v4 时，正常获取，但是后面会一直卡在“合1”，因为 v2 没有释放
         *
         *        |====|
         *        | v1 |
         * |====| |----|
         * | v2 | | v3 |
         * |----| |====|
         * | v4 |
         * |====|
         */
        Iterator<MergedRegionVal> it = cellRangeMap.values().iterator();

        while (it.hasNext()) {
            MergedRegionVal val = it.next();

            // NOTE: 如果当前 cell 在当前 merged region 的最左上角的 cell 前面，那么它必不可能在其他 merged region 里面
            if (val.notInAnyRegion(cell)) {
                return cell;
            }

            if (val.inRegion(cell)) {
                // 大于等于更稳
                if (++val.visits >= val.mergedRegion.getNumberOfCells()) {
                    it.remove();
                }

                return val.firstCell;
            }
        }

        return cell;
    }

    @Nullable
    private String obtainMergedCellString(@Nullable Cell cell) {
        return CellUtils.getCellString(this.obtainMergedCell(cell));
    }

    /**
     * 用于高效处理合并单元格的问题
     * <p>
     * 左上角的行号 + 列号
     */
    private static class MergedRegionKey implements Comparable<MergedRegionKey> {
        private final int firstRow;
        private final int firstColumn;

        private MergedRegionKey(@Nonnull CellRangeAddress cra) {
            this.firstRow = cra.getFirstRow();
            this.firstColumn = cra.getFirstColumn();
        }

        /**
         * 1. 行号小的靠前，此时不管列号
         * 2. 行号相等，列号小的靠前
         * 3. 行号大的靠后，此时不管列号
         */
        @Override
        public int compareTo(@Nonnull MergedRegionKey o) {
            int rowCmp = Integer.compare(this.firstRow, o.firstRow);

            if (rowCmp == 0) {
                return Integer.compare(this.firstColumn, o.firstColumn);
            } else {
                return rowCmp;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MergedRegionKey that = (MergedRegionKey) o;

            if (firstRow != that.firstRow) return false;
            return firstColumn == that.firstColumn;
        }

        @Override
        public int hashCode() {
            int result = firstRow;
            result = 31 * result + firstColumn;
            return result;
        }
    }

    /**
     * 用于高效处理合并单元格的问题
     */
    private static class MergedRegionVal {
        /**
         * 合并单元格
         */
        @Nonnull
        private final CellRangeAddress mergedRegion;

        /**
         * 最左上角的 cell。虽然可能为 null，但是逻辑上依然合理
         */
        @Nullable
        private final Cell firstCell;

        /**
         * 当访问次数达到合并单元格的个数时，移除这个 val
         */
        private int visits;

        private MergedRegionVal(@Nonnull CellRangeAddress cra, @Nonnull Sheet sheet) {
            this.mergedRegion = cra;
            this.firstCell = SheetUtil.getCell(sheet, cra.getFirstRow(), cra.getFirstColumn());
        }

        private boolean inRegion(@Nonnull Cell cell) {
            return this.mergedRegion.isInRange(cell);
        }

        private boolean notInAnyRegion(@Nonnull Cell cell) {
            int rowIndex = cell.getRowIndex();
            int columnIndex = cell.getColumnIndex();
            int firstRow = this.mergedRegion.getFirstRow();
            int firstColumn = this.mergedRegion.getFirstColumn();

            if (rowIndex < firstRow) {
                return true;
            } else if (rowIndex == firstRow) {
                return columnIndex < firstColumn;
            }

            return false;
        }
    }
}
