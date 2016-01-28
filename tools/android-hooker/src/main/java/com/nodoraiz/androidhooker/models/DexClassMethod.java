package com.nodoraiz.androidhooker.models;

import java.util.List;

public class DexClassMethod {

    public enum enmMethodType{
        constructor,
        staticMethod,
        instanceMethod,
    }

    private enmMethodType methodType;

    private String className;
    private String methodName;
    private List<String> paramsType;
    private String returnType;

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public List<String> getParamsType() {
        return paramsType;
    }

    public String getReturnType() {
        return returnType;
    }

    public enmMethodType getMethodType() {
        return methodType;
    }

    public DexClassMethod(enmMethodType methodType, String className, String methodName, List<String> paramsType, String returnType) {
        this.methodType = methodType;
        this.className = className;
        this.methodName = methodName;
        this.paramsType = paramsType;
        this.returnType = returnType;
    }

    @Override
    public String toString() {

        StringBuffer stringBuffer = new StringBuffer();
        for(String param : paramsType){
            stringBuffer.append(param + ",");
        }
        String params = "";
        if(stringBuffer.length() > 0){
            params = stringBuffer.substring(0, stringBuffer.length()-1);
        }

        return this.methodType.name() + " "
                + this.returnType + " "
                + this.className + "."
                + this.methodName + "("
                + params +")";
    }

}
