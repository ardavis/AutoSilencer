package edu.kettering.autosilencer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;



public class AutoSilencerActivity extends Activity {
	
	// Finals
	final boolean ENABLED  = true;
	final boolean DISABLED = false;
	final int VIBRATE = 0;
	final int SILENT = 1;
	final int NORMAL = 2;
	public static final String PREFS_NAME = "PrefFile";
	
	// Global Variables
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, MMM dd hh:mm aaa");
	SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm aaa");
	
    private MyCalendar m_calendars[];
    private Spinner m_spinner_calendar;
    private String selectedCalendarId = "0";
	
	private enum Volume {
		VIBRATE, SILENT, NORMAL
	}
	
	private enum CalendarPoll {
		ONE_MINUTE(1), FIVE_MINUTES(5), TEN_MINUTES(10), 
		FIFTEEN_MINUTES(15), HALF_HOUR(30), ONE_HOUR(60), 
		THREE_HOURS(180);
		
		private int minutes;  
		   
	    private CalendarPoll(int minutes) {  
	        this.minutes = minutes;  
	    }  
	   
	    public int getMinutes() {  
	        return minutes;  
	    }  
	}
	
	
	public static TextView currentStateLabel;
	private TextView nextEventLabel;
	
	private ArrayList<Event> events = new ArrayList<Event>();
	private Event nextEvent;
	private Event currentEvent;
	
	private Button addButton;
	private Button viewUpEvents;
	private Button prefsButton;
	
	private ToggleButton toggleButton;
	
	private Date lastQueryAt;
	private Date currentTime;
	
	// Preferences
	private String eventVolume;
	private String calendarPollingInterval;
	private boolean ignoreAllDayEvents;
	
	private Volume volumeChoice = Volume.SILENT;
	private CalendarPoll calendarPoll = CalendarPoll.FIFTEEN_MINUTES;
	
	private int minuteCount = 0;
	
	private Volume volumeBeforeEvent;
	
	private NotificationManager nm;
	private Handler handler;
	private Runnable handlerTask = new Runnable() {
		@Override
		public void run() {
			checkForEvent();
			updateNextEventLabel();
			checkForEventComplete();
		}
	};
	
