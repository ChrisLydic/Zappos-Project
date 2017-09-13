package com.chrislydic.ilovezappos;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class PriceAlertService extends JobService {
	private static final String TAG = "PriceAlertService";
	private static final String ARG_PRICE = "price";

	public static Bundle getBundle( int price ) {
		Bundle args = new Bundle();
		args.putSerializable( ARG_PRICE, price );
		return args;
	}

	@Override
	public boolean onStartJob(JobParameters job) {
		int price = job.getExtras().getInt( ARG_PRICE );

		new FetchPriceTask().execute( price );

		return true;
	}

	@Override
	public boolean onStopJob(JobParameters job) {
		return true;
	}

	private class FetchPriceTask extends AsyncTask<Integer, Void, Boolean> {
		@Override
		protected Boolean doInBackground( Integer... terms ) {
			JSONObject result = null;
			try {
				result = new BitstampAPI().getPrice();
			} catch ( IOException exc ) {
				Log.e( TAG, "Failed to fetch URL: ", exc );
			}

			if ( result != null ) {
				try {
					if (terms[0] < result.getDouble( "last" )) {
						return true;
					}
				} catch ( JSONException exc ) {
					Log.e( TAG, "Failed to parse json" );
				}
			}

			return false;
		}

		@Override
		protected void onPostExecute( Boolean price ) {
			NotificationCompat.Builder mBuilder =
					new NotificationCompat.Builder(getApplicationContext())
							.setSmallIcon(R.drawable.border_bottom_row)
							.setContentTitle("My notification")
							.setContentText("Hello World!");

			Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);

			PendingIntent intent = PendingIntent.getActivity(getApplicationContext(), 0,
					notificationIntent, 0);

			mBuilder.setContentIntent(intent);


			// Sets an ID for the notification
			int mNotificationId = 001;
			// Gets an instance of the NotificationManager service
			NotificationManager mNotifyMgr =
					(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			// Builds the notification and issues it.
			mNotifyMgr.notify(mNotificationId, mBuilder.build());


		}
	}
}
