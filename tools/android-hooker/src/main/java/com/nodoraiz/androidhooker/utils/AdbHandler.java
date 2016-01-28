package com.nodoraiz.androidhooker.utils;

import com.nodoraiz.androidhooker.models.HookerException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.io.File;
import java.util.List;
import java.util.Observer;
import java.util.Set;
import java.util.TreeSet;

public class AdbHandler {

    private static final String PACKAGE_LINE = "package:";

    /**
     * Returns the device id based in the "adb devices" command
     * @param expectedValue NULL or empty for attached devices, in other case the expected device ID or part of it strings
     * @return
     * @throws HookerException
     */
    public static String getDeviceId(String expectedValue) throws HookerException {

        if(expectedValue == null) expectedValue = "";

        for(String line : Basics.readFullOutputFromCommand(new String[]{"adb", "devices"})){
            if(((!expectedValue.isEmpty() && line.contains(expectedValue)) || (expectedValue.isEmpty() && !line.contains(".")))
                    && line.contains("\tdevice")){
                return line.substring(0, line.indexOf("\t"));
            }
        }

        return null;
    }

    /**
     * Connects to a device
     * @param deviceId ID of the device based in the output of the command "adb devices".
     * @return
     * @throws HookerException
     */
    public static boolean connectDevice(String deviceId) throws HookerException {

        if(deviceId == null) {
            throw new HookerException(new IllegalArgumentException(), "DeviceID expected.");
        }

        try {
            String[] command = deviceId.contains(".") ? new String[]{"adb", "connect", deviceId} : new String[]{"adb", "devices"};
            for(String line : Basics.readFullOutputFromCommand(command)){
                if(line.contains("connected") || line.contains("\tdevice")) return true;
            }

            return false;

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error connecting to device");
        }
    }

    /**
     * Disconnects from a device
     * @param deviceId ID of the device based in the output of the command "adb devices"
     * @return
     * @throws HookerException
     */
    public static boolean disconnectDevice(String deviceId) throws HookerException {

        if(deviceId == null || deviceId.isEmpty()) {
            throw new HookerException(new IllegalArgumentException(), "DeviceID expected.");
        }

        try {
            String[] command = deviceId.isEmpty() ? new String[]{"adb", "disconnect"} : new String[]{"adb", "disconnect", "ip"};
            List<String> output = Basics.readFullOutputFromCommand(command);
            return output != null && output.size() == 1 && output.get(0).isEmpty();

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error disconnecting from device");
        }
    }

    /**
     * Returns the installed apps in a connected device or NULL if error
     * @param deviceId ID of the device based in the output of the command "adb devices"
     * @return
     * @throws HookerException
     */
    public static TreeSet<String> getInstalledApps(String deviceId) throws HookerException {

        if(deviceId == null || deviceId.isEmpty()) {
            throw new HookerException(new IllegalArgumentException(), "DeviceID expected.");
        }

        try{
            List<String> apks = Basics.readFullOutputFromCommand(new String[]{
                    "adb", "-s", deviceId, "shell", "pm", "list", "packages"
            });

            TreeSet<String> packageNames = null;
            if(apks != null) {
                for (String apk : apks) {
                    if(apk.startsWith(PACKAGE_LINE)) {

                        if(packageNames == null) packageNames = new TreeSet<String>();
                        packageNames.add(apk.substring(PACKAGE_LINE.length()));

                    }
                }
            }

            return packageNames;

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error getting installed apps");
        }
    }

    /**
     * Returns the file which points to the APK based on the device path
     * @param app Name of the app to get the path
     * @param deviceId ID of the device based in the output of the command "adb devices"
     * @return
     * @throws HookerException
     */
    public static String getPathToApk(String app, String deviceId) throws HookerException {

        try{
            List<String> output = Basics.readFullOutputFromCommand(new String[]{
                    "adb", "-s", deviceId, "shell", "pm", "path", app
            });

            if (output != null) {
                for(String line : output) {
                    if(line.startsWith("package:")) {
                        return line.substring("package:".length());
                    }
                }
            }

            return null;

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error getting path to apk: " + app);
        }
    }

