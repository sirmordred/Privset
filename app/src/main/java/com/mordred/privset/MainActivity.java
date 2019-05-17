package com.mordred.privset;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import com.mordred.zipsigner.ZipSigner;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "PrivsetBuilder";

    private Context ctx;

    private static final int PERMISSION_REQUEST_CODE = 54;

    private String manifestPath;
    private String aaptPath;
    private String zipalignerPath;

    private String workspacePath;

    private String resDir;
    private String resFile;

    private String resMainDir;
    private String targetApk;
    private String targetApkSigned;

    public static ArrayList<ConfigNodes> configNodes;
    public ArrayList<TempConfigNodes> modifiedConfigNodes;

    private ProgressDialog pd = null;
    private ProgressDialog pd2 = null;

    private FloatingActionButton runBtn;

    private String vendor_location = FileTools.LEGACY_NEXUS_DIRECTORY;
    private String vendor_partition = FileTools.VENDOR_DIRECTORY;

    private String overlayApk;

    public SharedPreferences preferences = null;

    public SharedPreferences secondaryPreferences = null;

    public RecyclerView mRecyclerView;
    public LinearLayoutManager llMng;
    public RowAdapter adapter;

    public boolean omsDevice;
    public boolean isNexus;

    private boolean isSorting = false;

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ctx == null) {
            ctx = getApplicationContext();
        }

        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        }

        if (secondaryPreferences == null) {
            secondaryPreferences = ctx.getSharedPreferences("otherPreferences", Context.MODE_PRIVATE);
        }

        configNodes = new ArrayList<>();
        modifiedConfigNodes = new ArrayList<>();

        String rootPath = ctx.getFilesDir().getAbsolutePath();
        workspacePath = rootPath + File.separator + "workspace";

        aaptPath = workspacePath + File.separator + "aapt";
        zipalignerPath = workspacePath + File.separator + "zipalign";
        manifestPath = workspacePath + File.separator + "AndroidManifest.xml";

        targetApk = workspacePath + File.separator + "frameworkresoverlay.apk";

        targetApkSigned = workspacePath + File.separator + "frameworkresoverlay-signed.apk";

        resMainDir =  workspacePath + File.separator + "res" + File.separator;
        resDir = resMainDir + "values";

        resFile = resDir + File.separator + "config.xml";

        //btn = findViewById(R.id.button);
        runBtn = findViewById(R.id.fab);

        mRecyclerView = findViewById(R.id.recyclerView);
        llMng = new LinearLayoutManager(MainActivity.this, OrientationHelper.VERTICAL, false);
        mRecyclerView.setLayoutManager(llMng);
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        new AsyncTask<Void,Void,Void>() {
            @Override
            protected void onPreExecute() {
                if (runBtn.isEnabled()) {
                    runBtn.setEnabled(false);
                }
                Log.i(TAG, "App is opened and configuring settings");
                if (pd == null) {
                    pd = new ProgressDialog(MainActivity.this);
                    pd.setMessage("Opening. Please wait...");
                    pd.setIndeterminate(true);
                    pd.setCancelable(false);
                }
                pd.show();
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                omsDevice = FileTools.systemSupportsOMS(ctx);
                if (omsDevice) {
                    Log.i(TAG,"Device supports OMS");
                } else {
                    Log.i(TAG,"Device supports RRO");
                }

                // set overlay apk destination paths
                if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P && !omsDevice) {
                    isNexus = FileTools.isGoogleDevice();
                    if (isNexus) {
                        Log.i(TAG, "Device is RRO and Google device");
                        overlayApk = vendor_partition + "frameworkresoverlay.apk";
                    } else {
                        Log.i(TAG, "Device is RRO but not Google device");
                        overlayApk = vendor_location + "frameworkresoverlay.apk";
                    }
                }

                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                    // P support
                    Log.i(TAG, "OS version is Android Pie");
                    if(!new File(FileTools.P_DIR + "frameworkresoverlay.apk").isFile()) {
                        // delete whole sharedPreferences
                        preferences.edit().clear().commit();
                    }
                } else {
                    Log.i(TAG, "OS version is not Android Pie");
                    // pre-P support
                    if (omsDevice) {
                        // check if overlay installed (via package manager)
                        // if not installed restart whole process
                        if (!FileTools.isPackageInstalled(ctx,FileTools.overlayPackageName)) {
                            preferences.edit().clear().commit();
                        }
                    } else {
                        // if there is no overlay apk in overlay folder restart whole process
                        if(!new File(overlayApk).isFile()) {
                            // delete whole sharedPreferences
                            preferences.edit().clear().commit();
                        }
                    }
                }

                // Phase 2
                File dir = new File(workspacePath);
                if(!dir.exists()) {
                    if (dir.mkdir()) {
                        Log.i(TAG, "Creating workspace dir");
                        dir.setWritable(true);
                        dir.setReadable(true);
                        dir.setExecutable(true);
                    }
                }

                File resDirectory = new File(resDir);
                if(!resDirectory.exists()) {
                    if (resDirectory.mkdirs()) {
                        Log.i(TAG, "Creating res dir");
                        resDirectory.setReadable(true);
                        resDirectory.setWritable(true);
                    }
                }

                File aaptFile = new File(aaptPath);
                File zipalignerFile = new File(zipalignerPath);

                if (!aaptFile.isFile()) {
                    Log.i(TAG, "Copying AAPT compiler binary from asset");
                    copyFromAsset(ctx, getPreferredAapt(), aaptPath);
                    if (aaptFile.isFile()) {
                        aaptFile.setExecutable(true,true);
                    }
                }

                if (!zipalignerFile.isFile()) {
                    Log.i(TAG, "Copying Zipaligner binary from asset");
                    copyFromAsset(ctx, getPreferredZipaligner(), zipalignerPath);
                    if (zipalignerFile.isFile()) {
                        zipalignerFile.setExecutable(true,true);
                    }
                }

                if (!(new File(manifestPath).isFile())) {
                    Log.i(TAG, "Creating manifest.xml");
                    FileTools.printToXml(FileTools.getManifestString(secondaryPreferences.getInt("PRIOR",50), omsDevice), manifestPath);
                }

                configureBaseConfig();
                return null;
            }

            @Override
            protected void onPostExecute(Void t) {
                // create user interface
                adapter = new RowAdapter(configNodes, ctx);
                mRecyclerView.setAdapter(adapter);

                if (pd != null && pd.isShowing()) {
                    pd.dismiss();
                }

                if (!runBtn.isEnabled()) {
                    runBtn.setEnabled(true);
                }

                copyRescueSystem();
                super.onPostExecute(t);
            }
        }.execute();


        runBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (runBtn.isEnabled()) {
                    runBtn.setEnabled(false);
                }
                boolean isUsable = true;
                String warningMessage = "";

                if (FileTools.requestRoot()) {
                    Log.i(TAG, "Root is not supported on this device");
                    warningMessage = "Sorry, App requires root privilege !";
                    isUsable = false;
                }

                if (isUsable) {
                    if (FileTools.systemSupportsCMTE(ctx)) {
                        Log.i(TAG, "Detected uncompatible CMTE");
                        warningMessage = "Sorry, App does not work on systems which have Cyanogenmod Theme Engine(CMTE), you can try to change your ROM";
                        isUsable = false;
                    }
                }

                // deploy
                if (isUsable) {
                    Log.i(TAG, "Switching to compilation and deployment phase");
                    configureUserInput();
                } else {
                    Toast.makeText(ctx,warningMessage,Toast.LENGTH_LONG).show();
                    if (!runBtn.isEnabled()) {
                        runBtn.setEnabled(true);
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // dont do anything
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    if (!isSorting) {
                        isSorting = true;
                        adapter.sortAndShow(newText);
                        isSorting = false;
                    }
                }
                return false;
            }
        });
        return true;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_list_modified_settings:
                if (modifiedConfigNodes.size() > 0) {
                    Intent intent4 = new Intent(MainActivity.this, ModifiedSettingsActivity.class);
                    intent4.putExtra("configNodeList", modifiedConfigNodes);
                    startActivity(intent4);
                    finish();
                } else {
                    Toast.makeText(ctx,"You haven't change any setting yet!",Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.menu_settings:
                // open settings activity
                Intent intent6 = new Intent(MainActivity.this, Settings.class);
                startActivity(intent6);
                finish();
                break;
            case R.id.menu_info:
                // open info activity
                Intent intent5 = new Intent(MainActivity.this, Info.class);
                startActivity(intent5);
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("StaticFieldLeak")
    private void configureUserInput() {
        new AsyncTask<Void,Void,Void>() {
            boolean isCreatable = false;
            @Override
            protected void onPreExecute() {
                if (runBtn.isEnabled()) {
                    runBtn.setEnabled(false);
                }
                Log.i(TAG, "Compilation process is started");
                if (pd2 == null) {
                    pd2 = new ProgressDialog(MainActivity.this);
                    pd2.setMessage("Applying changes. Please wait...");
                    pd2.setIndeterminate(true);
                    pd2.setCancelable(false);
                }
                pd2.show();
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {

                // produce config.xml
                StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                        "<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">\n");

                for (ConfigNodes iterNodes : configNodes) {
                    if (iterNodes.getNewValue().equals("")) {
                        if (!iterNodes.getNodeFromPref().equals(iterNodes.getNodeFromDefPref())) {
                            xml.append("    ").append(iterNodes.getXmlStringFromPref());
                            isCreatable = true;
                        }
                    } else {
                        // handle invalidBoolean and invalidString
                        if (iterNodes.getType().equals("bool")) {
                            if (FileTools.invalidBoolean(iterNodes.getNewValue())) {
                                continue;
                            }
                        } else if (iterNodes.getType().equals("integer")) {
                            if (FileTools.invalidInteger(iterNodes.getNewValue())) {
                                continue;
                            }
                        }
                        if (!iterNodes.getNewValue().equals(iterNodes.getNodeFromPref()))  {
                            if (!iterNodes.getNewValue().equals(iterNodes.getNodeFromDefPref())) {
                                xml.append("    ").append(iterNodes.getXmlStringFromEdx());
                                iterNodes.setColor(Color.parseColor("#FF5353"));
                            } else {
                                iterNodes.delNodeFromPref();
                                iterNodes.setColor(Color.parseColor("#80CBC6"));
                            }
                            iterNodes.setNodeToPref(iterNodes.getNewValue());
                            isCreatable = true;
                        }
                    }
                }
                xml.append("</resources>");

                if (isCreatable) {
                    // save the file to res/values/config.xml
                    FileTools.printToXml(xml.toString(),resFile);
                    Log.i(TAG, "User changes are valid and creating res.xml");
                }
                //TODOX

                // compile
                if (new File(resFile).isFile()) {
                    String[] args = {
                            aaptPath, //The location of AAPT
                            "p",
                            "-M",manifestPath,
                            "-S",resMainDir,
                            "-I","/system/framework/framework-res.apk",
                            "-F",targetApk,
                            "-f","--include-meta-data",
                            "--auto-add-overlay"
                    };
                    // building overlay apk file
                    executeCmd(args, targetApk, true);
                    Log.i(TAG, "Compiling overlay with AAPT");
                }

                File targetApkFile = new File(targetApk);

                // Sign and align apk
                if (targetApkFile.isFile()) {
                    Log.i(TAG, "Overlay is compiled with AAPT succesfully");
                    try {
                        Log.i(TAG, "Signing created overlay");
                        new ZipSigner().signZip(targetApk, targetApkSigned);
                    } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                        Log.i(TAG, "Error while signing created overlay");
                        e.printStackTrace();
                    }
                    // if signing operation success delete unsigned apk and make signed apk path as default path
                    if (new File(targetApkSigned).isFile()) {
                        Log.i(TAG, "Created overlay has been succesfully signed");
                        // delete unsigned apk first
                        if (targetApkFile.delete()) {
                            Log.i(TAG, "Unsigned unnecessary overlay apk has been deleted succesfully");
                            // zipalign apk
                            Log.i(TAG, "Aligning signed overlay");
                            String[] alignArgs = {zipalignerPath, "4", targetApkSigned, targetApk};
                            executeCmd(alignArgs, targetApk, true);
                            if (!targetApkFile.isFile()) {
                                Log.i(TAG, "Aligning has been failed, anyway keep going");
                                // aligning apk failed but we dont care, just lets keep move on
                                FileTools.move(targetApkSigned,targetApk,false);
                            }
                        }
                    }
                }

                // deploy apk
                if (targetApkFile.isFile()) {
                    Log.i(TAG, "Overlay has been succesfully created now deploying");
                    deployApk();
                } else {
                    Log.i(TAG, "Overlay has been failed");
                }
                // finish
                return null;
            }

            @Override
            protected void onPostExecute(Void t) {
                if (pd2 != null && pd2.isShowing()) {
                    pd2.dismiss();
                }
                // show reboot dialog to user
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                    // Pie support
                    if (isCreatable && new File(FileTools.P_DIR + "frameworkresoverlay.apk").isFile()) {
                        Log.i(TAG, "Framework changes are applied succesfully, showing reboot dialog to user");
                        Toast.makeText(ctx,"Changes are applied succesfully",Toast.LENGTH_LONG).show();
                        FileTools.dispResDialog(MainActivity.this);
                    } else {
                        Log.i(TAG, "Framework changes are not applied, failed");
                        // there are errors so keep
                    }
                } else {
                    // pre-P support
                    if (omsDevice) {
                        // check if overlay installed
                        if (isCreatable && FileTools.isPackageInstalled(ctx,FileTools.overlayPackageName)) {
                            Log.i(TAG, "Framework changes are applied succesfully, showing reboot dialog to user");
                            Toast.makeText(ctx,"Changes are applied succesfully",Toast.LENGTH_LONG).show();
                            FileTools.dispResDialog(MainActivity.this);
                        } else {
                            Log.i(TAG, "Framework changes are not applied, failed");
                            // there are errors so keep
                        }
                    } else {
                        if (isCreatable && new File(overlayApk).isFile()) {
                            Log.i(TAG, "Framework changes are applied succesfully, showing reboot dialog to user");
                            Toast.makeText(ctx,"Changes are applied succesfully",Toast.LENGTH_LONG).show();
                            FileTools.dispResDialog(MainActivity.this);
                        } else {
                            Log.i(TAG, "Framework changes are not applied, failed");
                            // there are errors so keep
                        }
                    }
                }

                if (!runBtn.isEnabled()) {
                    runBtn.setEnabled(true);
                }

                super.onPostExecute(t);
            }
        }.execute();
    }

    private void deployApk() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // P support
            FileTools.mountSystemAsRW(ctx);
            FileTools.move(targetApk, FileTools.P_DIR + "frameworkresoverlay.apk",true);
            FileTools.setPerm(644, FileTools.P_DIR + "frameworkresoverlay.apk");
            FileTools.mountSystemAsRO(ctx);
        } else {
            // pre-P support
            if (omsDevice) { // while checking omsDevice, if device is Oreo, this boolean is always true
                // install apk with root
                FileTools.installWithRoot(targetApk);
                FileTools.enableOverlay(ctx, FileTools.overlayPackageName);
            } else {
                FileTools.mountSystemAsRW(ctx);
                if (isNexus) {
                    // For Nexus devices
                    FileTools.mountVendorAsRW(ctx);
                    String vendor_symlink = FileTools.PIXEL_NEXUS_DIRECTORY;
                    FileTools.createNewFolder(vendor_symlink,true);
                    FileTools.createNewFolder(vendor_partition,true);
                    // On nexus devices, put framework overlay to /vendor/overlay/
                    FileTools.move(targetApk, overlayApk,true);
                    FileTools.setPermRec(644, vendor_symlink);
                    FileTools.setPermRec(644, vendor_partition);
                    FileTools.setPerm(755, vendor_symlink);
                    FileTools.setPerm(755, vendor_partition);
                    FileTools.setCtx(vendor_symlink);
                    FileTools.setCtx(vendor_partition);
                    FileTools.mountVendorAsRO(ctx);
                } else {
                    // For Non-Nexus devices
                    FileTools.createNewFolder(vendor_location,true);
                    FileTools.move(targetApk, overlayApk,true);
                    FileTools.setPermRec(644, vendor_location);
                    FileTools.setPerm(755, vendor_location);
                    FileTools.setCtx(vendor_location);
                }
                FileTools.mountSystemAsRO(ctx);
            }
        }
    }

    private void executeCmd(String[] command, String targetP, boolean forceWithSu) {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(command);
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (process != null) {
                process.destroy();
            }

            if (forceWithSu) {
                // if its not compiled with normal exec, try with also execWithSu
                if (!new File(targetP).isFile()) {
                    Shell.SU.run(command);
                }
            }

        }
    }

    // for copy,write io operations
    private void copyFromAsset(Context ct, String fileName, String targetPath) {
        try (InputStream in = ct.getAssets().open(fileName);
             OutputStream out = new FileOutputStream(targetPath)){
            byte[] buffer = new byte[1024];
            int read;
            while((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch(IOException e) {
            Log.e("tag", "Failed to copy asset file: ", e);
        }
    }

    private void configureBaseConfig() {
        for (String sBool : ConfigStrings.boolConfigs) {
            int resId = Resources.getSystem().getIdentifier(sBool,"bool", "android");
            if (resId != 0) {
                if (Resources.getSystem().getBoolean(resId)) {
                    configNodes.add(new ConfigNodes(sBool,"true","bool", preferences));
                } else {
                    configNodes.add(new ConfigNodes(sBool,"false","bool", preferences));
                }
            }
        }
        for (String sInt : ConfigStrings.intConfigs) {
            int resId2 = Resources.getSystem().getIdentifier(sInt,"integer", "android");
            if (resId2 != 0) {
                configNodes.add(new ConfigNodes(sInt,String.valueOf(
                        Resources.getSystem().getInteger(resId2)),"integer", preferences));
            }
        }
        for (String sString : ConfigStrings.stringConfigs) {
            int resId3 = Resources.getSystem().getIdentifier(sString,"string", "android");
            if (resId3 != 0) {
                configNodes.add(new ConfigNodes(sString,
                        Resources.getSystem().getString(resId3),"string", preferences));
            }
        }
        for (ConfigNodes cnfN: configNodes) {
            if (cnfN.getColor() == Color.parseColor("#FF5353")) {
                String mTitle2 = cnfN.getConfigTitle();
                String mDescription2 = ctx.getResources().getString(ctx.getResources().getIdentifier(cnfN.configString,"string",ctx.getPackageName()));
                String mDefaultValue2 = cnfN.getNodeFromDefPref();
                String mCurrentValue2 = cnfN.getNodeFromPref();
                modifiedConfigNodes.add(new TempConfigNodes(mTitle2,mDescription2,mDefaultValue2,mCurrentValue2));
            }
        }
        // sort it
        Collections.sort(configNodes,new Comparator<ConfigNodes>() {
            public int compare(ConfigNodes conf1,ConfigNodes conf2){
                return conf1.getConfigTitle().compareTo(conf2.getConfigTitle());
            }
        });
    }

    private String getPreferredAapt() {
        String finalStr;
        if (!Arrays.toString(Build.SUPPORTED_ABIS).contains("86")) {
            if (!Arrays.asList(Build.SUPPORTED_64_BIT_ABIS).isEmpty()) {
                finalStr = "aapt-x64";
            } else {
                finalStr = "aapt-x32";
            }
        } else {
            finalStr = "aapt-x86";
        }
        return finalStr;
    }

    private String getPreferredZipaligner() {
        String finalStr;
        if (!Arrays.toString(Build.SUPPORTED_ABIS).contains("86")) {
            if (!Arrays.asList(Build.SUPPORTED_64_BIT_ABIS).isEmpty()) {
                finalStr = "zipalign64";
            } else {
                finalStr = "zipalign";
            }
        } else {
            finalStr = "zipalign86";
        }
        return finalStr;
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Exit from app ?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // exit app
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    public void copyRescueSystem() {
        if (checkPermission()) {
            copyRescuePkg();
        } else {
            requestPermission();
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void copyRescuePkg() {
        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                File rescuePkgDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                        File.separator + "PrivsetRescueSystem");
                if (!rescuePkgDir.exists()) {
                    rescuePkgDir.mkdir();
                }
                File rescuePkg = new File(rescuePkgDir,"PrivsetRescue.zip");
                if (!rescuePkg.isFile()) {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P && !omsDevice) {
                        // P support
                        copyFromAsset(ctx, "rescue-oms.dat",
                                rescuePkg.getAbsolutePath());
                    } else {
                        // pre-P support
                        if (omsDevice) {
                            copyFromAsset(ctx, "rescue-oms.dat",
                                    rescuePkg.getAbsolutePath());
                        } else {
                            copyFromAsset(ctx, "rescue-rro.dat",
                                    rescuePkg.getAbsolutePath());
                        }
                    }
                }
                return null;
            }
            @Override
            protected void onPostExecute(Void t) {
                showOneTimeAd();
                super.onPostExecute(t);
            }
        }.execute();
    }

    public void showOneTimeAd() {
        if (secondaryPreferences.getBoolean("APP_START",true)) {
            AdmobAd adService = new AdmobAd(ctx);
            adService.showAd();
            secondaryPreferences.edit().putBoolean("APP_START",false).commit();
        }
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.
                PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Write External Storage permission allows us to " +
                    "do store rescue package. Please allow this permission in App Settings.",
                    Toast.LENGTH_LONG).show();
            showOneTimeAd();
        } else {
            Toast.makeText(MainActivity.this, "Write External Storage permission allows us to " +
                            "do store rescue package. Please allow this permission",
                    Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission Granted, Now you can copy rescue package .");
                    copyRescuePkg();
                } else {
                    Log.i(TAG, "Permission Denied, You cannot copy rescue package .");
                    Toast.makeText(ctx,"Failed, Rescue package cannot be copied, " +
                            "You should grant required permission",Toast.LENGTH_LONG).show();
                }
                showOneTimeAd();
                break;
        }
    }
}
