package com.chrislydic.ilovezappos;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.EntryXComparator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Retrieves transaction history and uses it to display a price history chart.
 */
public class HistoryFragment extends Fragment {
	private static final String TAG = HistoryFragment.class.getSimpleName();

	private LineDataSet lineDataSet;
	private LineChart chart;

	@Nullable
	@Override
	public View onCreateView( LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
		View v = inflater.inflate( R.layout.fragment_history, container, false );

		chart = (LineChart) v.findViewById( R.id.chart );
		chart.getAxisRight().setEnabled( false );
		chart.getAxisLeft().setTextSize( 12 );
		chart.getXAxis().setPosition( XAxis.XAxisPosition.BOTTOM );
		chart.getXAxis().setTextSize( 12 );
		chart.getXAxis().setValueFormatter( new DateFormatter() );
		chart.getLegend().setEnabled( false );
		chart.getDescription().setEnabled( false );
		chart.setDragEnabled( false );
		chart.setPinchZoom( false );
		chart.setScaleEnabled( false );
		chart.setDoubleTapToZoomEnabled( false );

		refresh();

		return v;
	}

	/**
	 * Get new data from the api.
	 */
	public void refresh() {
		new FetchHistoryTask().execute();
	}

	/**
	 * Update the ui with new data.
	 */
	private void updateUI() {
		LineData lineData = new LineData( lineDataSet );
		chart.setData( lineData );
		chart.invalidate();
	}

	/**
	 * Task that gets api data and turns it into a valid dataset for the chart library.
	 */
	private class FetchHistoryTask extends AsyncTask<Void, Void, List<Entry>> {
		@Override
		protected List<Entry> doInBackground( Void... terms ) {
			JSONArray result = null;
			try {
				result = new BitstampAPI().getHistory();
			} catch ( IOException exc ) {
				Log.e( TAG, "Failed to fetch URL: ", exc );
			} catch ( JSONException exc ) {
				Log.e( TAG, "Failed to parse json" );
			}

			List<Entry> entries = new ArrayList<Entry>();

			if ( result != null ) {
				try {
					for ( int i = 0; i < result.length(); i++ ) {
						JSONObject dataPoint = result.getJSONObject( i );
						Entry entry = new Entry( dataPoint.getLong( "date" ), (float) dataPoint.getDouble( "price" ) );
						if ( i > 0 ) {
							// for entries that occur at the same time, only use the entry with highest value
							Entry prev = entries.get( entries.size() - 1 );
							if ( prev.getX() == entry.getX() && prev.getY() < entry.getY() ) {
								entries.remove( entries.size() - 1 );
								entries.add( entry );
							} else if ( prev.getX() != entry.getX() ) {
								entries.add( entry );
							}
						} else {
							entries.add( entry );
						}
					}
					Collections.sort( entries, new EntryXComparator() );
				} catch ( JSONException exc ) {
					Log.e( TAG, "Failed to parse json" );
				}
			}
			return entries;
		}

		@Override
		protected void onPostExecute( List<Entry> entries ) {
			lineDataSet = new LineDataSet( entries, "Bitcoin Price" );
			lineDataSet.setColor( ContextCompat.getColor( getContext(), R.color.colorAccent ) );
			lineDataSet.setFillColor( ContextCompat.getColor( getContext(), R.color.colorAccent ) );
			lineDataSet.setLineWidth( 2f );
			lineDataSet.setDrawValues( false );
			lineDataSet.setMode( LineDataSet.Mode.CUBIC_BEZIER );
			lineDataSet.setDrawFilled( true );
			lineDataSet.setDrawCircles( false );
			updateUI();
		}
	}

	/**
	 * Class for displaying the date time values on the x axis properly.
	 */
	private class DateFormatter implements IAxisValueFormatter {
		@Override
		public String getFormattedValue( float value, AxisBase axis ) {
			SimpleDateFormat dateFormat = new SimpleDateFormat( "MM/dd HH:mm" );
			Date date = new Date( (long) value * 1000 );
			return dateFormat.format( date );
		}
	}
}