    /**
     * Returns the file with the downloaded APK or NULL if the APK couldn't be downloaded
     * @param apkFilePath Path to the apk file in the device
     * @param outputDir Path to the dir where to copy the apk
     * @param deviceId ID of the device based in the output of the command "adb devices"
     * @return
     * @throws HookerException
     */
    public static File downloadApk(String apkFilePath, File outputDir, String deviceId) throws HookerException {

        try{
            if(!outputDir.exists()){
                outputDir.mkdir();
            }
            String apkFileName = new File(apkFilePath).getName();
            Basics.readFullOutputFromCommand(new String[]{
                    "adb", "-s", deviceId, "pull", apkFilePath, outputDir.getAbsolutePath() + File.separator + apkFileName
            });

            File result = new File(outputDir.getAbsolutePath() + File.separator + apkFileName);

            return !result.exists() ? null : result;

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error downloading app from device");
        }
    }

    /**
     * Read from the logcat of the device
     *
     * @param flushAfterRead TRUE if you want to clear the logcat after read
     * @param deviceId ID of the device based in the output of the command "adb devices"
     * @return
     * @throws HookerException
     */
    public static List<String> readLogcat(boolean flushAfterRead, String deviceId) throws HookerException {

        try {

            List<String> output = Basics.readFullOutputFromCommand(new String[]{
                    "adb", "-s", deviceId, "logcat", "-d"
            });

            if(flushAfterRead) {
                Basics.executeCommand(new String[]{
                        "adb", "-s", deviceId, "logcat", "-c"
                });
            }

            return output;

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error reading logcat");
        }
    }

    /**
     * Returns the installed apps in a device
     *
     * @param deviceId ID of the device based in the output of the command "adb devices"
     * @param observer Observer to be updated with the progress
     * @return
     * @throws HookerException
     */
    public static String[] getInstalledApps(String deviceId, Observer observer) throws HookerException {

        String[] result = null;

        TreeSet<String> packageNames = AdbHandler.getInstalledApps(deviceId);
        if (packageNames == null && observer != null) {
            observer.update(null, "Can't get the list of apps");

        } else {
            result = AdbHandler.getInstalledApps(deviceId).toArray(new String[0]);
        }

        return result;
    }

    /**
     * Extract classes from an app
     *
     * @param fullAppName Full name of the app, example com.whatsapp
     * @param observer Observer to be updated with the progress
     * @param deviceId ID of the device based in the output of the command "adb devices"
     * @return
     * @throws HookerException
     */
    public static String[] extractClassesFromSelectedApp(String fullAppName, String deviceId, Observer observer) throws HookerException {

        String[] result;

        // get path to APK
        if(observer != null) observer.update(null, "Locating apk in the device");
        String apkFilePath = AdbHandler.getPathToApk(fullAppName, deviceId);
        if(apkFilePath == null){
            if(observer != null) observer.update(null, "Error when searching for the path of the APK");
            return null;
        }


        // download APK
        if(observer != null) observer.update(null, "Downloading APK");
        File downloadedApk = AdbHandler.downloadApk(apkFilePath, Configuration.TEMP_DIR, deviceId);
        if (!downloadedApk.exists()){
            if(observer != null) observer.update(null, "The APK couldn't be downloaded");
            return null;
        }


        // identify classes
        if(observer != null) observer.update(null, "Extracting classes");
        Set<String> classes = DexParser.findClasses("", downloadedApk, false);
        if(classes.isEmpty()){
            if(observer != null) observer.update(null, "Classes couldn't be retrieved");
            return null;

        } else {
            result = classes.toArray(new String[classes.size()]);
        }

        // dump the extracted classes into temp file
        Basics.writeFile(Configuration.TEMP_FILE.getAbsolutePath(), StringUtils.join(classes, ","), false);
        if (observer != null) observer.update(null, "Classes also extracted in file: " + Configuration.TEMP_FILE.getAbsolutePath());

        return result;
    }

