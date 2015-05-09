package net.falutin.dogword;

import android.app.Activity;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * Handle settings / preferences
 */

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            EditTextPreference timerSecondsPref = (EditTextPreference) findPreference("pref_timer_minutes");
            timerSecondsPref
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            int value;
                            try {
                                value = Integer.valueOf(newValue.toString());
                            } catch (NumberFormatException nfe) {
                                return false;
                            }
                            return (value > 0 && value < 24 * 60);
                        }
                    });
        }
    }

}