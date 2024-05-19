package ru.articus.assorti;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

public class MainActivity extends AppCompatActivity {

    private static WebView webview;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new DBHelper(this);

        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        webview = findViewById(R.id.webView);
        webview.getSettings().setJavaScriptEnabled(true);
        webview.addJavascriptInterface(new JavaScriptInterface(this), "webView_Storage");
        webview.loadUrl("https://app.cafe-assorti.ru/");
        //https://app.cafe-assorti.ru/
        //https://dev.arcafe.su/assorti/mobile_app

        WebViewClient webViewClient = new WebViewClient()
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView webview, String url)
            {
                if (url.startsWith("tel:") || url.startsWith("sms:") || url.startsWith("smsto:") || url.startsWith("mms:") || url.startsWith("mmsto:"))
                {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        };
        webview.setWebViewClient(webViewClient);
    }


    @Override
    public void onBackPressed() {
        if (webview.canGoBack()) {
            webview.goBack();
        } else {
            openQuitDialog();
        }
    }

    public static void Reload(){
        webview.post(new Runnable() {
            @Override
            public void run() {
                webview.loadUrl("https://app.cafe-assorti.ru/");
            }
        });
    }

    private void openQuitDialog()
    {
        new AlertDialog.Builder(this)
                .setTitle("Выйти из приложения?")
                .setNegativeButton("Нет", null)
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {
                        MainActivity.super.onBackPressed();
                    }
                }).create().show();
    }

    public class JavaScriptInterface {
        Context mContext;

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        Cursor cr;
        final char kv = (char) 34;
        Boolean isOld;

        JavaScriptInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void saveJson(String getJson, String nameKey) {
            cv.put(DBHelper.KEY_DATA, getJson);
            cv.put(DBHelper.KEY_NAME, nameKey);
            try {
                cr = db.rawQuery("SELECT * FROM mainTable WHERE name = " + kv  + nameKey + kv, null);
                isOld = cr.getCount() > 0;
            } catch (Exception e) {
                Log.e("SQLiteRawQuery", e.toString());
            }

            if (!isOld) {
                db.insert(DBHelper.MAIN_TABLE, null, cv);
            } else {
                db.update(DBHelper.MAIN_TABLE, cv, "name = " + kv + nameKey + kv, null);
            }
            cr.close();
        }

        @JavascriptInterface
        public String loadJson(String nameKey) {
            cr = db.query(DBHelper.MAIN_TABLE, null, "name = " + kv + nameKey + kv, null, null, null, null);
            cr.moveToFirst();
            if(cr.getCount() != 0) {
                return cr.getString(1);
            }
            else{return null;}

        }

        @JavascriptInterface
        public String loadString(String nameKey) {
            return loadJson(nameKey);
        }

        @JavascriptInterface
        public void saveString(String getString, String nameKey) {
            saveJson(getString, nameKey);
        }

        @JavascriptInterface
        public void saveBool(Boolean getBool ,String nameKey){
            if(getBool){
                saveJson("1", nameKey);
            }else {
                saveJson("0", nameKey);
            }
        }

        @JavascriptInterface
        public Boolean loadBool(String nameKey){
            String b =  loadJson(nameKey);
            if(b.equals("1")){
                return true;
            }else if (b.equals("0")){
                return false;
            }
            return null;
        }

        @JavascriptInterface
        public String getFirebaseToken() {
            try {
                Task task = FCMService.getToken(mContext);
                Tasks.await(task);
                return task.getResult().toString();

            }catch (Exception e) {
                Log.e("Token:ERROR", "" + e.getMessage());
            }
            return null;
        }
    }
}