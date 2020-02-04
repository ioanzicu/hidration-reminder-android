package android.example.com.hidrationreminder;

import android.content.*;
import android.example.com.hidrationreminder.sync.ReminderTasks;
import android.example.com.hidrationreminder.sync.WaterReminderIntentService;
import android.example.com.hidrationreminder.utilities.NotificationUtils;
import android.example.com.hidrationreminder.utilities.PreferenceUtilities;
import android.example.com.hidrationreminder.utilities.ReminderUtilities;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {
    private TextView mWaterCountDisplay;
    private TextView mChargingCountDisplay;
    private ImageView mChargingImageView;

    private Toast mToast;
    IntentFilter mChargingIntentFilter;
    ChargingBroadcastReceiver mChargingReceiver;
    private static boolean isCharging;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /** Get the views **/
        mWaterCountDisplay = findViewById(R.id.tv_water_count);
        mChargingCountDisplay = findViewById(R.id.tv_charging_reminder_count);
        mChargingImageView = findViewById(R.id.iv_power_increment);

        /** Set the original values in the UI **/
        updateWaterCount();
        updateChargingReminderCount();

        ReminderUtilities.scheduleChargingReminder(this);

        /** Setup the shared preference listener **/
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        mChargingIntentFilter = new IntentFilter();
        mChargingIntentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        mChargingIntentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);

        mChargingReceiver = new ChargingBroadcastReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mChargingReceiver, mChargingIntentFilter);

        /** Determine the current charging state **/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
            isCharging = batteryManager.isCharging();

        } else {

            // For old phones, OS versions
            // sticky broadcast that contains a lot of information about the battery state.
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            // for the receiver. Pass in your intent filter as well. Passing in null means that you're
            // getting the current state of a sticky broadcast - the intent returned will contain the
            // battery information you need.
            Intent currentBatteryStatusIntent = this.registerReceiver(null, intentFilter);
            // BatteryManager.BATTERY_STATUS_CHARGING or BatteryManager.BATTERY_STATUS_FULL. This means
            // the battery is currently charging.
            int batteryStatus = currentBatteryStatusIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                    batteryStatus == BatteryManager.BATTERY_STATUS_FULL;
        }

        // charging method
        showCharging(isCharging);

        /** Register the receiver for future state changes **/
        registerReceiver(mChargingReceiver, mChargingIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        /** Unregister the receiver **/
        unregisterReceiver(mChargingReceiver);
    }

    /**
     * Updates the TextView to display the new water count from SharedPreferences
     */
    private void updateWaterCount() {
        int waterCount = PreferenceUtilities.getWaterCount(this);
        mWaterCountDisplay.setText(waterCount + "");
    }

    /**
     * Updates the TextView to display the new charging reminder count from SharedPreferences
     */
    private void updateChargingReminderCount() {
        int chargingReminders = PreferenceUtilities.getChargingReminderCount(this);
        String formattedChargingReminders = getResources().getQuantityString(
                R.plurals.charge_notification_count, chargingReminders, chargingReminders);
        mChargingCountDisplay.setText(formattedChargingReminders);

    }

    private void showCharging(boolean isCharging) {
        if (isCharging) {
            mChargingImageView.setImageResource(R.drawable.ic_power_pink_80px);
        } else {
            mChargingImageView.setImageResource(R.drawable.ic_power_grey_80px);
        }
    }

    /**
     * Adds one to the water count and shows a toast
     */
    public void incrementWater(View view) {
        if (mToast != null) mToast.cancel();
        mToast = Toast.makeText(this, R.string.water_chug_toast, Toast.LENGTH_SHORT);
        mToast.show();

        Intent incrementWaterCountIntent = new Intent(this, WaterReminderIntentService.class);
        incrementWaterCountIntent.setAction(ReminderTasks.ACTION_INCREMENT_WATER_COUNT);
        startService(incrementWaterCountIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /** Cleanup the shared preference listener **/
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * This is a listener that will update the UI when the water count or charging reminder counts
     * change
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PreferenceUtilities.KEY_WATER_COUNT.equals(key)) {
            updateWaterCount();
        } else if (PreferenceUtilities.KEY_CHARGING_REMINDER_COUNT.equals(key)) {
            updateChargingReminderCount();
        }
    }

    private class ChargingBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            boolean isCharging = action.equals(Intent.ACTION_POWER_CONNECTED);
            showCharging(isCharging);
        }
    }
}
