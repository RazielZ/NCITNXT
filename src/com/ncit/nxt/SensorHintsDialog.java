package com.ncit.nxt;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class SensorHintsDialog extends DialogFragment{
	
	public SensorHintsDialog() {
	}

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		
        View view = inflater.inflate(R.layout.hints_dialog, container);
        getDialog().setTitle("Motion Hints");

        return view;
    }
}
