package model;

public class Util {
    public static <T> T RandomPick(T[] arr){
        int randomIndex = (int) (Math.random() * arr.length);
        return arr[randomIndex];
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "";
        int lastIndexOfDot = fileName.lastIndexOf('.');
        if (lastIndexOfDot <= 0) return "";
        return fileName.substring(lastIndexOfDot + 1);
    }
}
