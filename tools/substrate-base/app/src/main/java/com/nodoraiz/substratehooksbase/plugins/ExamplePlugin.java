package com.nodoraiz.substratehooksbase.plugins;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.nodoraiz.substratehooksbase.plugins.base.BasePlugin;
import com.saurik.substrate.MS;

import java.lang.reflect.Method;

public class ExamplePlugin extends BasePlugin{


    @Override
    public String getPluginName() {
        return "ActivityStartExample";
    }

    @Override
    public String getClassNameToHook() {
        return "android.app.Activity";
    }

    @Override
    public Method getMethodNameToHook(Class hookedClass) throws NoSuchMethodException {
        return hookedClass.getMethod("startActivity", Intent.class, Bundle.class);
    }

    @Override
    public Object modifyAction(MS.MethodAlteration hookedMethod, Object capturedInstance, Object... args) throws Throwable {

        Log.d(this.getPluginName(), "Let's hook!");
        return hookedMethod.invoke(capturedInstance, args);
    }
}