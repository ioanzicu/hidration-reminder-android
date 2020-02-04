package android.example.com.hidrationreminder.sync;

import android.app.IntentService;
import android.content.Intent;
import androidx.annotation.Nullable;

public class WaterReminderIntentService extends IntentService {

    public WaterReminderIntentService() {
        super(WaterReminderIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        String action = intent.getAction();
        ReminderTasks.executeTask(this, action);
    }
}
