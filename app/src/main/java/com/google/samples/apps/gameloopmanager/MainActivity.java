/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.gameloopmanager;

import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The Activity responsible for displaying all of the apps compatible with the
 * com.google.intent.action.TEST_LOOP custom Intent.
 */
public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
  }

  @Override
  protected void onResume() {
    super.onResume();
    ListView lv = (ListView) findViewById(R.id.listView);

    final Intent mainIntent = new Intent("com.google.intent.action.TEST_LOOP", null);
    mainIntent.setType("application/javascript");
    mainIntent.addCategory(Intent.CATEGORY_DEFAULT);

    final List<ResolveInfo> pkgAppsList =
        getPackageManager().queryIntentActivities(mainIntent, 0);

    Collections.sort(pkgAppsList, new Comparator<ResolveInfo>() {
      @Override
      public int compare(ResolveInfo o1, ResolveInfo o2) {
        return o1.activityInfo.packageName
            .compareTo(o2.activityInfo.packageName);
      }
    });

    final StableArrayAdapter adapter = new StableArrayAdapter(MainActivity.this, pkgAppsList);
    lv.setAdapter(adapter);

    lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view,
          int position, long id) {
        Intent i = new Intent(MainActivity.this, TestLoopsActivity.class)
            .putExtra("resolveInfo", pkgAppsList.get(position));
        startActivity(i);
      }
    });
  }
}
