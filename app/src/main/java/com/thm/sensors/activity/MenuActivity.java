package com.thm.sensors.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toolbar;

import com.thm.sensors.R;
import com.thm.sensors.Util;

public final class MenuActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menu_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setActionBar(toolbar);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                Intent intent = new Intent(this, SlaveActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                intent.putExtra("sensor", Util.PROXIMITY);
                startActivity(intent);
                break;
            case R.id.button2:
                intent = new Intent(this, SlaveActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                intent.putExtra("sensor", Util.HEARTBEAT);
                startActivity(intent);
                break;
            case R.id.button3:
                intent = new Intent(this, SlaveActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                intent.putExtra("sensor", Util.ACCELERATION);
                startActivity(intent);
                break;
            case R.id.button4:
                intent = new Intent(this, MasterActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                break;
        }
    }
}
