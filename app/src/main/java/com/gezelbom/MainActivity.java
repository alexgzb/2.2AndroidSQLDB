package com.gezelbom;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * Main Activity for the prime finder. Prime finder is a super simple app that
 * checks for prime numbers and prints last found prime to the screen and also
 * saves that value in the shared preferences (sp) file so that it can continue
 * from this number when the app starts the next time. When the maximum value
 * has been reached the sp file is cleared and the finder stops looking for more
 * primes. Uses Handler to print to the GUI thread.
 * 
 * @author Alex Gezelbom
 * 
 */
public class MainActivity extends ListActivity {

	private long current = 1;
	private PrimeDBAdapter dbAdapter = null;
	private SimpleCursorAdapter adapter;
	private static final String TAG = "primeSQLMain";
	TextView textViewFound = null;
	TextView textViewCount = null;
	Button buttonStart = null;
	Button buttonShow = null;
	Button buttonStop = null;
	Handler handler;
	SQLiteDatabase database = null;
	final long MAX = Long.MAX_VALUE;
	Thread thread;

	/**
	 * OnCreate method starts the app
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initialise the variables
		textViewFound = (TextView) findViewById(R.id.textViewFound);
		textViewCount = (TextView) findViewById(R.id.TextViewCount);
		buttonStart = (Button) findViewById(R.id.buttonStart);
		buttonShow = (Button) findViewById(R.id.buttonShow);
		buttonStop = (Button) findViewById(R.id.buttonStop);

		// Create a db adapter and open a database session
		dbAdapter = new PrimeDBAdapter(this);
		dbAdapter.open();

		// Initialise the handler and override the handleMsssage method
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {

				// If the bundle received does not contain a String with the key
				// max
				if (msg.getData().get("max") == null) {

					// Store the value received in Shared prefs sp file
					// and print it to the the logcat debug and the textView
					current = (Long) msg.obj;

					/*
					 * spedit.putLong("lastPrime", current); spedit.commit();
					 */
					textViewFound.setText(getString(R.string.found) + " "
							+ current);
					textViewCount.setText(getString(R.string.count) + " "
							+ dbAdapter.getCount());
					Log.d(TAG, "Storing " + current + " as the last prime");
					dbAdapter.createRow(current);
					// database.execSQL("INSERT INTO " + TABLE_NAME +
					// "(timeOfStorage, value) VALUES(datetime(), "+ current
					// +");");

