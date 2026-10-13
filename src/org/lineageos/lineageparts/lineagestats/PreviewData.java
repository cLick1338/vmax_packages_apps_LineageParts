/*
 * SPDX-FileCopyrightText: 2012 The CyanogenMod Project
 * SPDX-FileCopyrightText: 2017-2023 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.lineagestats;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.PreferenceScreen;

import org.json.JSONException;
import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

public class PreviewData extends SettingsPreferenceFragment {
    private static final String RAW = "preview_raw";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preview_data);

        final PreferenceScreen prefSet = getPreferenceScreen();
        final Context context = requireActivity();

        // same builder as the upload, so this is the exact request body
        try {
            prefSet.findPreference(RAW).setSummary(StatsUploadJobService.buildStatsRequest(
                    Utilities.getUniqueID(context), Utilities.getDevice(),
                    Utilities.getModVersion(), Utilities.getCountryCode(context),
                    Utilities.getResolution(context)).toString(2));
        } catch (JSONException e) {
            prefSet.findPreference(RAW).setSummary(e.toString());
        }
    }
}
