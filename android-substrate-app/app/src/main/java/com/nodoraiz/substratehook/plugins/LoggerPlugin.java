package com.nodoraiz.substratehook.plugins;

import android.util.Log;
import com.saurik.substrate.MS;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class LoggerPlugin {

    /**
     * The next variable has to contain all classes which will be hooked separated by comma symbol
     *  NOTE: initially it hasn't any value, but the JAR app will overwrite it before compile
     */
    private final static String[] CLASSES_TO_HOOK = { "" };
    private final static String HOOK_LOG_BREADCRUMB = "HOOK_LOG_BREADCRUMB";
    private final static String PARAMETERS_SEPARATOR = "##P_S##";

    public void apply(){

        try {
            for(String clazzBunch : CLASSES_TO_HOOK) {

                String[] classesToHook = clazzBunch.split(",");

                for (final String className : classesToHook) {

                    Log.d(this.getClass().getName(), "LETS HOOK -" + className + "-");

                    MS.hookClassLoad(className, new MS.ClassLoadHook() {

                        public void classLoaded(Class<?> hookedClass) {

                            Log.d(this.getClass().getName(), "LOCATED CLASS: " + className);

                            for (final Method method : hookedClass.getDeclaredMethods()) {

                                String parameters = "";
                                for (Class parameterClazz : method.getParameterTypes()) {
                                    parameters += parameterClazz.getName() + ", ";
                                }
                                parameters = parameters.substring(0, parameters.length() - 2);
                                final String methodSignature = className + "." + method.getName() + "(" + parameters + ")";

                                if (Modifier.isAbstract(method.getModifiers())) {
                                    Log.d(this.getClass().getName(), "IGNORED ABSTRACT METHOD: " + methodSignature);

                                } else {

                                    try {

                                        Log.d(this.getClass().getName(), "HOOKED METHOD: " + methodSignature);
                                        MS.hookMethod(hookedClass, method, new MS.MethodAlteration() {

                                            public Object invoked(Object capturedInstance, Object... args) throws Throwable {

                                                try {
                                                    StringBuffer stringBuffer = new StringBuffer();
                                                    for(int i=0;i<args.length;i++){
                                                        stringBuffer.append(args[i] + ", ");
                                                    }

                                                    Log.i(HOOK_LOG_BREADCRUMB, "ENTER " + methodSignature + PARAMETERS_SEPARATOR + stringBuffer.substring(0, stringBuffer.length() - 2));
                                                    Object result = this.invoke(capturedInstance, args);
                                                    Log.i(HOOK_LOG_BREADCRUMB, "EXIT " + methodSignature);
                                                    return result;

                                                } catch (Exception e1) {
                                                    Log.d("SubstratePlugin", "Error level 1-a caught: " + e1.getMessage());
                                                }

                                                // if the previous code fails, then we try to keep at least the trace
                                                try {
                                                    Log.i(HOOK_LOG_BREADCRUMB, "ENTER " + methodSignature + PARAMETERS_SEPARATOR);
                                                    Object result = this.invoke(capturedInstance, args);
                                                    Log.i(HOOK_LOG_BREADCRUMB, "EXIT " + methodSignature);
                                                    return result;

                                                } catch (Exception e1) {
                                                    Log.d("SubstratePlugin", "Error level 1-b caught: " + e1.getMessage());

                                                    // last attempt to recovery from an error
                                                    return this.invoke(capturedInstance, args);
                                                }
                                            }
                                        });

                                    } catch (Exception e2) {
                                        Log.d("SubstratePlugin", "Error level 2 caught: " + e2.getMessage());
                                    }
                                }
                            }
                        }
                    });
                }
            }

        } catch (Throwable e3) {
            Log.d("SubstratePlugin", "Error level 3 caught: " + e3.getMessage());
        }
    }
}
