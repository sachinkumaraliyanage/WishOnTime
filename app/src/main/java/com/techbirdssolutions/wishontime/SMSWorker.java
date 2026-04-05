package com.techbirdssolutions.wishontime;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.ArrayList;
import java.util.Arrays;

public class SMSWorker extends Worker {
    public SMSWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override
    public Result doWork() {
        Intent intent = new Intent(getApplicationContext(), SMSForegroundService.class);
        intent.putExtra("msg", getInputData().getString("msg"));
        intent.putStringArrayListExtra("list", new ArrayList<>(Arrays.asList(getInputData().getStringArray("list"))));

        getApplicationContext().startForegroundService(intent);
        return Result.success();
    }
}