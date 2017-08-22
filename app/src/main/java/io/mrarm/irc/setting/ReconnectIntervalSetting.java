package io.mrarm.irc.setting;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.util.SimpleTextWatcher;

public class ReconnectIntervalSetting extends SimpleSetting {

    private static final int sHolder = SettingsListAdapter.registerViewHolder(Holder.class,
            R.layout.settings_list_entry);

    private static List<Rule> sDefaultValue;
    public static final Type sListRuleType = new TypeToken<List<Rule>>(){}.getType();

    static {
        sDefaultValue = new ArrayList<>();
        sDefaultValue.add(new Rule(5000, 3));
        sDefaultValue.add(new Rule(30000, -1));
        sDefaultValue = Collections.unmodifiableList(sDefaultValue);
    }

    public static List<Rule> getDefaultValue() {
        return sDefaultValue;
    }

    public static List<Rule> parseRules(String value) {
        try {
            List<Rule> ret = SettingsHelper.getGson().fromJson(value, sListRuleType);
            if (ret != null)
                return ret;
        } catch (Exception ignored) {
        }
        return new ArrayList<>(getDefaultValue());
    }

    private List<Rule> mRules;

    public ReconnectIntervalSetting(String name, List<Rule> rules) {
        super(name, null);
        mRules = rules;
    }

    @Override
    public int getViewHolder() {
        return sHolder;
    }

    public static class Rule {

        public int reconnectDelay = -1;
        public int repeatCount = -1;

        public Rule() {
        }

        public Rule(int reconnectDelay, int repeatCount) {
            this.reconnectDelay = reconnectDelay;
            this.repeatCount = repeatCount;
        }

        public String getReconnectDelayAsString(Context context) {
            if (reconnectDelay != -1) {
                int delay = reconnectDelay / 1000;
                if ((delay % 60) == 0) {
                    delay /= 60;
                    if ((delay % 60) == 0) {
                        delay /= 60;
                        return context.getResources().getQuantityString(R.plurals.time_hours, delay, delay);
                    }
                    return context.getResources().getQuantityString(R.plurals.time_minutes, delay, delay);
                }
                return context.getResources().getQuantityString(R.plurals.time_seconds, delay, delay);
            }
            return null;
        }

    }

    public static class RulesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private AlertDialog mDialog;
        private List<Rule> mRules;
        private boolean mCurrentOkButtonState = true;

        public RulesAdapter(List<Rule> rules) {
            mRules = rules;
        }

        public void setDialog(AlertDialog dialog) {
            mDialog = dialog;
        }

