package com.chrislydic.ilovezappos;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Makes calls to the bitstamp API.
 */
public class BitstampAPI {
	private static final String URL_ORDERBOOK = "https://www.bitstamp.net/api/v2/order_book/btcusd/";
	private static final String URL_HISTORY = "https://www.bitstamp.net/api/v2/transactions/btcusd/";
	private static final String URL_PRICE = "https://www.bitstamp.net/api/v2/ticker_hour/btcusd/";

	public String getUrl( String urlSpec ) throws IOException {
		URL url = new URL( urlSpec );
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

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

	public JSONObject getOrderBook() throws IOException, JSONException {
		return new JSONObject( getUrl( URL_ORDERBOOK ) );
	}

	public JSONArray getHistory() throws IOException, JSONException {
		return new JSONArray( getUrl( URL_HISTORY ) );
	}

	public JSONObject getPrice() throws IOException, JSONException {
		return new JSONObject( getUrl( URL_PRICE ) );
	}
}
