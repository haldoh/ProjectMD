package mobidata.project;

import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class S6Map extends MapActivity{
	//costanti
	public static final int ALL_POI_CODE = 87;
	public static final int ONE_POI_CODE = 88;
	public static final String POI_MAP_LABEL = "poimaplabel";
	private static final String[] PROJ = {POIProvider.Poi.NAME,
							 POIProvider.Poi.CATEGORY,
							 POIProvider.Poi.LATITUDE,
							 POIProvider.Poi.LONGITUDE,
							 POIProvider.Poi._ID
							 };
	//variabili
	private int req;
	private String user;
	private Cursor cursor;
	//oggetti view
	MapView map;
	MapController mapC;
	Location loc;
	GeoPoint locPoint;
	GeoPoint poiPoint;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.s6map);
        //recupera user da intent
        Intent parentInt = getIntent();
        user = parentInt.getStringExtra("user");
        req = parentInt.getIntExtra(POI_MAP_LABEL, 0);
        //Locazione attuale
        Criteria cr = new Criteria();
        cr.setAccuracy(Criteria.ACCURACY_FINE);
        LocationManager locMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        String locProv = null;
        locProv = locMan.getBestProvider(cr, true);
        loc = locMan.getLastKnownLocation(locProv);
        //Gestione mappa
        map = (MapView) findViewById(R.id.map);
        mapC = map.getController();
        locPoint = new GeoPoint((int)(loc.getLatitude()*1E6), (int)(loc.getLongitude()*1E6));
        mapC.animateTo(locPoint);
        //Query
        String where = "";
        if(req == S6Map.ALL_POI_CODE)
        	where = POIProvider.Poi.USER + " = '" + S2Main.serverUser + "' or " + POIProvider.Poi.USER + "= '" + user + "'";
        else if(req == S6Map.ONE_POI_CODE)
        	where = POIProvider.Poi._ID + "= '" + parentInt.getStringExtra(POIProvider.Poi._ID) +"'";
        cursor = managedQuery(POIProvider.Poi.CONTENT_URI, PROJ, where, null, null);
        //Visualizzo locazione utente e poi
        List<Overlay> listOv = map.getOverlays();
        MapItemOverlay mapOverlay = new MapItemOverlay(this.getResources().getDrawable(R.drawable.icon), this);
        OverlayItem myLoc = new OverlayItem(locPoint, "La Mia Posizione", "La Mia Posizione");
        myLoc.setMarker(this.getResources().getDrawable(R.drawable.pin));
        mapOverlay.addOverlay(myLoc);
        //Carica POI
        if(cursor != null){
        	cursor.moveToFirst();
        	do{
        		mapOverlay.addOverlay(convPoiToOverlay(cursor));
        	}while(cursor.moveToNext());
        }
        listOv.add(mapOverlay);
        //Opzioni zoom
        mapC.setZoom(15);
        map.setBuiltInZoomControls(true);
	}
	protected boolean isRouteDisplayed() {
		return false;
	}
	/*
	 * Converte il POI puntato dal cursore in un OverlayItem
	 */
	private OverlayItem convPoiToOverlay(Cursor cursor){
		//Coordinate poi
		int lat = (int) (Double.parseDouble(cursor.getString(cursor.getColumnIndex(POIProvider.Poi.LATITUDE)))*1E6);
		int lng = (int) (Double.parseDouble(cursor.getString(cursor.getColumnIndex(POIProvider.Poi.LONGITUDE)))*1E6);
		GeoPoint p = new GeoPoint(lat, lng);
		//Nuovo oggetto OverlayItem
		OverlayItem item = new OverlayItem(p, cursor.getString(cursor.getColumnIndex(POIProvider.Poi.NAME)), cursor.getString(cursor.getColumnIndex(POIProvider.Poi._ID)));
		//icona categoria
		String cat = cursor.getString(cursor.getColumnIndex(POIProvider.Poi.CATEGORY));
		if(cat.equals("museo"))
			item.setMarker(this.getResources().getDrawable(R.drawable.museo));
		else if(cat.equals("bar"))
			item.setMarker(this.getResources().getDrawable(R.drawable.bar));
		else if(cat.equals("shopping"))
			item.setMarker(this.getResources().getDrawable(R.drawable.shopping));
		else if(cat.equals("cinema"))
			item.setMarker(this.getResources().getDrawable(R.drawable.cinema));
		else if(cat.equals("ristorante"))
			item.setMarker(this.getResources().getDrawable(R.drawable.ristorante));
		return item;
	}
	/*
	 * Sottoclasse di ItemizedOverlay
	 */
	private class MapItemOverlay extends ItemizedOverlay<OverlayItem>{
		private ArrayList<OverlayItem> mOverlay = new ArrayList<OverlayItem>();
		private Context mContext;

		public MapItemOverlay(Drawable defaultMarker){
			super(boundCenterBottom(defaultMarker));
		}
		public MapItemOverlay(Drawable defaultMarker, Context context){
			super(defaultMarker);
			mContext = context;
		}
		public void addOverlay(OverlayItem item){
			mOverlay.add(item);
			populate();
		}
		protected OverlayItem createItem(int i){
			OverlayItem item = mOverlay.get(i);
			OverlayItem newItem = new OverlayItem(item.getPoint(), item.getTitle(), item.getSnippet());
			newItem.setMarker(boundCenterBottom(item.getMarker(0)));
			return newItem;
		}
		public int size(){
			return mOverlay.size();
		}
		protected boolean onTap(int index){
			if(index == 0)
				//Fa apparire un toast se l'oggetto tappato è la mia locazione
				Toast.makeText(mContext, mOverlay.get(index).getTitle(), Toast.LENGTH_LONG).show();
			else{
				//Avvia S4Details se tap su poi
				Intent detInt = new Intent(mContext, S4Details.class);
				detInt.putExtra("user", user);
				detInt.putExtra(S4Details.CODE_LABEL, S4Details.MAP_CODE);
				detInt.putExtra(POIProvider.Poi._ID, mOverlay.get(index).getSnippet());
				startActivity(detInt);
			}
			return true;
		}
	}
}