	private final BroadcastReceiver intentReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (Intent.ACTION_TIME_TICK.equals(action))
			{
				handler.post(handlerTask);
				minuteCount++;
				minuteTracker();
			}
		}
	};
	
	private Boolean isEnabled = true;
	
	// Audio Manager
	AudioManager am;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        registerIntentFilters();
        //getCalendars();
        //populateCalendarSpinner();
        getUpcomingEvents(this);
        
        setupAudioManager();
        updateCurrentStateLabel();
        
        restorePreferences();
        
        handler = new Handler();
        startCheckingForEvents();
        
        setupInitialState();
        setupNotification();
        
        prepareToggleButton();
        prepareUpcomingEventsButton();
        prepareAddButton();
        prepareSettingsButton();
    }
   
    public void restorePreferences()
    {
    	// Restore preferences
        //SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        
    	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(AutoSilencerActivity.this);
        eventVolume = settings.getString("eventVolume", "Does not exist");
        calendarPollingInterval = settings.getString("calendarPollingInterval", "Does not exist");
        ignoreAllDayEvents = settings.getBoolean("ignoreAllDayEvents", true);
        
        convertVolume();
        convertCalendarPollingInterval();
        

    }

    public void convertCalendarPollingInterval()
    {
    	if (calendarPollingInterval.equals("1 Minute"))
    	{
    		calendarPoll = CalendarPoll.ONE_MINUTE;
    	}
    	else if (calendarPollingInterval.equals("5 Minutes"))
    	{
    		calendarPoll = CalendarPoll.FIVE_MINUTES;
    	}
    	else if (calendarPollingInterval.equals("10 Minutes"))
    	{
    		calendarPoll = CalendarPoll.TEN_MINUTES;
    	}
    	else if (calendarPollingInterval.equals("15 Minutes"))
    	{
    		calendarPoll = CalendarPoll.FIFTEEN_MINUTES;
    	}
    	else if (calendarPollingInterval.equals("30 Minutes"))
    	{
    		calendarPoll = CalendarPoll.HALF_HOUR;
    	}
    	else if (calendarPollingInterval.equals("1 Hour"))
    	{
    		calendarPoll = CalendarPoll.ONE_HOUR;
    	}
    	else if (calendarPollingInterval.equals("3 Hours"))
    	{
    		calendarPoll = CalendarPoll.THREE_HOURS;
    	}
    	else
    	{
    		System.out.println("Problem with the Calendar Polling Interval");
    	}
    }
    
    public void convertVolume()
    {
        if (eventVolume.equals("Vibrate"))
        {
        	volumeChoice = Volume.VIBRATE;
        }
        else if (eventVolume.equals("Silent"))
        {
        	volumeChoice = Volume.SILENT;
        }
        else 
        {
        	volumeChoice = Volume.NORMAL;
        }
    }
    
    public void savePreferences()
    {
    	// Save user preferences. We need an Editor object to
        // make changes. All objects are from android.context.Context
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(AutoSilencerActivity.this);
        SharedPreferences.Editor editor = settings.edit();
        
        editor.putString("eventVolume", eventVolume);
        editor.putString("calendarPollingInterval", calendarPollingInterval);
        editor.putBoolean("ignoreAllDayEvents", ignoreAllDayEvents);

        // Don't forget to commit your edits!!!
        editor.commit();
    }
    
    public void checkVolumeBeforeEvent()
    {
    	switch(am.getRingerMode())
    	{
    		case AudioManager.RINGER_MODE_VIBRATE:
    			volumeBeforeEvent = Volume.VIBRATE;
    			break;
    		
    		case AudioManager.RINGER_MODE_SILENT:
    			volumeBeforeEvent = Volume.SILENT;
    			break;
    			
    		default:
    			volumeBeforeEvent = Volume.NORMAL;
    			break;
    		
    	}
    }
    
    public void registerIntentFilters()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        this.registerReceiver(intentReceiver, filter);
        
        SettingsContentObserver settingsContentObserver = new SettingsContentObserver( new Handler() ); 
        settingsContentObserver.setContext(getApplicationContext());
        this.getApplicationContext().getContentResolver().registerContentObserver( 
        		android.provider.Settings.System.CONTENT_URI, true, 
        		settingsContentObserver );

    }
    
    public void unregisterIntentFilters()
    {
    	this.unregisterReceiver(intentReceiver);
    }
    
    public void setupNotification() 
    {
    	int icon = R.drawable.as_icon;
        CharSequence tickerText = "AutoSilencer is now running...";
        long when = System.currentTimeMillis();
        
        if (nm == null)
        	  nm = (NotificationManager)  getSystemService(NOTIFICATION_SERVICE);

        Context context = getApplicationContext();
        Notification notification = new Notification(icon, tickerText, when);
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        CharSequence contentTitle = "AutoSilencer";
        CharSequence contentText = "Open AutoSilencer";
        Intent notificationIntent = new Intent(this, AutoSilencerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        nm.notify(1, notification);
    }
    
    public void eventTakingPlaceNotification()
    {
    	int icon = R.drawable.as_icon;
    	CharSequence tickerText = nextEvent.title() + " has started.";
        long when = System.currentTimeMillis();
        
        if (nm == null)
        	  nm = (NotificationManager)  getSystemService(NOTIFICATION_SERVICE);

        Context context = getApplicationContext();
        Notification notification = new Notification(icon, tickerText, when);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        CharSequence contentTitle = nextEvent.title() + " has started.";
        CharSequence contentText = timeFormat.format(nextEvent.beginTime()) + " - " + timeFormat.format(nextEvent.endTime());
        Intent notificationIntent = new Intent(this, AutoSilencerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
        nm.notify(2, notification);
    }
    
    @Override
    protected void onRestart() {
        super.onRestart();
        //getUpcomingEvents(this);
        //setupNotification();
        restorePreferences();
        registerIntentFilters();
        startCheckingForEvents();
    }
    
    @Override
    protected void onPause() {
    	super.onPause();
    	savePreferences();
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	stopCheckingForEvents();
    	unregisterIntentFilters();
    	savePreferences();
    	//nm.cancelAll();
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
    
    public void prepareSettingsButton()
    {
    	prefsButton = (Button) findViewById(R.id.prefs);
    	prefsButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent showPreferences = new Intent(getApplicationContext(), MyPreferencesActivity.class);
				startActivity(showPreferences);
			}
		});
    }
    
    public void setupInitialState()
    {	
    	// Enabled or Disabled
    	toggleButton = (ToggleButton) findViewById(R.id.enableDisable);
    	toggleButton.setEnabled(isEnabled);
    }
    
    public void updateCurrentStateLabel()
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
    			
    		default:
    			currentStateLabel.setText(R.string.normal);
    			break;
    		
    	}
    }
    
    public void updateNextEventLabel()
    {
    	// Next Event
    	try {
    		nextEventLabel = (TextView) findViewById(R.id.nextEvent);
    		nextEventLabel.setText(nextEvent.title() + "\n" + simpleDateFormat.format(nextEvent.beginTime()));
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
    	getUpcomingEvents(this);
    	handlerTask.run();
    }
    
    public void stopCheckingForEvents()
    {
    	handler.removeCallbacks(handlerTask);
    }
    
    public void checkForEvent()
    {
    	currentTime = new Date();
    	try {
    		// Check if an event has started
    		if (nextEvent.beginTime().getDate() == currentTime.getDate())
	    	{
	    		if ((nextEvent.beginTime().getHours() < currentTime.getHours()) ||
	        			(nextEvent.beginTime().getHours() == currentTime.getHours() &&
	        			nextEvent.beginTime().getMinutes() == currentTime.getMinutes()))
	    		{
	    			checkVolumeBeforeEvent();
		    		eventTakingPlaceNotification();
		    		
		    		determineVolume();
		    		
		    		System.out.println("Event has started.");
		    		currentEvent = nextEvent;
		    		events.remove(0);
		    		getUpcomingEvents(this);
		    		
		    		if (!events.isEmpty())
		        	{
		    			nextEvent = events.get(0);
		    			nextEventLabel.setText("No events");
		        	}
		    	}
		    	else
		    	{
					Toast.makeText(getApplicationContext(), "Next Event: " + simpleDateFormat.format(nextEvent.beginTime()), Toast.LENGTH_SHORT).show();
		    	}
    		}
    	} catch (Exception e) {
			Toast.makeText(getApplicationContext(), "No events found within interval specified!", Toast.LENGTH_SHORT).show();
		}
    }
    
    public void determineVolume()
    {
    	
    	switch(volumeChoice)
    	{
    		case VIBRATE:
    			setVibrate();
    			break;
    			
    		case SILENT:
    			setSilent();
    			break;
    			
    		case NORMAL:
    			setNormal();
    			break;
    	}
    }
    
    public void checkForEventComplete()
    {
    	currentTime = new Date();
    	try
    	{
    		if (currentEvent.endTime().getDate() == currentTime.getDate())
	    	{
	    		if ((currentEvent.endTime().getHours() < currentTime.getHours()) ||
	    			(currentEvent.endTime().getHours() == currentTime.getHours() &&
	    					currentEvent.endTime().getMinutes() == currentTime.getMinutes()))
	    		{
	    			setVolumeTo(volumeBeforeEvent);
	    			nm.cancel(2);
	    			updateCurrentStateLabel();
	    			currentEvent = null;
	    		}
    		}
    	} catch (Exception e) {
    		System.out.println("No current event");
    	}
    }
    
//    private void populateCalendarSpinner() {
//    	m_spinner_calendar = (Spinner) findViewById(R.id.spinner_calendar);
//    	ArrayAdapter<?> l_arrayAdapter = new ArrayAdapter(this.getApplicationContext(), android.R.layout.simple_spinner_item, m_calendars);
//    	l_arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//    	
//    	m_spinner_calendar.setAdapter(l_arrayAdapter);
//    	m_spinner_calendar.setSelection(0);
//    	m_spinner_calendar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//
//			@Override
//			public void onItemSelected(AdapterView<?> p_parent, View p_view, int p_pos, long p_id) {
//				selectedCalendarId = m_calendars[(int)p_id].id;
//				getUpcomingEvents(AutoSilencerActivity.this);
//				Log.v("Calendar ID", selectedCalendarId);
//			}
//
//			@Override
//			public void onNothingSelected(AdapterView<?> arg0) {}
//		});
//    }
    
    private void getCalendars() {
    	String[] l_projection = new String[]{"_id", "displayName"};
    	Uri l_calendars = Uri.parse("content://com.android.calendar/calendars");
    	
    	Cursor l_managedCursor = this.managedQuery(l_calendars, l_projection, null, null, null); // All Calendars
    	
    	if (l_managedCursor.moveToFirst()) {
    		m_calendars = new MyCalendar[l_managedCursor.getCount()];
    		String l_calName;
    		String l_calId;
    		int l_cnt = 0;
    		int l_nameCol = l_managedCursor.getColumnIndex(l_projection[1]);
    		int l_idCol = l_managedCursor.getColumnIndex(l_projection[0]);
    		
    		do {
    			l_calName = l_managedCursor.getString(l_nameCol);
    			l_calId = l_managedCursor.getString(l_idCol);
    			m_calendars[l_cnt] = new MyCalendar(l_calName, l_calId);
    			++l_cnt;
    		} while (l_managedCursor.moveToNext());
    	}
    }
    
    public void setVolumeTo(Volume vol)
    {
    	switch (vol)
    	{
    		case VIBRATE:
    			setVibrate();
    			break;
    			
    		case SILENT:
    			setSilent();
    			break;
    			
    		case NORMAL:
    			setNormal();
    			break;
    	}
    }
    
    // Set the phone to vibrate
    public void setVibrate()
    {
    	am.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
    	currentStateLabel.setText(R.string.vibrate);
    	// Change the text of the main menu current state
    }
    
    // Set the phone to silent
    public void setSilent()
    {
    	am.setRingerMode(AudioManager.RINGER_MODE_SILENT);
    	currentStateLabel.setText(R.string.silent);
    	// Change the text of the main menu current state
    }
    
    public void setNormal()
    {
    	am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
    	am.adjustVolume(AudioManager.ADJUST_LOWER, 0);
    	currentStateLabel.setText(R.string.normal);
    }
    
    // Set the phone to normal volume
    public void normal()
    {
    	am.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
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
					//setVibrate();
					//Toast.makeText(getApplicationContext(), "Vibrate", Toast.LENGTH_SHORT).show();
				}
				else {
					isEnabled = false;
					stopCheckingForEvents();
					// Testing
					//setSilent();
					//Toast.makeText(getApplicationContext(), "Silent", Toast.LENGTH_SHORT).show();
				}
			}
		});
    }
    
    private void addEvent() {
    	Intent intent = new Intent(Intent.ACTION_EDIT);
    	intent.setType("vnd.android.cursor.item/event");
    	intent.putExtra("title", "Andy Test Event");
    	intent.putExtra("description", "This is a simple test for Calendar API");
    	intent.putExtra("eventLocation", "@home");
    	intent.putExtra("beginTime", System.currentTimeMillis() + 60*1000);
    	intent.putExtra("endTime", System.currentTimeMillis() + 120*1000);
    	intent.putExtra("allDay", 0);	
    	intent.putExtra("eventStatus", 1); // Status: 0 - Tentative, 1 - Confirmed, 2 - Cancelled
    	intent.putExtra("visibility", 0); // Visibility: 0 - Default, 1 - Confidential, 2 - Private, 3 - Public
    	intent.putExtra("transparency", 0); // Transparency: 0 - Opaque (No Timing Conflict Allowed), 1 - Transparent (Allow Overlap of Scheduling)
    	intent.putExtra("hasAlarm", 0); // Alarm: 0 - False, 1 - True
    	
    	try {
    		startActivityForResult(intent, RESULT_OK); 
    	} catch (Exception e) {
    		Toast.makeText(this.getApplicationContext(), "Sorry, no compatible calendar is found!", Toast.LENGTH_LONG).show();
    	}
    }
    
    
    
    private void clearEvents() 
    {
    	currentTime = new Date();
    	
    	events = new ArrayList<Event>();
    	nextEvent = null;
    }
    
    private void removeExtraEvents()
    {
//		while ((nextEvent.beginTime().getHours() < currentTime.getHours()) ||
//			(nextEvent.beginTime().getHours() == currentTime.getHours() &&
//					nextEvent.beginTime().getMinutes() <= currentTime.getMinutes()))
//		{
//			if (!events.isEmpty())
//			{
//    			events.remove(0);
//    			
//    			if (!events.isEmpty())
//    			{
//    				nextEvent = events.get(0);
//    			}
//			}
//			else
//			{
//				System.out.println("No extra events to remove!");
//			}
//		}
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
    	try
    	{
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
    	} catch (Exception e) {
    		System.out.println("No events for selected calendar");
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
	    		final Boolean allDay = !eventCursor.getString(3).equals("0");
	    		
	    		if (ignoreAllDayEvents && !allDay)
	    		{
	    			Event newEvent = new Event(title, begin, end);
	    			// Add the event to the ArrayList
					events.add(newEvent);
	    		}
	    		else if (!ignoreAllDayEvents)
	    		{
	    			Event newEvent = new Event(title, begin, end, allDay);
	    			// Add the event to the ArrayList
					events.add(newEvent);
	    		}
	    		else
	    		{
	    			System.out.println("Possible error with Ignoring All Day Events");
	    		}
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
    
    
    public void minuteTracker()
    {
    	switch (calendarPoll)
		{
			case ONE_MINUTE:
				if (minuteCount == 1)
				{
					getUpcomingEvents(this);
				}
				break;
				
			case FIVE_MINUTES:
				if (minuteCount == 5)
				{
					getUpcomingEvents(this);
				}
				break;
				
			case TEN_MINUTES:
				if (minuteCount == 10)
				{
					getUpcomingEvents(this);
				}
				break;
				
			case FIFTEEN_MINUTES:
				if (minuteCount == 15)
				{
					getUpcomingEvents(this);
				}
				break;
				
			case HALF_HOUR:
				if (minuteCount == 30)
				{
					getUpcomingEvents(this);
				}
				break;
				
			case ONE_HOUR:
				if (minuteCount == 60)
				{
					getUpcomingEvents(this);
				}
				break;
			
			case THREE_HOURS:
				if (minuteCount == 180)
				{
					getUpcomingEvents(this);
				}
				break;
		}
    }
    
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
















