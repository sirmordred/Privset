package com.mordred.privset;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.support.v7.app.AlertDialog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;


/**
 * Created by mordred on 17.05.2017.
 */

public class FileTools {

    // These strings control the legacy overlay location
    public static final String VENDOR_DIRECTORY = "/vendor/overlay/";
    public static final String PIXEL_NEXUS_DIRECTORY = "/system/overlay/";
    public static final String LEGACY_NEXUS_DIRECTORY = "/system/vendor/overlay/";
    public static final String P_DIR = "/system/app/";
    public static final String DATA_RESOURCE_DIRECTORY = "/data/resource-cache/";


    public static final String overlayPackageName = "com.privset.frameworkresoverlay";

         // Predetermined list of new Nexus/Pixel Devices
    private static final String[] GOOGLE_DEVICE_LIST = new String[]{
            "angler", // Nexus 6P
            "bullhead", // Nexus 5X
            "flounder", // Nexus 9
            "dragon", // Pixel C
            "marlin", // Pixel
            "sailfish", // Pixel XL
            "walleye", // Pixel 2
            "muskie", // Pixel XL 2
            "taimen", // Pixel ?
    };

    public static void setCtx(final String foldername) {
        Shell.SU.run("chcon -R u:object_r:system_file:s0 " + foldername);
    }

    public static void setPerm(final int permission, final String foldername) {
        Shell.SU.run("chmod " + permission + " " + foldername);
    }

    public static void setPermRec(final int permission, final String foldername) {
        Shell.SU.run("chmod -R " + permission + " " + foldername);
    }

    private static boolean isToyBox(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences("otherPreferences", Context.MODE_PRIVATE);
        // default style is "toybox" style, because aosp has toybox not toolbox
        boolean isToybox = true;
        if (prefs.contains("usingToybox")) {
            isToybox = prefs.getBoolean("usingToybox", true);
        } else {
            Process process = null;
            try {
                Runtime rt = Runtime.getRuntime();
                process = rt.exec(new String[]{"readlink", "/system/bin/mount"});
                try (BufferedReader stdInput = new BufferedReader(new
                        InputStreamReader(process.getInputStream()))) {
                    // if it has toolbox instead of toybox, handle
                    if (stdInput.readLine().equals("toolbox")) {
                        isToybox = false;
                    }
                }
            } catch (Exception e) {
                isToybox = true;
            } finally {
                prefs.edit().putBoolean("usingToybox", isToybox).commit();
                if (process != null) {
                    process.destroy();
                }
            }
        }
        return isToybox;
    }

    public static void mountSystemAsRW(Context ct) {
        String cmdMountRW;
        if (isToyBox(ct)) {
            cmdMountRW = "mount -o rw,remount /system";
        } else {
            cmdMountRW = "mount -o remount,rw /system";
        }
        Shell.SU.run(cmdMountRW);
    }

    public static void mountVendorAsRW(Context ct) {
        String cmdMountRWVendor;
        if (isToyBox(ct)) {
            cmdMountRWVendor = "mount -o rw,remount /vendor";
        } else {
            cmdMountRWVendor = "mount -o remount,rw /vendor";
        }
        Shell.SU.run(cmdMountRWVendor);
    }

    public static void mountSystemAsRO(Context ct) {
        String cmdMountRO;
        if (isToyBox(ct)) {
            cmdMountRO = "mount -o ro,remount /system";
        } else {
            cmdMountRO = "mount -o remount,ro /system";
        }
        Shell.SU.run(cmdMountRO);
    }

    public static void mountVendorAsRO(Context ct) {
        String cmdMountROVendor;
        if (isToyBox(ct)) {
            cmdMountROVendor = "mount -o ro,remount /vendor";
        } else {
            cmdMountROVendor = "mount -o remount,ro /vendor";
        }
        Shell.SU.run(cmdMountROVendor);
    }

    public static void mountDataAsRO(Context ct) {
        String cmdMountROData;
        if (isToyBox(ct)) {
            cmdMountROData = "mount -o ro,remount /data";
        } else {
            cmdMountROData = "mount -o remount,ro /data";
        }
        Shell.SU.run(cmdMountROData);
    }

