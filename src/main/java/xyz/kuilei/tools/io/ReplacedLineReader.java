package xyz.kuilei.tools.io;

import lombok.Getter;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.Charset;

/**
 * @author JiaKun Xu, 2024-03-18 14:47:47
 */
public class ReplacedLineReader implements Closeable {
    @Nonnull
    private final BufferedReader reader;
    @Nonnull
    private final LineReplacer replacer;
    @Getter
    @Nonnull
    private final Charset charset;

    public ReplacedLineReader(@Nonnull final LineReplacer replacer, @Nonnull final File file, @Nullable final String encoding) throws IOException {
        Charset charset = Charsets.toCharset(encoding);

        this.reader = new BufferedReader(new InputStreamReader(FileUtils.openInputStream(file), charset));
        this.replacer = replacer;
        this.charset = charset;
    }

    public ReplacedLineReader(@Nonnull final LineReplacer replacer, @Nonnull final File file) throws IOException {
        this(replacer, file, null);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    @Nullable
    public String readLine() throws IOException {
        String line = reader.readLine();

        if (line == null) {
            return null;
        }
        return replacer.replace(line);
    }
}
