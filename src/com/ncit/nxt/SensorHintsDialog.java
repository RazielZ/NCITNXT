package com.ncit.nxt;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class SensorHintsDialog extends DialogFragment {
	
	private CheckBox checkBox;
	private SharedPreferences shPrefs;
	private SharedPreferences.Editor shPrefsEditor;
	private Button bOk;
	
	public SensorHintsDialog() {
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		
        View view = inflater.inflate(R.layout.hints_dialog, container);
        getDialog().setTitle("Motion Hints");
        
        shPrefs = getActivity().getSharedPreferences("show_hints", 0);
        shPrefsEditor = shPrefs.edit();
        
		//Dont't show again button
		checkBox = (CheckBox) view.findViewById(R.id.checkBox);
		if (shPrefs.getBoolean("show_hints", true) == true) {
			checkBox.setChecked(true);
		}
		checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				shPrefsEditor.putBoolean("show_hints", isChecked);
				shPrefsEditor.commit();
				Log.d("prefs", "pref= " + shPrefs.getBoolean("show_hints", true));
			}
		});
		
		// Ok button event
		bOk = (Button) view.findViewById(R.id.bOk);
		bOk.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});

        return view;
    }
}
