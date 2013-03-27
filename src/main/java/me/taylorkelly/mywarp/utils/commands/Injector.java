package me.taylorkelly.mywarp.utils.commands;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import me.taylorkelly.mywarp.utils.WarpLogger;

public class Injector {
    private Object[] args;
    private Class<?>[] argClasses;

    public Injector(Object... args) {
        this.args = args;
        argClasses = new Class[args.length];
        for (int i = 0; i < args.length; ++i) {
            argClasses[i] = args[i].getClass();
        }
    }

    public Object getInstance(Class<?> clazz) {
        try {
            Constructor<?> ctr = clazz.getConstructor(argClasses);
            ctr.setAccessible(true);
            return ctr.newInstance(args);
        } catch (NoSuchMethodException e) {
            WarpLogger.severe("Error initializing commands class " + clazz
                    + ": ");
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            WarpLogger.severe("Error initializing commands class " + clazz
                    + ": ");
            e.printStackTrace();
            return null;
        } catch (InstantiationException e) {
            WarpLogger.severe("Error initializing commands class " + clazz
                    + ": ");
            e.printStackTrace();
            return null;
        } catch (IllegalAccessException e) {
            WarpLogger.severe("Error initializing commands class " + clazz
                    + ": ");
            e.printStackTrace();
            return null;
        }
    }
}
