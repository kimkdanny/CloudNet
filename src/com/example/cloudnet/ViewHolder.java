package com.example.cloudnet;

import android.view.View;
import android.widget.TextView;


public class ViewHolder {
	TextView name;
	TextView phone;
	
	ViewHolder (View v){
		name = (TextView) v.findViewById(R.id.nameId);
		phone = (TextView) v.findViewById(R.id.phoneId);
	}
}
