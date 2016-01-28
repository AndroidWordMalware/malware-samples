package com.nodoraiz.substratehooksbase;

import com.nodoraiz.substratehooksbase.plugins.ExamplePlugin;

public class PluginManager {

    public static final boolean DEBUG = true;

    static void initialize(){

        new ExamplePlugin().apply();

    }

}
