package io.mrarm.irc.setting.fragment;

import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import org.spongycastle.util.Pack;

import io.mrarm.irc.IRCService;
import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.setting.CheckBoxSetting;
import io.mrarm.irc.setting.IntervalSetting;
import io.mrarm.irc.setting.ReconnectIntervalSetting;
import io.mrarm.irc.setting.SettingsListAdapter;

public class ReconnectSettingsFragment extends SettingsListFragment
        implements NamedSettingsFragment {

    @Override
    public String getName() {
        return getString(R.string.pref_header_reconnect);
    }

    @Override
    public SettingsListAdapter createAdapter() {
        SettingsListAdapter a = new SettingsListAdapter(this);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        CheckBoxSetting mainSetting = new CheckBoxSetting(
                getString(R.string.pref_title_auto_reconnect),
                getString(R.string.pref_description_auto_reconnect), true);
        mainSetting.linkPreference(prefs, SettingsHelper.PREF_RECONNECT_ENABLED);
        a.add(mainSetting);
        a.add(new CheckBoxSetting(getString(R.string.pref_title_reconnect_connchg),
                getString(R.string.pref_description_reconnect_connchg), true)
                .linkPreference(prefs, SettingsHelper.PREF_RECONNECT_CONNCHG)
                .requires(mainSetting));
        a.add(new CheckBoxSetting(getString(R.string.pref_title_reconnect_wifi),
                getString(R.string.pref_description_reconnect_wifi), true)
                .linkPreference(prefs, SettingsHelper.PREF_RECONNECT_WIFI)
                .requires(mainSetting));
        a.add(new ReconnectIntervalSetting(getString(R.string.pref_title_reconnect_pattern))
                .linkPreference(prefs, SettingsHelper.PREF_RECONNECT_INTERVAL)
                .requires(mainSetting));
        CheckBoxSetting pingSetting = new CheckBoxSetting(getString(R.string.pref_title_ping_enabled),
                getString(R.string.pref_description_ping_enabled), false);
        pingSetting.linkPreference(prefs, SettingsHelper.PREF_PING_ENABLED);
        a.add(pingSetting);
        a.add(new CheckBoxSetting(getString(R.string.pref_title_ping_wifi),
                getString(R.string.pref_description_ping_wifi), true)
                .linkPreference(prefs, SettingsHelper.PREF_PING_WIFI)
                .requires(pingSetting));
        a.add(new IntervalSetting(getString(R.string.pref_title_ping_interval),
                15 * 60 * 1000)
                .setMinDuration(15 * 60 * 1000)
                .linkPreference(prefs, SettingsHelper.PREF_PING_INTERVAL)
                .requires(pingSetting));
        CheckBoxSetting bootSetting = new CheckBoxSetting(
                getString(R.string.pref_title_start_on_boot),
                getString(R.string.pref_description_start_on_boot), true);
        bootSetting.setChecked(isStartOnBootEnabled());
        bootSetting.addListener((e) -> setStartOnBootEnabled(bootSetting.isChecked()));
        a.add(bootSetting);
        return a;
    }

    private boolean isStartOnBootEnabled() {
        return getContext().getPackageManager().getComponentEnabledSetting(
                new ComponentName(getContext(), IRCService.BootReceiver.class))
                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

    private void setStartOnBootEnabled(boolean enabled) {
        getContext().getPackageManager().setComponentEnabledSetting(
                new ComponentName(getContext(), IRCService.BootReceiver.class),
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

}