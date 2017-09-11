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
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.LargeValueFormatter;
import com.github.mikephil.charting.utils.EntryXComparator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by chris on 9/9/2017.
 */

public class HistoryFragment extends Fragment {
	private static final String TAG = "HistoryFragment";

	private LineDataSet mLineDataSet;
	private LineChart mChart;

	@Nullable
	@Override
	public View onCreateView( LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
		View v = inflater.inflate(R.layout.fragment_history, container, false);

		mChart = (LineChart) v.findViewById(R.id.chart);
		mChart.getAxisRight().setEnabled(false);
		mChart.getAxisLeft().setTextSize( 12 );
		mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
		mChart.getXAxis().setTextSize( 12 );
		//mChart.getXAxis().setValueFormatter( new LargeValueFormatter() );
		mChart.getLegend().setEnabled( false );
		mChart.getDescription().setEnabled( false );
		mChart.setDragEnabled( false );
		mChart.setPinchZoom( false );
		mChart.setScaleEnabled( false );
		mChart.setDoubleTapToZoomEnabled( false );

		new FetchHistoryTask().execute();

		return v;
	}

	public void updateUI() {
		LineData lineData = new LineData(mLineDataSet);
		mChart.setData(lineData);
		mChart.invalidate();
	}

	private class FetchHistoryTask extends AsyncTask<Void, Void, List<Entry>> {
		@Override
		protected List<Entry> doInBackground( Void... terms ) {
			JSONArray result = null;
			try {
				result = new BitstampAPI().getHistory();
				Log.i( TAG, "Fetch contents of URL: " + result );
			} catch ( IOException exc ) {
				Log.e( TAG, "Failed to fetch URL: ", exc );
			}

			List<Entry> entries = new ArrayList<Entry>();

			if ( result != null ) {
				try {
					for ( int i = 0; i < result.length(); i++ ) {
						JSONObject dataPoint = result.getJSONObject( i );
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis( dataPoint.getInt( "date" ) * 1000 );
						cal.set( Calendar.SECOND, 0 );
						Entry entry = new Entry( cal.getTimeInMillis()/1000, (float) dataPoint.getDouble( "price" ) );
						entries.add( entry );
					}
					Collections.sort( entries, new EntryXComparator() );
				} catch ( JSONException exc ) {
					Log.e( TAG, "aaa" );
				}
			}
			return entries;
		}

		@Override
		protected void onPostExecute( List<Entry> entries ) {
			mLineDataSet = new LineDataSet(entries, "Bitcoin Price");
			mLineDataSet.setColor( ContextCompat.getColor(getContext(), R.color.colorAccent) );
			mLineDataSet.setFillColor( ContextCompat.getColor(getContext(), R.color.colorAccent) );
			mLineDataSet.setLineWidth( 2f );
			mLineDataSet.setMode( LineDataSet.Mode.HORIZONTAL_BEZIER );
			mLineDataSet.setDrawFilled( true );
			mLineDataSet.setDrawCircles( false );
			updateUI();
		}
	}
}
