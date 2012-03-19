package edu.kettering.autosilencer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import android.app.Activity;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;



public class AutoSilencerActivity extends Activity {
	
	// Finals
	final boolean ENABLED  = true;
	final boolean DISABLED = false;
	
	// Thread
	private volatile Thread runner;
	
	// Global Variables
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, MMM dd hh:mm aaa");
	
	private TextView currentStateLabel;
	private TextView nextEventLabel;
	
	private ArrayList<Event> events = new ArrayList<Event>();
	private Event nextEvent;
	
	private Button addButton;
	private Button viewUpEvents;
	
	private ToggleButton toggleButton;
	
	private Date lastQueryAt;
	
	private final static int INTERVAL = 1000 * 60;	// 10 seconds
	
	private Handler handler;
	
	private Runnable handlerTask = new Runnable() {
		@Override
		public void run() {
			checkForEvent();
			updateNextEventLabel();
		}
	};
	
	private final BroadcastReceiver intentReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			
			if (Intent.ACTION_TIME_CHANGED.equals(action))
			{
				handler.post(handlerTask);
			}
			
		}
	};
	
//	private Runnable handlerTask = new Runnable()
//	{
//		@Override
//		public void run() {
//			// Check if current time is equal to the next event time
//			checkForEvent();
//			updateNextEventLabel();
//			//Toast.makeText(getApplicationContext(), "Is it time yet?", Toast.LENGTH_SHORT).show();
//			handler.postDelayed(handlerTask, INTERVAL);
//		}
//	};
//	
	private Boolean isEnabled = true;
	
	// Audio Manager
	AudioManager am;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setupAudioManager();
        setupInitialState();
        
        getUpcomingEvents(this);
        
        prepareToggleButton();
        prepareUpcomingEventsButton();
        prepareAddButton();
        
        handler = new Handler();
        
        startCheckingForEvents();
          
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
        getUpcomingEvents(this);
        startCheckingForEvents();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	stopCheckingForEvents();
    }
    
    public void prepareUpcomingEventsButton()
    {
    	viewUpEvents = (Button) findViewById(R.id.viewUpEvents);
    	viewUpEvents.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				Bundle bundle = new Bundle();
		        bundle.putSerializable("events", events);
				
				Intent showUpcomingEvents = new Intent(getApplicationContext(), EventListActivity.class);
				showUpcomingEvents.putExtras(bundle);
				startActivity(showUpcomingEvents);
			}
		});
    }
    
    public void setupInitialState()
    {
    	// Current Volume State of the Device
    	currentStateLabel = (TextView) findViewById(R.id.currentState);
    	switch(am.getRingerMode())
    	{
    		case AudioManager.RINGER_MODE_VIBRATE:
    			currentStateLabel.setText(R.string.vibrate);
    			break;
    		
    		case AudioManager.RINGER_MODE_SILENT:
    			currentStateLabel.setText(R.string.silent);
    			break;
    			
    		case AudioManager.RINGER_MODE_NORMAL:
    			currentStateLabel.setText(R.string.normal);
    			break;
    			
    		default:
    			break;
    		
    	}
    	
    	// Enabled or Disabled
    	toggleButton = (ToggleButton) findViewById(R.id.enableDisable);
    	toggleButton.setEnabled(isEnabled);
    }
    
    public void updateNextEventLabel()
    {
    	// Next Event
    	try {
    		nextEventLabel = (TextView) findViewById(R.id.nextEvent);
    		nextEventLabel.setText(simpleDateFormat.format(nextEvent.beginTime()));
    	} catch (NullPointerException e) {
    		System.out.println("There are no events!");
    	}
    }
    
    public void setupAudioManager()
    {
    	am = (AudioManager) getBaseContext().getSystemService(Context.AUDIO_SERVICE);
    }
    
    public void startCheckingForEvents()
    {
    	handlerTask.run();
    }
    
    public void stopCheckingForEvents()
    {
    	handler.removeCallbacks(handlerTask);
    }
    
    public void checkForEvent()
    {
    	try {
    		Date currentTime = new Date();
    		System.out.println("Current Time: " + currentTime.getTime());
    		System.out.println("N Event Time: " + nextEvent.beginTime().getTime());
	    	if (currentTime.getTime() >= nextEvent.beginTime().getTime())
	    	{
	    		silent();
	    		
	    		while (nextEvent.beginTime().getTime() <= currentTime.getTime())
	    		{
		    		events.remove(0);
		    		nextEvent = events.get(0);
	    		}
	    		Toast.makeText(getApplicationContext(), "EVENT!!", Toast.LENGTH_SHORT).show();
	    	}
	    	else
	    	{
				Toast.makeText(getApplicationContext(), "Next Event: " + simpleDateFormat.format(nextEvent.beginTime()), Toast.LENGTH_SHORT).show();
	    	}
    	} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "No events found within interval specified!", Toast.LENGTH_SHORT).show();
		}
    }
    
    // Set the phone to vibrate
    public void vibrate()
    {
    	am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
    	currentStateLabel.setText(R.string.vibrate);
    	// Change the text of the main menu current state
    }
    
    // Set the phone to silent
    public void silent()
    {
    	am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    	currentStateLabel.setText(R.string.silent);
    	// Change the text of the main menu current state
    }
    
    public void stopRepeatingTask()
    {
    	handler.removeCallbacks(handlerTask);
    }
    
    private void prepareAddButton() {
    	addButton = (Button) findViewById(R.id.addCalendarEvent);
    	addButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				addEvent();
			}
    		
    	});
    }
    
    private void prepareToggleButton() {
    	toggleButton = (ToggleButton) findViewById(R.id.enableDisable);
    	toggleButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (toggleButton.isChecked()) {
					isEnabled = true;
					startCheckingForEvents();
					// Testing
					vibrate();
					Toast.makeText(getApplicationContext(), "Vibrate", Toast.LENGTH_SHORT).show();
				}
				else {
					isEnabled = false;
					stopCheckingForEvents();
					// Testing
					silent();
					Toast.makeText(getApplicationContext(), "Silent", Toast.LENGTH_SHORT).show();
				}
			}
		});
    }
    
    private void addEvent() {
    	Intent intent = new Intent(Intent.ACTION_EDIT);
    	intent.setType("vnd.android.cursor.item/event");
    	intent.putExtra("title", "Andy Calendar Tutorial Test");
    	intent.putExtra("description", "This is a simple test for Calendar API");
    	intent.putExtra("eventLocation", "@home");
    	intent.putExtra("beginTime", System.currentTimeMillis());
    	intent.putExtra("endTime", System.currentTimeMillis() + 1800*1000);
    	intent.putExtra("allDay", 0);	
    	intent.putExtra("eventStatus", 1); // Status: 0 - Tentative, 1 - Confirmed, 2 - Cancelled
    	intent.putExtra("visibility", 0); // Visibility: 0 - Default, 1 - Confidential, 2 - Private, 3 - Public
    	intent.putExtra("transparency", 0); // Transparency: 0 - Opaque (No Timing Conflict Allowed), 1 - Transparent (Allow Overlap of Scheduling)
    	intent.putExtra("hasAlarm", 1); // Alarm: 0 - False, 1 - True
    	
    	try {
    		startActivityForResult(intent, RESULT_OK); 
    	} catch (Exception e) {
    		Toast.makeText(this.getApplicationContext(), "Sorry, no compatible calendar is found!", Toast.LENGTH_LONG).show();
    	}
    }
    
    
    
    private void clearEvents() 
    {
    	events = new ArrayList<Event>();
    }
    
    private void getUpcomingEvents(Context context) {
    	
    	clearEvents();
    	
    	ContentResolver contentResolver = context.getContentResolver();
    	
    	// Fetch a list of all calendars on the device,
    	// display their names and whether the user has
    	// them selected for the display
    	
    	Uri uri = Uri.parse("content://com.android.calendar/calendars");
    	String[] projection = new String[]{"_id", "displayName", "selected"};
    	final Cursor cursor = contentResolver.query(uri, projection, null, null, null);
    	
    	HashSet<String> calendarIds = new HashSet<String>();
    	
    	while (cursor.moveToNext())
    	{
    		final String _id = cursor.getString(0);
    		final String displayName = cursor.getString(1);
    		final Boolean selected = !cursor.getString(2).equals("0");
    		
    		System.out.println("ID: " + _id);
    		System.out.println("Display Name: " + displayName);
    		System.out.println("Selected: " + selected);
    		
    		calendarIds.add(_id);
    	}

    	// For each calendar, display all the events for the next week.
    	for (String id : calendarIds)
    	{
	    	//Uri.Builder builder = Uri.parse("content://com.android.calendar/events/instances/when").buildUpon();
	    	Uri.Builder builder = Uri.parse("content://com.android.calendar/instances/when").buildUpon();
    		long now = new Date().getTime();
	    	ContentUris.appendId(builder, now);
			ContentUris.appendId(builder, now + 4*DateUtils.WEEK_IN_MILLIS);
	    	
	    	String[] eventProjection = new String[]{"title", "dtstart", "dtend", "allDay"};
	    	String eventSelection = "calendar_id=" + id;
	    	String eventOrder = "dtstart ASC, dtend ASC";
	    	Cursor eventCursor = contentResolver.query(builder.build(), eventProjection, eventSelection, null, eventOrder);
	    	
	    	while (eventCursor.moveToNext())
	    	{
	    		final String title = eventCursor.getString(0);
	    		final Date begin = new Date(eventCursor.getLong(1));
	    		final Date end = new Date(eventCursor.getLong(2));
	    		//final Boolean allDay = !eventCursor.getString(3).equals("0");
	    		
	    		Event newEvent = new Event(title, begin, end);
	    		
	    		// Add the event to the ArrayList
				events.add(newEvent);
	    	}
    	}
    	
    	if (!events.isEmpty())
    	{
			nextEvent = events.get(0);
			System.out.println("Title: " + nextEvent.title());
			System.out.println("Start: " + nextEvent.beginTime());
	    	lastQueryAt = new Date();
    	}
    }
    
    /*
	 * Utility Methods
	 */
	private static final String DATE_TIME_FORMAT = "EEE, MMM dd, yyyy, HH:mm aaa";
	
	public static String getDateTimeStr(int p_delay_min) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT);
		if (p_delay_min == 0) {
			return sdf.format(cal.getTime());
		} else {
			Date l_time = cal.getTime();
			l_time.setMinutes(l_time.getMinutes() + p_delay_min);
			return sdf.format(l_time);
		}
	}
	
	public static String getDateTimeStr(String p_time_in_millis) {
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT);
		Date l_time = new Date(Long.parseLong(p_time_in_millis));
		return sdf.format(l_time);
	}
}
















