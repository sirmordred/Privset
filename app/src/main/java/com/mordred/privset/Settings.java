package com.mordred.privset;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;

import static com.mordred.privset.FileTools.DATA_RESOURCE_DIRECTORY;
import static com.mordred.privset.FileTools.LEGACY_NEXUS_DIRECTORY;
import static com.mordred.privset.FileTools.PIXEL_NEXUS_DIRECTORY;
import static com.mordred.privset.FileTools.P_DIR;
import static com.mordred.privset.FileTools.VENDOR_DIRECTORY;

/**
 * Created by mordred on 07.05.2017.
 */

public class Settings extends AppCompatActivity {
    private ProgressDialog pd4 = null;
    private ProgressDialog pd5 = null;
    private Context ctx2;
    private boolean omsDev;
    private String manifestPath2;

    private TextView txtViewPrio;
    private TextView txtViewRes;

    public SharedPreferences secondPreferences2 = null;

    private AdView mAdView2;

    private String prioChangeDialogMessage;
    private String prioTextMessage;
    private String prioEdxHint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ActionBar abr2 = getActionBar();
        if (abr2 != null) {
            abr2.setDisplayHomeAsUpEnabled(true);
        }
        
        if (ctx2 == null) {
            ctx2 = getApplicationContext();
        }

        // banner ad
        mAdView2 = findViewById(R.id.adview_banner2);
        AdRequest adRequest = new AdRequest.Builder()
                .build();
        mAdView2.loadAd(adRequest);

        manifestPath2 = ctx2.getFilesDir().getAbsolutePath() + File.separator + "workspace" + File.separator + "AndroidManifest.xml";

