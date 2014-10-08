package mobidata.project;

import java.util.List;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

public class S5Add extends MapActivity{
	//variabili
	private String user;
	//oggetti view
	MapView map;
	MapController mapC;
	GeoPoint locPoint;
	Location loc;
	GeoPoint newPoint;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.s5add);
        //Recupera user da intent
        Intent parentInt = getIntent();
        user = parentInt.getStringExtra("user");
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
        //Visualizzo locazione utente
        AddMapOverlay mapOv = new AddMapOverlay();
        List<Overlay> listOv = map.getOverlays();
        listOv.clear();
        listOv.add(mapOv);
        map.invalidate();
        //Opzioni zoom
        mapC.setZoom(15);
        map.setBuiltInZoomControls(true);
        //Menu
        registerForContextMenu(map);
	}
	protected boolean isRouteDisplayed() {
		return false;
	}
	/*
	 * Creazione menu
	 */
	public void onCreateContextMenu (ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo){
		if(v == map){
			menu.setHeaderTitle("Aggiunta POI");
			menu.add(0, Menu.FIRST+1, 0, "Crea POI");	  
		  }
	}
	/*
	 * Comportamento menu
	 */
	public boolean onContextItemSelected(MenuItem item){
		int id = item.getItemId();
		if(id == Menu.FIRST+1){
			//Avvio una nuova activity passando le coordinate del nuovo poi e l'utente che lo sta creando
			Intent addInt = new Intent(this, S5Add2.class);
			addInt.putExtra(POIProvider.Poi.LATITUDE, newPoint.getLatitudeE6()/1E6);
			addInt.putExtra(POIProvider.Poi.LONGITUDE, newPoint.getLongitudeE6()/1E6);
			addInt.putExtra("user", user);
			startActivity(addInt);
    		return true;
		}
		return super.onContextItemSelected(item);
	}
	/*
	 * Classe per gestione overlay e tocco
	 */
	private class AddMapOverlay extends Overlay{
		private int lastEvent = -1;
		/*
		 * crea overlay
		 */
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when){
			super.draw(canvas, mapView, shadow);
			//Traduce locazione in punto pixel
			Point scrPnt = new Point();
			mapView.getProjection().toPixels(locPoint, scrPnt);
			//Visualizza marker
			Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.pin);
			canvas.drawBitmap(bmp, scrPnt.x-34, scrPnt.y-50, null);
			return true;
		}
		/*
		 * Registra coordinate dopo tocco singolo
		 */
		public boolean onTouchEvent(MotionEvent event, MapView mapView){
			if(event.getAction() == MotionEvent.ACTION_DOWN)
				lastEvent = MotionEvent.ACTION_DOWN;
			else if(event.getAction() == MotionEvent.ACTION_MOVE)
				lastEvent = MotionEvent.ACTION_MOVE;
			else if(event.getAction() == MotionEvent.ACTION_UP && lastEvent != MotionEvent.ACTION_MOVE){
				lastEvent = -1;
				newPoint = mapView.getProjection().fromPixels((int) event.getX(), (int) event.getY());
			}
			return false;
		}
		/*
		 * Attiva il menu definito nella classe S5Add
		 */
		public boolean onTap(GeoPoint p, MapView mapView){
			openContextMenu(map);
			return super.onTap(p, mapView);
		}
	}
	
}
