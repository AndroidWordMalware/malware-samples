package com.nodoraiz.substratehook;

import com.nodoraiz.substratehook.plugins.LoggerPlugin;

public class PluginManager {

    static void initialize(){

        new LoggerPlugin().apply();

    }
}
