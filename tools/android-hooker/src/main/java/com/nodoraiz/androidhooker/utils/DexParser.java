package com.nodoraiz.androidhooker.utils;

import com.nodoraiz.androidhooker.models.HookerException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DexParser {

    /**
     * Extract classes using dexdump
     *
     * @param pathToDexdump Path to the dexdump file
     * @param apk File to the apk
     * @param externalClassesToo TRUE if you want to extract external APK classes used
     * @return
     * @throws HookerException
     */
    public static Set<String> findClasses(String pathToDexdump, File apk, boolean externalClassesToo) throws HookerException {

        try {
            if (pathToDexdump == null) {
                throw new IllegalArgumentException("Dexdump path expected");
            }

            if (apk == null || !apk.exists()) {
                throw new IllegalArgumentException("Apk file expected");
            }

            String pathToDexFile = Basics.unzipFile(apk, Configuration.TEMP_DIR, "classes.dex");
            if (pathToDexFile == null) {
                throw new HookerException(new IOException(), "Can't extract classes.dex from APK");
            }

            BufferedReader bufferedReader =
                    Basics.getOutputBufferedReaderFromCommand(new String[]{
                            pathToDexdump + "dexdump",
                            externalClassesToo ? "-d" : "-h",
                            pathToDexFile
                    });

            Set<String> result = new TreeSet<String>();

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Class descriptor")) {
                    result.add(cleanClassName(getValuesUsingRegex(line, "'([^']+)'").get(0)));

                } else if (line.matches("[\\da-z]{6}:.*")) {
                    for (String className : getValuesUsingRegex(line, ("(L[a-zA-Z0-9_\\-\\.\\$\\/]+;)"))) {
                        result.add(cleanClassName(className));
                    }
                }
            }
            bufferedReader.close();

            return result;

        } catch (Exception e){
            Basics.logError(e);
            throw new HookerException(e, "Error finding classes");
        }
    }

    /**
     * Extract values from a line using a given regex
     *
     * @param line Line from where to extract values
     * @param regex Regex to apply to extract values
     * @return
     */
    private static List<String> getValuesUsingRegex(String line, String regex){

        List<String> result = new ArrayList<String>();

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);
        if (matcher.find())
        {
            for(int i=0; i<matcher.groupCount() ; i++) {
                result.add(matcher.group(i+1));
            }
        }
        return result;
    }

    /**
     * Clean the class name read from the output of the dexdump
     *
     * @param className Class name to clean
     * @return
     */
    private static String cleanClassName(String className){

        if(className != null && !className.isEmpty()){
            if(className.startsWith("L")){
                className = className.substring(1);
            }
            if(className.endsWith(";")){
                className = className.substring(0, className.length()-1);
            }
            className = className.replace("/", ".");
        }
        return className;
    }

}
