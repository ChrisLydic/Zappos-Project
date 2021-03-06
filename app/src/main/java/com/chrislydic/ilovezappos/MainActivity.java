package com.chrislydic.ilovezappos;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
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

/**
 * MainActivity handles menu input, handles price alert service creation and cancellation,
 * and creates fragments that handle transaction history and order book tables.
 */
public class MainActivity extends AppCompatActivity {
	private static final String PRICE_ALERT_JOB = "com.chrislydic.ilovezappos.pricealert";
	HistoryFragment historyFragment;
	OrderBookFragment bidsFragment;
	OrderBookFragment asksFragment;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );
		Toolbar toolbar = (Toolbar) findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		FragmentManager fm = getSupportFragmentManager();
		historyFragment = (HistoryFragment) fm.findFragmentById( R.id.history_fragment_container );
		bidsFragment = (OrderBookFragment) fm.findFragmentById( R.id.bids_fragment_container );
		asksFragment = (OrderBookFragment) fm.findFragmentById( R.id.asks_fragment_container );

		if ( historyFragment == null && bidsFragment == null && asksFragment == null ) {
			historyFragment = new HistoryFragment();
			bidsFragment = OrderBookFragment.newInstance( true );
			asksFragment = OrderBookFragment.newInstance( false );
			fm.beginTransaction()
					.add( R.id.history_fragment_container, historyFragment )
					.add( R.id.bids_fragment_container, bidsFragment )
					.add( R.id.asks_fragment_container, asksFragment )
					.commit();
		}
	}

	@Override
	public boolean onCreateOptionsMenu( Menu menu ) {
		getMenuInflater().inflate( R.menu.menu_main, menu );
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu( Menu menu ) {
		SharedPreferences sharedPref =
				this.getSharedPreferences( getString( R.string.saved_price_file ), Context.MODE_PRIVATE );
		// using -1 as default because only positive values are allowed for the price alert
		int price = sharedPref.getInt( getString( R.string.saved_price_alert ), -1 );

		if ( price == -1 ) { // price alert job doesn't exist
			menu.findItem( R.id.action_set_alert ).setVisible( true );
			menu.findItem( R.id.action_update_alert ).setVisible( false );
			menu.findItem( R.id.action_remove_alert ).setVisible( false );
		} else { // price alert job exists
			menu.findItem( R.id.action_set_alert ).setVisible( false );
			menu.findItem( R.id.action_update_alert ).setVisible( true );
			menu.findItem( R.id.action_remove_alert ).setVisible( true );
		}

		return super.onPrepareOptionsMenu( menu );
	}

	@Override
	public boolean onOptionsItemSelected( MenuItem item ) {
		int id = item.getItemId();

		if ( id == R.id.action_set_alert || id == R.id.action_update_alert ) {
			// ask user for price alert value and then create new price alert job

			LayoutInflater li = LayoutInflater.from( this );
			View promptsView = li.inflate( R.layout.price_prompt, null );

			AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder( this );
			alertDialogBuilder.setView( promptsView );
			alertDialogBuilder.setCancelable( false );

			final EditText priceAlertInput = (EditText) promptsView
					.findViewById( R.id.price_input );

			if ( id == R.id.action_update_alert ) { // if price alert exists, populate edit text
				SharedPreferences sharedPref =
						this.getSharedPreferences( getString( R.string.saved_price_file ), Context.MODE_PRIVATE );
				int price = sharedPref.getInt( getString( R.string.saved_price_alert ), 0 );
				priceAlertInput.setText( Integer.toString( price ) );
			}

			alertDialogBuilder.setPositiveButton(
					"OK",
					new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id ) {
							createPriceAlert( Integer.parseInt( priceAlertInput.getText().toString() ) );
							invalidateOptionsMenu();
							dialog.cancel();
						}
					} );

			alertDialogBuilder.setNegativeButton(
					"Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick( DialogInterface dialog, int id ) {
							dialog.cancel();
						}
					} );

			AlertDialog alertDialog = alertDialogBuilder.create();
			alertDialog.show();
			return true;
		} else if ( id == R.id.action_remove_alert ) {
			// cancel current price alert job and remove the price value stored in shared preferences

			FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher( new GooglePlayDriver( this ) );
			dispatcher.cancel( PRICE_ALERT_JOB );

			SharedPreferences.Editor editor =
					this.getSharedPreferences( getString( R.string.saved_price_file ), Context.MODE_PRIVATE ).edit();
			editor.remove( getString( R.string.saved_price_alert ) );
			editor.apply();

			invalidateOptionsMenu();
			return true;
		} else if ( id == R.id.action_refresh ) {
			historyFragment.refresh();
			bidsFragment.refresh();
			asksFragment.refresh();
		}

		return super.onOptionsItemSelected( item );
	}

	@Override
	protected void onResume() {
		super.onResume();
		// make sure menu is up to date, changes can occur in background
		//   due to price alert service
		invalidateOptionsMenu();
	}

	/**
	 * Create a new service that checks hourly if the price of bitcoin has
	 * fallen below priceFloor.
	 *
	 * @param priceFloor bitcoin price entered by user
	 */
	private void createPriceAlert( int priceFloor ) {
		FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher( new GooglePlayDriver( this ) );

		// cancel the price alert job if it is running
		dispatcher.cancel( PRICE_ALERT_JOB );

		// the current price alert value is stored using shared preferences
		SharedPreferences.Editor editor =
				this.getSharedPreferences( getString( R.string.saved_price_file ), Context.MODE_PRIVATE ).edit();
		editor.putInt( getString( R.string.saved_price_alert ), priceFloor );
		editor.apply();

		// create a price alert job that is recurring, lasts forever (until this app kills it),
		//   and runs every 59-60 minutes provided there is network access
		Job myJob = dispatcher.newJobBuilder()
				.setService( PriceAlertService.class )
				.setTag( PRICE_ALERT_JOB )
				.setRecurring( true )
				.setLifetime( Lifetime.FOREVER )
				.setTrigger( Trigger.executionWindow( 3540, 3600 ) )
				.setReplaceCurrent( true )
				.setRetryStrategy( RetryStrategy.DEFAULT_LINEAR )
				.setConstraints(
						Constraint.ON_ANY_NETWORK
				)
				.build();

		dispatcher.mustSchedule( myJob );
	}
}
