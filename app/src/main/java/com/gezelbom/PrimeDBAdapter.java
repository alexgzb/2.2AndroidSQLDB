package com.gezelbom;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Class that is a DatabaseAdapter for a SQLite Database that stores Prime
 * numbers and the timeOfStorage in a simple table called PRIMETABLE
 * Inner static class that implements a SQLiteOpenHelper to create and update the DB
 * 
 * @author Alex
 * 
 */
public class PrimeDBAdapter {

	private static final String TAG = "PrimeDBAdapter";

	// Variables
	private DatabaseHelper helper;
	private SQLiteDatabase database;
	private final Context context;

	// DB info
	private static final String DATABASE_NAME = "primedatabase";
	private static final String TABLE_NAME = "PRIMETABLE";
	private static final int DATABASE_VERSION = 1;

	// Columns
	public static final String COL_UID = "_id";
	public static final String COL_TIME = "timeOfStorage";
	public static final String COL_VALUE = "value";
	
	// SQL Statements
	private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME
			+ "(" + COL_UID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_TIME
			+ " TEXT, " + COL_VALUE + ", INTEGER);";
	private static final String DROP_TABLE = "DROP TABLE IF EXISTS "
			+ TABLE_NAME + ";";

	/**
	 * Constructor for the DBAdapter
	 * 
	 * @param c
	 *            Takes the context from which it has been initialised
	 */
	public PrimeDBAdapter(Context c) {
		context = c;
	}

	/**
	 * Method that where database object is created and ready to be used.
	 * 
	 * @return an instance of this class
	 * 
	 * @throws SQLException
	 */
	public void open() throws SQLException {
		helper = new DatabaseHelper(context);
		database = helper.getWritableDatabase();
	}

	/**
	 * Close the DBHelper object if it exists
	 */
	public void close() {
		if (helper != null) {
			helper.close();
		}
	}

	/**
	 * Insert a singel Prime value uses SQLite function datetime() to store the current date and time
	 * @param value
	 */
	public void createRow(Long value) {
		//Must use the execSQL to be able to use the datetime() function.
		String insertValue = "INSERT INTO " + TABLE_NAME + "(" + COL_TIME
				+ ", " + COL_VALUE + ") VALUES(datetime(), " + value + ");";
		database.execSQL(insertValue);
		Log.d(TAG, "Inserting to db");
	}

	/**
	 * Method that deletes all rows from the table
	 */
	public void deleteAllRows() {
		Log.d(TAG, "Deleting all rows in table");
		int rows = database.delete(TABLE_NAME, null, null);
		Log.d(TAG, "Deleted " + rows + " rows");
	}

	/**
	 * Returns an int row count
	 * @return
	 */
	public int getCount() {
		return database.query(TABLE_NAME,
				new String[] { COL_UID, COL_VALUE, COL_TIME }, null, null,
				null, null, null).getCount();
	}

	/**
	 * Returns an Cursor object containing all rows from PRIMETABLES.
	 * @return
	 */
	public Cursor fetchAllRows() {

		Cursor cursor = database.query(TABLE_NAME, new String[] { COL_UID,
				COL_VALUE, COL_TIME }, null, null, null, null, null);

		if (cursor != null) {
			cursor.moveToFirst();
		}
		return cursor;
	}

	/**
	 * The inner Class that extends SQLiteOpenHelper and gives direct access to the db
	 * The methods onCreate and onUpgrade must be and are implemented.
	 * @author Alex
	 *
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		/**
		 * Constructor takes only the context but uses the information found in the 
		 * Helper to create the DB with the super call.
		 * 
		 * @param context
		 */
		public DatabaseHelper(Context context) {
			//Context, database_name, CursorFactory, DBVersion
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		/**
		 * Creates the database using the CREATE_TABLE Statement
		 */
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_TABLE);
			Log.d(TAG, "Creating Database");
		}

		/**
		 * Firstly drop the current Table, and the create a new table.
		 */
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion);
			db.execSQL(DROP_TABLE);
			onCreate(db);
		}

	}
}
