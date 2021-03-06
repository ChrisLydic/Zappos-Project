package com.chrislydic.ilovezappos;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Service that checks hourly if the price of bitcoin has fallen below
 * the user given price.
 */
public class PriceAlertService extends JobService {
	private static final String TAG = PriceAlertService.class.getSimpleName();
	private static final int NOTIFICATION_ID = 1;
	private JobParameters jobParams;

	@Override
	public boolean onStartJob( JobParameters job ) {
		jobParams = job;
		new FetchPriceTask().execute();
		return true;
	}

	@Override
	public boolean onStopJob( JobParameters job ) {
		return true;
	}

	/**
	 * Task that gets api data and checks it against price alert value,
	 * showing a notification to the user if the bitcoin price has
	 * fallen below the price alert value.
	 */
	private class FetchPriceTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground( Void... terms ) {
			JSONObject result = null;
			try {
				result = new BitstampAPI().getPrice();
			} catch ( IOException exc ) {
				Log.e( TAG, "Failed to fetch URL: ", exc );
			} catch ( JSONException exc ) {
				Log.e( TAG, "Failed to parse json" );
			}

			if ( result != null ) {
				try {
					SharedPreferences sharedPref = getApplicationContext()
							.getSharedPreferences( getString( R.string.saved_price_file ), Context.MODE_PRIVATE );

					int savedPrice = sharedPref.getInt( getString( R.string.saved_price_alert ), 0 );
					return savedPrice > result.getDouble( "last" );
				} catch ( JSONException exc ) {
					Log.e( TAG, "Failed to parse json" );
				}
			}

			return false;
		}

		@Override
		protected void onPostExecute( Boolean showAlert ) {
			if ( !showAlert ) { // if the price alert was not triggered, continue to run the service
				jobFinished( jobParams, true );
				return;
			}

			SharedPreferences sharedPref = getApplicationContext()
					.getSharedPreferences( getString( R.string.saved_price_file ), Context.MODE_PRIVATE );
			int savedPrice = sharedPref.getInt( getString( R.string.saved_price_alert ), 0 );

			// the service is done so remove the set price alert value
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.remove( getString( R.string.saved_price_alert ) );
			editor.apply();

			String title = getString( R.string.price_alert_notification_title );
			String description = getString( R.string.price_alert_notification_description )
					+ " " + savedPrice + " " + getString( R.string.currency );

			// create a notification that will open the main activity when tapped
			NotificationCompat.Builder mBuilder =
					new NotificationCompat.Builder( getApplicationContext() )
							.setSmallIcon( R.mipmap.ic_launcher )
							.setContentTitle( title )
							.setContentText( description );

			Intent notificationIntent = new Intent( getApplicationContext(), MainActivity.class );

			notificationIntent.setFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_SINGLE_TOP );

			PendingIntent intent = PendingIntent
					.getActivity( getApplicationContext(), 0, notificationIntent, 0 );

			mBuilder.setContentIntent( intent );

			NotificationManager mNotifyMgr = (NotificationManager) getSystemService( NOTIFICATION_SERVICE );
			mNotifyMgr.notify( NOTIFICATION_ID, mBuilder.build() );

			jobFinished( jobParams, false );
		}
	}
}
