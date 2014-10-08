package mobidata.project;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

public class S5Add2 extends Activity implements OnItemSelectedListener, OnClickListener{
	//variabili
	private String user;
	private double lat;
	private double lng;
	private String addrStr;
	private String catStr;
	//oggetti view
	EditText name;
	EditText addr;
	Spinner cat;
	EditText descr;
	Button save;
	Button canc;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.s5add2);
		//Recupero dati dall'intent
		Intent parentInt = getIntent();
		user = parentInt.getStringExtra("user");
		lat = parentInt.getDoubleExtra(POIProvider.Poi.LATITUDE, 0);
		lng = parentInt.getDoubleExtra(POIProvider.Poi.LONGITUDE, 0);
		//Id oggetti
		name = (EditText) findViewById(R.id.name);
		addr = (EditText) findViewById(R.id.addr);
		cat = (Spinner) findViewById(R.id.cat);
		descr = (EditText) findViewById(R.id.descr);
		save = (Button) findViewById(R.id.save);
		canc = (Button) findViewById(R.id.cancel);
		//Setup spinner categorie
		cat.setOnItemSelectedListener(this);
		ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, S2Main.categories);
        cat.setAdapter(catAdapter);
		//Listener bottoni
		save.setOnClickListener(this);
		canc.setOnClickListener(this);
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
		//Visualizzo l'indirizzo trovato come default
		addr.setText(addrStr);
	}
	/*
	 * Comportamento bottoni
	 */
	public void onClick(View v) {
		if(v == save){
			//Salvo nel db le informazioni relative al nuovo poi, e mostro la schermata S4Details
			ContentValues values = new ContentValues(5);
			values.put(POIProvider.Poi.NAME, name.getText().toString().trim());
			values.put(POIProvider.Poi.LATITUDE, lat+"");
			values.put(POIProvider.Poi.LONGITUDE, lng+"");
			values.put(POIProvider.Poi.CATEGORY, catStr);
			values.put(POIProvider.Poi.USER, user);
			values.put(POIProvider.Poi.ADDRESS, addrStr.trim());
			values.put(POIProvider.Poi.DESCRIPTION, descr.getText().toString().trim());
			//Operazione di insert sul db
			Uri uri = getContentResolver().insert(POIProvider.Poi.CONTENT_URI, values);
			String[] PROJECTION = {POIProvider.Poi._ID};
			Cursor curs = managedQuery(uri, PROJECTION, null, null, null);
			//Lancio S4
			Intent detInt = new Intent(this, S4Details.class);
			curs.moveToFirst();
			detInt.putExtra(POIProvider.Poi._ID, curs.getInt(0)+"");
			detInt.putExtra(S4Details.CODE_LABEL, S4Details.ADD_CODE);
			detInt.putExtra("user", user);
			startActivity(detInt);
		} else if(v == canc){
			//Termino l'activity
			finish();
		}
		
	}
	/*
	 * Comportamento spinner
	 */
	public void onItemSelected(AdapterView<?> parent, View v, int posit, long rowID) {
		catStr = S2Main.categories[posit];
	}
	public void onNothingSelected(AdapterView<?> arg0) {
		// do nothing
	}
}
