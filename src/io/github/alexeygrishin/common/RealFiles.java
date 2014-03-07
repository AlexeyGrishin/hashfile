package io.github.alexeygrishin.common;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RealFiles implements Files {
    @Override
    public String[] resolveIds(String path) {
        File file = new File(path);
        if (file.isFile()) {
            return new String[] {path};
        }
        else {
            List<String> fileNames = new ArrayList<>();
            collectFiles(new File("."), file, fileNames);
            return fileNames.toArray(new String[fileNames.size()]);
        }
    }

    private void collectFiles(File root, File dir, List<String> names) {
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) {
                collectFiles(root, file, names);
            }
            else if (file.isFile() && file.canRead()) {
                names.add(file.getAbsolutePath());
            }
        }
    }

    @Override
    public String toKey(String id) {
        return new File(id).getName();
    }

    @Override
    public InputStream getInputStream(String id) {
        try {
            return new FileInputStream(id);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);  //TODO
        }
    }

    @Override
    public OutputStream getOutputStream(String id) {
        try {
            return new FileOutputStream(id);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