    /**
     * Craft, compile and install an app to hook the given classes
     *
     * @param classes Classes which will be hooked in the device
     * @param observer Observer to be updated with the progress
     * @param deviceId ID of the device based in the output of the command "adb devices"
     * @throws HookerException
     */
    public static void compileAndInstall(List<String> classes,String deviceId, Observer observer) throws HookerException {

        if(classes == null || classes.isEmpty()){
            throw new HookerException(new IllegalArgumentException(), "At least one class has to be defined before craft the hooking app.");
        }

        // distribute selected classes into multiple strings to avoid the Java string limit
        int numberOfClasses = 0;
        StringBuffer stringBuffer = new StringBuffer("\"");
        for (String clazz : classes) {
            numberOfClasses++;
            stringBuffer.append(clazz + ",");

            if (numberOfClasses > 50) {
                stringBuffer.append("\",\n\"");
                numberOfClasses = 0;
            }
        }
        stringBuffer.append("\"");

        // write classes in the Android substrate app
        Basics.overwriteInFile(Configuration.LOGGING_PLUGIN_FILE.getAbsolutePath(),
                "CLASSES_TO_HOOK =", ";", " {\n" + stringBuffer.toString() + "\n\t}");

        // prepare gradle for compilation
        Basics.writeFile(Configuration.SUBSTRATE_APP_DIR.getAbsolutePath() + "/local.properties",
                Configuration.GRADLE_LOCAL_PROPERTIES_CONTENT
                        .replaceAll(Configuration.SDK_DIR_TOKEN, Configuration.ANDROID_SDK_DIR.getAbsolutePath().replace("\\", "/")),
                false
        );

        String platformVersion = Configuration.ANDROID_PLATFORM.getName().substring(Configuration.ANDROID_PLATFORM.getName().indexOf("-") + 1);
        String build = Configuration.ANDROID_BUILD_TOOLS.getName().replace("_", " ");
        Basics.writeFile(Configuration.SUBSTRATE_APP_DIR.getAbsolutePath() + "/app/build.gradle",
                Configuration.GRADLE_BUILD_CONTENT
                        .replaceAll(Configuration.PLATFORM_TOKEN, platformVersion)
                        .replaceAll(Configuration.BUILD_TOOLS_TOKEN, build),
                false
        );

        // compile APK in debug mode using gradle
        if(observer != null) observer.update(null, "Compiling using gradle");
        String[] command;
        if(SystemUtils.IS_OS_WINDOWS){
            command = new String[]{ Configuration.SUBSTRATE_APP_GRADLE_WINDOWS_FILE.getAbsolutePath(), "assembleDebug"};
        } else {
            command = new String[]{"./gradlew", "assembleDebug"};
        }
        List<String> output = Basics.readFullOutputFromCommand(command, Configuration.SUBSTRATE_APP_DIR);
        boolean generatedOk = false;
        for(String line : output){
            if(line.contains("BUILD SUCCESSFUL")){
                generatedOk = true;
                break;
            }
        }
        if(!generatedOk){
            if(observer != null) observer.update(null, "Error generating APK");
            return;
        }

        // install APK generated with gradle
        if(observer != null) observer.update(null, "Compiled!, installing APK in device");
        output = Basics.readFullOutputFromCommand(new String[]{"adb", "-s", deviceId, "install", "-r", Configuration.SUBSTRATE_APK_DEBUG_FILE.getAbsolutePath()});
        if(!output.contains("Success")){
            if(observer != null) observer.update(null, "Error installing APK");
            return;

        } else {
            if(observer != null) observer.update(null, "Apk installed!, restart the device to start logging activity");
        }
    }

}
