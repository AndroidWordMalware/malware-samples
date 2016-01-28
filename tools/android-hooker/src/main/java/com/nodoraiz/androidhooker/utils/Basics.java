package com.nodoraiz.androidhooker.utils;

import com.nodoraiz.androidhooker.models.HookerException;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Basics {

    /**
     * Returns the BufferedReader where the output of a command will be binded
     *
     * @param command Command to execute
     * @return
     * @throws HookerException
     */
    public static BufferedReader getOutputBufferedReaderFromCommand(String[] command) throws HookerException {

        try {
            Process process = Runtime.getRuntime().exec(command);
            return new BufferedReader(new InputStreamReader(process.getInputStream()));

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error executing or reading the output of a command");
        }
    }

    /**
     * Read the output after execute a command
     *
     * @param command Command to execute
     * @return
     * @throws HookerException
     */
    public static List<String> readFullOutputFromCommand(String[] command) throws HookerException {

        try {
            return readFullOutputFromCommand(command, new File("."));

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error executing or reading the output of a command");
        }
    }

    /**
     * Read the output after execute a command
     *
     * @param command Command to execute
     * @param dir Dir from where to execute the command
     * @return
     * @throws HookerException
     */
    public static List<String> readFullOutputFromCommand(String[] command, File dir) throws HookerException {

        try{
            if(!dir.exists()){
                throw new IOException("The dir -" + dir.getAbsolutePath() + "- doesn't exist");
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(dir);
            Process process = processBuilder.start();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            List<String> result = new ArrayList<String>();
            String line = null;
            while ((line = bufferedReader.readLine()) != null){
                result.add(line);
            }
            bufferedReader.close();

            return result;

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error executing or reading the output of a command");
        }
    }

    /**
     * Executes a command
     *
     * @param command Command to execute
     * @throws HookerException
     */
    public static void executeCommand(String[] command) throws HookerException {

        try{
            Runtime.getRuntime().exec(command);

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error executing a command");
        }
    }

    /**
     * Write a content in a file
     *
     * @param filePath Path to file where oto write
     * @param content Content to write
     * @param append TRUE if you want to append, FALSE if you want to overwrite
     * @throws HookerException
     */
    public static void writeFile(String filePath, String content, boolean append) throws HookerException {

        try{
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filePath, append));
            bufferedWriter.write(content);
            bufferedWriter.flush();
            bufferedWriter.close();

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error writing into file: " + filePath);
        }
    }

    /**
     * Read and returns the content of a file
     *
     * @param file File to read
     * @return
     * @throws HookerException
     */
    public static String readFile(File file) throws HookerException {

        try{
            if(!file.exists()){
                throw new IOException("File not found");
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();

            return new String(data, "UTF-8");

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error reading into file: " + file.getAbsolutePath());
        }
    }

    /**
     * Unzip a specific file from a zipped file
     *
     * @param zipFile Zip file
     * @param outputFolder Dir where to unzip the zip file
     * @param fileName File name to extract from zip
     * @return
     * @throws HookerException
     */
    public static String unzipFile(File zipFile, File outputFolder, String fileName) throws HookerException {

        try {
            String result = null;

            if (zipFile == null || !zipFile.exists() || outputFolder == null || fileName == null || fileName.isEmpty()) {
                return null;
            }

            if (!outputFolder.exists() && !outputFolder.mkdirs()) {
                return null;
            }

            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            byte[] buffer = new byte[1024];
            File newFile;
            int size;
            FileOutputStream fileOutputStream;
            while (zipEntry != null) {

                newFile = new File(outputFolder.getAbsolutePath() + File.separator + zipEntry.getName());
                if (fileName == null || (fileName != null && zipEntry.getName().equals(fileName))) {

                    new File(newFile.getParent()).mkdirs();
                    fileOutputStream = new FileOutputStream(newFile);

                    while ((size = zipInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, size);
                    }

                    if (fileName == null) {
                        fileOutputStream.close();

                    } else {
                        result = newFile.getAbsolutePath();
                        break;
                    }

                }

                zipEntry = zipInputStream.getNextEntry();
            }

            zipInputStream.closeEntry();
            zipInputStream.close();

            return result;

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error unzipping file from: " + zipFile.getAbsolutePath());
        }
    }

    /**
     * Overwrites a section of a file
     *
     * @param filePath Path to file to read and write modified
     * @param start String to search where to start (not included). Use null if you want to overwrite from offset 0
     * @param end String to search where to end (not included. Use null if you want to overwrite until the end
     * @param newContent New content which overwrites the old one from the start to the end
     * @throws HookerException
     */
    public static void overwriteInFile(String filePath, String start, String end, String newContent) throws HookerException {

        StringBuffer content = new StringBuffer(Basics.readFile(new File(filePath)));
        int startIndex = start != null ? content.indexOf(start) + start.length() : 0;
        int endIndex = end != null ? content.indexOf(end, startIndex) : content.length();
        content.replace(startIndex, endIndex, newContent);
        Basics.writeFile(Configuration.LOGGING_PLUGIN_FILE.getAbsolutePath(), content.toString(), false);
    }

    /**
     * Append the error in the error log file
     * @param throwable
     */
    public static void logError(Throwable throwable){

        try{
            String message = "[" + new Date() + "] " + throwable.getMessage() + " => ";
            for(StackTraceElement stackTraceElement : throwable.getStackTrace()){
                message += stackTraceElement.toString() + "; ";
            }
            Basics.writeFile(Configuration.ERROR_LOG_FILE.getAbsolutePath(),  message, true);

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Unzip completely a zipped file
     *
     * @param pathToZip Path to zip file
     * @param outputPath Path to the dir where to extract the content of the zip file
     * @return
     */
    public static boolean fullUnzip(String pathToZip, String outputPath) throws HookerException {

        try{
            byte[] buffer = new byte[1024];
            File folder = new File(outputPath);
            if(!folder.exists()){
                folder.mkdir();
            }

            ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(pathToZip));
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            String fileName;
            File newFile;
            FileOutputStream fileOutputStream;
            int len;
            while(zipEntry != null){

                fileName = zipEntry.getName();
                if(!fileName.endsWith("/")) {
                    newFile = new File(outputPath + File.separator + fileName);
                    new File(newFile.getParent()).mkdirs();
                    fileOutputStream = new FileOutputStream(newFile);
                    while ((len = zipInputStream.read(buffer)) > 0) {
                        fileOutputStream.write(buffer, 0, len);
                    }

                    fileOutputStream.close();
                }
                zipEntry = zipInputStream.getNextEntry();
            }

            zipInputStream.closeEntry();
            zipInputStream.close();

            return true;

        } catch(IOException e) {
            Basics.logError(e);
            throw new HookerException(e, "Error unzipping file: " + pathToZip);
        }
    }
}