					// If the the bundle does contain a String with the key
					// "max"
					// Clear the sp with the key "lastPrime", and inform the
					// user by
					// Writing to the logcat file and the textview.
				} else {

					Log.d(TAG, "Max reached");
					/*
					 * Log.d(TAG, "Max reached, clearing shared prefs"); String
					 * message = msg.getData().getString("max");
					 * spedit.remove("lastPrime"); spedit.commit();
					 * tv2.setText(message); Log.d(TAG, message);
					 */
				}

			}
		};

		// If lastPrime is found in the sp then load the value
		/*
		 * current = sb.getLong("lastPrime", 1); if (current > 1) { Log.d(TAG,
		 * "Found a prime in the sharedPrefs file"); }
		 */

		startCounting(getWindow().getDecorView().getRootView());

	}

	/**
	 * Change the buttons enabled status. Create and start a new Thread with the
	 * Runnable PrimeCounter
	 * 
	 * @param V
	 *            The calling View
	 */
	public void startCounting(View V) {
		// Change status on the buttons
		buttonStart.setEnabled(false);
		buttonStop.setEnabled(true);
		// Create and start a new Thread and send the current value as argument
		thread = new Thread(new PrimeCounter(current));
		thread.start();
	}

	/**
	 * Override the OnDestroy method to interrupt the thread when the view is
	 * destroyed
	 */
	@Override
	protected void onDestroy() {
		thread.interrupt();
		super.onDestroy();
		dbAdapter.close();
	}

	/**
	 * Method to act on the stop button. sends an interrupt signal to the
	 * thread. Changes status on the buttons
	 * 
	 * @param v
	 *            The calling view
	 */
	public void stopCounting(View v) {
		buttonStart.setEnabled(true);
		buttonStop.setEnabled(false);
		thread.interrupt();
	}

	/**
	 * When drop button is clicked use the dbAdapter to clear the db
	 * 
	 * @param v
	 *            The calling view
	 */
	public void dropTable(View v) {
		dbAdapter.deleteAllRows();
	}

	/**
	 * When the show button is clicked Create a SimpleCursorAdapter and use the
	 * dbAdapter to create a cursor and populate the list
	 * 
	 * @param v
	 *            The calling view
	 */
	public void show(View v) {
		// Use the dbAdapter and fetch all rows method
		Cursor cursor = dbAdapter.fetchAllRows();
		// Create a fromColumns StringArray to define what columns to use
		String[] fromColumns = { PrimeDBAdapter.COL_UID,
				PrimeDBAdapter.COL_VALUE, PrimeDBAdapter.COL_TIME };
		// Create a toColumns int Array to define what views to use
		int[] toColumns = { R.id.TextViewID, R.id.textViewValue,
				R.id.textViewDate };

		// Setup the adapter
		// 1(Context)Context,
		// 2,(int)the Layout for a singe row
		// 3(Cursor),The cursor containing the data
		// 4(StringArray) define the columns to us.
		// 5(intArray) the view of each column in the layout
		adapter = new SimpleCursorAdapter(this, R.layout.prime_info, cursor,
				fromColumns, toColumns, 0);
		super.setListAdapter(adapter);
	}

	/**
	 * Inner class that implements runnable to be able to run in a separate
	 * thread Checks a given value if it is a prime and if so, it will create a
	 * Message and send it to the handler and sleep for a short while so that
	 * the GUI Thread has time to update the view Constructor takes a long
	 * 
	 * @author Alex
	 * 
	 */
	class PrimeCounter implements Runnable {

		long num = 1;

		/**
		 * Constructor takes a long value as the starting number
		 * 
		 * @param startValue
		 */
		public PrimeCounter(long startValue) {
			num = startValue;
		}

		/**
		 * Constructor that does uses the current value 1 as startValue
		 */
		public PrimeCounter() {
		}

		/**
		 * Start checking when the Runnable is started
		 */
		@Override
		public void run() {
			check();
		}

		/**
		 * The Check method uses the current long and checks if it is less than
		 * the MAX value in a while loop, if the number is a prime. If it is a
		 * prime it sends the number back to the handler in a message object
		 * (bundle) and sleeps for a short while. If it is not a prime the
		 * method increments the current number by 2 and loops. When MAX has
		 * been reached. The method sends back a string with key "max" to the
		 * handler
		 */
		public void check() {
			try {
				while (num < MAX) {
					Message msg = Message.obtain();
					if (isPrime(num)) {

						msg.obj = num;
						handler.sendMessage(msg);

						Thread.sleep(500);
					}

					num += 2;

				}

				Message msg = Message.obtain();
				Log.d(TAG, "Counter has reached Max");
				Bundle bundle = new Bundle();
				bundle.putString("max", "Counter has reached Max");
				msg.setData(bundle);
				handler.sendMessage(msg);

				// If the Thread is interrupted.
			} catch (InterruptedException e) {
				Log.d(TAG, "Thread interrupted");
			}

		}

		/**
		 * Method that checks a candidate value whether it is a prime or not
		 * 
		 * @param candidate
		 *            the value to check
		 * @return returns true if the value is a prime and false if not.
		 */
		private boolean isPrime(long candidate) {

			long sqrt = (long) Math.sqrt(candidate);

			for (long i = 3; i <= sqrt; i += 2) {
				if (candidate % i == 0) {
					return false;
				}
			}
			return true;
		}

	}
}