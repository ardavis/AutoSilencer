package edu.kettering.autosilencer;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.database.Cursor;

class MyCalendar {
	public String name;
	public String id;
	
	// Default Constructor
	public MyCalendar(String _name, String _id) {
		name = _name;
		id = _id;
	}
	
	// When calling toString, only return the name of the Calendar
	@Override
	public String toString() {
		return name;
	}
}

public class AutoSilencerActivity extends Activity {
	
	/*
	 * UI Methods
	 */
	private Spinner m_spinner_calendar;
	private Button m_button_add;
	private Button m_button_getEvents;
	private TextView m_text_event;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Get Calendar List and Populate the View
        getCalendars();
        populateCalendarSpinner();
        populateAddBtn();
        populateTextEvent();
        populateGetEventsBtn();
    }
    
    private void populateCalendarSpinner() {
    	m_spinner_calendar = (Spinner) findViewById(R.id.spinner_calendar);
    	ArrayAdapter<?> l_arrayAdapter = new ArrayAdapter(this.getApplicationContext(), android.R.layout.simple_spinner_item, m_calendars);
    	l_arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	
    	m_spinner_calendar.setAdapter(l_arrayAdapter);
    	m_spinner_calendar.setSelection(0);
    	m_spinner_calendar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> p_parent, View p_view, int p_pos, long p_id) {
				m_selectedCalendarId = m_calendars[(int)p_id].id;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
    }
    
    private void populateAddBtn() {
    	m_button_add = (Button) findViewById(R.id.button_add);
    	m_button_add.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				addEvent();
			}
    		
    	});
    }
    
    private void populateGetEventsBtn() {
    	m_button_getEvents = (Button) findViewById(R.id.button_get_events);
    	m_button_getEvents.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				getLastThreeEvents();
				
			}
		});
    }
    
    private void populateTextEvent() {
    	m_text_event = (TextView) findViewById(R.id.text_event);
    	String l_str = 
    			"Title: Blah Blah\n" +
				"Description: Blah Blah\n" +
				"Event Location: Blah\n" +
				"Start Time: " + getDateTimeStr(0) + "\n" +
				"End Time: " + getDateTimeStr(30) + "\n" +
				"Event Status: Blah\n" +
				"All Day: Blah\n" +
				"Has Alarm: Blah\n";
    	m_text_event.setText(l_str);
    }
    
    
    /*
     * Data Methods
     */
	private MyCalendar m_calendars[];
	private String m_selectedCalendarId = "0";
	
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
    
    private void addEvent() {
    	Intent l_intent = new Intent(Intent.ACTION_EDIT);
    	l_intent.setType("vnd.android.cursor.item/event");
    	l_intent.putExtra("title", "Andy Calendar Tutorial Test");
    	l_intent.putExtra("description", "This is a simple test for Calendar API");
    	l_intent.putExtra("eventLocation", "@home");
    	l_intent.putExtra("beginTime", System.currentTimeMillis());
    	l_intent.putExtra("endTime", System.currentTimeMillis() + 1800*1000);
    	l_intent.putExtra("allDay", 0);	
    	l_intent.putExtra("eventStatus", 1); // Status: 0 - Tentative, 1 - Confirmed, 2 - Cancelled
    	l_intent.putExtra("visibility", 0); // Visibility: 0 - Default, 1 - Confidential, 2 - Private, 3 - Public
    	l_intent.putExtra("transparency", 0); // Transparency: 0 - Opaque (No Timing Conflict Allowed), 1 - Transparent (Allow Overlap of Scheduling)
    	l_intent.putExtra("hasAlarm", 1); // Alarm: 0 - False, 1 - True
    	
    	try {
    		startActivity(l_intent); 
    	} catch (Exception e) {
    		Toast.makeText(this.getApplicationContext(), "Sorry, no compatible calendar is found!", Toast.LENGTH_LONG).show();
    	}
    }
    
    private void getLastThreeEvents() {
    	Uri l_eventUri = Uri.parse("content://com.android.calendar/events");
    	String[] l_projection = new String[]{"title", "dtstart", "dtend"};
    	Cursor l_managedCursor = this.managedQuery(l_eventUri, l_projection, "calendar_id=" + m_selectedCalendarId, null, "dtstart DESC, dtend DESC");
    	
    	if (l_managedCursor.moveToFirst()) {
    		int l_cnt = 0;
    		String l_title;
    		String l_begin;
    		String l_end;
    		StringBuilder l_displayText = new StringBuilder();
    		int l_colTitle = l_managedCursor.getColumnIndex(l_projection[0]);
    		int l_colBegin = l_managedCursor.getColumnIndex(l_projection[1]);
    		int l_colEnd = l_managedCursor.getColumnIndex(l_projection[1]);
    		
    		do {
    			l_title = l_managedCursor.getString(l_colTitle);
    			l_begin = getDateTimeStr(l_managedCursor.getString(l_colBegin));
    			l_end = getDateTimeStr(l_managedCursor.getString(l_colEnd));
    			l_displayText.append(l_title + "\n" + l_begin + "\n" + l_end + "\n---------------\n");
    			++l_cnt;
    		} while (l_managedCursor.moveToNext() && l_cnt < 3);
    		
    		m_text_event.setText(l_displayText.toString());
    	}
    	
    }
    
    /*
	 * Utility Methods
	 */
	private static final String DATE_TIME_FORMAT = "yyy MMM dd, HH:mm:ss";
	
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
















