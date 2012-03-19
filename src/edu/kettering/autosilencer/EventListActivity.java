package edu.kettering.autosilencer;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class EventListActivity extends ListActivity {
	SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aaa");
	SimpleDateFormat day = new SimpleDateFormat("dd");
	SimpleDateFormat month = new SimpleDateFormat("MMM");
	
	ArrayList<Event> events;
	
	public void onCreate(Bundle elist) {
		super.onCreate(elist);

		Intent i = getIntent();
		events = (ArrayList<Event>) i.getSerializableExtra("events");
		
		if (events != null)
		{
			ArrayList<HashMap<String, String>> list = initializeTestEvents();
			String[] from = { "eventTitle", "eventTimes","dayNumber", "month" };
			int[] to = {R.id.eventTitle, R.id.eventTimes, R.id.dayNumber, R.id.month };
			
			SimpleAdapter adapter = new SimpleAdapter(EventListActivity.this,
					list, R.layout.upcoming_event_list_item_layout, from, to);
			
			setListAdapter(adapter);
		}
	}
	
	public void onStart()
	{
		super.onStart();
		events = null;
	}
	
	public ArrayList<HashMap<String, String>> initializeTestEvents()
	{
		ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < events.size(); i++)
		{	
			
			list.add(putData(events.get(i).title(), 
					"Start: " + sdf.format(events.get(i).beginTime()) + " End: " + sdf.format(events.get(i).endTime()), 
					day.format(events.get(i).beginTime()), 
					month.format(events.get(i).beginTime())));
		}
		return list;
	}
	
	private HashMap<String, String> putData(String title, String times, String dayNumber, String month) {
		HashMap<String, String> item = new HashMap<String, String>();
		item.put("eventTitle", title);
		item.put("eventTimes", times);
		item.put("dayNumber", dayNumber);
		item.put("month", month);
		return item;
	}
}
