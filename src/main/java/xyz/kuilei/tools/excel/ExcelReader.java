package xyz.kuilei.tools.excel;

import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * @author JiaKun Xu, 2023-09-08 11:09:44
 */
public class ExcelReader implements Closeable {
    private Workbook workbook;
    private Iterator<Sheet> sheetIterator;

    @Getter
    private Sheet currentSheet;
    private SheetReader currentSheetReader;

    public ExcelReader(@Nonnull File file) throws IOException {
        workbook = WorkbookFactory.create(file, null, true);
        sheetIterator = workbook.sheetIterator();
    }

    /**
     * @return null if EOF
     */
    @Nullable
    public List<String> read() {
        List<String> line;

        if ((line = nextLine()) != null) {
            return line;
        }

        if ((line = nextSheetNextLine()) != null) {
            return line;
        }

        return null;
    }

    @Override
    public void close() {
        final Workbook workbook = this.workbook;

        if (workbook != null) {
            this.workbook = null;
            sheetIterator = null;
            currentSheet = null;
            currentSheetReader = null;

            IOUtils.closeQuietly(workbook);
        }
    }

    @Nullable
    private List<String> nextLine() {
        final SheetReader reader = this.currentSheetReader;

        if (reader == null) {
            return null;
        }

        List<String> line;

        while ((line = reader.read()) != null) {
            if (line.size() != 0) {
                return line;
            }
        }

        skipCurrentSheet();
        return null;
    }

    @Nullable
    private List<String> nextSheetNextLine() {
        final Iterator<Sheet> iterator = this.sheetIterator;

        if (iterator == null) {
            return null;
        }

        List<String> line;

        while (iterator.hasNext()) {
            currentSheet = iterator.next();
            currentSheetReader = new SheetReader(currentSheet);

            if ((line = nextLine()) != null) {
                return line;
            }
        }

        sheetIterator = null;
        return null;
    }

    public void skipCurrentSheet() {
        Sheet sheet = currentSheet;

        if (sheet != null) {
            currentSheet = null;
            currentSheetReader = null;
        }
    }
}