        private void updateDialogOkButtonState() {
            boolean isDataValid = (mRules.size() > 0);
            for (int i = 0; i < mRules.size(); i++) {
                Rule rule = mRules.get(i);
                if (rule.reconnectDelay == -1 || (rule.repeatCount == -1 && i != mRules.size() - 1)) {
                    isDataValid = false;
                    break;
                }
            }
            mCurrentOkButtonState = isDataValid;
            mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isDataValid);
        }

        private void updateDialogOkButtonState(boolean newPotentialState) {
            if (newPotentialState != mCurrentOkButtonState)
                updateDialogOkButtonState();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.settings_reconnect_rule, viewGroup, false);
            return new RuleViewHolder(view, this);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            ((RuleViewHolder) viewHolder).bind(mRules.get(position));
        }

        @Override
        public int getItemCount() {
            return mRules.size();
        }

        public static class RuleViewHolder extends RecyclerView.ViewHolder {

            private static final int SPINNER_SECONDS = 0;
            private static final int SPINNER_MINUTES = 1;
            private static final int SPINNER_HOURS = 2;

            private EditText mReconnectDelayText;
            private Spinner mReconnectDelaySpinner;
            private EditText mRepeatCountText;

            public RuleViewHolder(View v, RulesAdapter adapter) {
                super(v);

                mReconnectDelaySpinner = v.findViewById(R.id.rule_duration_type);
                ArrayAdapter<CharSequence> spinnerAdapter = new ArrayAdapter<>(itemView.getContext(),
                        R.layout.simple_spinner_item, android.R.id.text1,
                        itemView.getResources().getStringArray(R.array.duration_types));
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mReconnectDelaySpinner.setAdapter(spinnerAdapter);

                mReconnectDelayText = v.findViewById(R.id.rule_interval);
                mRepeatCountText = v.findViewById(R.id.rule_repeat_times);

                View more = v.findViewById(R.id.rule_more);
                more.setOnClickListener((View view) -> {
                    PopupMenu menu = new PopupMenu(view.getContext(), view, GravityCompat.END);
                    MenuInflater inflater = menu.getMenuInflater();
                    inflater.inflate(R.menu.menu_reconnect_rule, menu.getMenu());
                    menu.setOnMenuItemClickListener((MenuItem item) -> {
                        if (item.getItemId() == R.id.action_add) {
                            adapter.mRules.add(getAdapterPosition() + 1, new Rule());
                            adapter.notifyItemInserted(getAdapterPosition() + 1);
                            adapter.updateDialogOkButtonState(false);
                            return true;
                        } else if (item.getItemId() == R.id.action_delete) {
                            if (adapter.mRules.size() > 1) {
                                adapter.mRules.remove(getAdapterPosition());
                                adapter.notifyItemRemoved(getAdapterPosition());
                                adapter.updateDialogOkButtonState(true);
                            }
                            return true;
                        }
                        return false;
                    });
                    menu.show();
                });

                mReconnectDelayText.addTextChangedListener(new SimpleTextWatcher((Editable s) -> {
                    updateReconnectDelay(adapter);
                }));
                mReconnectDelaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        updateReconnectDelay(adapter);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
                mRepeatCountText.addTextChangedListener(new SimpleTextWatcher((Editable s) -> {
                    Rule rule = adapter.mRules.get(getAdapterPosition());
                    try {
                        rule.repeatCount = Integer.parseInt(mRepeatCountText.getText().toString());
                    } catch (NumberFormatException e) {
                        rule.repeatCount = -1;
                    }

                    if (getAdapterPosition() == adapter.mRules.size() - 1) { // last item
                        if (mRepeatCountText.getText().length() > 0) {
                            // add a new empty item
                            adapter.mRules.add(getAdapterPosition() + 1, new Rule());
                            adapter.notifyItemInserted(getAdapterPosition() + 1);
                        }
                    } else if (getAdapterPosition() == adapter.mRules.size() - 2) {
                        int ii = adapter.mRules.size() - 1;
                        Rule lastRule = adapter.mRules.get(ii);
                        if (lastRule.reconnectDelay == -1 && lastRule.repeatCount == -1) {
                            // remove last, empty rule
                            adapter.mRules.remove(ii);
                            adapter.notifyItemRemoved(ii);
                        }
                    }
                    adapter.updateDialogOkButtonState();
                }));
            }

            private void updateReconnectDelay(RulesAdapter adapter) {
                int mp = 1;
                switch (mReconnectDelaySpinner.getSelectedItemPosition()) {
                    case SPINNER_SECONDS:
                        mp = 1000; // seconds
                        break;
                    case SPINNER_MINUTES:
                        mp = 1000 * 60; // minutes
                        break;
                    case SPINNER_HOURS:
                        mp = 1000 * 60 * 60; // hours
                        break;
                }

                Rule rule = adapter.mRules.get(getAdapterPosition());
                try {
                    rule.reconnectDelay = (int) (Double.parseDouble(mReconnectDelayText.getText().toString()) * mp);
                } catch (NumberFormatException e) {
                    rule.reconnectDelay = -1;
                }
                adapter.updateDialogOkButtonState(rule.reconnectDelay > 0);
            }

            public void bind(Rule rule) {
                if (rule.reconnectDelay != -1) {
                    int reconnectDelay = rule.reconnectDelay;
                    int spinnerItemId = SPINNER_SECONDS;
                    if ((reconnectDelay % 1000) != 0) {
                        mReconnectDelayText.setText(String.valueOf(reconnectDelay / 1000.0));
                        mReconnectDelaySpinner.setSelection(spinnerItemId);
                    } else {
                        reconnectDelay /= 1000;
                        if ((reconnectDelay % 60) == 0) {
                            reconnectDelay /= 60;
                            spinnerItemId = SPINNER_MINUTES;
                            if ((reconnectDelay % 60) == 0) {
                                reconnectDelay /= 60;
                                spinnerItemId = SPINNER_HOURS;
                            }
                        }

                        mReconnectDelayText.setText(String.valueOf(reconnectDelay));
                    }
                    mReconnectDelaySpinner.setSelection(spinnerItemId);
                } else {
                    mReconnectDelayText.setText("");
                    mReconnectDelaySpinner.setSelection(SPINNER_SECONDS);
                }

                mRepeatCountText.setText(rule.repeatCount == -1 ? "" : String.valueOf(rule.repeatCount));
            }

        }

    }

    public static class Holder extends SimpleSetting.Holder<ReconnectIntervalSetting> {


        public Holder(View itemView, SettingsListAdapter adapter) {
            super(itemView, adapter);
        }

        @Override
        public void bind(ReconnectIntervalSetting entry) {
            super.bind(entry);

            StringBuilder builder = new StringBuilder();
            boolean first = true;
            Context context = itemView.getContext();
            String delim = context.getString(R.string.text_comma);
            for (Rule rule : getEntry().mRules) {
                if (first)
                    first = false;
                else
                    builder.append(delim);
                builder.append(rule.getReconnectDelayAsString(context));
                if (rule.repeatCount != -1)
                    builder.append(context.getResources().getQuantityString(R.plurals.reconnect_desc_tries, rule.repeatCount, rule.repeatCount));
            }
            setValueText(builder.length() > 0 ? builder.toString() : null);
        }

        private View buildDialogView(RulesAdapter rules) {
            View view = LayoutInflater.from(itemView.getContext()).inflate(
                    R.layout.settings_reconnect_dialog, null);

            RecyclerView recyclerView = view.findViewById(R.id.rules);
            recyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext()));

            recyclerView.setAdapter(rules);

            return view;
        }

        @Override
        public void onClick(View v) {
            List<Rule> rules = new ArrayList<>(getEntry().mRules);
            if (rules.size() == 0)
                rules.add(new Rule());

            RulesAdapter adapter = new RulesAdapter(rules);

            View dialogView = buildDialogView(adapter);

            AlertDialog dialog = new AlertDialog.Builder(itemView.getContext())
                    .setPositiveButton(R.string.action_ok, (DialogInterface dialogInterface, int which) -> {
                        String newValue = SettingsHelper.getGson().toJson(rules);
                        /* TODO: Persist?
                        if (callChangeListener(newValue)) {
                            persistString(newValue);
                            notifyChanged();
                        } */
                    })
                    .setView(dialogView)
                    .setTitle(getEntry().mName)
                    .create();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            adapter.setDialog(dialog);
            dialog.show();
            dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }

    }

}