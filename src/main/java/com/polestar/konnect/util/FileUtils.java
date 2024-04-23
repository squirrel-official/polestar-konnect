package com.polestar.konnect.util;


import com.itextpdf.text.Image;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FileUtils {

   public static void copyAllFiles(String source, String destinationPath) throws IOException {
       File srcDir = new File(source);
       String destination = destinationPath + LocalDateTime.now().toLocalDate();
       File destDir = new File(destination);
       try {
           Files.move(srcDir.toPath(), destDir.toPath(), new StandardCopyOption[]{StandardCopyOption.REPLACE_EXISTING});
       }catch (DirectoryNotEmptyException de){
          File tempDirectory = new File(destination+"/temp");
           Files.move(srcDir.toPath(), tempDirectory.toPath(), new StandardCopyOption[]{StandardCopyOption.REPLACE_EXISTING});
           Files.createDirectory(srcDir.toPath());
           org.apache.commons.io.FileUtils.copyDirectory(tempDirectory , destDir);
           org.apache.commons.io.FileUtils.deleteDirectory(tempDirectory);
       }
       org.apache.commons.io.FileUtils.forceMkdir(srcDir);
    }

    public static boolean isNotImage(Path file) {
        return !imageCheck(file);
    }

    public static boolean imageCheck(Path file) {
        try {
            String mimetype = Files.probeContentType(file);
            return mimetype != null && mimetype.split("/")[0].equals("image");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean checkNotHidden(Path eachFilePath) {
        try {
            return !Files.isHidden(eachFilePath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void deleteImages(String source) {
        File srcDir = new File(source);
        Arrays.stream(srcDir.listFiles()).filter(f->imageCheck(f.toPath())).forEach(File::delete);
    }

    static class GenericExtFilter implements FilenameFilter {
        public boolean accept(File file, String name) {
            return imageCheck(file.toPath());
        }
    }
    public static Set<Image> getListOfAllFiles(String path) throws IOException {
        return Files.walk(Paths.get(path))
                .filter(Files::isRegularFile)
                .filter(FileUtils::imageCheck)
                .filter(FileUtils::checkNotHidden)
                .sorted()
                .map(FileUtils::formatImages).filter(Objects::nonNull).collect(Collectors.toSet());
    }


    public static void purgeFilesOlderThanNDays(String path, int days) throws IOException {
        Set<Path> paths = Files.walk(Paths.get(path))
                .filter(Files::isRegularFile)
                .filter(FileUtils::isNotImage)
                .filter(FileUtils::checkNotHidden).filter(Objects::nonNull).collect(Collectors.toSet());

        for (Path eachPath : paths) {
            BasicFileAttributes basicFileAttributes = Files.readAttributes(eachPath, BasicFileAttributes.class);
            long fileTimeInMillis = basicFileAttributes.creationTime().toMillis();
            long currentTimeInMillis = DateTime.now().minusDays(days).getMillis();
            if (fileTimeInMillis < currentTimeInMillis) {
                Files.delete(eachPath);
            }
        }
    }


    private static Image formatImages(Path file) {
        Image image;
        try {
            image = Image.getInstance(file.toString());
            image.scaleAbsolute(300, 300);
            return image;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
