package com.example.cloudnet;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

public class SMSReceiver extends BroadcastReceiver {
	private final String DEBUG_TAG = getClass().getSimpleName().toString();
	private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
	private Context mContext;
	private String number;
	private Intent mIntent;
	public static final String SMS_EXTRA_NAME = "pdus";
	Context main;
	
	
	// Retrieve SMS
	public void onReceive(Context context, Intent intent) {
		// Get the SMS map from Intent
		Bundle extras = intent.getExtras();

		String messages = "";

		if (extras != null) {
			// Get received SMS array
			Object[] smsExtra = (Object[]) extras.get(SMS_EXTRA_NAME);

			// Get ContentResolver object for pushing encrypted SMS to the
			// incoming folder
			ContentResolver contentResolver = context.getContentResolver();
			String number = null;
			for (int i = 0; i < smsExtra.length; ++i) {
				SmsMessage sms = SmsMessage.createFromPdu((byte[]) smsExtra[i]);

				String body = sms.getMessageBody().toString();
				String address = sms.getOriginatingAddress();

				messages += "SMS from " + address + " :\n";
				messages += body + "\n";
				number = address;
				// Here you can add any your code to work with incoming SMS
				// I added encrypting of all received SMS
			}
			
			if (messages.contains("yes")){
				System.out.println("YESSS");
				Intent i = new Intent(context,MainActivity.class); //context from onRecieve(context,intentData)
				i.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK); //required if ur app is not currently running.
				i.putExtra("SMSBODY",number); //get smsbody from the getMessageBody() (from your link)
				context.startActivity(i);//			}
			}
			// Display SMS message
			Toast.makeText(context, messages, Toast.LENGTH_SHORT).show();
		}

		// WARNING!!!
		// If you uncomment the next line then received SMS will not be put to
		// incoming.
		// Be careful!
		// this.abortBroadcast();
	}
	
	

	public static SmsMessage[] getMessagesFromIntent(Intent intent) {
		Object[] messages = (Object[]) intent.getSerializableExtra("pdus");
		byte[][] pduObjs = new byte[messages.length][];

		for (int i = 0; i < messages.length; i++) {
			pduObjs[i] = (byte[]) messages[i];
		}
		byte[][] pdus = new byte[pduObjs.length][];
		int pduCount = pdus.length;
		SmsMessage[] msgs = new SmsMessage[pduCount];
		for (int i = 0; i < pduCount; i++) {
			pdus[i] = pduObjs[i];
			msgs[i] = SmsMessage.createFromPdu(pdus[i]);
		}
		return msgs;
	}

	/**
	 * The notification is the icon and associated expanded entry in the status
	 * bar.
	 */
	protected void showNotification(int contactId, String message) {
		// Display notification...
	}
}