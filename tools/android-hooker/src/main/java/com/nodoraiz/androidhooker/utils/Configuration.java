package com.nodoraiz.androidhooker.utils;

import com.nodoraiz.androidhooker.models.HookerException;
import org.apache.commons.lang.SystemUtils;

import javax.swing.*;
import java.io.File;
import java.io.IOException;

public class Configuration {


    // ##################################################################
    // ##################################################################
    //              Global parameters
    // ##################################################################
    // ##################################################################

    public static final File TEMP_DIR = new File("tmp");
    public static final File LOG_DIR = new File("log");

    public static final File ERROR_LOG_FILE = new File( LOG_DIR.getAbsoluteFile() + "/error.log");
    public static final File LOGCAT_FILE = new File( LOG_DIR.getAbsoluteFile() + "/logcat.log");


    // ##################################################################
    // ##################################################################
    //              Configuration parameters
    // ##################################################################
    // ##################################################################

    private static final File SETUP_FILE = new File("setup.conf");
    private static final String SETUP_FILE_SEPARATOR = ";";

    public static File ANDROID_SDK_DIR = null;
    public static File ANDROID_BUILD_TOOLS = null;
    public static File ANDROID_PLATFORM = null;
    public static int COMMAND_SECONDS_TIMEOUT = -1;


    // ##################################################################
    // ##################################################################
    //              Substrate app generation parameters
    // ##################################################################
    // ##################################################################

    public static File SUBSTRATE_ZIPPED_PROJECT_FILE = new File("substrateApp.zip");
    public static final File TEMP_FILE = new File( TEMP_DIR.getAbsoluteFile() + "/out.tmp");
    public static final File SUBSTRATE_APP_DIR = new File("unzipped-android-substrate-app");
    public static final File SUBSTRATE_APP_GRADLE_LINUX_FILE = new File(SUBSTRATE_APP_DIR.getAbsolutePath() + "/gradlew");
    public static final File SUBSTRATE_APP_GRADLE_WINDOWS_FILE = new File(SUBSTRATE_APP_DIR.getAbsolutePath() + "\\gradlew.bat");
    public static final File LOGGING_PLUGIN_FILE = new File(SUBSTRATE_APP_DIR.getAbsoluteFile() + "/app/src/main/java/com/nodoraiz/substratehook/plugins/LoggerPlugin.java");
    public static final File SUBSTRATE_APK_DEBUG_FILE = new File(SUBSTRATE_APP_DIR.getAbsoluteFile() + "/app/build/outputs/apk/app-debug.apk");
    public static final String SDK_DIR_TOKEN = "#SDK_DIR";
    public static final String BUILD_TOOLS_TOKEN = "#BUILD_TOOLS_DIR";
    public static final String PLATFORM_TOKEN = "#PLATFORM_DIR";
    public static final String GRADLE_LOCAL_PROPERTIES_CONTENT = "sdk.dir=" + SDK_DIR_TOKEN;
    public static final String GRADLE_BUILD_CONTENT = "apply plugin: 'com.android.application'\n" +
            "\n" +
            "android {\n" +
            "    compileSdkVersion " + PLATFORM_TOKEN + "\n" +
            "    buildToolsVersion \"" + BUILD_TOOLS_TOKEN + "\"\n" +
            "\n" +
            "    defaultConfig {\n" +
            "        applicationId \"com.nodoraiz.substratehook\"\n" +
            "        minSdkVersion " + PLATFORM_TOKEN + "\n" +
            "        targetSdkVersion " + PLATFORM_TOKEN + "\n" +
            "        versionCode 1\n" +
            "        versionName \"1.0\"\n" +
            "    }\n" +
            "    buildTypes {\n" +
            "        release {\n" +
            "            minifyEnabled false\n" +
            "            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'\n" +
            "        }\n" +
            "    }\n" +
            "}\n" +
            "\n" +
            "dependencies {\n" +
            "    compile fileTree(dir: 'libs', include: ['*.jar'])\n" +
            "    compile files('libs/substrate-api.jar')\n" +
            "}";

    public static boolean isValidConfiguration() {

        return ANDROID_SDK_DIR != null && ANDROID_SDK_DIR.exists()
                && ANDROID_BUILD_TOOLS != null && ANDROID_BUILD_TOOLS.exists()
                && ANDROID_PLATFORM != null && ANDROID_PLATFORM.exists()
                && COMMAND_SECONDS_TIMEOUT > 0;
    }