    public static void mountDataAsRW(Context ct) {
        String cmdMountRWData;
        if (isToyBox(ct)) {
            cmdMountRWData = "mount -o rw,remount /data";
        } else {
            cmdMountRWData = "mount -o remount,rw /data";
        }
        Shell.SU.run(cmdMountRWData);
    }

    public static void createNewFolder(String foldername, boolean withRoot) {
        File folder = new File(foldername);
        if (!folder.exists()) {
            if (withRoot) {
                Shell.SU.run("mkdir " + foldername);
            } else {
                folder.mkdirs();
            }
        }
    }

    public static void delete(String directory) {
        File dir = new File(directory);
        if (dir.exists()) {
            StringBuilder command = new StringBuilder("rm -rf ");
            if (dir.isDirectory()) {
                for (File child : dir.listFiles()) {
                    command.append(child.getAbsolutePath()).append(" ");
                }
            } else {
                command.append(directory);
            }
            Shell.SU.run(command.toString());
        }
    }

    public static void move(String source, String destination, boolean withRoot) {
        File in = new File(source);
        File out = new File(destination);
        if (withRoot) {
            Shell.SU.run("mv -f " + source + " " + destination);
        } else {
            moveFile(in, out);
        }
    }

    private static void moveFile(final File srcFile, final File destFile) {
        if (!srcFile.isFile()) {
            return;
        }
        if (destFile.isFile()) {
            destFile.delete();
        }
        if (!srcFile.renameTo(destFile)) {
            if (srcFile.isFile() && !destFile.exists()) {
                if (copyFile(srcFile,destFile)) {
                    srcFile.delete();
                }
            }
        }
    }

