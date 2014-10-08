package mobidata.project;

import java.util.HashMap;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
/*
 * Content Provider per il database dell'applicazione
 */
public class POIProvider extends ContentProvider{
	//costanti
	private static final String DB_NAME = "POIdb";
	private static final String POI_TABLE = "poi";
	private static final String USERS_TABLE = "users";
	private static final int POI = 1;
	private static final int POI_ID = 2;
	private static final int USERS = 3;
	private static final int USERS_ID = 4;
	private static final UriMatcher MATCHER;
	//variabili
	private static HashMap<String, String> POI_PROJECTION;
	private static HashMap<String, String> USERS_PROJECTION;
	private SQLiteDatabase poidb;
	/*
	 * Struttura della tabella dei POI
	 */
	public static final class Poi implements BaseColumns{
		// URI
		public static final Uri CONTENT_URI = Uri.parse("content://mobidata.project.POIProvider/" + POI_TABLE);
		// ordinamento di default
		public static final String DEFAULT_SORT_ORDER = "name";
		// nomi degli attributi
		public static final String NAME = "name";
		public static final String CATEGORY = "cat";
		public static final String DESCRIPTION = "desc";
		public static final String ADDRESS = "addr";
		public static final String LONGITUDE = "long";
		public static final String LATITUDE = "lat";
		public static final String USER = "user";
	}
	/*
	 * definizione dei riferimenti agli elementi della classe Poi
	 */
	static {
		MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		MATCHER.addURI("mobidata.project.POIProvider", POI_TABLE, POI);
		MATCHER.addURI("mobidata.project.POIProvider", POI_TABLE + "/#", POI_ID);
		POI_PROJECTION = new HashMap<String, String>();
		POI_PROJECTION.put(POIProvider.Poi._ID, POIProvider.Poi._ID);
		POI_PROJECTION.put(POIProvider.Poi.NAME, POIProvider.Poi.NAME);
		POI_PROJECTION.put(POIProvider.Poi.CATEGORY, POIProvider.Poi.CATEGORY);
		POI_PROJECTION.put(POIProvider.Poi.DESCRIPTION, POIProvider.Poi.DESCRIPTION);
		POI_PROJECTION.put(POIProvider.Poi.ADDRESS, POIProvider.Poi.ADDRESS);
		POI_PROJECTION.put(POIProvider.Poi.LONGITUDE, POIProvider.Poi.LONGITUDE);
		POI_PROJECTION.put(POIProvider.Poi.LATITUDE, POIProvider.Poi.LATITUDE);
		POI_PROJECTION.put(POIProvider.Poi.USER, POIProvider.Poi.USER);
	}
	/*
	 * Struttura della tabella Users
	 */
	public static final class Users implements BaseColumns{
		//URI
		public static final Uri CONTENT_URI = Uri.parse("content://mobidata.project.POIProvider/" + USERS_TABLE);
		// ordinamento di default
		public static final String DEFAULT_SORT_ORDER = "name";
		// nomi degli attributi
		public static final String NAME = "name";
	}
	/*
	 * definizione dei riferimenti agli elementi della classe Users
	 */
	static {
		MATCHER.addURI("mobidata.project.POIProvider", USERS_TABLE, USERS);
		MATCHER.addURI("mobidata.project.POIProvider", USERS_TABLE + "/#", USERS_ID);
		USERS_PROJECTION = new HashMap<String, String>();
		USERS_PROJECTION.put(POIProvider.Users._ID, POIProvider.Users._ID);
		USERS_PROJECTION.put(POIProvider.Users.NAME, POIProvider.Users.NAME);
	}
	/*
	 * Definizione del database gestito dal Content Provider
	 */
	private class POIdb extends SQLiteOpenHelper{
		//Constructor
		public POIdb(Context context){
			super(context, DB_NAME, null, 1);
		}
		public void onCreate(SQLiteDatabase db) {
			Cursor c;
			//Se le tabelle poi o users non esistono, vengono create
			//Cerca tabella poi
			c = db.rawQuery("SELECT * FROM sqlite_master WHERE type = 'table' and name = '" + POI_TABLE + "';", null);
			try{
				if(c.getCount() == 0){
					//crea tabella poi
					String sql ="CREATE TABLE " + POI_TABLE + "(" +
								Poi._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
								Poi.NAME + " TEXT, " +
								Poi.CATEGORY + " TEXT, " +
								Poi.DESCRIPTION + " TEXT, " +
								Poi.ADDRESS + " TEXT, " +
								Poi.LONGITUDE + " TEXT, " +
								Poi.LATITUDE + " TEXT, " +
								Poi.USER + " TEXT" +
								");";
					db.execSQL(sql);
				}
			} finally {
				c.close();
			}
			//Cerca tabella users
			c = db.rawQuery("SELECT * FROM sqlite_master WHERE type = 'table' and name = '" + USERS_TABLE + "';", null);
			try{
				if(c.getCount() == 0){
					//crea tabella users
					String sql ="CREATE TABLE " + USERS_TABLE + "(" +
								Users._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
								Users.NAME + " TEXT" +
								");";
					db.execSQL(sql);
				}
			} finally {
				c.close();
			}
		}
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			android.util.Log.w("POIdb", "Questo aggiornamento cancella tutto il contenuto del db e lo reinizializza...");
			db.execSQL("DROP TABLE IF EXISTS " + POI_TABLE + ";");
			db.execSQL("DROP TABLE IF EXISTS " + USERS_TABLE + ";");
			onCreate(db);
		}
	}
	/*
	 * Implementazione onCreate del Content Provider
	 */
	public boolean onCreate() {
		poidb = (new POIdb(getContext())).getWritableDatabase();
		return (poidb == null) ? false : true;
	}
	/*
	 * Implementazione query del content provider
	 */
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		//Variabili
		SQLiteQueryBuilder queryB = new SQLiteQueryBuilder();
		String order;
		Cursor curs;
		//Definizione tabella su cui eseguire la query
		queryB.setTables(getTableFromUri(uri));
		//Definizione colonne da mostrare nella query
		if(isTableUri(uri)){
			queryB.setProjectionMap(getProjection(uri));
		} else {
			queryB.appendWhere("_id = " + uri.getPathSegments().get(1));
		}
		//Definizione ordine
		if(TextUtils.isEmpty(sortOrder))
			order = getDefaultSortOrder(uri);
		else
			order = sortOrder;
		//Esecuzione query
		curs = queryB.query(poidb, projection, selection, selectionArgs, null, null, order);
		return curs;
	}
	/*
	 * Ritorna l'ordinamento di default della tabella
	 */
	private String getDefaultSortOrder(Uri uri) {
		String table = getTableFromUri(uri);
		if(table.equals(POI_TABLE))
			return Poi.DEFAULT_SORT_ORDER;
		if(table.equals(USERS_TABLE))
			return Users.DEFAULT_SORT_ORDER;
		else
			return null;
	}
	/*
	 * Ritorna la projection list della tabella
	 */
	private HashMap<String,String> getProjection(Uri uri){
		int match = MATCHER.match(uri);
		switch (match){
			case POI:
				return POI_PROJECTION;
			case USERS:
				return USERS_PROJECTION;
			default:
				return null;
		}
	}
	/*
	 * Ritorna vero se l'Uri è riferito alle tabelle poi o users
	 */
	private boolean isTableUri(Uri uri) {
		int match = MATCHER.match(uri);
		return(match == POI || match == USERS);
	}
	/*
	 * Ritorna la tabella riferita dall'Uri
	 */
	private String getTableFromUri(Uri uri) {
		int match = MATCHER.match(uri);		
		if(match == POI || match == POI_ID)
			return POI_TABLE;
		else if(match == USERS || match == USERS_ID)
			return USERS_TABLE;
		else
			return null;
	}
	/*
	 * Implementazione delete del content provider
	 */
	public int delete(Uri uri, String where, String[] whereArg) {
		int result = 0;
		//Richiamo metodo delete del database
		result = poidb.delete(getTableFromUri(uri), where, whereArg);
		return result;
	}
	/*
	 * Implementazione insert del content provider
	 */
	public Uri insert(Uri uri, ContentValues argVal) {
		ContentValues val;
		long rowID;
		//Controllo se i valori sono presenti
		if(argVal != null){
			val = new ContentValues(argVal);
		} else{
			throw new IllegalArgumentException("Nessun valore da inserire.");
		}
		//Operazioni sull'uri dipendenti dalla tabella
		String table = getTableFromUri(uri);
		String nullColumnHack = null;
		Uri contentUri = null;
		if(table.equals(POI_TABLE)){
			nullColumnHack = Poi.NAME;
			contentUri = Poi.CONTENT_URI;
		}
		else if(table.equals(USERS_TABLE)){
			nullColumnHack = Users.NAME;
			contentUri = Users.CONTENT_URI;
		}
		//richiamo metodo insert del database
		rowID = poidb.insert(getTableFromUri(uri), nullColumnHack, val);
		//notifica modifica dati
		if(rowID > 0){
			Uri endUri = ContentUris.withAppendedId(contentUri, rowID);
			getContext().getContentResolver().notifyChange(endUri, null);
			return endUri;
		}
		throw new SQLException("Operazione insert fallita su " + uri);
	}
	/*
	 * Implementazione update del content provider
	 */
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		int result = 0;
		//Esecuzione update sul db
		result = poidb.update(getTableFromUri(uri), values, selection, selectionArgs);
		return result;
	}
	/*
	 * Implementazione getType del content provider
	 */
	public String getType(Uri uri) {
		int match = MATCHER.match(uri);
		String collType = "vnd.android.cursor.dir/vnd.mobidata.project.";
		String singType = "vnd.android.cursor.item/vnd.mobidata.project.";
		switch (match){
			case POI:
				return collType + POI_TABLE;
			case USERS:
				return collType + USERS_TABLE;
			case POI_ID:
				return singType + POI_TABLE;
			case USERS_ID:
				return singType + USERS_TABLE;
			default:
				return null;
		}
	}
}
