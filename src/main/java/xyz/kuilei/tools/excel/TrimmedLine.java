package xyz.kuilei.tools.excel;

import cn.hutool.core.lang.Assert;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 用来处理表头匹配问题，因为有些表头不是从第 1 列开始，这让我想到了 trim
 *
 * @author JiaKun Xu, 2023-09-12 15:25:01
 */
public class TrimmedLine {
    @Nonnull
    private final ArrayList<String> line;
    private final int from;  // include
    private final int to;    // exclude

    private TrimmedLine(@Nonnull ArrayList<String> line, int from, int to) {
        Assert.isTrue((line.size() != 0));
        Assert.isTrue(0 <= from && from < to && to <= line.size());

        this.line = line;
        this.from = from;
        this.to = to;
    }

    /**
     * 构造前后去除空白的TrimmedLine数据行
     * 可能会额外去除首尾红白,后续有逻辑会根据header重新填充
     *
     * @return null if trimmed to empty
     * @see String#trim()
     */
    @Nullable
    public static TrimmedLine fromLine(@Nullable List<String> line) {
        if (CollectionUtils.isEmpty(line)) {
            return null;
        } else {
            if (!(line instanceof ArrayList)) {
                line = new ArrayList<>(line);
            }

            int from = 0;
            int to = line.size();

            while (from < to && StringUtils.isBlank(line.get(from))) {
                ++from;
            }
            while (from < to && StringUtils.isBlank(line.get(to - 1))) {
                --to;
            }

            if (from == to) {
                return null;
            } else {
                return new TrimmedLine((ArrayList<String>) line, from, to);
            }
        }
    }

    public boolean equalsLine(@Nullable List<String> line) {
        return ListUtils.isEqualList(this.line.subList(from, to), line);
    }

    @Override
    public String toString() {
        return line.subList(from, to).toString();
    }

    /**
     * ---------------------
     * |    | h1 | h2 |    |
     * ---------------------
     * | a1 | a2 | a3 | a4 |
     * ---------------------
     * | b1 |    |    | b4 |
     * ---------------------
     * 按上例获取，结果是
     * 1. [a2, a3];
     * 2. [ , ].
     *
     * You have to PAY ATTENTION!
     *
     * 能这么做是因为 {@link SheetReader#read()} 是从 0 开始读的，
     * 所以 header 的 cell list 与 data 一样都是从 0 开始，不会错列。
     *
     * 如果 header 的 cell list 是 [1, ?)，data 是 [0, ?)，
     * 那么按照 header 截取 data 是错列的，
     * 因为 {@link TrimmedLine#from} 无论如何都是从 0 开始的。
     */
    @Nullable
    public List<String> asHeaderGetDataLine(@Nonnull TrimmedLine data) {
        return asHeaderGetDataLine(data, false);
    }

    @Nullable
    public List<String> asHeaderGetDataLine(@Nonnull TrimmedLine data, boolean force) {
        final int headerFrom = this.from;
        final int headerTo = this.to;
//        final ArrayList<String> headerLine = this.line;
        final int dataFrom = data.from;
        final int dataTo = data.to;
        final ArrayList<String> dataLine = data.line;

        if (headerFrom <= dataFrom && dataTo <= headerTo) {
            if ((headerFrom == dataFrom && dataTo == headerTo) && (dataFrom == 0 && dataTo == dataLine.size())) {
                return dataLine;
            } else {
                return headerGetDataLine(this, data);
            }
        } else {
            if (force) {
                return headerGetDataLine(this, data);
            } else {
                return null;
            }
        }
    }

    /**
     * 不匹配的补全 ""
     */
    @Nonnull
    private static List<String> headerGetDataLine(@Nonnull TrimmedLine header, @Nonnull TrimmedLine data) {
        final int headerFrom = header.from;
        final int headerTo = header.to;
//        final ArrayList<String> headerLine = this.line;
        final int dataFrom = data.from;
        final int dataTo = data.to;
        final ArrayList<String> dataLine = data.line;

        final ArrayList<String> ret = new ArrayList<>(headerTo - headerFrom);

        for (int i = headerFrom; i < headerTo; ++i) {
            if (i >= dataFrom && i < dataTo) {
                ret.add(dataLine.get(i));
            } else {
                ret.add(StringUtils.EMPTY);
            }
        }

        return ret;
    }
}
