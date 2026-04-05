package com.techbirdssolutions.wishontime;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.color.DynamicColors;
import android.text.Editable;
import android.text.TextWatcher;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<String[]> permissionLauncher;
    private ActivityResultLauncher<Intent> contactPickerLauncher;

    private List<ContactModel> includeList = new ArrayList<>();
    private List<ContactModel> excludeList = new ArrayList<>();
    private List<ContactModel> currentDisplayList = includeList;
    private ContactAdapter contactAdapter;
    private TextView tvListTitle;

    private long selectedTimeMillis = 0;
    private TextView tvSelectedTime;
    private RadioButton rbInclude;

    private String selectedAccountName = null;
    private String selectedAccountType = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        DynamicColors.applyToActivityIfAvailable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        tvListTitle = findViewById(R.id.tvListTitle);
        rbInclude = findViewById(R.id.rbInclude);
        
        setupRecyclerView();

        // Permission Handling
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                    if (Boolean.FALSE.equals(result.get(Manifest.permission.SEND_SMS))) {
                        Toast.makeText(this, "SMS Permission Denied", Toast.LENGTH_SHORT).show();
                    }
                    checkBatteryOptimization();
                });

        permissionLauncher.launch(new String[]{
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.GET_ACCOUNTS
        });

        contactPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleContactPicked(result.getData().getData());
                    }
                });

        findViewById(R.id.btnPickTime).setOnClickListener(v -> showTimePicker());
        findViewById(R.id.btnAddContact).setOnClickListener(v -> pickContact());

        TextInputLayout tilMessage = findViewById(R.id.tilMessage);
        EditText etMessage = findViewById(R.id.etMessage);

        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSmsCounter(s.toString(), tilMessage);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        findViewById(R.id.btnStart).setOnClickListener(v -> scheduleSmsTask());
        findViewById(R.id.btnStop).setOnClickListener(v -> stopSchedule());
        
        // Navigation to About Page
        findViewById(R.id.toolbar).setOnClickListener(v -> openAboutPage());
        findViewById(R.id.btnAboutDeveloper).setOnClickListener(v -> openAboutPage());

        findViewById(R.id.btnSelectAccount).setOnClickListener(v -> showAccountPicker());

        RadioGroup modeGroup = findViewById(R.id.modeGroup);
        modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbInclude) {
                currentDisplayList = includeList;
                tvListTitle.setText("Include List:");
            } else {
                currentDisplayList = excludeList;
                tvListTitle.setText("Exclude List:");
            }
            updateAdapter();
        });
    }

    private void setupRecyclerView() {
        RecyclerView rvContacts = findViewById(R.id.rvContacts);
        contactAdapter = new ContactAdapter(currentDisplayList, position -> {
            currentDisplayList.remove(position);
            contactAdapter.notifyItemRemoved(position);
            String listType = (currentDisplayList == includeList) ? "Include" : "Exclude";
            Toast.makeText(this, "Removed from " + listType, Toast.LENGTH_SHORT).show();
        });

        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        rvContacts.setAdapter(contactAdapter);
    }

    private void updateAdapter() {
        contactAdapter = new ContactAdapter(currentDisplayList, position -> {
            currentDisplayList.remove(position);
            contactAdapter.notifyItemRemoved(position);
            String listType = (currentDisplayList == includeList) ? "Include" : "Exclude";
            Toast.makeText(this, "Removed from " + listType, Toast.LENGTH_SHORT).show();
        });
        ((RecyclerView) findViewById(R.id.rvContacts)).setAdapter(contactAdapter);
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .setTitleText("Select Schedule Time")
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            Calendar selected = Calendar.getInstance();
            selected.set(Calendar.HOUR_OF_DAY, picker.getHour());
            selected.set(Calendar.MINUTE, picker.getMinute());
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);

            if (selected.getTimeInMillis() <= System.currentTimeMillis()) {
                selected.add(Calendar.DAY_OF_MONTH, 1);
            }

            selectedTimeMillis = selected.getTimeInMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            tvSelectedTime.setText("Scheduled for: " + sdf.format(selected.getTime()));
        });

        picker.show(getSupportFragmentManager(), "TIME_PICKER");
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        contactPickerLauncher.launch(intent);
    }

    private void handleContactPicked(Uri contactUri) {
        String[] projection = {
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        try (android.database.Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                String number = cursor.getString(1).replaceAll("[^0-9+]", "");
                ContactModel contact = new ContactModel(name, number);

                if (!currentDisplayList.contains(contact)) {
                    currentDisplayList.add(contact);
                    contactAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    private void checkBatteryOptimization() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void scheduleSmsTask() {
        String msg = ((EditText) findViewById(R.id.etMessage)).getText().toString();
        if (msg.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedTimeMillis == 0) {
            Toast.makeText(this, "Please select a time", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> recipients = getFinalRecipients();
        if (recipients.isEmpty()) {
            Toast.makeText(this, "No recipients found", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, SMSForegroundService.class);
        intent.putExtra("msg", msg);
        intent.putStringArrayListExtra("list", new ArrayList<>(recipients));
        intent.putExtra("scheduledTime", selectedTimeMillis);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        Toast.makeText(this, "Schedule Started", Toast.LENGTH_SHORT).show();
    }

    private List<String> getFinalRecipients() {
        Set<String> allContacts = getAllSystemContacts();
        Set<String> finalSet = new LinkedHashSet<>();

        if (rbInclude.isChecked()) {
            for (ContactModel cm : includeList) {
                finalSet.add(cm.getNumber());
            }
        } else {
            finalSet.addAll(allContacts);
            for (ContactModel cm : excludeList) {
                finalSet.remove(cm.getNumber());
            }
        }

        return new ArrayList<>(finalSet);
    }

    private void showAccountPicker() {
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType("com.google");
        
        if (accounts.length == 0) {
            Toast.makeText(this, "No Google accounts found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] accountNames = new String[accounts.length + 1];
        accountNames[0] = "All Accounts (Show Everything)";
        for (int i = 0; i < accounts.length; i++) {
            accountNames[i + 1] = accounts[i].name;
        }

        new AlertDialog.Builder(this)
                .setTitle("Filter by Google Account")
                .setItems(accountNames, (dialog, which) -> {
                    if (which == 0) {
                        selectedAccountName = null;
                        selectedAccountType = null;
                        ((Button)findViewById(R.id.btnSelectAccount)).setText("All Accounts (Tap to Filter)");
                    } else {
                        selectedAccountName = accounts[which - 1].name;
                        selectedAccountType = accounts[which - 1].type;
                        ((Button)findViewById(R.id.btnSelectAccount)).setText("Filtered: " + selectedAccountName);
                    }
                    Toast.makeText(this, "Filter applied to 'Exclude' mode", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private Set<String> getAllSystemContacts() {
        Set<String> uniqueNumbers = new LinkedHashSet<>();
        Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
        
        String selection = null;
        String[] selectionArgs = null;

        if (selectedAccountName != null) {
            // This is a bit complex as we need to join with RawContacts to filter by account
            // For simplicity in a basic app, we can query Data.CONTENT_URI which includes account info if we join right
            uri = ContactsContract.Data.CONTENT_URI;
            selection = ContactsContract.Data.MIMETYPE + "=? AND " +
                    ContactsContract.RawContacts.ACCOUNT_NAME + "=? AND " +
                    ContactsContract.RawContacts.ACCOUNT_TYPE + "=?";
            selectionArgs = new String[]{
                    ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                    selectedAccountName,
                    selectedAccountType
            };
        }

        Cursor cursor = getContentResolver().query(uri, null, selection, selectionArgs, null);

        if (cursor != null) {
            int numIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
            if (numIndex != -1) {
                while (cursor.moveToNext()) {
                    String num = cursor.getString(numIndex);
                    if (num != null) {
                        uniqueNumbers.add(num.replaceAll("[^0-9+]", ""));
                    }
                }
            }
            cursor.close();
        }
        return uniqueNumbers;
    }

    private void updateSmsCounter(String message, TextInputLayout til) {
        if (message.isEmpty()) {
            til.setHelperText("SMS 1: 0/160");
            return;
        }
        int length = message.length();
        int smsCount = (length + 159) / 160;
        int remainingInCurrent = 160 - (length % 160);
        if (length % 160 == 0 && length > 0) remainingInCurrent = 0;
        
        String helper = "SMS " + smsCount + ": " + (160 - remainingInCurrent) + "/160";
        if (smsCount > 1) {
            helper += " (Total Parts: " + smsCount + ")";
        }
        til.setHelperText(helper);
    }

    private void stopSchedule() {
        stopService(new Intent(this, SMSForegroundService.class));
        Toast.makeText(this, "All Tasks Cancelled", Toast.LENGTH_SHORT).show();
    }

    private void openAboutPage() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }
}
