package edu.kettering.autosilencer;

public class MyCalendar {
	
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
