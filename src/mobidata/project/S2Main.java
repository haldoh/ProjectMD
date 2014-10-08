package mobidata.project;

import java.io.IOException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class S2Main extends ListActivity implements OnItemSelectedListener, OnClickListener{
	//costanti
	public static final String[] range = {"Tutti i POI", "100m","200m","500m","1km","2km"};
	public static final String[] categories = {"museo","cinema","ristorante","bar","shopping"};
	public static final String serverUser = "poiserveruser";
	public static final String POIServerUrl = "http://webmind.dico.unimi.it/care/poi_server.php";
	//variabili
	private int selectedRange = 0;
	private  String[] selectedCat = {"","","","",""};
	private String user;
	//oggetti view
	ListView cat;
	Button cerca;
	Button aggiungi;
	Button elimina;
	Button scarica;
	Button eliminaServer;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.s2main);      
        //Riferimenti ad oggetti nel layout
        TextView userText = (TextView) findViewById(R.id.userName);
        cerca = (Button) findViewById(R.id.cerca);
        aggiungi = (Button) findViewById(R.id.aggiungi);
        elimina = (Button) findViewById(R.id.elimina);
        scarica = (Button) findViewById(R.id.scarica);
        eliminaServer = (Button) findViewById(R.id.eliminaserver);
        //Attivo listener sui bottoni
        cerca.setOnClickListener(this);
        aggiungi.setOnClickListener(this);
        elimina.setOnClickListener(this);
        scarica.setOnClickListener(this);
        eliminaServer.setOnClickListener(this);
        //Recupera user dall'intent
        Intent parentInt = getIntent();
        user = parentInt.getStringExtra("user");
        if(user != null)
        	userText.setText(user);
        /*
         * setup lista raggio ricerca
         */
        Spinner rad = (Spinner) findViewById(R.id.range);
        rad.setOnItemSelectedListener(this);
        ArrayAdapter<String> radAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, range);
        rad.setAdapter(radAdapter);
        
        /*
         * setup lista categorie
         */
        cat = (ListView) findViewById(android.R.id.list);
        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(this, R.layout.list_item, categories);
        cat.setAdapter(catAdapter);
        cat.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        
	}
	/*
	 * Comportamento spinner
	 */
	public void onItemSelected(AdapterView<?> parent, View v, int posit, long rowID) {
		switch (posit){
		case 0:
			selectedRange = 0;
			break;
		case 1:
			selectedRange = 100;
			break;
		case 2:
			selectedRange = 200;
			break;
		case 3:
			selectedRange = 500;
			break;
		case 4:
			selectedRange = 1000;
			break;
		case 5:
			selectedRange = 2000;
			break;
		default:
			selectedRange = 0;
		}
	}
	public void onNothingSelected(AdapterView<?> parent) {
		// do nothing
	}
	/*
	 * Comportamento lista categorie
	 */
	public void onListItemClick(ListView parent, View v, int posit, long rowID){
		for(int i = 0; i < categories.length; i++){
			if(cat.isItemChecked(i))
				selectedCat[i] = categories[i];
			else
				selectedCat[i] = "";
		}
	}
	/*
	 * Determina l'azione da eseguire a seconda del bottone cliccato
	 */
	public void onClick(View v) {
		if(v == cerca){
			//Azione su bottone cerca - cerca POI, lancia activity S3List
			search();
		} else if(v == aggiungi){
			//Azione su bottone aggiungi - aggiungi nuovo POI, lancia activity S5Add
			Intent add = new Intent(this, S5Add.class);
			add.putExtra("user", user);
			startActivity(add);
		} else if (v == elimina){
			//Azione su bottone elimina - elimina POI personali
			//Creo messaggio per utente
			AlertDialog.Builder message = new AlertDialog.Builder(this);
			message.setTitle("Attenzione!");
			message.setMessage("Tutti i POI inseriti dall'utente " + user + " stanno per essere eliminati. Continuare?");
			message.setPositiveButton("Sì", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						//Elimina
						deletePOI(user);
					}
				});
			message.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						//Non fa nulla - Ritorna alla schermata di inserimento utente
					}
				});
			message.show();
		} else if (v == scarica){
			//Azione su bottone scarica - scarica POI da server
			getPOIFromServer();
		} else if (v == eliminaServer){
			//Azione su bottone elimnaServer - elimina POI scaricati
			//Creo messaggio per utente
			AlertDialog.Builder message = new AlertDialog.Builder(this);
			message.setTitle("Attenzione!");
			message.setMessage("Tutti i POI scaricati dal server stanno per essere eliminati. Continuare?");
			message.setPositiveButton("Sì", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						//Elimina poi da server
						deletePOI(serverUser);
					}
				});
			message.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dlg, int sumthin) {
						//Non fa nulla - Ritorna alla schermata di inserimento utente
					}
				});
			message.show();
		}
	}
	/*
	 * Avvia ricerca
	 */
	private void search(){
		Intent searchInt = new Intent(this, S3List.class);
		searchInt.putExtra("user", user);
		searchInt.putExtra("range", selectedRange);
		searchInt.putExtra("categories", selectedCat);
		startActivity(searchInt);
	}
	/*
	 * Scarica POI dal server
	 */
	private void getPOIFromServer() {
		//Inizializzo il gestore di richieste http
		HttpGet uriRequest = new HttpGet(POIServerUrl);
		HttpClient client = new DefaultHttpClient();
		ResponseHandler<String> ansHandler = new BasicResponseHandler();
		String answer = "";
		//Eseguo la richiesta
		try {
			answer = client.execute(uriRequest, ansHandler);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//Divido la stringa ottenuta in singoli POI
		String[] POIs = answer.split("\\|");
		//Per ciascun POI ottenuto, inserisco nel db
		for(String poi : POIs){
			insertPOI(poi, serverUser);
		}
	}
	/*
	 * Inserisci il POI nel db
	 */
	private void insertPOI(String poi, String owner) {
		//Separo i valori del singolo POI
		String[] parts = poi.split(";");
		ContentValues values = new ContentValues(5);
		values.put(POIProvider.Poi.NAME, parts[0].trim());
		values.put(POIProvider.Poi.LATITUDE, parts[1].trim());
		values.put(POIProvider.Poi.LONGITUDE, parts[2].trim());
		values.put(POIProvider.Poi.CATEGORY, parts[3].trim());
		values.put(POIProvider.Poi.USER, owner);
		//Operazione di insert sul db
		getContentResolver().insert(POIProvider.Poi.CONTENT_URI, values);
	}

	/*
	 * Elimina POI da db
	 */
	private void deletePOI(String owner){
		getContentResolver().delete(POIProvider.Poi.CONTENT_URI, POIProvider.Poi.USER + " = '" + owner + "'", null);
	}
}
