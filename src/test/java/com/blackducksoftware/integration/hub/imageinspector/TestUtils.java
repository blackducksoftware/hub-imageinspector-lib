package com.blackducksoftware.integration.hub.imageinspector;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;

public class TestUtils {
    public static File createTempDirectory() throws IOException {
        final File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
        if (!(temp.delete())) {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }
        if (!(temp.mkdir())) {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }
        return (temp);
    }

    public static boolean contentEquals(final File file1, final File file2, final List<String> exceptLinesContainingThese) throws IOException {
        System.out.println(String.format("Comparing %s %s", file1.getAbsolutePath(), file2.getAbsolutePath()));
        int ignoredLineCount = 0;
        int matchedLineCount = 0;
        final List<String> lines1 = FileUtils.readLines(file1, StandardCharsets.UTF_8);
        final List<String> lines2 = FileUtils.readLines(file2, StandardCharsets.UTF_8);

        if (lines1.size() != lines2.size()) {
            System.out.println("Files' line counts are different");
            return false;
        }
        for (int i = 0; i < lines1.size(); i++) {
            final String line1 = lines1.get(i);
            final String line2 = lines2.get(i);
            boolean skip = false;
            if (exceptLinesContainingThese != null) {
                for (final String ignoreMe : exceptLinesContainingThese) {
                    if (line1.contains(ignoreMe) || line2.contains(ignoreMe)) {
                        skip = true;
                        ignoredLineCount++;
                    }
                }
            }
            if (skip) {
                continue;
            }
            if (!line2.equals(line1)) {
                System.out.println(String.format("File comparison: These lines do not match:\n%s\n%s", lines1.get(i), lines2.get(i)));
                return false;
            } else {
                matchedLineCount++;
            }
        }
        System.out.println(String.format("These files match (%d lines matched; %d lines ignored)", matchedLineCount, ignoredLineCount));
        return true;
    }
}
