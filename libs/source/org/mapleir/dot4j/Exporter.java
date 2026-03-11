/*
 * Decompiled with CFR 0.152.
 */
package org.mapleir.dot4j;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import org.mapleir.dot4j.model.DotGraph;
import org.mapleir.dot4j.model.Serialiser;

public class Exporter {
    private static final File DOT_EXECUTABLE = new File("dot/dot.exe");
    private final String src;

    private Exporter(String src) {
        this.src = src;
    }

    public static Exporter fromString(String src) {
        return new Exporter(src);
    }

    public static Exporter fromFile(File src) throws IOException {
        try (FileInputStream is = new FileInputStream(src);){
            Exporter exporter = Exporter.fromString(Exporter.readStream(is));
            return exporter;
        }
    }

    public static Exporter fromGraph(DotGraph graph) {
        return Exporter.fromString(new Serialiser(graph).serialise());
    }

    public void export(File file) throws IOException {
        Path tempDirPath = Files.createTempDirectory(this.getTempDirectory().toPath(), null, new FileAttribute[0]);
        File dotFile = new File(tempDirPath.toString(), "graphsrc.dot");
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(dotFile), StandardCharsets.UTF_8));){
            bw.write(this.src);
        }
        String[] args = System.getProperty("os.name").toLowerCase().indexOf("win") >= 0 ? new String[]{'\"' + DOT_EXECUTABLE.getAbsolutePath() + '\"', "-Tpng", '\"' + dotFile.getAbsolutePath() + '\"', "-o", '\"' + file.getAbsolutePath() + '\"'} : new String[]{"dot", "-Tpng", dotFile.getAbsolutePath(), "-o", file.getAbsolutePath()};
        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = builder.start();
        builder.redirectError(ProcessBuilder.Redirect.INHERIT);
        try {
            process.waitFor();
        }
        catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    private File getTempDirectory() {
        File tempDir = new File(String.format("%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, "graphsrc"));
        if (!tempDir.exists()) {
            tempDir.mkdir();
        }
        return tempDir;
    }

    private static String readStream(InputStream in) throws IOException {
        int read;
        byte[] buf = new byte[in.available()];
        int total = 0;
        while ((read = in.read(buf, total, Math.min(100000, buf.length - total))) > 0) {
            total += read;
        }
        return new String(buf, StandardCharsets.UTF_8);
    }
}

