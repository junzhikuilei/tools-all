package xyz.kuilei.tools.io;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.apache.commons.io.IOUtils.EOF;

/**
 * @author JiaKun Xu, 2022-11-11 10:49
 * @see org.apache.commons.io.LineIterator
 */
public class AnySeparatorLineIterator implements Iterator<String>, Closeable {
    /**
     * 行分隔符的字节数组
     */
    @Nonnull
    private final char[] separatorChars;

    /**
     * reader
     */
    @Nonnull
    private final InputStreamReader reader;
    /**
     * 读取时的字节缓存，大小参照 buffered reader
     */
    private char[] charBuffer = new char[8192];
    /**
     * 行构造器
     */
    private StringBuilder lineBuilder = new StringBuilder();
    /**
     * 行缓存
     */
    @Nonnull
    private final List<String> cachedLines = new LinkedList<>();

    /**
     * reader is EOF?
     */
    private boolean readerEOF = false;
    /**
     * 行构造器中下一行的起始位置
     */
    private int nextLineFrom = 0;
    /**
     * 行构造器中下一次的查找位置
     */
    private int nextFindFrom = 0;

    public AnySeparatorLineIterator(@Nonnull final String separator,
                                    @Nonnull final File file,
                                    @Nullable final String encoding) throws IOException {
        if (separator.isEmpty()) {
            throw new IllegalArgumentException("separator is empty");
        }

        reader = new InputStreamReader(FileUtils.openInputStream(file), Charsets.toCharset(encoding));
        separatorChars = separator.toCharArray();
    }

    public AnySeparatorLineIterator(@Nonnull final String separator,
                                    @Nonnull final File file) throws IOException {
        this(separator, file, null);
    }

    /**
     * @throws IllegalStateException if read failed
     */
    @Override
    public boolean hasNext() {
        final List<String> cachedLines = this.cachedLines;

        if (cachedLines.size() != 0) {
            return true;
        }
//        if (lineBuilder.length() != 0) {
//            if (readerEOF) {
//                return findTheLastLine();
//            }
//        }
        if (readerEOF) {
            return false;
        }

        final InputStreamReader reader = this.reader;
        final char[] charBuffer = this.charBuffer;
        final StringBuilder lineBuilder = this.lineBuilder;

        try {
            while (true) {
                final int n = reader.read(charBuffer);

                if (n < 0) {
                    readerEOF = true;
                    return findTheLastLine();
                } else if (n == 0) {
                    // do nothing
                } else {
                    lineBuilder.append(charBuffer, 0, n);
                    if (findLines()) {
                        return true;
                    }
                }
            }
        } catch (IOException e1) {
            try {
                close();
            } catch (IOException e2) {
                e1.addSuppressed(e2);
            }
            throw new IllegalStateException(e1);
        }
    }

    @Override
    public String next() {
        if (hasNext()) {
            return cachedLines.remove(0);
        }

        throw new NoSuchElementException("No more lines");
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Remove unsupported on AnySeparatorLineIterator");
    }

    @Override
    public void close() throws IOException {
        readerEOF = true;

        cachedLines.clear();
        lineBuilder = null;
        charBuffer = null;
        reader.close();
    }

    private boolean findLines() {
        final char[] separatorChars = this.separatorChars;
        final int separatorLength = separatorChars.length;
        final StringBuilder builder = this.lineBuilder;
        final int builderLength = builder.length();
        final List<String> cachedLines = this.cachedLines;

        int lineFrom = nextLineFrom;
        int findFrom = nextFindFrom;
        int findTo = builderLength - findFrom;
        int start;

        while ((start = indexOf(builder, findFrom, findTo, separatorChars)) != EOF) {
            final int separatorFrom = findFrom + start;

            cachedLines.add(builder.substring(lineFrom, separatorFrom));

            lineFrom = separatorFrom + separatorLength;
            findFrom = lineFrom;
            findTo = builderLength - findFrom;
        }

        if (lineFrom != nextLineFrom) { // found
            nextLineFrom = 0;
            nextFindFrom = 0;
            builder.delete(0, lineFrom); // compact
            return true;
        } else { // not found
            // NO change next line from
            nextFindFrom = (builderLength > separatorLength) ? (builderLength - separatorLength + 1) : 0;
            // NO compact
            return false;
        }
    }

    private boolean findTheLastLine() {
        if (lineBuilder.length() != 0) {
            cachedLines.add(lineBuilder.toString());
            return true;
        } else {
            return false;
        }
    }

    /**
     * @see String#indexOf(char[], int, int, char[], int, int, int)
     */
    private static int indexOf(@Nonnull final CharSequence source, final int sourceOffset, final int sourceCount,
                               @Nonnull final char[] target) {
        int targetCount = target.length;

        if (0 >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (targetCount == 0) {
            return 0;
        }

        char first = target[0];
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset; i <= max; i++) {
            /* Look for first character. */
            if (source.charAt(i) != first) {
                while (++i <= max && source.charAt(i) != first) ;
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = 1; j < end && source.charAt(j) == target[k]; j++, k++) ;

                if (j == end) {
                    /* Found whole string. */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }
}
