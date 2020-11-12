package com.bethel.mycoolwallet.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.TextView;

import com.bethel.mycoolwallet.R;
import com.bethel.mycoolwallet.utils.Commons;
import com.bethel.mycoolwallet.utils.CrashReporter;
import com.bethel.mycoolwallet.utils.Iso8601Format;
import com.google.common.io.CharStreams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class DebugActivity extends BaseActivity {
    private static final int REQUEST_CODE_CREATE_DOCUMENT = 0;
    TextView textView ;

    public static void start(Context context) {
        context.startActivity(new Intent(context, DebugActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
        textView = findViewById(R.id.textView);
        AsyncTask.execute(() -> {
            File crashFile = CrashReporter.getCrashTraceFile();// getBackgroundTracesFile();
           final String data = readCrashReporter(crashFile);
           runOnUiThread(()-> textView.setText(data));
        });
    }

    private String readCrashReporter(File crashFile) {
        String data;
        try {
            InputStream is = new FileInputStream(crashFile);
            InputStreamReader inReader = new InputStreamReader(is, Commons.UTF_8);

            BufferedReader textIn = new BufferedReader(inReader);
            StringBuilder bufferText = new StringBuilder();

            CharStreams.copy(textIn, bufferText);
            textIn.close();
            data = bufferText.toString();
        } catch (IOException e) {
            data = e.getMessage();
        }
        return data;
    }

    private void backupCrashReporter() {

        final DateFormat dateFormat = new Iso8601Format("yyyy-MM-dd-HH-mm");
        dateFormat.setTimeZone(TimeZone.getDefault());

        // filename = bitcoin-wallet-backup-testnet-2020-11-05-09-52
        final StringBuilder filename = new StringBuilder("crash_reporter");
        filename.append('-');
        filename.append(dateFormat.format(new Date()));
        filename.append(".txt");

        // 请求创建文件
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/txt");
        intent.putExtra(Intent.EXTRA_TITLE, filename.toString());
        startActivityForResult(intent, REQUEST_CODE_CREATE_DOCUMENT);
    }

}
