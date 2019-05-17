package com.mordred.privset;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RowAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ConfigNodes> mList;
    private Context ct;

    public RowAdapter(List<ConfigNodes> list, Context ct) {
        this.mList = list;
        this.ct = ct;
    }

    public void sortAndShow(final String searchKey) {
        if (mList != null) {
            Collections.sort(mList, new Comparator<ConfigNodes>() {
                @Override
                public int compare(ConfigNodes a, ConfigNodes b) {
                    String firstCnfTitle = a.getConfigTitle().toLowerCase();
                    String secondCnfTitle = b.getConfigTitle().toLowerCase();
                    String searchKeyLC = searchKey.toLowerCase();
                    if (firstCnfTitle.contains(searchKeyLC) && !secondCnfTitle.contains(searchKeyLC)) {
                        return -1;
                    } else if (!firstCnfTitle.contains(searchKeyLC) && secondCnfTitle.contains(searchKeyLC)) {
                        return 1;
                    } else {
                        return firstCnfTitle.compareTo(secondCnfTitle);
                    }
                }
            });
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConfigViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_config, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ConfigNodes object = mList.get(position);
        if (object != null) {
            ((ConfigViewHolder) holder).mTitle.setText(object.getConfigTitle());
            ((ConfigViewHolder) holder).mTitle.setTextColor(object.getColor());
            ((ConfigViewHolder) holder).mDescription.setText(ct.getResources().getString(ct.getResources().getIdentifier(object.configString,"string",ct.getPackageName())));
            ((ConfigViewHolder) holder).mDefVal.setText("Default value: " + object.getNodeFromDefPref());
            ((ConfigViewHolder) holder).mInput.setHint("Current value: " + object.getNodeFromPref());
            ((ConfigViewHolder) holder).mInput.setTag(position);
            ((ConfigViewHolder) holder).mInput.setText(object.getNewValue());

        }
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    public class ConfigViewHolder extends RecyclerView.ViewHolder {
        public TextView mTitle;
        public TextView mDescription;
        public EditText mInput;
        public TextView mDefVal;

        public ConfigViewHolder(View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.titleTextView);
            mDescription = itemView.findViewById(R.id.descriptionTextView);
            mDefVal = itemView.findViewById(R.id.defValTextView);
            mInput = itemView.findViewById(R.id.inputEditText);
            mInput.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
                public void afterTextChanged(Editable editable) {
                    if(mInput.getTag() != null) {
                        mList.get((int)mInput.getTag()).setNewValue(editable.toString());
                    }
                }
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            });
            mInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (!b) {
                        hideKeyboard(view.getContext(), view);
                    }
                }
            });
        }
    }

    private static void hideKeyboard(Context context, View view) {
        if (context != null && view != null) {
            InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (mgr != null) {
                mgr.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}
