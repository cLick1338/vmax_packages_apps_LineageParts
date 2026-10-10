//Vmax
package org.lineageos.lineageparts.statusbar;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.util.ArrayMap;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.SeekBarPreference;
import androidx.preference.TwoStatePreference;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

/** Quick settings tile grid, appearance and animation. */
public class QuickSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String SYSTEMUI = "com.android.systemui";
    private static final String KEY_RESET = "qs_settings_reset";
    private static final int DEFAULT_LABEL_SIZE = 14;
    private static final int DEFAULT_ANIMATION_DURATION = 800;

    private static final String[] SEEKBAR_KEYS = {
            Settings.System.QS_LAYOUT_COLUMNS,
            Settings.System.QS_LAYOUT_ROWS,
            Settings.System.QQS_LAYOUT_ROWS,
            Settings.System.QS_LAYOUT_COLUMNS_LANDSCAPE,
            Settings.System.QS_LAYOUT_ROWS_LANDSCAPE,
            Settings.System.QQS_LAYOUT_ROWS_LANDSCAPE,
            Settings.System.QS_TILE_LABEL_SIZE,
            Settings.System.QS_TILE_ANIMATION_DURATION,
    };
    private static final String[] LIST_KEYS = {
            Settings.System.QS_TILE_ANIMATION_STYLE,
            Settings.System.QS_TILE_ANIMATION_INTERPOLATOR,
    };
    private static final String[] SWITCH_KEYS = {
            Settings.System.QS_TILE_VERTICAL_LAYOUT,
            Settings.System.QS_TILE_LABEL_HIDE,
            Settings.System.QUICK_SETTINGS_VIBRATE,
    };

    private final ArrayMap<String, Integer> mDefaults = new ArrayMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.quick_settings);
        getActivity().setTitle(R.string.qs_settings_title);

        loadDefaults();
        for (String key : SEEKBAR_KEYS) {
            findPreference(key).setOnPreferenceChangeListener(this);
        }
        for (String key : LIST_KEYS) {
            findPreference(key).setOnPreferenceChangeListener(this);
        }
        findPreference(Settings.System.QS_TILE_LABEL_HIDE).setOnPreferenceChangeListener(this);
        findPreference(KEY_RESET).setOnPreferenceClickListener(p -> {
            resetToDefaults();
            return true;
        });
        updateState();
    }

    private void loadDefaults() {
        for (boolean landscape : new boolean[] { false, true }) {
            final int columns = getSystemUiInteger("quick_settings_num_columns", landscape,
                    landscape ? 4 : 2);
            final int qqsTiles = getSystemUiInteger("quick_qs_panel_max_tiles", landscape, 4);
            mDefaults.put(landscape ? Settings.System.QS_LAYOUT_COLUMNS_LANDSCAPE
                    : Settings.System.QS_LAYOUT_COLUMNS, columns);
            mDefaults.put(landscape ? Settings.System.QS_LAYOUT_ROWS_LANDSCAPE
                    : Settings.System.QS_LAYOUT_ROWS,
                    getSystemUiInteger("quick_settings_max_rows", landscape, landscape ? 2 : 4));
            mDefaults.put(landscape ? Settings.System.QQS_LAYOUT_ROWS_LANDSCAPE
                    : Settings.System.QQS_LAYOUT_ROWS,
                    Math.max(1, (qqsTiles + columns - 1) / Math.max(1, columns)));
        }
        mDefaults.put(Settings.System.QS_TILE_LABEL_SIZE, DEFAULT_LABEL_SIZE);
        mDefaults.put(Settings.System.QS_TILE_ANIMATION_DURATION, DEFAULT_ANIMATION_DURATION);
    }

    private int getSystemUiInteger(String name, boolean landscape, int fallback) {
        try {
            final Context sysui = getContext().createPackageContext(SYSTEMUI, 0);
            final Configuration config =
                    new Configuration(sysui.getResources().getConfiguration());
            config.orientation = landscape ? Configuration.ORIENTATION_LANDSCAPE
                    : Configuration.ORIENTATION_PORTRAIT;
            final Resources res = sysui.createConfigurationContext(config).getResources();
            final int id = res.getIdentifier(name, "integer", SYSTEMUI);
            return id != 0 ? res.getInteger(id) : fallback;
        } catch (PackageManager.NameNotFoundException e) {
            return fallback;
        }
    }

    // Unset or zero means SystemUI uses its default.
    private int getValue(String key) {
        final int value = Settings.System.getInt(getContentResolver(), key, 0);
        return value > 0 ? value : mDefaults.getOrDefault(key, 0);
    }

    private void updateState() {
        for (String key : SEEKBAR_KEYS) {
            ((SeekBarPreference) findPreference(key)).setValue(getValue(key));
        }
        for (String key : LIST_KEYS) {
            ((ListPreference) findPreference(key)).setValue(String.valueOf(getValue(key)));
        }
        for (String key : SWITCH_KEYS) {
            ((TwoStatePreference) findPreference(key)).setChecked(getValue(key) != 0);
        }
        updateLabelSize(getValue(Settings.System.QS_TILE_LABEL_SIZE),
                getValue(Settings.System.QS_TILE_LABEL_HIDE) != 0);
        updateAnimation(getValue(Settings.System.QS_TILE_ANIMATION_STYLE),
                getValue(Settings.System.QS_TILE_ANIMATION_DURATION));
    }

    private void updateLabelSize(int size, boolean labelHidden) {
        final Preference pref = findPreference(Settings.System.QS_TILE_LABEL_SIZE);
        pref.setSummary(getString(R.string.qs_tile_label_size_value, size));
        pref.setEnabled(!labelHidden);
    }

    private void updateAnimation(int style, int duration) {
        final Preference pref = findPreference(Settings.System.QS_TILE_ANIMATION_DURATION);
        pref.setSummary(getString(R.string.qs_tile_animation_duration_value, duration));
        pref.setEnabled(style != 0);
        findPreference(Settings.System.QS_TILE_ANIMATION_INTERPOLATOR).setEnabled(style != 0);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final String key = preference.getKey();
        if (Settings.System.QS_TILE_LABEL_HIDE.equals(key)) {
            updateLabelSize(getValue(Settings.System.QS_TILE_LABEL_SIZE), (Boolean) newValue);
            return true;
        }
        final int value = newValue instanceof Integer
                ? (Integer) newValue : Integer.parseInt((String) newValue);
        Settings.System.putInt(getContentResolver(), key, value);
        if (Settings.System.QS_TILE_LABEL_SIZE.equals(key)) {
            updateLabelSize(value, getValue(Settings.System.QS_TILE_LABEL_HIDE) != 0);
        } else if (Settings.System.QS_TILE_ANIMATION_STYLE.equals(key)) {
            updateAnimation(value, getValue(Settings.System.QS_TILE_ANIMATION_DURATION));
        } else if (Settings.System.QS_TILE_ANIMATION_DURATION.equals(key)) {
            updateAnimation(getValue(Settings.System.QS_TILE_ANIMATION_STYLE), value);
        }
        return true;
    }

    private void resetToDefaults() {
        final ContentResolver resolver = getContentResolver();
        for (String key : SWITCH_KEYS) {
            ((TwoStatePreference) findPreference(key)).setChecked(false);
        }
        for (String[] keys : new String[][] { SEEKBAR_KEYS, LIST_KEYS, SWITCH_KEYS }) {
            for (String key : keys) {
                resolver.call(Settings.System.CONTENT_URI, Settings.CALL_METHOD_DELETE_SYSTEM,
                        key, null);
            }
        }
        updateState();
    }
}
