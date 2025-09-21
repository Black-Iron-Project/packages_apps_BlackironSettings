/*
 * Copyright (C) 2016-2025 BlackIron Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blackiron.settings.fragments;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.blackiron.ThemeUtils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import com.blackiron.settings.fragments.quicksettings.QsHeaderImageSettings;
import com.blackiron.settings.preferences.SecureSettingSwitchPreference;
import com.blackiron.settings.preferences.SystemSettingSwitchPreference;
import com.blackiron.settings.utils.SystemRestartUtils;
import com.blackiron.settings.utils.SystemUtils;

import lineageos.providers.LineageSettings;

@SearchIndexable
public class QuickSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    public static final String TAG = "QuickSettings";

    private static final String KEY_SHOW_BRIGHTNESS_SLIDER = "qs_show_brightness_slider";
    private static final String KEY_BRIGHTNESS_SLIDER_POSITION = "qs_brightness_slider_position";
    private static final String KEY_BRIGHTNESS_SLIDER_HAPTIC = "qs_brightness_slider_haptic";
    private static final String KEY_SHOW_AUTO_BRIGHTNESS = "qs_show_auto_brightness";
    private static final String KEY_QS_UI_STYLE  = "qs_tile_ui_style";
    private static final String KEY_QS_PANEL_STYLE  = "qs_panel_style";
    private static final String KEY_QS_WIDGETS_ENABLED  = "qs_widgets_enabled";
    private static final String KEY_QS_REFACTOR_ENABLED = "qs_refactor_enabled";
    private static final String KEY_QS_SPLIT_SHADE_ENABLED = "qs_split_shade_enabled";

    private ListPreference mShowBrightnessSlider;
    private ListPreference mBrightnessSliderPosition;
    private SwitchPreferenceCompat mBrightnessSliderHaptic;
    private SwitchPreferenceCompat mShowAutoBrightness;
    private ListPreference mQsUI;
    private ListPreference mQsPanelStyle;
    private SystemSettingSwitchPreference mQsWidgetsPref;
    private SecureSettingSwitchPreference mQsRefactorEnabled;
    private Preference mSplitShadePref;

    private static ThemeUtils mThemeUtils;

    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.blackiron_settings_quicksettings);

        mThemeUtils = new ThemeUtils(getContext());

        final Context mContext = getContext();
        final ContentResolver resolver = mContext.getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mShowBrightnessSlider = findPreference(KEY_SHOW_BRIGHTNESS_SLIDER);
        if (mShowBrightnessSlider != null) {
            mShowBrightnessSlider.setOnPreferenceChangeListener(this);
        }

        boolean showSlider = LineageSettings.Secure.getIntForUser(resolver,
                LineageSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1, UserHandle.USER_CURRENT) > 0;

        mBrightnessSliderPosition = findPreference(KEY_BRIGHTNESS_SLIDER_POSITION);
        if (mBrightnessSliderPosition != null) {
            mBrightnessSliderPosition.setEnabled(showSlider);
        }

        mBrightnessSliderHaptic = findPreference(KEY_BRIGHTNESS_SLIDER_HAPTIC);
        if (mBrightnessSliderHaptic != null) {
            mBrightnessSliderHaptic.setEnabled(showSlider);
            mBrightnessSliderHaptic.setOnPreferenceChangeListener(this);
        }

        mQsRefactorEnabled = (SecureSettingSwitchPreference) findPreference(KEY_QS_REFACTOR_ENABLED);
        if (mQsRefactorEnabled != null) {
            mQsRefactorEnabled.setOnPreferenceChangeListener(this);
        }

        mShowAutoBrightness = findPreference(KEY_SHOW_AUTO_BRIGHTNESS);
        boolean automaticAvailable = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);
        if (mShowAutoBrightness != null) {
            if (automaticAvailable) {
                mShowAutoBrightness.setEnabled(showSlider);
            } else {
                prefScreen.removePreference(mShowAutoBrightness);
            }
        }

        mQsUI = (ListPreference) findPreference(KEY_QS_UI_STYLE);
        if (mQsUI != null) {
            mQsUI.setOnPreferenceChangeListener(this);
        }

        mQsPanelStyle = (ListPreference) findPreference(KEY_QS_PANEL_STYLE);
        if (mQsPanelStyle != null) {
            mQsPanelStyle.setOnPreferenceChangeListener(this);
        }

        mSplitShadePref = findPreference(KEY_QS_SPLIT_SHADE_ENABLED);
        if (mSplitShadePref != null) {
            mSplitShadePref.setOnPreferenceChangeListener(this);
        }

        mQsWidgetsPref = findPreference(KEY_QS_WIDGETS_ENABLED);
        if (mQsWidgetsPref != null) {
            mQsWidgetsPref.setOnPreferenceChangeListener(this);
        }

        checkQSOverlays(mContext);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getContext().getContentResolver();

        if (preference == mShowBrightnessSlider) {
            int value = Integer.parseInt((String) newValue);
            if (mBrightnessSliderPosition != null) mBrightnessSliderPosition.setEnabled(value > 0);
            if (mBrightnessSliderHaptic != null) mBrightnessSliderHaptic.setEnabled(value > 0);
            if (mShowAutoBrightness != null) mShowAutoBrightness.setEnabled(value > 0);
            return true;
        } else if (preference == mBrightnessSliderHaptic) {
            int value = (boolean) newValue ? 1 : 0;
            Settings.System.putIntForUser(resolver,
                    Settings.System.QS_BRIGHTNESS_SLIDER_HAPTIC, value, UserHandle.USER_CURRENT);
            return true;
        } else if (preference == mQsUI) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.QS_TILE_UI_STYLE, value, UserHandle.USER_CURRENT);
            updateQsStyle(getContext());
            checkQSOverlays(getContext());
            return true;
        } else if (preference == mQsRefactorEnabled) {
            SystemRestartUtils.restartSystemUI(getContext());
            return true;
        } else if (preference == mQsPanelStyle) {
            int value = Integer.parseInt((String) newValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.QS_PANEL_STYLE, value, UserHandle.USER_CURRENT);
            updateQsPanelStyle(getContext());
            checkQSOverlays(getContext());
            return true;
        } else if (preference == mQsWidgetsPref) {
            SystemUtils.showSystemUiRestartDialog(getContext());
            return true;
        } else if (preference == mSplitShadePref) {
            int value = (boolean) newValue ? 1 : 0;
            Settings.System.putIntForUser(resolver,
                    KEY_QS_SPLIT_SHADE_ENABLED, value, UserHandle.USER_CURRENT);
            updateSplitShadeEnabled(getActivity());
            return true;
        }
        return false;
    }

    private void updateSplitShadeEnabled(Context context) {
        ContentResolver resolver = context.getContentResolver();
        boolean splitShadeEnabled = Settings.System.getIntForUser(
                resolver,
                KEY_QS_SPLIT_SHADE_ENABLED , 0, UserHandle.USER_CURRENT) != 0;
        String splitShadeStyleCategory = "android.theme.customization.better_qs";
        String overlayThemeTarget  = "com.android.systemui";
        String overlayThemePackage  = "com.android.system.qs.ui.better_qs";
        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(context);
        }
        mHandler.postDelayed(() -> {
            mThemeUtils.setOverlayEnabled(splitShadeStyleCategory, overlayThemeTarget, overlayThemeTarget);
            if (splitShadeEnabled) {
                mThemeUtils.setOverlayEnabled(splitShadeStyleCategory, overlayThemePackage, overlayThemeTarget);
            }
        }, 1250);
    }

    public static void reset(Context mContext) {
        ContentResolver resolver = mContext.getContentResolver();
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_BATTERY_STYLE, -1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_BT_SHOW_DIALOG, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_SHOW_BATTERY_PERCENT, 2, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_TRANSPARENCY, 100, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_TILE_UI_STYLE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_LAYOUT_COLUMNS_LANDSCAPE, 4, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QQS_LAYOUT_ROWS, 2, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QQS_LAYOUT_ROWS_LANDSCAPE, 1, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_LAYOUT_COLUMNS, 2, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_TILE_VERTICAL_LAYOUT, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_TILE_LABEL_HIDE, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_TILE_LABEL_SIZE, 14, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                Settings.System.QS_BRIGHTNESS_SLIDER_HAPTIC, 0, UserHandle.USER_CURRENT);
        Settings.System.putIntForUser(resolver,
                LineageSettings.Secure.QS_SHOW_BRIGHTNESS_SLIDER, 1, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.QS_BRIGHTNESS_SLIDER_POSITION, 0, UserHandle.USER_CURRENT);
        LineageSettings.Secure.putIntForUser(resolver,
                LineageSettings.Secure.QS_SHOW_AUTO_BRIGHTNESS, 1, UserHandle.USER_CURRENT);
        updateQsStyle(mContext);
        updateQsPanelStyle(mContext);
        QsHeaderImageSettings.reset(mContext);
    }

    private static void updateQsStyle(Context context) {
        ContentResolver resolver = context.getContentResolver();

        boolean isA11Style = Settings.System.getIntForUser(resolver,
                Settings.System.QS_TILE_UI_STYLE , 0, UserHandle.USER_CURRENT) != 0;

        String qsUIStyleCategory = "android.theme.customization.qs_ui";
        String overlayThemeTarget  = "com.android.systemui";
        String overlayThemePackage  = "com.android.system.qs.ui.A11";

        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(context);
        }

        mThemeUtils.setOverlayEnabled(qsUIStyleCategory, overlayThemeTarget, overlayThemeTarget);

        if (isA11Style) {
            mThemeUtils.setOverlayEnabled(qsUIStyleCategory, overlayThemePackage, overlayThemeTarget);
        }
    }

    private static void updateQsPanelStyle(Context context) {
        ContentResolver resolver = context.getContentResolver();

        int qsPanelStyle = Settings.System.getIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE, 0, UserHandle.USER_CURRENT);

        String qsPanelStyleCategory = "android.theme.customization.qs_panel";
        String overlayThemeTarget  = "com.android.systemui";
        String overlayThemePackage  = "com.android.systemui";

        switch (qsPanelStyle) {
            case 1:
              overlayThemePackage = "com.android.system.qs.outline";
              break;
            case 2:
            case 3:
              overlayThemePackage = "com.android.system.qs.twotoneaccent";
              break;
            case 4:
              overlayThemePackage = "com.android.system.qs.shaded";
              break;
            case 5:
              overlayThemePackage = "com.android.system.qs.cyberpunk";
              break;
            case 6:
              overlayThemePackage = "com.android.system.qs.neumorph";
              break;
            case 7:
              overlayThemePackage = "com.android.system.qs.reflected";
              break;
            case 8:
              overlayThemePackage = "com.android.system.qs.surround";
              break;
            case 9:
              overlayThemePackage = "com.android.system.qs.thin";
              break;
            default:
              break;
        }

        if (mThemeUtils == null) {
            mThemeUtils = ThemeUtils.getInstance(context);
        }

        mThemeUtils.setOverlayEnabled(qsPanelStyleCategory,
                overlayThemePackage, overlayThemeTarget);
    }

    private void checkQSOverlays(Context context) {
        ContentResolver resolver = context.getContentResolver();
        int isA11Style = Settings.System.getIntForUser(resolver,
                Settings.System.QS_TILE_UI_STYLE , 0, UserHandle.USER_CURRENT);
        int qsPanelStyle = Settings.System.getIntForUser(resolver,
                Settings.System.QS_PANEL_STYLE , 0, UserHandle.USER_CURRENT);

        int index = mQsUI.findIndexOfValue(Integer.toString(isA11Style));
        if (index >= 0) mQsUI.setSummary(mQsUI.getEntries()[index]);
        mQsUI.setValue(Integer.toString(isA11Style));

        index = mQsPanelStyle.findIndexOfValue(Integer.toString(qsPanelStyle));
        if (index >= 0) mQsPanelStyle.setSummary(mQsPanelStyle.getEntries()[index]);
        mQsPanelStyle.setValue(Integer.toString(qsPanelStyle));
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.BLKI_SETTINGS;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blackiron_settings_quicksettings);
}

