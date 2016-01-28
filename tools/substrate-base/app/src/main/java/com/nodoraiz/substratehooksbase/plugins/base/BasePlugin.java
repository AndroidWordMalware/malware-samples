package com.nodoraiz.substratehooksbase.plugins.base;

import android.util.Log;

import com.nodoraiz.substratehooksbase.PluginManager;
import com.saurik.substrate.MS;

import java.lang.reflect.Method;

public abstract class BasePlugin {

    public abstract String getPluginName();
    public abstract String getClassNameToHook();
    public abstract Method getMethodNameToHook(Class hookedClass) throws NoSuchMethodException;
    public abstract Object modifyAction(MS.MethodAlteration hookedMethod, Object capturedInstance, Object... args) throws Throwable;

    public void apply(){

        final BasePlugin plugin = this;
        try {

            MS.hookClassLoad(plugin.getClassNameToHook(), new MS.ClassLoadHook() {

                public void classLoaded(Class<?> hookedClass) {

                    if(PluginManager.DEBUG) Log.d(plugin.getPluginName(), plugin.getPluginName() + " hooked");

                    Method hookedMethod = null;
                    try {
                        hookedMethod = plugin.getMethodNameToHook(hookedClass);
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                    if (hookedMethod == null) {
                        if(PluginManager.DEBUG) Log.d(plugin.getPluginName(), "Method not found");

                    } else {

                        if(PluginManager.DEBUG) Log.d(plugin.getPluginName(), plugin.getPluginName() + " method hooked");

                        MS.hookMethod(hookedClass, hookedMethod, new MS.MethodAlteration() {

                            public Object invoked(Object capturedInstance, Object... args) throws Throwable {

                                if(PluginManager.DEBUG) Log.d(plugin.getPluginName(), plugin.getPluginName() + " method alteration");
                                return plugin.modifyAction(this, capturedInstance, args);
                            }

                        });
                    }
                }
            });

        } catch (Throwable e) {
            Log.e("SubstratePlugin", "Error caught: " + e.getMessage());
        }

    }
}
