package com.chrislydic.ilovezappos;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by chris on 9/10/2017.
 */

public class BitstampAPI {
	private static final String TAG = "BitstampApi";

	public String getUrl( String urlSpec ) throws IOException {
		URL url = new URL( urlSpec );
		HttpURLConnection connection = (HttpURLConnection)url.openConnection();

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			InputStream in = connection.getInputStream();

			if ( connection.getResponseCode() != HttpURLConnection.HTTP_OK ) {
				throw new IOException( connection.getResponseMessage() + ": with " + urlSpec );
			}

			int bytesRead = 0;
			byte[] buffer = new byte[1024];
			while ( ( bytesRead = in.read( buffer ) ) > 0 ) {
				out.write( buffer, 0, bytesRead );
			}
			out.close();
			return new String( out.toByteArray() );
		} finally {
			connection.disconnect();
		}
	}

	public JSONObject getOrderBook() throws IOException {
		try {
			return new JSONObject( getUrl( "https://www.bitstamp.net/api/v2/order_book/btcusd/" ) );
		} catch ( JSONException exc ) {
			return null;//TODO real return
		}
	}

	public JSONArray getHistory() throws IOException {
		try {
			return new JSONArray( getUrl("https://www.bitstamp.net/api/v2/transactions/btcusd/") );
		} catch ( JSONException exc ) {
			return null;//TODO real return
		}
	}

	public JSONObject getPrice() throws IOException {
		try {
			return new JSONObject( getUrl("https://www.bitstamp.net/api/v2/ticker_hour/btcusd/") );
		} catch ( JSONException exc ) {
			return null;//TODO real return
		}
	}
}