    private static boolean copyFile(File inputFile, File outputFile) {
        try (InputStream in = new FileInputStream(inputFile);
             OutputStream out = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[1024];
            int read;
            while((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch(IOException e) {
            return false;
        }
        return true;
    }

    private static Boolean isOreo() {
        return (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) ||
                (Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1);
    }

    // This method configures the new devices and their configuration of their vendor folders
    public static Boolean isGoogleDevice() {
        return Arrays.asList(GOOGLE_DEVICE_LIST).contains(Build.DEVICE);
    }

    private static void restartDevice() {
        Shell.SU.run("reboot");
    }

    public static boolean requestRoot() {
        return !Shell.SU.available();
    }

    public static void installWithRoot(String apkDest) {
        Shell.SU.run("pm install -r " + apkDest);
    }

    public static void uninstallWithRoot(Context c, String packageName) {
        if (isPackageInstalled(c,packageName)) {
            Shell.SU.run("pm uninstall " + packageName);
        }
    }

    public static boolean isPackageInstalled(Context context, String pkg) {
        PackageInfo p;
        try {
            p = context.getPackageManager().getPackageInfo(pkg, 0);
        } catch (Exception e) {
            // App is not installed
            return false;
        }
        return p != null;
    }

    public static boolean systemSupportsCMTE(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("otherPreferences", Context.MODE_PRIVATE);
        boolean usesCMTE = false;
        if (prefs.contains("supports_cmte")) {
            usesCMTE = prefs.getBoolean("supports_cmte", false);
        } else {
            String output = listToString(Shell.SU.run("tm"));
            if (!output.equals("")) {
                usesCMTE = output.contains("tm list");
            }
            prefs.edit().putBoolean("supports_cmte", usesCMTE).commit();
        }
        return usesCMTE;
    }

    public static boolean systemSupportsOMS(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("otherPreferences", Context.MODE_PRIVATE);
        boolean usesOMS = false;
        if (prefs.contains("supports_oms")) {
            usesOMS = prefs.getBoolean("supports_oms", false);
        } else {
            if (isOreo()) {
                usesOMS = true;
            } else {
                if (hasOMS(context)) {
                    usesOMS = true;
                }
            }
            prefs.edit().putBoolean("supports_oms", usesOMS).commit();
        }
        return usesOMS;
    }

    public static void printToXml(String data, String xmlPath) {
        File saveFile = new File(xmlPath);
        FileWriter fw = null;
        BufferedWriter bw = null;
        PrintWriter pw = null;
        try {
            if (saveFile.isFile()) {
                saveFile.delete();
            }
            saveFile.createNewFile();
            fw = new FileWriter(saveFile);
            bw = new BufferedWriter(fw);
            pw = new PrintWriter(bw);

            pw.write(data);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (pw != null) {
                    pw.close();
                }

                if (bw != null) {
                    bw.close();
                }

                if(fw != null) {
                    fw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getManifestString(int priorValue, boolean omsTheme) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>\n" +
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    package=\"com.privset.frameworkresoverlay\"\n" +
                "    android:versionName=\"1.0\">\n" +
                "    <overlay android:targetPackage=\"android" + (!omsTheme ? ("\" android:priority=\"" + priorValue) : "") + (isOreo() ? "\" android:isStatic=\"false" : "") + "\"/>\n" +
                "    <application android:label=\"com.privset.frameworkresoverlay\" android:hasCode=\"false\">\n    </application>\n" +
                "</manifest>";
    }

    public static boolean invalidBoolean(String s) {
        return !(s.equals("true") || s.equals("false"));
    }

    public static boolean invalidInteger(String s) {
        return !s.matches("[-+]?\\d*\\.?\\d+");
    }

    private static String listToString(List<String> inp) {
        StringBuilder res = new StringBuilder();
        if (inp != null && inp.size() > 0) {
            for (String st : inp) {
                res.append(st);
            }
        }
        return res.toString();
    }

    public static void dispResDialog(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Restart device ?")
                .setMessage("To apply changes, You need to restart your device")
                .setPositiveButton("Yes, now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // reboot device
                        restartDevice();
                    }
                })
                .setNegativeButton("No, later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private static boolean hasOMScommands(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("otherPreferences", Context.MODE_PRIVATE);
        boolean usesOMScommands = false;
        if (prefs.contains("has_oms_commands")) {
            usesOMScommands = prefs.getBoolean("has_oms_commands", false);
        } else {
            String result = listToString(Shell.SU.run("cmd overlay"));
            if (!result.equals("")) {
                if (result.contains("The overlay manager has already been initialized.") ||
                        result.contains("Overlay manager (overlay) commands:")) {
                    usesOMScommands = true;
                }
            }
            prefs.edit().putBoolean("has_oms_commands", usesOMScommands).commit();
        }
        return usesOMScommands;
    }

    private static boolean hasOMS(Context context) {
        try {
            Class<?> overlayClass = Class.forName("android.content.om.IOverlayManager");
            if (overlayClass != null) {
                ActivityManager activityManager = (ActivityManager)
                        context.getSystemService(Context.ACTIVITY_SERVICE);
                assert activityManager != null;
                List<ActivityManager.RunningServiceInfo> services =
                        activityManager.getRunningServices(Integer.MAX_VALUE);

                for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
                    if (runningServiceInfo.service.getClassName().equals(overlayClass.getName())) {
                        return true;
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public static void enableOverlay(Context ctx, String ovPkgName) {
        if (isOreo() || hasOMScommands(ctx)) {
            Shell.SU.run("cmd overlay enable " + ovPkgName);
        } else {
            String cmdEnable = "CLASSPATH=" + ctx.getApplicationInfo().publicSourceDir
                    + " /system/bin/app_process /system/bin " + ctx.getPackageName() +
                    ".NougatOmsRoot enable";
            Shell.SU.run(cmdEnable);
        }
    }

    public static void disableOverlay(Context ctx, String ovPkgName) {
        if (isOreo() || hasOMScommands(ctx)) {
            Shell.SU.run("cmd overlay disable " + ovPkgName);
        } else {
            String cmdDisable = "CLASSPATH=" + ctx.getApplicationInfo().publicSourceDir
                    + " /system/bin/app_process /system/bin " + ctx.getPackageName() +
                    ".NougatOmsRoot disable";
            Shell.SU.run(cmdDisable);
        }
    }

    public static void changePriority(Context ctx, String ovPkgName,
                                      boolean highestPrio) {
        if (isOreo() || hasOMScommands(ctx)) {
            if (highestPrio) {
                Shell.SU.run("cmd overlay set-priority " + ovPkgName + " highest");
            } else {
                Shell.SU.run("cmd overlay set-priority " + ovPkgName + " lowest");
            }
        } else {
            String cmdPrio = "CLASSPATH=" + ctx.getApplicationInfo().publicSourceDir
                    + " /system/bin/app_process /system/bin " + ctx.getPackageName() +
                    ".NougatOmsRoot " + (highestPrio ? "setHighestPriority" : "setLowestPriority");
            Shell.SU.run(cmdPrio);
        }
    }
}
