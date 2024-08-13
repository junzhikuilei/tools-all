package xyz.kuilei.tools.test;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.Test;
import xyz.kuilei.tools.excel.ExcelReader;
import xyz.kuilei.tools.excel.TrimmedLine;
import xyz.kuilei.tools.io.AnySeparatorLineIterator;
import xyz.kuilei.tools.io.LineReplacer;
import xyz.kuilei.tools.io.ReplacedLineInputStream;
import xyz.kuilei.tools.io.ReplacedLineReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author JiaKun Xu, 2024-02-28 09:06:56
 */
public class TestAll {
    /**
     * OK
     */
    @Test
    public void testLineIterator() throws IOException {
        try (AnySeparatorLineIterator ite = new AnySeparatorLineIterator(
                "\r\n",
                new File("D:\\testdata\\1.txt"),
                StandardCharsets.UTF_8.name()
        )) {
            int count = 0;
            while (ite.hasNext()) {
                ++count;
                System.out.println(ite.next());
            }
            System.out.println(String.format("[%d]", count));
        }
    }

    @Test
    public void testExcelReader() throws IOException {
        try (ExcelReader reader = new ExcelReader(
                new File("D:\\testdata\\1.xlsx")
        )) {
            List<String> line;
            TrimmedLine currentHeader = null;
            Sheet currentSheet = null;

            while ((line = reader.read()) != null) {
//                System.out.println(line);

                // trimmed
                final TrimmedLine trimmedLine = TrimmedLine.fromLine(line);
                final Sheet sheet = reader.getCurrentSheet();

                // sheet changed
                if (sheet != currentSheet) {
                    currentSheet = sheet;
                    currentHeader = null;
                    System.out.println("----");
                }
                // make sure header is available
                if (currentHeader == null && trimmedLine != null) {
                    currentHeader = trimmedLine;
                    System.out.println(currentHeader);
                    continue; // remember
                }

                if (currentHeader != null && trimmedLine != null) {
                    System.out.println(currentHeader.asHeaderGetDataLine(trimmedLine));
                }
//                System.out.println(TrimmedLine.fromLine(line));
            }
        }
    }

    @Test
    public void testReplacedLine() throws IOException {
        long start;

        LineReplacer replacer = line -> StringUtils.replaceEach(line, new String[]{"`", ",", "${sp}", "\\"}, new String[]{"~", "`", ",", "\\\\"});
        String encoding = StandardCharsets.UTF_8.name();

        File inFile = new File("D:\\testdata\\2-大量替换.txt");
        File outFile = new File("D:\\testdata\\2-大量替换.replaced.txt");

        for (int i = 0; i < 1; i++) {
            ReplacedLineReader reader = new ReplacedLineReader(replacer, inFile, encoding);
//            String line;
//            while ((line = reader.readLine()) != null) {
//                System.out.println(line);
//            }
//            reader.close();

//            BufferedInputStream in = new BufferedInputStream(new FileInputStream(inFile));
            ReplacedLineInputStream in = new ReplacedLineInputStream(reader);
            FileOutputStream out = new FileOutputStream(outFile);

            start = System.currentTimeMillis();
            IOUtils.copy(in, out);
            System.out.println(System.currentTimeMillis() - start);

            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }
}
