package com.leancloud.leancloudfeedbackdemo;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;
import com.leancloud.modules.feedback.*;


public class MainActivity extends ActionBarActivity {
    private FeedbackAgent agent = new FeedbackAgent(MainActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //requestWindowFeature(Window.FEATURE_ACTION_BAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button feedbackButton = (Button)findViewById(R.id.feedback);
        feedbackButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                agent.startDefaultThreadActivity();
            }
        });
        this.agent.sync();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
