package xyz.kuilei.tools.io;

import javax.annotation.Nonnull;

/**
 * @author JiaKun Xu, 2024-01-09 20:16:41
 */
public interface LineReplacer {
    @Nonnull
    String replace(@Nonnull String line);
}