    public static boolean saveConfiguration(String sdkDir, String buildToolsDir, String platformDir, int timeout) throws HookerException {

        boolean result = false;

        File sdkDirFile = new File(sdkDir);
        File buildToolDirFile = new File(buildToolsDir);
        File platformDirFile = new File(platformDir);

        if(sdkDirFile.exists() && buildToolDirFile.exists() && platformDirFile.exists() && timeout > 0){
            ANDROID_SDK_DIR = sdkDirFile;
            ANDROID_BUILD_TOOLS = buildToolDirFile;
            ANDROID_PLATFORM = platformDirFile;
            COMMAND_SECONDS_TIMEOUT = timeout;

            String line = ANDROID_SDK_DIR.getAbsolutePath() + SETUP_FILE_SEPARATOR +
                    ANDROID_BUILD_TOOLS + SETUP_FILE_SEPARATOR +
                    ANDROID_PLATFORM + SETUP_FILE_SEPARATOR +
                    COMMAND_SECONDS_TIMEOUT + SETUP_FILE_SEPARATOR;

            Basics.writeFile(SETUP_FILE.getAbsolutePath(), line, false);
            result = true;
        }

        return result;
    }

    public static void initApp() throws HookerException {

        // create log dir
        if( !(LOG_DIR.exists() || LOG_DIR.mkdirs()) ){
            throw new HookerException(new IOException(), "Can't create dir" + LOG_DIR.getAbsolutePath());
        }

        // create temp dir
        if( !(TEMP_DIR.exists() || TEMP_DIR.mkdirs()) ){
            throw new HookerException(new IOException(), "Can't create dir: " + TEMP_DIR.getAbsolutePath());
        }

        // check if we are not running from JAR
        if(!Configuration.class.getResource("Configuration.class").toString().startsWith("jar:")){
            SUBSTRATE_ZIPPED_PROJECT_FILE = new File("./src/main/resources/substrateApp.zip".replace("/", File.separator));
        }

        // unzip substrate app: first check if some important files exists
        if( !(SUBSTRATE_APP_DIR.exists() && SUBSTRATE_APP_GRADLE_LINUX_FILE.exists() && LOGGING_PLUGIN_FILE.exists()) ){

            // if it doesn't exists, then remove the dir and try to unzip again
            SUBSTRATE_APP_DIR.delete();

            // extract substrateApp.zip from resources if it doesn't exist
            if(!SUBSTRATE_ZIPPED_PROJECT_FILE.exists()) {
                try {
                    Basics.unzipFile(
                            new File(Configuration.class.getProtectionDomain().getCodeSource().getLocation().toURI()),
                            new File("."),
                            SUBSTRATE_ZIPPED_PROJECT_FILE.getName()
                    );

                } catch (Exception e) {
                    Basics.logError(e);
                    throw new HookerException(new IOException(), "Can't unzip substrate app. Reason:" + e.getMessage());
                }
            }

            // unzip and check again if the dir and some important files exists this time
            if(! (SUBSTRATE_ZIPPED_PROJECT_FILE.exists() &&
                    Basics.fullUnzip(SUBSTRATE_ZIPPED_PROJECT_FILE.getAbsolutePath(), SUBSTRATE_APP_DIR.getAbsolutePath())
                    && SUBSTRATE_APP_DIR.exists() && SUBSTRATE_APP_GRADLE_LINUX_FILE.exists() && LOGGING_PLUGIN_FILE.exists()) ){
                throw new HookerException(new IOException(), "Can't unzip substrate app in: " + SUBSTRATE_APP_DIR.getAbsolutePath());
            }
        }

        // if *nix, set executable flag to gradlew file
        if(SystemUtils.IS_OS_LINUX) {
            try {
                Runtime.getRuntime().exec("chmod +x " + SUBSTRATE_APP_GRADLE_LINUX_FILE.getAbsolutePath());
            } catch (IOException e) {
                throw new HookerException(e, "Can't set executable flag on gradlew in: " + SUBSTRATE_APP_GRADLE_LINUX_FILE.getAbsolutePath());
            }
            if(!SUBSTRATE_APP_GRADLE_LINUX_FILE.canExecute()){
                throw new HookerException(new IOException(), "Can't set executable flag on gradlew in: " + SUBSTRATE_APP_GRADLE_LINUX_FILE.getAbsolutePath());
            }
        }
    }

    public static boolean loadSetup() throws HookerException {

        ANDROID_SDK_DIR = ANDROID_BUILD_TOOLS = ANDROID_PLATFORM = null;
        COMMAND_SECONDS_TIMEOUT = -1;

        if(SETUP_FILE.exists()){
            try {
                String line = Basics.readFile(SETUP_FILE);
                if(line != null && !line.isEmpty()){
                    String[] tokens = line.split(SETUP_FILE_SEPARATOR);
                    if(tokens.length == 4){
                        ANDROID_SDK_DIR = new File(tokens[0]);
                        ANDROID_BUILD_TOOLS = new File(tokens[1]);
                        ANDROID_PLATFORM = new File(tokens[2]);
                        COMMAND_SECONDS_TIMEOUT = Integer.parseInt(tokens[3]);
                    }
                }

            } catch (HookerException e) {
                throw new HookerException(e, "Error loading setup file");
            }
        }

        return isValidConfiguration();
    }

    /**
     * Sets a theme for the UI
     */
    public static void setUItheme() {

        try {
            for (UIManager.LookAndFeelInfo lookAndFeelInfo : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(lookAndFeelInfo.getName())) {
                    UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
                    break;
                }
            }
        } catch (Exception e) { }

    }
}
