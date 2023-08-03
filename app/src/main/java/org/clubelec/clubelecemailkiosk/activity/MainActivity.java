package org.clubelec.clubelecemailkiosk.activity;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.elevation.SurfaceColors;

import org.clubelec.clubelecemailkiosk.R;
import org.clubelec.clubelecemailkiosk.helper.DatabaseHelper;
import org.clubelec.clubelecemailkiosk.helper.SharedPrefManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_NOT_FIRST_LAUNCH = "not_first_launch";
    private static final String PREF_PASSWORD = "password";
    private static final int REQUEST_CODE_EXPORT_EMAILS = 2;
    private static final int REQUIRED_TAP_COUNT = 10;
    private static final long MAX_TAP_DELAY = 3000;
    private DatabaseHelper databaseHelper;
    private EditText editTextEmailAddress;
    private CheckBox checkBox;
    private Button button;
    private int tapCount = 0;
    private int passwordMaxLength = 6;
    private Handler handler;
    private Runnable tapResetRunnable;
    private SharedPrefManager sharedPrefManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setStatusBarColor(SurfaceColors.SURFACE_2.getColor(this));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        getWindow().getDecorView()
                .setOnSystemUiVisibilityChangeListener(visibility -> {
                    if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                        hideSystemUI(getWindow());
                    }
                });

        sharedPrefManager = SharedPrefManager.getInstance(getApplicationContext());
        boolean isNotFirstLaunch = sharedPrefManager.getBool(PREF_NOT_FIRST_LAUNCH);

        if (!isNotFirstLaunch) {
            showFirstLaunchPasswordInputDialog();
            hideSystemUI(getWindow());
        }

        databaseHelper = new DatabaseHelper(this);

        editTextEmailAddress = findViewById(R.id.editTextTextEmailAddress);
        editTextEmailAddress.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                hideKeyboard(v);
            }
        });
        checkBox = findViewById(R.id.checkBox);
        button = findViewById(R.id.button);

        button.setOnClickListener(v -> onButtonClick());

        ImageView logoImageView = findViewById(R.id.activity_main_clubelec_logo);
        logoImageView.setOnClickListener(v -> onLogoClick());

        handler = new Handler();
        tapResetRunnable = () -> tapCount = 0;
    }

    private void showFirstLaunchPasswordInputDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] filters = new InputFilter[]{new InputFilter.LengthFilter(passwordMaxLength)};
        input.setFilters(filters);
        new AlertDialog.Builder(this)
                .setTitle(R.string.first_launch_password)
                .setMessage(R.string.first_launch_password_desc)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String password = input.getText().toString();
                    if (!TextUtils.isEmpty(password) && password.length() == passwordMaxLength) {
                        sharedPrefManager.saveBool(PREF_NOT_FIRST_LAUNCH, true);
                        sharedPrefManager.saveString(PREF_PASSWORD, password);
                    } else {
                        showFirstLaunchPasswordInputDialog();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void onButtonClick() {
        String email = editTextEmailAddress.getText().toString();
        boolean isCheckBoxChecked = checkBox.isChecked();

        if (TextUtils.isEmpty(email)) {
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorDialog(getString(R.string.invalid_email_format));
        } else {
            if (!isCheckBoxChecked) {
                showErrorDialog(getString(R.string.checkbox_unchecked));
            } else {
                showConfirmationDialog(getString(R.string.is_email_correct) + email);
            }
        }
    }

    private void showConfirmationDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirmation_dialog)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    String email = editTextEmailAddress.getText().toString();
                    saveEmailToDatabase(email);
                    editTextEmailAddress = findViewById(R.id.editTextTextEmailAddress);
                    checkBox = findViewById(R.id.checkBox);
                    editTextEmailAddress.setText("");
                    checkBox.setChecked(false);
                    Toast.makeText(MainActivity.this, getString(R.string.email_saved), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void saveEmailToDatabase(String email) {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_EMAIL, email);
        db.insert(DatabaseHelper.TABLE_EMAILS, null, values);
    }


    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void onLogoClick() {
        tapCount++;
        if (tapCount == REQUIRED_TAP_COUNT) {
            handler.removeCallbacks(tapResetRunnable);
            handler.postDelayed(tapResetRunnable, MAX_TAP_DELAY);
            showPasswordDialog();
        } else if (tapCount > REQUIRED_TAP_COUNT) {
            handler.removeCallbacks(tapResetRunnable);
            tapCount = 1;
        } else {
            handler.removeCallbacks(tapResetRunnable);
            handler.postDelayed(tapResetRunnable, MAX_TAP_DELAY);
        }
    }

    private void showPasswordDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        InputFilter[] filters = new InputFilter[]{new InputFilter.LengthFilter(passwordMaxLength)};
        input.setFilters(filters);

        new AlertDialog.Builder(this)
                .setTitle(R.string.enter_password)
                .setView(input)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    String enteredPassword = input.getText().toString();
                    if (enteredPassword.length() != passwordMaxLength) {
                        showErrorDialog(getString(R.string.enter_password_incorrect_format));
                    } else {
                        String storedPassword = sharedPrefManager.getString(PREF_PASSWORD);

                        if (enteredPassword.equals(storedPassword)) {
                            showActionsDialog();
                        } else {
                            showErrorDialog(getString(R.string.enter_password_incorrect));
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showActionsDialog() {
        String[] actions = {getString(R.string.show_emails), getString(R.string.export_emails), getString(R.string.clear_database)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.select_action)
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEmails();
                            break;
                        case 1:
                            openExportActivity();
                            break;
                        case 2:
                            clearDatabase();
                            break;
                    }
                })
                .show();
    }

    private void showEmails() {
        List<String> emails = getEmailsFromDatabase();
        StringBuilder builder = new StringBuilder();
        for (String email : emails) {
            builder.append(email).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.saved_emails)
                .setMessage(builder.toString())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void clearDatabase() {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_EMAILS, null, null);
        Toast.makeText(this, getString(R.string.database_cleared), Toast.LENGTH_SHORT).show();
    }

    private void openExportActivity() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "emails.txt");
        startActivityForResult(intent, REQUEST_CODE_EXPORT_EMAILS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_EXPORT_EMAILS && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                exportEmails(uri);
            }
        }
    }

    private void exportEmails(Uri uri) {
        List<String> emailsList = getEmailsFromDatabase();

        if (emailsList.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_emails_found), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));
                for (String email : emailsList) {
                    writer.write(email);
                    writer.newLine();
                }
                writer.close();
                Toast.makeText(this, getString(R.string.emails_exported), Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, getString(R.string.export_failed), Toast.LENGTH_SHORT).show();
        }
    }

    private List<String> getEmailsFromDatabase() {
        List<String> emails = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        String[] projection = {DatabaseHelper.COLUMN_EMAIL};
        Cursor cursor = db.query(DatabaseHelper.TABLE_EMAILS, projection, null, null, null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String email = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EMAIL));
                emails.add(email);
            }
            cursor.close();
        }

        return emails;
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        hideSystemUI(getWindow());
    }

    @Override
    public void onResume() {
        super.onResume();
        hideSystemUI(getWindow());
    }

    @Override
    public void onBackPressed() {

    }

    public void hideSystemUI(Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.getInsetsController().hide(WindowInsets.Type.systemBars());
        } else {
            View decorView = window.getDecorView();
            int uiVisibility = decorView.getSystemUiVisibility();
            uiVisibility |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
            uiVisibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            uiVisibility |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            uiVisibility |= View.SYSTEM_UI_FLAG_IMMERSIVE;
            uiVisibility |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiVisibility);
        }
    }
}