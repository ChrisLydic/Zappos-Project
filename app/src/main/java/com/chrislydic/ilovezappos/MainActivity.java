package com.chrislydic.ilovezappos;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );
		Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		FragmentManager fm = getSupportFragmentManager();
		Fragment historyFragment = fm.findFragmentById( R.id.history_fragment_container );
		Fragment bidsFragment = fm.findFragmentById( R.id.bids_fragment_container );
		Fragment asksFragment = fm.findFragmentById( R.id.asks_fragment_container );

		if ( historyFragment == null ) {
			historyFragment = new HistoryFragment();
			fm.beginTransaction()
					.add( R.id.history_fragment_container, historyFragment )
					.commit();
		}

		if ( bidsFragment == null ) {
			bidsFragment = OrderBookFragment.newInstance(true);
			fm.beginTransaction()
					.add( R.id.bids_fragment_container, bidsFragment )
					.commit();
		}

		if ( asksFragment == null ) {
			asksFragment = OrderBookFragment.newInstance(false);
			fm.beginTransaction()
					.add( R.id.asks_fragment_container, asksFragment )
					.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate( R.menu.menu_main, menu );
		return true;
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if ( id == R.id.action_set_alert ) {


			invalidateOptionsMenu();
			return true;
		} else if ( id == R.id.action_update_alert ) {
			LayoutInflater li = LayoutInflater.from(this);
			View promptsView = li.inflate(R.layout.price_prompt, null);

			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
			alertDialogBuilder.setView( promptsView );
			alertDialogBuilder.setCancelable(false);

			final EditText priceAlertInput = (EditText) promptsView
					.findViewById(R.id.price_input);

			alertDialogBuilder.setPositiveButton(
					"OK",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							//dispatcher.cancel("my-unique-tag");
							createPriceAlert( Integer.parseInt(priceAlertInput.getText().toString()) );
							dialog.cancel();
						}
					});

			alertDialogBuilder.setNegativeButton(
					"Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							dialog.cancel();
						}
					});

			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
			return true;
		} else if ( id == R.id.action_remove_alert ) {
			invalidateOptionsMenu();
			return true;
		}

		return super.onOptionsItemSelected( item );
	}

	private void createPriceAlert(int priceFloor) {
		// Create a new dispatcher using the Google Play driver.
		FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));

		Job myJob = dispatcher.newJobBuilder()
				// the JobService that will be called
				.setService(PriceAlertService.class)
				// uniquely identifies the job
				.setTag("my-unique-tag")
				// one-off job
				.setRecurring(false)
				// don't persist past a device reboot
				.setLifetime( Lifetime.UNTIL_NEXT_BOOT)
				// start between 0 and 60 seconds from now
				.setTrigger( Trigger.executionWindow(0, 60))
				// don't overwrite an existing job with the same tag
				.setReplaceCurrent(false)
				// retry with exponential backoff
				.setRetryStrategy( RetryStrategy.DEFAULT_EXPONENTIAL)
				// constraints that need to be satisfied for the job to run
				.setConstraints(
						// only run on an unmetered network
						Constraint.ON_UNMETERED_NETWORK,
						// only run when the device is charging
						Constraint.DEVICE_CHARGING
				)
				.setExtras(PriceAlertService.getBundle( priceFloor ))
				.build();

		dispatcher.mustSchedule(myJob);
	}
}
