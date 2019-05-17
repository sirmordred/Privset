package com.mordred.privset;

import android.app.ActionBar;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by mordred on 29.10.2017.
 */

public class ModifiedSettingsActivity extends AppCompatActivity {

    private ArrayList<TempConfigNodes> listConfigNodes;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modifiedsettings);

        ActionBar abr = getActionBar();

        if (abr != null) {
            abr.setDisplayHomeAsUpEnabled(true);
        }

        LinearLayout nodeContain = findViewById(R.id.nodeContainer);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            listConfigNodes = (ArrayList<TempConfigNodes>) extras.getSerializable("configNodeList");
        }

        if (listConfigNodes != null) {
            for (TempConfigNodes nd: listConfigNodes) {
                View child = getLayoutInflater().inflate(R.layout.item_config_modified, null);
                TextView tv1 = child.findViewById(R.id.titleTextView2);
                TextView tv2 = child.findViewById(R.id.descriptionTextView2);
                TextView tv3 = child.findViewById(R.id.curValTextView2);
                TextView tv4 = child.findViewById(R.id.defValTextView2);
                tv1.setText(nd.getTitle2());
                tv2.setText(nd.getDescription2());
                tv3.setText("Current Value = " + nd.getCurrentValue2());
                tv4.setText("Default Value = " + nd.getDefaultValue2());
                nodeContain.addView(child);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(ModifiedSettingsActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(ModifiedSettingsActivity.this)
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
