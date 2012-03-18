package edu.kettering.autosilencer;

import java.util.Date;

public class Event {
	
	// Properties
	private String title;
	private Date beginTime;
	private Date endTime;
	private String description;
	private String location;
	private Boolean isAllDay;
	
	// Constructor
	public Event(String title, Date bTime, Date eTime)
	{
		this.title = title;
		this.beginTime = bTime;
		this.endTime = eTime;
		this.isAllDay = false;
		this.description = "";
		this.location = "";
	}
	
	public Event(String title, Date bTime, Date eTime, String desc, String loc, Boolean allDay)
	{
		this.title = title;
		this.beginTime = bTime;
		this.endTime = eTime;
		this.description = desc;
		this.location = loc;
		this.isAllDay = allDay;
	}
	
	// Get the Title of an Event
	public String title()
	{
		return this.title;
	}
	
	// Set the Title of an Event
	public void setTitle(String newTitle)
	{
		this.title = newTitle;
	}
	
	// Get the Begin Time of an Event
	public Date beginTime()
	{
		return this.beginTime;
	}
	
	// Set the Begin Time of an Event
	public void setBeginTime(Date newBeginTime)
	{
		this.beginTime = newBeginTime;
	}
	
	// Get the End Time of an Event
	public Date endTime()
	{
		return this.endTime;
	}
	
	// Set the End Time of an Event
	public void setEndTime(Date newEndTime)
	{
		this.endTime = newEndTime;
	}
	
	// Get the Event's Description
	public String description()
	{
		return this.description;
	}
	
	// Set the Event's Description
	public void setDescription(String desc)
	{
		this.description = desc;
	}
	
	// Get the Event's Location
	public String location()
	{
		return this.location;
	}
	
	// Set the Event's Location
	public void setLocation(String loc)
	{
		this.location = loc;
	}
	
	// Get the Event's All Day status
	public Boolean isAllDay()
	{
		return this.isAllDay;
	}
	
	// Set the Event's All Day status
	public void setAllDay(Boolean allDay)
	{
		this.isAllDay = allDay;
	}

}