        if (secondPreferences2 == null) {
            secondPreferences2 = ctx2.getSharedPreferences("otherPreferences", Context.MODE_PRIVATE);
        }

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // Pie support
            prioEdxHint = "Valid values are 0 and 1";
            prioTextMessage = "Change overlay priority\n1 for Highest priority\n0 for Lowest priority";
            prioChangeDialogMessage = "Enter custom priority number (Only 1 (highest priority) and 0 (lowest priority) values are valid";
        } else {
            // Pre-P support
            omsDev = FileTools.systemSupportsOMS(ctx2);

            if (omsDev) {
                prioEdxHint = "Valid values are 0 and 1";
                prioTextMessage = "Change overlay priority\n1 for Highest priority\n0 for Lowest priority";
                prioChangeDialogMessage = "Enter custom priority number (Only 1 (highest priority) and 0 (lowest priority) values are valid";
            } else {
                prioEdxHint = "Valid range 1...240";
                prioTextMessage = "Change overlay priority (1...240)\n(Default priority is 50)\n(Current priority is " + secondPreferences2.getInt("PRIOR",50)  + ")";
                prioChangeDialogMessage = "Enter custom priority number (WARNING: Changing priority will reset your previously installed custom configurations, Do not change priority number unless you face issue)";
            }
        }

        RadioGroup radioGroup =  findViewById(R.id.overlaySysType);

        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            // Pie support
            radioGroup.check(R.id.radioOms);
        } else {
            // pre-P support
            if (omsDev) {
                radioGroup.check(R.id.radioOms);
            } else {
                radioGroup.check(R.id.radioRro);
            }
        }

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioOms) {
                    secondPreferences2.edit().putBoolean("supports_oms", true).commit();
                    Toast.makeText(Settings.this, "OMS system is selected and configured", Toast.LENGTH_LONG).show();
                } else if (checkedId == R.id.radioRro) {
                    secondPreferences2.edit().putBoolean("supports_oms", false).commit();
                    Toast.makeText(Settings.this, "RRO system is selected and configured", Toast.LENGTH_LONG).show();
                }
            }
        });

        txtViewRes = findViewById(R.id.txtRes);
        txtViewRes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (txtViewRes.isEnabled()) {
                    txtViewRes.setEnabled(false);
                }

                if (!FileTools.requestRoot()) {
                    new AlertDialog.Builder(Settings.this)
                            .setMessage("Restart Installed Custom Configurations ?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    reset();
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    if (!txtViewRes.isEnabled()) {
                                        txtViewRes.setEnabled(true);
                                    }
                                }
                            })
                            .show();
                } else {
                    Toast.makeText(ctx2,"Sorry, App requires root privilege",Toast.LENGTH_LONG).show();
                }

                if (!txtViewRes.isEnabled()) {
                    txtViewRes.setEnabled(true);
                }
            }
        });

        txtViewPrio = findViewById(R.id.priorityText);
        txtViewPrio.setText(prioTextMessage);
        txtViewPrio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (txtViewPrio.isEnabled()) {
                    txtViewPrio.setEnabled(false);
                }

                if (!FileTools.requestRoot()) {
                    LayoutInflater inflater = Settings.this.getLayoutInflater();
                    final View dialogView = inflater.inflate(R.layout.priority_dialog, null);
                    final EditText edt = dialogView.findViewById(R.id.edit1);
                    edt.setHint(prioEdxHint);
                    new AlertDialog.Builder(Settings.this)
                            .setView(dialogView)
                            .setMessage(prioChangeDialogMessage)
                            .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                                @SuppressLint("StaticFieldLeak")
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    final int nPriorityVal = getNewPriorityValue(edt.getText().toString().trim());
                                    if (dialog != null) {
                                        dialog.dismiss();
                                    }
                                    changeOverlayPriority(nPriorityVal);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    //pass
                                    if (dialog != null) {
                                        dialog.dismiss();
                                    }
                                    if (!txtViewPrio.isEnabled()) {
                                        txtViewPrio.setEnabled(true);
                                    }

                                }
                            })
                            .show();
                } else {
                    Toast.makeText(ctx2,"Sorry, App requires root privilege",Toast.LENGTH_LONG).show();
                }

                if (!txtViewPrio.isEnabled()) {
                    txtViewPrio.setEnabled(true);
                }
            }
        });
    }

    /** Called when leaving the activity */
    @Override
    public void onPause() {
        if (mAdView2 != null) {
            mAdView2.pause();
        }
        super.onPause();
    }

    /** Called when returning to the activity */
    @Override
    public void onResume() {
        super.onResume();
        if (mAdView2 != null) {
            mAdView2.resume();
        }
    }

    /** Called before the activity is destroyed */
    @Override
    public void onDestroy() {
        if (mAdView2 != null) {
            mAdView2.destroy();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                Intent intent4 = new Intent(Settings.this, MainActivity.class);
                startActivity(intent4);
                finish();
                break;
            case R.id.menu_info:
                // open info activity
                Intent intent3 = new Intent(Settings.this, Info.class);
                startActivity(intent3);
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("StaticFieldLeak")
    public void changeOverlayPriority(int mPrioVal) {
        new AsyncTask<Integer,Void,Integer>() {
            @Override
            protected void onPreExecute() {
                if (pd5 == null) {
                    pd5 = new ProgressDialog(Settings.this);
                    pd5.setMessage("Changing priority. Please wait...");
                    pd5.setIndeterminate(true);
                    pd5.setCancelable(false);
                }
                pd5.show();
                super.onPreExecute();
            }

            @Override
            protected Integer doInBackground(Integer... params) {
                int newPriorityVal = params[0];
                if (newPriorityVal != -1 && newPriorityVal != -2) {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                        if (newPriorityVal == 0) {
                            FileTools.changePriority(ctx2, FileTools.overlayPackageName, false);
                        } else if (newPriorityVal == 1) {
                            FileTools.changePriority(ctx2, FileTools.overlayPackageName, true);
                        }
                    } else {
                        // pre-P support
                        if (omsDev) {
                            if (newPriorityVal == 0) {
                                FileTools.changePriority(ctx2, FileTools.overlayPackageName, false);
                            } else if (newPriorityVal == 1) {
                                FileTools.changePriority(ctx2, FileTools.overlayPackageName, true);
                            }
                        } else {
                            // change manifest and save new priority val
                            FileTools.printToXml(FileTools.getManifestString(newPriorityVal, omsDev), manifestPath2);
                            secondPreferences2.edit().putInt("PRIOR", newPriorityVal).commit();

                            // reset previously installed configurations
                            FileTools.mountSystemAsRW(ctx2);
                            FileTools.mountDataAsRW(ctx2);
                            FileTools.mountVendorAsRW(ctx2);
                            FileTools.delete(DATA_RESOURCE_DIRECTORY +
                                    "overlays.list");

                            FileTools.delete(LEGACY_NEXUS_DIRECTORY + "frameworkresoverlay.apk");
                            FileTools.delete(PIXEL_NEXUS_DIRECTORY + "frameworkresoverlay.apk");
                            FileTools.delete(VENDOR_DIRECTORY + "frameworkresoverlay.apk");
                            String legacy_resource_idmap =
                                    (LEGACY_NEXUS_DIRECTORY.substring(1, LEGACY_NEXUS_DIRECTORY.length()) +
                                            "frameworkresoverlay")
                                            .replace("/", "@") + ".apk@idmap";
                            String pixel_resource_idmap =
                                    (PIXEL_NEXUS_DIRECTORY.substring(1, PIXEL_NEXUS_DIRECTORY.length()) +
                                            "frameworkresoverlay")
                                            .replace("/", "@") + ".apk@idmap";
                            String vendor_resource_idmap =
                                    (VENDOR_DIRECTORY.substring(1, VENDOR_DIRECTORY.length()) +
                                            "frameworkresoverlay")
                                            .replace("/", "@") + ".apk@idmap";

                            FileTools.delete(DATA_RESOURCE_DIRECTORY +
                                    legacy_resource_idmap);
                            FileTools.delete(DATA_RESOURCE_DIRECTORY +
                                    pixel_resource_idmap);
                            FileTools.delete(DATA_RESOURCE_DIRECTORY +
                                    vendor_resource_idmap);
                            FileTools.mountVendorAsRO(ctx2);
                            FileTools.mountDataAsRO(ctx2);
                            FileTools.mountSystemAsRO(ctx2);
                        }
                    }
                }
                return newPriorityVal;
            }

            @Override
            protected void onPostExecute(Integer newestPriorVal) {
                if (pd5 != null && pd5.isShowing()) {
                    pd5.dismiss();
                }
                if (newestPriorVal == -1) {
                    Toast.makeText(ctx2,"Failed, You entered invalid priority number",Toast.LENGTH_LONG).show();
                } else if (newestPriorVal == -2) {
                    Toast.makeText(ctx2,"Failed, You entered priority number same as previous one",Toast.LENGTH_LONG).show();
                } else {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        if (!omsDev) {
                            txtViewPrio.setText("Change overlay priority (1...240) (Default priority is 50) (Current priority is " + secondPreferences2.getInt("PRIOR",50)  + ")");
                        }
                    }
                    Toast.makeText(ctx2,"Success, Priority is changed succesfully",Toast.LENGTH_LONG).show();
                    FileTools.dispResDialog(Settings.this);
                }
                if (!txtViewPrio.isEnabled()) {
                    txtViewPrio.setEnabled(true);
                }
                super.onPostExecute(newestPriorVal);
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    public void reset() {
        new AsyncTask<Void,Void,Void>() {
            @Override
            protected void onPreExecute() {
                if (txtViewRes.isEnabled()) {
                    txtViewRes.setEnabled(false);
                }
                if (pd4 == null) {
                    pd4 = new ProgressDialog(Settings.this);
                    pd4.setMessage("Restarting installed configurations. Please wait...");
                    pd4.setIndeterminate(true);
                    pd4.setCancelable(false);
                }
                pd4.show();
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Void... params) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                    // P support
                    FileTools.mountSystemAsRW(ctx2);
                    FileTools.delete(P_DIR + "frameworkresoverlay.apk");
                    FileTools.mountSystemAsRO(ctx2);
                } else {
                    // pre-P support
                    if (omsDev) {
                        FileTools.disableOverlay(ctx2, FileTools.overlayPackageName);
                        FileTools.uninstallWithRoot(ctx2,FileTools.overlayPackageName);
                    } else {
                        FileTools.mountSystemAsRW(ctx2);
                        FileTools.mountDataAsRW(ctx2);
                        FileTools.mountVendorAsRW(ctx2);
                        FileTools.delete(DATA_RESOURCE_DIRECTORY +
                                "overlays.list");

                        FileTools.delete(LEGACY_NEXUS_DIRECTORY + "frameworkresoverlay.apk");
                        FileTools.delete(PIXEL_NEXUS_DIRECTORY + "frameworkresoverlay.apk");
                        FileTools.delete(VENDOR_DIRECTORY + "frameworkresoverlay.apk");
                        String legacy_resource_idmap =
                                (LEGACY_NEXUS_DIRECTORY.substring(1, LEGACY_NEXUS_DIRECTORY.length()) +
                                        "frameworkresoverlay")
                                        .replace("/", "@") + ".apk@idmap";
                        String pixel_resource_idmap =
                                (PIXEL_NEXUS_DIRECTORY.substring(1, PIXEL_NEXUS_DIRECTORY.length()) +
                                        "frameworkresoverlay")
                                        .replace("/", "@") + ".apk@idmap";
                        String vendor_resource_idmap =
                                (VENDOR_DIRECTORY.substring(1, VENDOR_DIRECTORY.length()) +
                                        "frameworkresoverlay")
                                        .replace("/", "@") + ".apk@idmap";

                        FileTools.delete(DATA_RESOURCE_DIRECTORY +
                                legacy_resource_idmap);
                        FileTools.delete(DATA_RESOURCE_DIRECTORY +
                                pixel_resource_idmap);
                        FileTools.delete(DATA_RESOURCE_DIRECTORY +
                                vendor_resource_idmap);
                        FileTools.mountVendorAsRO(ctx2);
                        FileTools.mountDataAsRO(ctx2);
                        FileTools.mountSystemAsRO(ctx2);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void t) {
                if (pd4 != null && pd4.isShowing()) {
                    pd4.dismiss();
                }
                Toast.makeText(ctx2,"Custom configurations are succesfully removed",Toast.LENGTH_LONG).show();
                FileTools.dispResDialog(Settings.this);
                if (!txtViewRes.isEnabled()) {
                    txtViewRes.setEnabled(true);
                }
                super.onPostExecute(t);
            }
        }.execute();
    }

    public int getNewPriorityValue(String s) {
        if (s != null) {
            if (s.matches("[-+]?\\d*\\.?\\d+")) {
                int newPriorVal = Integer.parseInt(s);
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                    // P support
                    if ((newPriorVal == 1) || (newPriorVal == 0)) {
                        return newPriorVal;
                    } else {
                        return -1;
                    }
                } else {
                    // pre-P support
                    if (omsDev) {
                        if ((newPriorVal == 1) || (newPriorVal == 0)) {
                            return newPriorVal;
                        } else {
                            return -1;
                        }
                    } else {
                        if ((newPriorVal >= 1) && (newPriorVal <= 240)) {
                            if (secondPreferences2.getInt("PRIOR",50) != newPriorVal) {
                                return newPriorVal;
                            } else {
                                return -2;
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(Settings.this)
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
}
