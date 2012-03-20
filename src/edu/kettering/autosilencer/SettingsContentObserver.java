package edu.kettering.autosilencer;

import android.app.Application;
import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class SettingsContentObserver extends ContentObserver {

	AudioManager am;
	Context context;
	
	public SettingsContentObserver(Handler handler) {
	    super(handler);
	} 
	
	public void setContext(Context context)
	{
		this.context = context;
		am = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
	}
	
	@Override
	public boolean deliverSelfNotifications() {
	     return super.deliverSelfNotifications(); 
	}
	
	@Override
	public void onChange(boolean selfChange) {
	    super.onChange(selfChange);
	    Log.v("SETTINGS", "Settings change detected");
	    
	    switch(am.getRingerMode())
    	{
    		case AudioManager.RINGER_MODE_VIBRATE:
    			AutoSilencerActivity.currentStateLabel.setText(R.string.vibrate);
    			break;
    		
    		case AudioManager.RINGER_MODE_SILENT:
    			AutoSilencerActivity.currentStateLabel.setText(R.string.silent);
    			break;
    			
    		default:
    			AutoSilencerActivity.currentStateLabel.setText(R.string.normal);
    			break;
    		
    	}
	}
}
