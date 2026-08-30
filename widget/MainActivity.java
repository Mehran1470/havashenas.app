package com.havashenas.hormozgan;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(CityWidgetPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
