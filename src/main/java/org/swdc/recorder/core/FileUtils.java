package org.swdc.recorder.core;

import java.io.File;

public class FileUtils {

    public static boolean deleteAnyFile(File file) {
        if (!file.isDirectory()) {
            return file.delete();
        } else {
            File [] files = file.listFiles();
            if (files == null) {
                return file.delete();
            } else {
                boolean result = true;
                for (File f : files) {
                    if (f.isDirectory()) {
                        result = result && deleteAnyFile(f) && f.delete();
                    } else {
                        result = result && f.delete();
                    }
                }
                result = result && file.delete();
                return result;
            }
        }
    }

}
