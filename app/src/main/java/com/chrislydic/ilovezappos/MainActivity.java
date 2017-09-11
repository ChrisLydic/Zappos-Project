package com.chrislydic.ilovezappos;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

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
		if ( id == R.id.action_settings ) {
			return true;
		}

		return super.onOptionsItemSelected( item );
	}
}
