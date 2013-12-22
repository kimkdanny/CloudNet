package com.example.cloudnet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;
import com.tjerkw.slideexpandable.library.ActionSlideExpandableListView;

public class MainActivity extends Activity implements OnTouchListener,
		OnClickListener, OnQueryTextListener {
	ArrayList<String> phones = new ArrayList<String>();
	ArrayList<String> names = new ArrayList<String>();
	ArrayList<String> phoneList = new ArrayList<String>();
	ArrayList<String> nameList = new ArrayList<String>();
	LinkedList<String> phoneHashTable = new LinkedList<String>();
	LinkedList<String> phoneAccessor = new LinkedList<String>();
	ArrayList<String> finalName = new ArrayList<String>();
	JSONArray nArray = new JSONArray();
	JSONArray pArray = new JSONArray();
	Cursor pCur;
	Cursor cur;
	ImageButton reload;
	ActionBar actionBar;
	ParseObject object;
	private static final String key = "CloudKey";
	SharedPreferences sp;
	String myNumber;
	boolean found = false;
	String myObjectId;
	ActionSlideExpandableListView listview;
	private static String SENT = "SMS_SENT";
	private static String DELIVERED = "SMS_DELIVERED";
	private static int MAX_SMS_MESSAGE_LENGTH = 160;
	static Context mContext;
	SearchView searchView;
	boolean first = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setActionBar();
		mContext = getApplicationContext();
		Parse.initialize(this, "SyxvmqLVBzGWyL1IMvGVIeTiBwg8aeRmcwQHw34Y",
				"dVvBSxfT8XP1Xi6Ua4lYscq5zBxXwjk7zGXrQbb0");
		sp = getSharedPreferences(key, 0);
		myObjectId = sp.getString("objectId", null);
		// myObjectId = null;
		// System.out.println("THE OBJECT ID " + myObjectId);
		myNumber = getMy10DigitPhoneNumber();
		try {
			getContactInformation();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// system.out.println("PHONE LIST " + phoneList);
		listview = (ActionSlideExpandableListView) findViewById(R.id.list);
		int length = nameList.size();
		merge_sort(0, length - 1);
		listview.setAdapter(new MyContacts(getApplicationContext(), nameList));
		listview.setItemActionListener(
				new ActionSlideExpandableListView.OnActionClickListener() {

					@Override
					public void onClick(View listView, View buttonview,
							int position) {
						if (buttonview.getId() == R.id.buttonA) {
							Intent callIntent = new Intent(Intent.ACTION_CALL);
							callIntent.setData(Uri.parse("tel:"
									+ phoneList.get(position)));
							startActivity(callIntent);
						} else {
							Intent intentsms = new Intent(Intent.ACTION_VIEW,
									Uri.parse("sms:" + phoneList.get(position)));
							intentsms.putExtra("sms_body", "");
							startActivity(intentsms);
						}
					}

					// note that we also add 1 or more ids to the
					// setItemActionListener
					// this is needed in order for the listview to discover
					// the buttons
				}, R.id.buttonA, R.id.buttonB);
	}

	// ---sends an SMS message to another device---
	public static void sendSMS(String phoneNumber, String message) {
		// system.out.println("WTF");
		PendingIntent piSent = PendingIntent.getBroadcast(mContext, 0,
				new Intent(SENT), 0);
		PendingIntent piDelivered = PendingIntent.getBroadcast(mContext, 0,
				new Intent(DELIVERED), 0);
		SmsManager smsManager = SmsManager.getDefault();

		int length = message.length();
		if (length > MAX_SMS_MESSAGE_LENGTH) {
			// system.out.println("WTF");

			ArrayList<String> messagelist = smsManager.divideMessage(message);
			smsManager.sendMultipartTextMessage(phoneNumber, null, messagelist,
					null, null);
		} else {
			// system.out.println("WTF");

			smsManager.sendTextMessage(phoneNumber, null, message, piSent,
					piDelivered);
		}
	}

	private String getMyPhoneNumber() {
		TelephonyManager mTelephonyMgr;
		mTelephonyMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		return mTelephonyMgr.getLine1Number();
	}

	private String getMy10DigitPhoneNumber() {
		String s = getMyPhoneNumber();
		if (s.startsWith("+1"))
			return s.substring(2);
		else
			return s.substring(1);
	}

	@SuppressLint("NewApi")
	private void setActionBar() {
		actionBar = getActionBar();
		actionBar.setDisplayShowHomeEnabled(false);
		View mActionBarView = getLayoutInflater().inflate(
				R.layout.customactionbar, null);
		actionBar.setCustomView(mActionBarView);
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
		reload = (ImageButton) mActionBarView.findViewById(R.id.reload);
		reload.setOnTouchListener(this);
		reload.setOnClickListener(this);
		searchView = (SearchView) mActionBarView.findViewById(R.id.search);
		searchView.setOnQueryTextListener(this);
	}

	public void getContactInformation() throws IOException {
		ContentResolver cr = getContentResolver();
		cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null,
				null);
		phones = new ArrayList();
		names = new ArrayList();
		phoneList = new ArrayList();
		nameList = new ArrayList();
		if (cur.getCount() > 0) {
			while (cur.moveToNext()) {
				String id = cur.getString(cur
						.getColumnIndex(ContactsContract.Contacts._ID));
				String name = cur
						.getString(cur
								.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
				if (Integer
						.parseInt(cur.getString(cur
								.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {

					pCur = cr.query(
							ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
							null,
							ContactsContract.CommonDataKinds.Phone.CONTACT_ID
									+ " = ?", new String[] { id }, null);

					while (pCur.moveToNext()) {
						String phoneNo = pCur
								.getString(pCur
										.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
						phones.add(phoneNo);
						String n = pCur
								.getString(pCur
										.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
						names.add(n);
					}

				}
			}
		}
		cur.close();
		pCur.close();

		Pattern p = Pattern
				.compile("1?-?\\(?\\s?[0-9]{3}\\)?\\s?-?\\s?[0-9]{3}\\s?-?\\s?[0-9]{4}\\s?");
		for (int i = 0; i < phones.size(); i++) {
			String person = names.get(i);
			String str = phones.get(i);
			Matcher m = p.matcher(str);
			while (m.find()) {
				String match = str.substring(m.start(), m.end());
				Pattern pattern = Pattern.compile("[0-9]");
				Matcher matcher = pattern.matcher(match);
				String answer = "";
				while (matcher.find()) {
					answer += match.substring(matcher.start(), matcher.end());
				}
				if (answer.length() > 10) {
					answer = answer.substring(1, answer.length());
				}

				if (answer.indexOf("1800") != 0 && answer.length() == 10
						&& answer.indexOf("800") != 0) {
					phoneList.add(answer);
					nameList.add(names.get(i));
				}
			}
		}
	}

	public void uploadToCloudFirst() throws ParseException {
		JSONArray phoneArray = new JSONArray();
		JSONArray nameArray = new JSONArray();

		for (int i = 0; i < phoneList.size(); i++) {
			phoneArray.put(phoneList.get(i));
			nameArray.put(nameList.get(i));
		}
		ParseQuery<ParseObject> phone = ParseQuery.getQuery("Phones");
		if (myObjectId != null) {
			ParseObject user = phone.get(myObjectId);
			user.put("mynumber", myNumber);
			user.put("phone", phoneArray);
			user.put("name", nameArray);
			user.saveInBackground(new SaveCallback() {

				public void done(ParseException e) {
					if (e != null) {
						Toast.makeText(getApplicationContext(),
								"Error Uploading: " + e.getMessage(),
								Toast.LENGTH_LONG).show();
					} else {
						ParseQuery<ParseObject> pq = ParseQuery
								.getQuery("HashTable");
						JSONArray ids = null;
						try {
							ParseObject hash = pq.get("YrDBLrh5p8");
							ids = hash.getJSONArray("hashtable");
							phoneHashTable = new LinkedList<String>();
							int length = ids.length();
							for (int i = 0; i < length; i++) {
								phoneHashTable.add(ids.getString(i));
							}
							retrieveContacts();

						} catch (ParseException e1) {
							e1.printStackTrace();
						} catch (JSONException e1) {
							e1.printStackTrace();
						}
					}
				}
			});
		} else {
			object = new ParseObject("Phones");
			object.put("mynumber", myNumber);
			object.put("phone", phoneArray);
			object.put("name", nameArray);
			object.saveInBackground(new SaveCallback() {

				public void done(ParseException e) {
					if (e != null) {
						Toast.makeText(getApplicationContext(),
								"Error Uploading: " + e.getMessage(),
								Toast.LENGTH_LONG).show();
					} else {
						String id = object.getObjectId();
						SharedPreferences sp = getSharedPreferences(key, 0);
						Editor edit = sp.edit();
						edit.putString("objectId", id);
						edit.commit();

						ParseQuery<ParseObject> pq = ParseQuery
								.getQuery("HashTable");
						JSONArray ids = null;
						try {
							ParseObject hash = pq.get("YrDBLrh5p8");
							ids = hash.getJSONArray("hashtable");

							int length = ids.length();
							for (int i = 0; i < length; i++) {
								phoneHashTable.add(ids.getString(i));
							}
							phoneHashTable.add(0, myNumber);

							int position = binarySearch(phoneHashTable,
									myNumber, 0, length);

							int hashLength = phoneHashTable.size();
							JSONArray json = new JSONArray();
							for (int j = 0; j < hashLength; j++) {
								json.put(phoneHashTable.get(j));
							}
							hash.put("hashtable", json);
							hash.saveInBackground();
							retrieveContacts();

						} catch (ParseException e1) {
							e1.printStackTrace();
						} catch (JSONException e1) {
							e1.printStackTrace();
						}
					}
				}
			});
		}
		// system.out.println(phoneArray);
		// system.out.println(nameArray);
	}

	public int binarySearch(LinkedList<String> hash, String value, int low,
			int high) {
		if (high < low) {
			if (high == -1) {
				hash.add(0, value);
			} else
				hash.add(low, value);
			return high;
		}
		int mid = low + ((high - low) / 2);
		if (mid == hash.size()) {
			hash.add(value);
			return mid;
		}
		if (hash.get(mid).compareTo(value) > 0)
			return binarySearch(hash, value, low, mid - 1);
		else if (hash.get(mid).compareTo(value) < 0)
			return binarySearch(hash, value, mid + 1, high);
		else {
			found = true;
			return mid; // found
		}
	}

	public void retrieveContacts() throws ParseException {
		int length = phoneList.size();
		phoneAccessor = new LinkedList<String>();
		for (int i = 0; i < length; i++) {
			if (phoneHashTable.contains(phoneList.get(i))) {
				if (!phoneList.get(i).equals(myNumber))
					phoneAccessor.add(phoneList.get(i));
			}
		}
		// system.out.println("ACCESS PHONE LIST " + phoneList);

		// system.out.println("ACCESS " + phoneAccessor);
		new AsyncRetriever().execute();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (v.getId()) {
		case (R.id.reload):
			switch (event.getAction()) {
			case (MotionEvent.ACTION_DOWN):
				reload.setBackgroundColor(Color.parseColor("#2878a5"));
				break;
			case (MotionEvent.ACTION_UP):
				reload.setBackgroundColor(Color.parseColor("#5ea9dd"));
				break;
			}
		}
		return false;
	}

	@Override
	public void onClick(View arg0) {
		if (arg0.getId() == R.id.reload) {
			nameList = new ArrayList();
			phoneList = new ArrayList();
			first = false;
			try {
				getContactInformation();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				uploadToCloudFirst();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public class MyContacts extends ArrayAdapter<String> {
		Context context;
		ArrayList<String> nameStrings;

		public MyContacts(Context c, ArrayList<String> u) {
			super(c, R.layout.customrow, R.id.nameId, u);
			this.context = c;
			this.nameStrings = u;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			ViewHolder holder = null;
			if (row == null) {
				LayoutInflater inflater = (LayoutInflater) context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				row = inflater.inflate(R.layout.customrow, parent, false);
				holder = new ViewHolder(row);
				row.setTag(holder);
			} else {
				holder = (ViewHolder) row.getTag();
			}

			holder.name.setText(nameStrings.get(position));
			holder.phone.setText(phoneList.get(position));

			return row;
		}
	}

	class AsyncRetriever extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			ParseQuery parse = ParseQuery.getQuery("Phones");
			int access = phoneAccessor.size();
			// system.out.println("ACCESS" + phoneAccessor);
			for (int i = 0; i < access; i++) {
				parse.whereContains("mynumber", phoneAccessor.get(i));
				try {
					List<ParseObject> results = parse.find();
					// system.out.println("SIZE " + results.size());
					if (results.size() > 0) {
						nArray = results.get(0).getJSONArray("name");
						pArray = results.get(0).getJSONArray("phone");
					}
					// system.out.println(nArray);
					// system.out.println(pArray);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			int alength = nArray.length();
			for (int i = 0; i < alength; i++) {
				try {
					nameList.add(nArray.getString(i));
					phoneList.add(pArray.getString(i));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			merge_sort(0, phoneList.size() - 1);
			// system.out.println("SORTED NAME " + nameList);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			listview.setAdapter(new MyContacts(getApplicationContext(),
					nameList));

			listview.setItemActionListener(
					new ActionSlideExpandableListView.OnActionClickListener() {

						@Override
						public void onClick(View listView, View buttonview,
								int position) {
							if (buttonview.getId() == R.id.buttonA) {
								Intent callIntent = new Intent(
										Intent.ACTION_CALL);
								callIntent.setData(Uri.parse("tel:"
										+ phoneList.get(position)));
								startActivity(callIntent);
							} else {
								Intent intentsms = new Intent(
										Intent.ACTION_VIEW, Uri.parse("sms:"
												+ phoneList.get(position)));
								intentsms.putExtra("sms_body", "");
								startActivity(intentsms);
							}
						}

						// note that we also add 1 or more ids to the
						// setItemActionListener
						// this is needed in order for the listview to discover
						// the buttons
					}, R.id.buttonA, R.id.buttonB);
		}
	}

	public void merge_sort(int low, int high) {
		int mid;
		if (low < high) {
			mid = (low + high) / 2;
			merge_sort(low, mid);
			merge_sort(mid + 1, high);
			merge(low, mid, high);
		}
	}

	public void merge(int low, int mid, int high) {
		int h, i, j, k;
		ArrayList<String> b = new ArrayList();
		ArrayList<String> tempP = new ArrayList();
		h = low;
		i = low;
		j = mid + 1;

		while ((h <= mid) && (j <= high)) {
			if (nameList.get(h).compareTo(nameList.get(j)) <= 0) {
				b.add(nameList.get(h));
				tempP.add(phoneList.get(h));
				h++;
			} else {
				b.add(nameList.get(j));
				tempP.add(phoneList.get(j));
				j++;
			}
		}
		if (h > mid) {
			for (k = j; k <= high; k++) {
				b.add(nameList.get(k));
				tempP.add(phoneList.get(k));
			}
		} else {
			for (k = h; k <= mid; k++) {
				b.add(nameList.get(k));
				tempP.add(phoneList.get(k));
			}
		}
		for (k = low; k <= high; k++) {
			nameList.set(k, b.get(k - low));
			phoneList.set(k, tempP.get(k - low));
		}
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		return false;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		System.out.println("SUBMIT");
		sendSMS(query,
				"Would you let me access to your contact information? Msg 'yes' if so.");
		return true;
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		Intent i = getIntent();
		System.out.println("NEW TEXT MESSGAEEEFSDFS");

		if (i.getStringExtra("SMSBODY") != null) {
			String msg = i.getStringExtra("SMSBODY");
			System.out.println("NEW TEXT MESSGAEEE");
			new SearchRetriever().execute(msg);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// TODO Auto-generated method stub
		super.onNewIntent(intent);
		Intent i = getIntent();
		System.out.println("NEW TEXT MESSGAEEEFSDFS");

		if (i.getStringExtra("SMSBODY") != null) {
			String msg = i.getStringExtra("SMSBODY");
			System.out.println("MESSAGE " + msg);
			System.out.println("NEW TEXT MESSGAEEE");
			new SearchRetriever().execute(msg);
		}
	}

	class SearchRetriever extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... search) {
			System.out.println("START");
			ParseQuery<ParseObject> parse = ParseQuery.getQuery("Phones");
			parse.whereContains("mynumber", search[0]);
			try {
				List<ParseObject> results = parse.find();
				System.out.println("RESULTS " + results.size());
				System.out.println(results);
				if (results.size() > 0) {
					nArray = results.get(0).getJSONArray("name");
					pArray = results.get(0).getJSONArray("phone");
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			int alength = nArray.length();
			for (int i = 0; i < alength; i++) {
				try {
					nameList.add(nArray.getString(i));
					phoneList.add(pArray.getString(i));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			merge_sort(0, nameList.size() - 1);
			// system.out.println("SORTED NAME " + nameList);
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			listview.setAdapter(new MyContacts(getApplicationContext(),
					nameList));

			listview.setItemActionListener(
					new ActionSlideExpandableListView.OnActionClickListener() {

						@Override
						public void onClick(View listView, View buttonview,
								int position) {
							if (buttonview.getId() == R.id.buttonA) {
								Intent callIntent = new Intent(
										Intent.ACTION_CALL);
								callIntent.setData(Uri.parse("tel:"
										+ phoneList.get(position)));
								startActivity(callIntent);
							} else {
								Intent intentsms = new Intent(
										Intent.ACTION_VIEW, Uri.parse("sms:"
												+ phoneList.get(position)));
								intentsms.putExtra("sms_body", "");
								startActivity(intentsms);
							}
						}

						// note that we also add 1 or more ids to the
						// setItemActionListener
						// this is needed in order for the listview to discover
						// the buttons
					}, R.id.buttonA, R.id.buttonB);
		}
	}
}
