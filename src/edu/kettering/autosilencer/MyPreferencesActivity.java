package edu.kettering.autosilencer;

import java.util.prefs.Preferences;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class MyPreferencesActivity extends PreferenceActivity {
	
    boolean CheckboxPreference;
    String ListPreference;
    String editTextPreference;
    String ringtonePreference;
    String secondEditTextPreference;
    String customPref;
	
	SharedPreferences settings;
	SharedPreferences.Editor editor;
	
	ListPreference eventVolumePref;
	ListPreference calendarPollingIntervalPref;
	CheckBoxPreference prepareIgnoreAllDayEventsPref;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            
            settings = getPreferenceScreen().getSharedPreferences();
            editor = settings.edit();
            
            prepareEventVolumePref();
            prepareCalendarPollingIntervalPref();
            prepareIgnoreAllDayEventsPref();
            
            
            // Get the custom preference
            Preference customPref = (Preference) findPreference("addCustomEvent");
            customPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

                    public boolean onPreferenceClick(Preference preference) {
                        Toast.makeText(getBaseContext(),
                                        "The custom preference has been clicked",
                                        Toast.LENGTH_LONG).show();
                        SharedPreferences customSharedPreference = getSharedPreferences(
                                        "myCustomSharedPrefs", Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = customSharedPreference
                                        .edit();
                        editor.putString("addCustomEvent",
                                        "The preference has been clicked");
                        editor.commit();
                        return true;
                    }

            });
    }
	
    public void prepareEventVolumePref()
    {
    	eventVolumePref = (ListPreference) findPreference("eventVolume");
    	eventVolumePref.setSummary(settings.getString("eventVolume", "No Value Set"));
    	eventVolumePref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
    		
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				
				System.out.println("Event Volume: " + newValue);
				eventVolumePref.setSummary(newValue.toString());
				editor.putString("eventVolume", newValue.toString());
				editor.commit();
				return true;
			}
		});
    }
    
    public void prepareCalendarPollingIntervalPref()
    {
    	calendarPollingIntervalPref = (ListPreference) findPreference("calendarPollingInterval");
    	calendarPollingIntervalPref.setSummary(settings.getString("calendarPollingInterval", "No Value Set"));
    	calendarPollingIntervalPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				System.out.println("Calendar Polling Interval: " + newValue);
				calendarPollingIntervalPref.setSummary(newValue.toString());
				editor.putString("calendarPollingInterval", newValue.toString());
				editor.commit();
				return true;
			}
		});
    }
    
    public void prepareIgnoreAllDayEventsPref()
    {
    	prepareIgnoreAllDayEventsPref = (CheckBoxPreference) findPreference("ignoreAllDayEvents");
    	
    	boolean ignoreAllDayEvents = settings.getBoolean("ignoreAllDayEvents", true);
    	
    	setIgnoreSummary(ignoreAllDayEvents);
    	
    	prepareIgnoreAllDayEventsPref.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				System.out.println("Ignore All Day Events: " + newValue);
				
				setIgnoreSummary(newValue);
				editor.putBoolean("ignoreAllDayEvents", (Boolean) newValue);
				editor.commit();
				return true;
			}
		});
    }
    
    public void setIgnoreSummary(Object value)
    {
    	if ((Boolean) value)
    		prepareIgnoreAllDayEventsPref.setSummary("Ignored");
    	else
    		prepareIgnoreAllDayEventsPref.setSummary("Not Ignored");
    }
    
    


//    private void getPrefs() {
//            // Get the xml/preferences.xml preferences
//            SharedPreferences prefs = PreferenceManager
//                            .getDefaultSharedPreferences(getBaseContext());
//            CheckboxPreference = prefs.getBoolean("checkboxPref", true);
//            ListPreference = prefs.getString("listPref", "nr1");
//            editTextPreference = prefs.getString("editTextPref",
//                            "Nothing has been entered");
//            ringtonePreference = prefs.getString("ringtonePref",
//                            "DEFAULT_RINGTONE_URI");
//            secondEditTextPreference = prefs.getString("SecondEditTextPref",
//                            "Nothing has been entered");
//            // Get the custom preference
//            SharedPreferences mySharedPreferences = getSharedPreferences(
//                            "myCustomSharedPrefs", Activity.MODE_PRIVATE);
//            customPref = mySharedPreferences.getString("myCusomPref", "");
//    }

}
