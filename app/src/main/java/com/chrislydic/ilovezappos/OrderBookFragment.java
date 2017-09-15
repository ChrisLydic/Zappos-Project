package com.chrislydic.ilovezappos;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Order book fragment retrieves and displays info on buys and asks.
 */
public class OrderBookFragment extends Fragment {
	private static final String TAG = OrderBookFragment.class.getSimpleName();
	private static final String ARG_IS_BID = "list_type";

	private List<Order> orders;
	private OrderAdapter adapter;
	// true if the fragment handles bids, false if it handles asks
	private boolean isBid;

	/**
	 * Create new fragment with isBid argument.
	 *
	 * @param isBid true if the fragment handles bids, false if it handles asks
	 * @return instance of OrderBookFragment
	 */
	public static OrderBookFragment newInstance( boolean isBid ) {
		Bundle args = new Bundle();
		args.putSerializable( ARG_IS_BID, isBid );

		OrderBookFragment fragment = new OrderBookFragment();
		fragment.setArguments( args );
		return fragment;
	}

	@Override
	public void onCreate( @Nullable Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );

		isBid = (boolean) getArguments().getSerializable( ARG_IS_BID );
	}

	@Nullable
	@Override
	public View onCreateView( LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState ) {
		View v = inflater.inflate( R.layout.fragment_orderbook, container, false );

		orders = new ArrayList<>();
		adapter = new OrderAdapter( orders );

		RecyclerView mOrderRecyclerView = (RecyclerView) v.findViewById( R.id.order_recycler_view );
		mOrderRecyclerView.setLayoutManager( new LinearLayoutManager( getContext() ) );
		mOrderRecyclerView.setAdapter( adapter );

		if ( !isBid ) {
			// change string values in layout from the defaults if the fragment is handling asks

			TextView nameHeader = (TextView) v.findViewById( R.id.orderbook_name );
			nameHeader.setText( R.string.asks_order_book_name );

			TextView priceHeader = (TextView) v.findViewById( R.id.price_header );
			priceHeader.setText( R.string.ask_order );
		}

		refresh();

		return v;
	}

	/**
	 * Get new data from the api.
	 */
	public void refresh() {
		new FetchOrdersTask().execute();
	}

	/**
	 * Update the ui with new data.
	 */
	private void updateUI() {
		adapter.setOrders( orders );
		adapter.notifyDataSetChanged();
	}

	private class OrderHolder extends RecyclerView.ViewHolder {
		public TextView bitcoinPrice, amount, value;

		private OrderHolder( View view ) {
			super( view );
			bitcoinPrice = (TextView) view.findViewById( R.id.price );
			amount = (TextView) view.findViewById( R.id.amount );
			value = (TextView) view.findViewById( R.id.value );
		}
	}

	private class OrderAdapter extends RecyclerView.Adapter<OrderHolder> {
		private List<Order> orderList;

		public OrderAdapter( List<Order> orders ) {
			orderList = orders;
		}

		@Override
		public OrderHolder onCreateViewHolder( ViewGroup parent, int viewType ) {
			View orderView = LayoutInflater.from( parent.getContext() )
					.inflate( R.layout.order_row, parent, false );

			return new OrderHolder( orderView );
		}

		@Override
		public void onBindViewHolder( OrderHolder holder, int position ) {
			Order order = orderList.get( position );
			holder.bitcoinPrice.setText( String.format( Locale.ENGLISH, "%.2f", order.getBitcoinPrice() ) );
			holder.amount.setText( String.format( Locale.ENGLISH, "%.5f", order.getAmount() ) );
			holder.value.setText( String.format( Locale.ENGLISH, "%.5f", order.getValue() ) );
		}

		@Override
		public int getItemCount() {
			return orderList.size();
		}

		public void setOrders( List<Order> orderList ) {
			this.orderList = orderList;
		}
	}

	/**
	 * Task that gets api data and turns it into a list of Order objects
	 * for the order book table
	 */
	private class FetchOrdersTask extends AsyncTask<Void, Void, List<Order>> {
		private final String BIDS = "bids";
		private final String ASKS = "asks";

		@Override
		protected List<Order> doInBackground( Void... terms ) {
			JSONObject result = null;
			try {
				result = new BitstampAPI().getOrderBook();
			} catch ( IOException exc ) {
				Log.e( TAG, "Failed to fetch order book data: ", exc );
			} catch ( JSONException exc ) {
				Log.e( TAG, "Failed to parse json" );
			}

			List<Order> orders = new ArrayList<>();

			if ( result != null ) {
				try {
					JSONArray ordersArray;
					if ( isBid ) {
						ordersArray = result.getJSONArray( BIDS );
					} else {
						ordersArray = result.getJSONArray( ASKS );
					}

					for ( int i = 0; i < ordersArray.length(); i++ ) {
						JSONArray orderArray = ordersArray.getJSONArray( i );

						double bitcoinPrice = orderArray.getDouble( 0 );
						double amount = orderArray.getDouble( 1 );

						Order order = new Order( bitcoinPrice, amount );

						orders.add( order );
					}
				} catch ( JSONException exc ) {
					Log.e( TAG, "Failed to parse json" );
				}
			}
			return orders;
		}

		@Override
		protected void onPostExecute( List<Order> orders ) {
			OrderBookFragment.this.orders = orders;
			updateUI();
		}
	}
}
