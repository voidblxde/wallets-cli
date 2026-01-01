package app.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReportService {
    private PrintWriter out = new PrintWriter(System.out, true);
    private PrintWriter fileOut;

    public void setConsole() {
        closeFileOnly();
        out = new PrintWriter(System.out, true);
    }

    public void setFile(Path path) {
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            closeFileOnly();
            fileOut = new PrintWriter(Files.newBufferedWriter(path), true);
            out = fileOut;
        } catch (IOException e) {
            throw new RuntimeException("Не могу открыть файл для вывода: " + path + ": " + e.getMessage(), e);
        }
    }

    public void println(String s) {
        out.println(s);
        out.flush();
    }

    public void close() {
        closeFileOnly();
    }

    private void closeFileOnly() {
        if (fileOut != null) {
            fileOut.flush();
            fileOut.close();
            fileOut = null;
        }
    }
}
