package xyz.kuilei.tools.io;

import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.io.IOUtils.EOF;

/**
 * @author JiaKun Xu, 2024-03-18 15:05:05
 */
@ThreadSafe
public class ReplacedLineInputStream extends InputStream {
    @Nonnull
    private final ReplacedLineReader reader;
    private boolean readerEOF;
    private volatile boolean readerClosed;

    private byte[] line;
    private int lineFrom = EOF;

    @Nonnull
    private final byte[] newLine;
    private int newLineFrom = EOF;

    public ReplacedLineInputStream(@Nonnull final ReplacedLineReader reader) {
        this.reader = reader;
        newLine = IOUtils.LINE_SEPARATOR.getBytes(reader.getCharset());
    }

    public ReplacedLineInputStream(@Nonnull final LineReplacer replacer, @Nonnull final File file, @Nullable final String encoding) throws IOException {
        this(new ReplacedLineReader(replacer, file, encoding));
    }

    public ReplacedLineInputStream(@Nonnull final LineReplacer replacer, @Nonnull final File file) throws IOException {
        this(replacer, file, null);
    }

    @Override
    public synchronized int read() throws IOException {
        if (lineEOF() == false) {
            return line[lineFrom++] & 0xff;
        }
        if (newLineEOF() == false) {
            return newLine[newLineFrom++] & 0xff;
        }

        fill();
        return lineEOF() ? EOF : line[lineFrom++] & 0xff;
    }

    @Override
    public synchronized int read(@Nonnull final byte[] b, final int off, final int len) throws IOException {
        if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int from = off;
        final int to = off + len;

        for (; ; ) {
            final int remaining = to - from;
            if (remaining == 0) {
                return len;
            }

            if (lineEOF() == false) {
                final int copyable = Math.min(remaining, lineAvailable());
                System.arraycopy(line, lineFrom, b, from, copyable);
                lineFrom += copyable;
                from += copyable;
                continue;  // remember
            }
            if (newLineEOF() == false) {
                final int copyable = Math.min(remaining, newLineAvailable());
                System.arraycopy(newLine, newLineFrom, b, from, copyable);
                newLineFrom += copyable;
                from += copyable;
                continue;  // remember
            }

            fill();
            if (lineEOF()) {
                return (from == off) ? EOF : from - off;
            }
        }
    }

    @Override
    public long skip(long n) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int available() {
        return 0;
    }

    @Override
    public synchronized void mark(int readlimit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void close() throws IOException {
        if (readerClosed) {
            return;
        }

        synchronized (reader) {
            if (readerClosed) {
                return;
            }
            readerClosed = true;
        }

        reader.close();  // 可以不放到 synchronized
    }

    // --------------------------------
    // line related
    // --------------------------------
    private boolean lineEOF() {
        if (line == null) {
            return true;
        }

        boolean eof = lineFrom >= line.length;
        if (eof) {
            clearLine();
        }
        return eof;
    }

    private int lineAvailable() {
        return line.length - lineFrom;
    }

    private void clearLine() {
        line = null;
        lineFrom = EOF;
    }

    // --------------------------------
    // new line related
    // --------------------------------
    private boolean newLineEOF() {
        if (newLineFrom == EOF) {
            return true;
        }

        boolean eof = newLineFrom >= newLine.length;
        if (eof) {
            clearNewLine();
        }
        return eof;
    }

    private int newLineAvailable() {
        return newLine.length - newLineFrom;
    }

    private void clearNewLine() {
        newLineFrom = EOF;
    }

    // --------------------------------
    // fill
    // --------------------------------
    private void fill() throws IOException {
        if (readerClosed) {
            return;
        }

        String replacedLine;

        synchronized (reader) {
            if (readerClosed) {
                return;
            }

            if (readerEOF) {
                return;
            }
            replacedLine = reader.readLine();
        }

        if (replacedLine == null) {
            readerEOF = true;
            return;
        }

        line = replacedLine.getBytes(reader.getCharset());
        lineFrom = 0;
        newLineFrom = 0;
    }
}
