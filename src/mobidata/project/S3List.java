package mobidata.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class S3List extends ListActivity implements OnClickListener{
	//costanti
	private static final String[] PROJECTION = {POIProvider.Poi._ID,
												POIProvider.Poi.NAME,
												POIProvider.Poi.CATEGORY,
												POIProvider.Poi.LATITUDE,
												POIProvider.Poi.LONGITUDE};
	private static final int DETAILS_CODE = 50;
	public static final String DISTANCE_LABEL = "distance";
	public static final String LIST_POSITION_LABEL = "listPos";
	//variabili
	private String user;
	private int selectedRange = 0;
	private String[] selectedCat;
	private Cursor cursor;
	private ArrayList<Map<String,String>> orderedPOIs = new ArrayList<Map<String, String>>();
	private ArrayList<Map<String,String>> visiblePOIs = new ArrayList<Map<String, String>>();
	private String oldFirstPOIID = "";
	private String oldLastPOIID = "";
	private int oldSize = 0;
	private int[] visiblePOIsPos = {-1, -1, -1, -1, -1};
	private String[] columns = {POIProvider.Poi.CATEGORY, POIProvider.Poi.NAME, DISTANCE_LABEL};
    private int[] colIDs = {R.id.cat, R.id.name, R.id.dist};
	ListAdapter listAd;
	private int listPoint = 0;
	public Location loc;
	LocationManager locMan = null;
	String locProv = "";
	//oggetti view
	ListView list;
	TextView textIndex;
	Button back;
	Button next;
	Button map;
	Button add;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.s3list);
        //Recupera extra dall'intent
        Intent parentInt = getIntent();
        user = parentInt.getStringExtra("user");
        selectedRange = parentInt.getIntExtra("range", 0);
        selectedCat = parentInt.getStringArrayExtra("categories");
        //Recupera elementi layout
        list = (ListView) findViewById(android.R.id.list);
        textIndex = (TextView) findViewById(R.id.indexTxt);
        back = (Button) findViewById(R.id.indietro);
        next = (Button) findViewById(R.id.avanti);
        map = (Button) findViewById(R.id.map);
        add = (Button) findViewById(R.id.add);
        //Attivo listener
        back.setOnClickListener(this);
        next.setOnClickListener(this);
        map.setOnClickListener(this);
        add.setOnClickListener(this);
        /*
         * Locazione attuale
         */
        locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria cr = new Criteria();
        cr.setAccuracy(Criteria.ACCURACY_FINE);
        locProv = locMan.getBestProvider(cr, true);
        loc = locMan.getLastKnownLocation(locProv);
        LocationListener locListener = new LocationListener(){
			public void onLocationChanged(Location location) {
				updatedLocation();
			}
			public void onProviderDisabled(String provider) {
			}
			public void onProviderEnabled(String provider) {
			}
			public void onStatusChanged(String provider, int status, Bundle extras) {
			}
        };
        //Registro per update locazione
        locMan.requestLocationUpdates(locProv, 1000, 25, locListener);
        //creazione lista
        orderPOIsByLocation();
        filterByRange();
        setVisiblePOIs();
        setClickableButtons();
        
	}
	/*
	 * Operazioni da eseguire su update della locazione
	 */
	private void updatedLocation(){
		loc = locMan.getLastKnownLocation(locProv);
		//Ricalcola la lista di poi corrispondenti ai criteri scelti
		orderPOIsByLocation();
        filterByRange();
        setVisiblePOIs();
        setClickableButtons();
        /*
         * confronta con vecchio risultato per decidere se notificare l'utente
         * Se il primo o l'ultimo poi della lista è cambiato, o se è cambiato il numero di poi trovati, notifica l'utente        
         */
        if(oldSize != getPOIsSize() || !oldFirstPOIID.equals(getFirstPOIID()) || !oldLastPOIID.equals(getLastPOIID()))
        	notifyChange();
	}
	/*
	 * Notifica cambiamenti
	 */
	private void notifyChange(){
		Toast.makeText(this, "Lista di POI aggiornata in base alla nuova locazione", Toast.LENGTH_LONG).show();
	}
	/*
	 * Metodo per gestire i poi visualizzati nella schermata
	 */
	private void setVisiblePOIs() {
		visiblePOIs.clear();
		for(int i = listPoint; i < listPoint + 5; i++){
			if(i < orderedPOIs.size()){
				visiblePOIs.add(orderedPOIs.get(i));
				visiblePOIsPos[i-listPoint] = i;
			}
		}
		listAd = new SimpleAdapter(this, visiblePOIs, R.layout.poi_list_item, columns, colIDs);
		list.setAdapter(listAd);
		list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		int visibleIndex = listPoint+4 < orderedPOIs.size()-1 ? listPoint+4 : orderedPOIs.size()-1; 
		String text = "POI " + (listPoint+1) + "-" + (visibleIndex+1) + " su " + orderedPOIs.size();
		textIndex.setText(text);
	}
	/*
	 * Metodo per gestire la visualizzazione dei poi per vicinanza alla locazione attuale
	 * Restituisce una lista di poi rappresentati come Map, ordinati per distanza crescente
	 */
	private void orderPOIsByLocation() {
		String where = whereBuild();
        cursor = managedQuery(POIProvider.Poi.CONTENT_URI, PROJECTION, where, null, null);
		int columnNum = cursor.getColumnCount();
		//se orderedPOIs era già popolato, salvo il primo e l'ultimo elemento della lista, e la sua dimensione
		oldFirstPOIID = getFirstPOIID();
		oldLastPOIID = getLastPOIID();
		oldSize = getPOIsSize();
		orderedPOIs.clear();
		//Posiziona il cursore sul primo elemento ottenuto dalla query
		if(cursor.moveToFirst()){
			do{
				//scorre i risultati della query ed acquisisce i dati
				HashMap<String, String> singlePOI = new HashMap<String, String>();
				for(int i = 0; i < columnNum; i++)
					singlePOI.put(cursor.getColumnName(i), cursor.getString(i));
				//Associa all'elemento la giusta icona
				String cat = singlePOI.get(POIProvider.Poi.CATEGORY);
				String catIcon = getCatIcon(cat);
				singlePOI.put(POIProvider.Poi.CATEGORY, catIcon);
				//calcola distanza
				int distance = (int)getDistance(singlePOI);
				//inserisci distanza nel poi
				singlePOI.put(DISTANCE_LABEL, distance+"");
				//inserisci poi nella lista
				insertInOrder(singlePOI, orderedPOIs);
			}while(cursor.moveToNext());
		}
	}
	/*
	 * Associa ad ogni categoria di POI la giusta icona
	 */
	private String getCatIcon(String cat) {
		if(cat.equals("museo"))
			return R.drawable.museo+"";
		if(cat.equals("bar"))
			return R.drawable.bar+"";
		if(cat.equals("cinema"))
			return R.drawable.cinema+"";
		if(cat.equals("ristorante"))
			return R.drawable.ristorante+"";
		if(cat.equals("shopping"))
			return R.drawable.shopping+"";
		return "0";
	}
	/*
	 * Elimina gli elementi fuori dal range specificato
	 */
	private void filterByRange(){
		if(selectedRange > 0){
			int i = 0;
			int tmp = Integer.parseInt(orderedPOIs.get(0).get(DISTANCE_LABEL));
			while(i < orderedPOIs.size()-1 && tmp <= selectedRange){
				tmp = Integer.parseInt(orderedPOIs.get(i+1).get(DISTANCE_LABEL));
				i++;
			}
			for(int j = orderedPOIs.size()-1; j > i; j--){
				orderedPOIs.remove(orderedPOIs.size()-1);
			}
		}
	}
	/*
	 * Inserisce il poi singlePOI nella lista in ordine crescente di distanza dalla locazione attuale
	 */
	private int insertInOrder(HashMap<String, String> singlePOI, ArrayList<Map<String, String>> orderedPOIs) {
		float distance = Float.parseFloat(singlePOI.get(DISTANCE_LABEL));
		//Se lista è vuota oppure l'elemento è più distente dell'ultimo elemento della lista, inserisci in fondo
		if(orderedPOIs.isEmpty() || distance >= Float.parseFloat(orderedPOIs.get(orderedPOIs.size()-1).get(DISTANCE_LABEL))){
			orderedPOIs.add(singlePOI);
			return 0;
		}
		//altrimenti scorro la lista per trovare la posizione giusta
		else{
			for(int i = 0; i < orderedPOIs.size(); i++){
				if(distance < Float.parseFloat(orderedPOIs.get(i).get(DISTANCE_LABEL))){
					orderedPOIs.add(i, singlePOI);
					return 0;
				}
			}
		}
		return -1;
	}
	/*
	 * Calcola la distanza fra la posizione attuale e il poi rappresentato da singlePOI
	 */
	private float getDistance(HashMap<String, String> singlePOI) {
		//ottieni lat e long del poi
		double latitude = Double.parseDouble(singlePOI.get(POIProvider.Poi.LATITUDE));
		double longitude = Double.parseDouble(singlePOI.get(POIProvider.Poi.LONGITUDE));
		Location poiLoc = new Location(loc);
		poiLoc.setLatitude(latitude);
		poiLoc.setLongitude(longitude);
		//calcola distanza
		float result = loc.distanceTo(poiLoc);
		return result;
	}
	/*
	 * Costruisce la clausola where in base alle categorie selezionate.
	 * Se nessuna categoria selezionata, cerca fra tutti i poi.
	 * I poi vengono cercati fra i poi del server e quelli dell'utente connesso.
	 */
	private String whereBuild() {
		String temp = "";
		for(int i = 0; i < selectedCat.length; i++){
			if(!selectedCat[i].equals("")){
				if(!temp.equals(""))
					temp = temp + " or ";
				temp = temp + POIProvider.Poi.CATEGORY + " = '" + selectedCat[i] + "' ";
			}
		}
		if(temp.equals(""))
			return "user = '" + S2Main.serverUser + "' or user = '" + user + "'";
			//return null;
		else
			return "(" + temp + ") and (user = '" + S2Main.serverUser + "' or user = '" + user + "')";
			//return temp;
	}
	/*
	 * Operazioni su selezione elemento lista
	 */
	public void onListItemClick(ListView parent, View v, int posit, long rowID){
		int pos = list.getCheckedItemPosition();
		Map<String, String> poi = orderedPOIs.get(visiblePOIsPos[pos]);
		Intent detailsInt = new Intent(this, S4Details.class);
		detailsInt.putExtra(POIProvider.Poi._ID, poi.get(POIProvider.Poi._ID));
		detailsInt.putExtra(LIST_POSITION_LABEL, visiblePOIsPos[pos]);
		detailsInt.putExtra(S4Details.CODE_LABEL, S4Details.LIST_CODE);
		detailsInt.putExtra("user", user);
		startActivityForResult(detailsInt, DETAILS_CODE);
	}
	/*
	 * Comportamento bottoni back e next
	 */
	public void onClick(View v) {
		if(v == back){
			//Se premo tasto back e posso tornare indietro, decrementa puntatore
			if(listPoint >= 5){
				listPoint = listPoint - 5;
				setVisiblePOIs();
				setClickableButtons();
			}
		}
		else if(v == next){
			//Se premo tasto next, e ci sono altri poi da visualizzare, incremento contatore
			listPoint = listPoint + 5;
			setVisiblePOIs();
			setClickableButtons();
		}
		else if(v == add){
			//Tasto add mostra schermata S5Add
			Intent addInt = new Intent(this, S5Add.class);
			addInt.putExtra("user", user);
			startActivity(addInt);
		}
		else if(v == map){
			//tasto map mostra la mappa
			Intent mapInt = new Intent(this, S6Map.class);
			mapInt.putExtra("user", user);
			mapInt.putExtra(S6Map.POI_MAP_LABEL, S6Map.ALL_POI_CODE);
			startActivity(mapInt);
		}
	}
	/*
	 * imposta possibilità di cliccare bottoni avanti e indietro
	 */
	private void setClickableButtons(){
		if(listPoint == 0)
			back.setClickable(false);
		else
			back.setClickable(true);
		if(listPoint + 5 < orderedPOIs.size())
			next.setClickable(true);
		else
			next.setClickable(false);
	}
	/*
	 * Ritorno dall'activity S4Details dopo eliminazione POI
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == DETAILS_CODE && data != null){
			//Rimuovo dalla lista il poi eliminato
			orderedPOIs.remove(data.getIntExtra(LIST_POSITION_LABEL, -1));
	        //resetto visualizzazione
			listPoint = 0;
			setVisiblePOIs();
	        setClickableButtons();
		}
	}
	/*
	 * L'ID del primo POI della lista ordinata
	 */
	private String getFirstPOIID(){
		if(!orderedPOIs.isEmpty())
			return orderedPOIs.get(0).get(POIProvider.Poi._ID);
		else
			return "";
	}
	/*
	 * L'ID dell'ultimo POI della lista ordinata
	 */
	private String getLastPOIID(){
		if(!orderedPOIs.isEmpty())
			return orderedPOIs.get(orderedPOIs.size()-1).get(POIProvider.Poi._ID);
		else
			return "";
	}
	/*
	 * La dimensione della lista ordinata di POI
	 */
	private int getPOIsSize(){
		if(!orderedPOIs.isEmpty())
			return orderedPOIs.size();
		else
			return 0;
	}
}
