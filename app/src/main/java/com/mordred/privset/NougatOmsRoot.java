package com.mordred.privset;

import android.annotation.SuppressLint;
import android.os.IBinder;
import android.os.RemoteException;

import java.lang.reflect.Method;

import android.content.om.IOverlayManager;
import android.support.annotation.Keep;

/**
 * Created by mordred on 02.11.2017.
 */

public class NougatOmsRoot {

    private static IOverlayManager mService = null;

    @SuppressLint("PrivateAPI")
    @Keep
    public static void main(String[] args) {
        try {
            Class<?> localClass = Class.forName("android.os.ServiceManager");
            if (localClass != null) {
                Method getService = localClass.getMethod("getService", new Class[] {String.class});
                if (getService != null) {
                    getService.setAccessible(true);
                    Object result = getService.invoke(localClass, new Object[]{"overlay"});
                    if(result != null) {
                        IBinder binder = (IBinder) result;
                        mService = IOverlayManager.Stub.asInterface(binder);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (mService != null) {
            if (args.length > 0) {
                String arg = args[0];
                switch (arg) {
                    case "enable":
                        if (runEnableDisableOMS7Overlay(true)) {
                            // sop success
                            System.out.println("Success, Enabling Privset overlay package");
                        } else {
                            // sop error
                            System.out.println("Failure, Enabling Privset overlay package");
                        }
                        break;
                    case "disable":
                        if (runEnableDisableOMS7Overlay(false)) {
                            // sop success
                            System.out.println("Success, Disabling Privset overlay package");
                        } else {
                            // sop error
                            System.out.println("Failure, Disabling Privset overlay package");
                        }
                        break;
                    case "setHighestPriority":
                        if (runSetPriority(true)) {
                            // sop success
                            System.out.println("Success, Priority change of Privset overlay package -highest-");
                        } else {
                            // sop error
                            System.out.println("Failure, Priority change of Privset overlay package -highest-");
                        }
                        break;
                    case "setLowestPriority":
                        if (runSetPriority(false)) {
                            // sop success
                            System.out.println("Success, Priority change of Privset overlay package -lowest-");
                        } else {
                            // sop error
                            System.out.println("Failure, Priority change of Privset overlay package -lowest-");
                        }
                        break;
                    default:
                        // sop error
                }
            }
        }
    }

    // TODO implement isPrivsetOverlayEnabled check method

    @Keep
    private static boolean runEnableDisableOMS7Overlay(boolean enable) {
        try {
            return mService.setEnabled(FileTools.overlayPackageName, enable, 0, false);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Keep
    private static boolean runSetPriority(boolean isHighest) {
        try {
            if (isHighest) {
                return mService.setHighestPriority(FileTools.overlayPackageName, 0);
            } else {
                return mService.setLowestPriority(FileTools.overlayPackageName, 0);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }
}
