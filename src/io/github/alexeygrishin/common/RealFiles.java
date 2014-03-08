package io.github.alexeygrishin.common;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RealFiles implements Files {
    @Override
    public Source[] getSources(String path) {
        File file = new File(path);
        if (file.isFile() || !file.exists()) {
            return new Source[] {new RealSource(file, file.getName())};
        }
        else {
            List<Source> sources = new ArrayList<>();
            collectSources(new File(path), file, sources);
            return sources.toArray(new Source[sources.size()]);
        }
    }

    private void collectSources(File root, File dir, List<Source> sources) {
        for (File file: dir.listFiles()) {
            if (file.isDirectory()) {
                collectSources(root, file, sources);
            }
            else if (file.isFile() && file.canRead()) {
                sources.add(new RealSource(file, root.toPath().relativize(file.toPath()).toString()));
            }
        }
    }


}
