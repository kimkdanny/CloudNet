package com.example.cloudnet;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;

import android.content.Context;
import android.os.AsyncTask;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class AsyncRetrieve extends AsyncTask <Void,Void,Void>{
	LinkedList<String> phoneAccessor;
	JSONArray names = new JSONArray();
	JSONArray phones = new JSONArray();
	Context c;

	public AsyncRetrieve(LinkedList<String> access, MainActivity context) {
		phoneAccessor = access;
		this.c = context;
		Parse.initialize(c, "SyxvmqLVBzGWyL1IMvGVIeTiBwg8aeRmcwQHw34Y",
				"dVvBSxfT8XP1Xi6Ua4lYscq5zBxXwjk7zGXrQbb0");	
	}

	@Override
	protected Void doInBackground(Void... arg0) {
		ParseQuery parse = ParseQuery.getQuery("Phones");
		int access = phoneAccessor.size();
		for(int i =0;i<access;i++){
			parse.whereContains("mynumber", phoneAccessor.get(i));
			try {
				List<ParseObject> results = parse.find();
				System.out.println("SIZE " + results.size());
				if (results.size() >0){
					names = results.get(0).getJSONArray("name");
					phones = results.get(0).getJSONArray("phone");
				}		
				System.out.println(names);
				System.out.println(phones);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
		return null;
	}
	
	
	


}
