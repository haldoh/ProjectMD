package mobidata.project;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class S1Login extends Activity implements OnClickListener{
	//variabili
	private Cursor curs = null;
	//oggetti view
	EditText userText;
	Button button;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.s1login);
        //get objects IDs
        userText = (EditText) findViewById(R.id.userLog);
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(this);
	}
	public void onClick(View v) {
		if(v == button){
			//ottieni lo user inserito
			String user = userText.getText().toString();
			//cerca user nel db
			String where = POIProvider.Users.NAME + "='" + user + "'";
			String[] proj = {POIProvider.Users.NAME};
			curs = managedQuery(POIProvider.Users.CONTENT_URI, proj, where, null, null);
			if(curs.getCount() == 0 && !user.equals(S2Main.serverUser)){
				//Se l'utente non esiste, chiedi se crearlo
				reqUserAdd(user);
			} else {
				//Se l'utente esiste già, effettua il login
				login(user);
			}
		}
	}
	/*
	 * Visualizza messaggio se utente non esiste
	 */
	private void reqUserAdd(final String user) {
		//Creo messaggio per utente
		AlertDialog.Builder message = new AlertDialog.Builder(this);
		message.setTitle("Attenzione!");
		message.setMessage("L'utente inserito non esiste. Si desidera crearlo?");
		message.setPositiveButton("Sì", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dlg, int sumthin) {
					//Inserisco l'utente
					userAdd(user);
				}
			});
		message.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dlg, int sumthin) {
					//Non fa nulla - Ritorna alla schermata di inserimento utente
				}
			});
		message.show();
		
	}
	/*
	 * Aggiunge nuovo utente
	 */
	private void userAdd(final String user){
		AlertDialog.Builder message = new AlertDialog.Builder(this);
		message.setTitle("Attenzione!");
		message.setMessage("Sta per essere creato il nuovo utente '" + user + "'. Continuare?");
		message.setPositiveButton("Sì", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dlg, int sumthin) {
					//Inserisco l'utente
					ContentValues values = new ContentValues(1);
					values.put(POIProvider.Users.NAME, user);
					getContentResolver().insert(POIProvider.Users.CONTENT_URI, values);
					login(user);
				}
			});
		message.setNegativeButton("No", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dlg, int sumthin) {
					//Non fa nulla - Ritorna alla schermata di inserimento utente
				}
			});
		message.show();
	}
	/*
	 * Accesso alla schermata principale dell'applicazione
	 */
	private void login(String user) {
		//Lancia l'activity S2Main
		Intent log = new Intent(this, S2Main.class);
		log.putExtra("user", user);
		startActivity(log);
	}
}
