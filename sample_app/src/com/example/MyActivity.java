package com.example;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;

public class MyActivity extends Activity
{
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        AsyncTask t = new AsyncTask() {
            @Override
            protected Object doInBackground(Object... objects) {
                try {
                    TestBobikQuery.main(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        t.execute();
    }
}
