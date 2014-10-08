package mobidata.project;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class S4Details extends Activity implements OnClickListener{
	//costanti
	private static final String[] PROJECTION = {POIProvider.Poi._ID,
												POIProvider.Poi.NAME,
												POIProvider.Poi.CATEGORY,
												POIProvider.Poi.LATITUDE,
												POIProvider.Poi.LONGITUDE,
												POIProvider.Poi.ADDRESS,
												POIProvider.Poi.DESCRIPTION};
	public static final int LIST_CODE = 77;
	public static final int ADD_CODE = 78;
	public static final int MAP_CODE = 79;
	public static final String CODE_LABEL = "codelabel";
	//variabili
	private Cursor cursor;
	private String user;
	private String catStr;
	private String nameStr;
	private String descrStr;
	private String addrStr;
	private String id;
	private int listPos;
	private int req;
	//oggetti view
	TextView cat;
	TextView name;
	TextView descr;
	TextView addr;
	Button delete;
	Button map;
	Button home;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.s4details);
        //recupero dati dall'intent
        Intent parentInt = getIntent();
        id = parentInt.getStringExtra(POIProvider.Poi._ID);
        user = parentInt.getStringExtra("user");
        req = parentInt.getIntExtra(CODE_LABEL, 0);
        if(req == LIST_CODE)
        	listPos = parentInt.getIntExtra(S3List.LIST_POSITION_LABEL, -1);
        cursor = managedQuery(POIProvider.Poi.CONTENT_URI, PROJECTION, POIProvider.Poi._ID + "='" + id + "'", null, null);
        cursor.moveToFirst();
        nameStr = cursor.getString(cursor.getColumnIndex(POIProvider.Poi.NAME));
        catStr = cursor.getString(cursor.getColumnIndex(POIProvider.Poi.CATEGORY));
        addrStr = cursor.getString(cursor.getColumnIndex(POIProvider.Poi.ADDRESS));
        descrStr = cursor.getString(cursor.getColumnIndex(POIProvider.Poi.DESCRIPTION));
        //Oggetti layout
        cat = (TextView) findViewById(R.id.cat);
        name = (TextView) findViewById(R.id.name);
        addr = (TextView) findViewById(R.id.addr);
        descr = (TextView) findViewById(R.id.descr);
        delete = (Button) findViewById(R.id.elimina);
        map = (Button) findViewById(R.id.mappa);
        home = (Button) findViewById(R.id.home);
        delete.setOnClickListener(this);
        map.setOnClickListener(this);
        home.setOnClickListener(this);
        if(addrStr == null){
        	//coordinate poi
        	double lat = Double.parseDouble(cursor.getString(cursor.getColumnIndex(POIProvider.Poi.LATITUDE)));
			double lng = Double.parseDouble(cursor.getString(cursor.getColumnIndex(POIProvider.Poi.LONGITUDE)));
			//Determino indirizzo nuovo poi
			Geocoder geoCoder = new Geocoder(getBaseContext(), Locale.getDefault());
			addrStr = "";
			try{
				List<Address> addresses = geoCoder.getFromLocation(lat, lng, 1);
				if(addresses.size() > 0){
					for (int i = 0; i < addresses.get(0).getMaxAddressLineIndex(); i++)
						addrStr += addresses.get(0).getAddressLine(i) + " ";
				}
			}
			catch(IOException e){
				e.printStackTrace();
			}
			//Se trovo un indirizzo, lo salvo nel db
			if(!addrStr.equals("")){
				ContentValues values = new ContentValues();
				values.put(POIProvider.Poi.ADDRESS, addrStr);
				getContentResolver().update(POIProvider.Poi.CONTENT_URI, values, POIProvider.Poi._ID + "= '" + id + "'", null);
			}
        }
        //Visualizza informazioni
        name.setText(nameStr);
        cat.setText(catStr);
        addr.setText(addrStr);
        descr.setText(descrStr);
	}
	/*
	 * Comportamento bottoni
	 */
	public void onClick(View v) {
		if(v == map){
			//Tasto map mostra la mappa con posizione utente e pos poi
			Intent mapInt = new Intent(this, S6Map.class);
			mapInt.putExtra("user", user);
			mapInt.putExtra(S6Map.POI_MAP_LABEL, S6Map.ONE_POI_CODE);
			mapInt.putExtra(POIProvider.Poi._ID, id);
			startActivity(mapInt);
		}else if(v == delete){
			//Tasto delete elimina il poi, e ritorna all'activity corretta
			AlertDialog.Builder message = new AlertDialog.Builder(this);
			message.setTitle("Attenzione!");
			message.setMessage("Questo POI sta per essere eliminato. Continuare?");
			message.setPositiveButton("Sì", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						//Elimino poi
						deletePOI();
					}
				});
			message.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						//Non fa nulla - Ritorna alla schermata di inserimento utente
					}
				});
			message.show();	
		} else if(v == home){
			//Ritorna alla schermata principale
			Intent mainInt = new Intent(this, S2Main.class);
			mainInt.putExtra("user", user);
			startActivity(mainInt);
		}
	}
	/*
	 * Cancella POI e definisce comportamento in base all'activity che ha aperto i dettagli
	 */
	private void deletePOI(){
		getContentResolver().delete(POIProvider.Poi.CONTENT_URI, POIProvider.Poi._ID + "='" + id + "'", null);
		Intent returnInt;
		if(req == LIST_CODE){
			returnInt  = new Intent();
			returnInt.putExtra(S3List.LIST_POSITION_LABEL, listPos);
			setResult(RESULT_OK, returnInt);
			finish();
		} else if(req == ADD_CODE){
			returnInt = new Intent(this, S2Main.class);
			returnInt.putExtra("user", user);
			startActivity(returnInt);
		} else if(req == MAP_CODE){
			returnInt = new Intent(this, S6Map.class);
			returnInt.putExtra("user", user);
			returnInt.putExtra(S6Map.POI_MAP_LABEL, S6Map.ALL_POI_CODE);
			startActivity(returnInt);
		}
	}
}
