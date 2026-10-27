package com.katyaflix.uploadservice.util;

import com.katyaflix.uploadservice.dto.enums.FileType;

import java.util.ArrayList;
import java.util.Arrays;

public class FileTypeUtil {



    private final static ArrayList<String> videoExtensions = new ArrayList<>(Arrays.asList("mp4", "mkv", "webm", "avi", "mov", "m4v", "wmv", "flv", "mpeg", "mpg", "ts", "m2ts"));

    private final static ArrayList<String> subtitleExtensions = new ArrayList<>(Arrays.asList("srt", "ass", "ssa", "vtt", "sub", "idx", "sup"));

    public static FileType getFileType(String extension) {
        extension = extension.toLowerCase();

        if (videoExtensions.contains(extension)) {
            return FileType.VIDEO;
        }

        if (subtitleExtensions.contains(extension)) {
            return FileType.SUBTITLE;
        }

        return FileType.UNSUPPORTED;
    }

}
