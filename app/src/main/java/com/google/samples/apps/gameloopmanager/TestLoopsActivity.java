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

import android.Manifest.permission;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** An Activity for launching the test loops specified in the AndroidManifest. */
public class TestLoopsActivity extends AppCompatActivity {

  private static final int TEST_LOOP_REQUEST_CODE = 1;
  private static final int REQUEST_STORAGE_PERMISSION = 2;

  private static final String CHECKED_SCENARIOS = "checkedScenarios";
  private static final String TIMEOUT = "timeout";

  private boolean runningTestLoop = false;
  private ResolveInfo resolveInfo;
  private final Set<Integer> checkedScenarios = new TreeSet<>();
  private long timeout;

  private TestLoopGroupAdapter adapter;
  private Button launchAllButton;
  private ExpandableListView expandableListView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_test_loops);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    launchAllButton = (Button) findViewById(R.id.run_all);
    launchAllButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        startLoopFromUi();
      }
    });

    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    timeout = prefs.getLong(TIMEOUT, 3 * 60 * 1000);

    if (savedInstanceState != null) {
      checkedScenarios.addAll(savedInstanceState.getIntegerArrayList(CHECKED_SCENARIOS));
    }

    resolveInfo = getIntent().getParcelableExtra("resolveInfo");

    adapter = new TestLoopGroupAdapter(this, checkedScenarios);
    expandableListView = (ExpandableListView) findViewById(R.id.test_loop_list);
    expandableListView.setAdapter(adapter);
    expandableListView.setOnChildClickListener(new OnChildClickListener() {
      @Override
      public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
          int childPosition, long id) {
        if (childPosition == 0) {
          CheckBox checkBox = (CheckBox) v;
          if (checkBox.isChecked()) {
            for (int item : adapter.getGroup(groupPosition).getLoops()) {
              checkedScenarios.remove(item);
            }
          } else {
            for (int item : adapter.getGroup(groupPosition).getLoops()) {
              checkedScenarios.add(item);
            }
          }
        } else {
          Integer data = adapter.getChild(groupPosition, childPosition - 1);
          if (!checkedScenarios.remove(data)) {
            checkedScenarios.add(data);
          }
        }

        adapter.notifyDataSetChanged();
        updateTestLoopButton();
        return true;
      }
    });

    new LoadManifestDataFromApplicationInfoTask().execute();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case TEST_LOOP_REQUEST_CODE:
        runningTestLoop = false;
        break;
      case REQUEST_STORAGE_PERMISSION:
        if (resultCode == RESULT_OK) {
          startLoopFromUi();
        } else {
          Toast.makeText(this, R.string.must_accept_permission, Toast.LENGTH_LONG).show();
        }
        break;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_test_loops, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.set_timeout:
        final EditText editText =
            (EditText) getLayoutInflater().inflate(R.layout.view_edit_timeout, null, false);
        editText.setText(String.valueOf(timeout / 1000));
        new Builder(this)
            .setTitle(R.string.set_timeout)
            .setView(editText)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                try {
                  timeout = Integer.parseInt(editText.getText().toString()) * 1000;
                  SharedPreferences prefs =
                      PreferenceManager.getDefaultSharedPreferences(TestLoopsActivity.this);
                  SharedPreferences.Editor editor = prefs.edit();
                  editor.putLong(TIMEOUT, timeout);
                  editor.apply();
                } catch (NumberFormatException e) {
                  Toast.makeText(TestLoopsActivity.this, R.string.must_be_number, Toast.LENGTH_LONG)
                      .show();
                }
              }
            })
            .show();
        return true;
      case android.R.id.home:
        onBackPressed();
        return true;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);

    outState.putIntegerArrayList(CHECKED_SCENARIOS, new ArrayList<>(checkedScenarios));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    launchAllButton = null;
    expandableListView = null;
  }

  public void startLoopFromUi() {
    if (!checkOrRequestPermission()) {
      return;
    }
    new RunScenariosTask(
        resolveInfo.activityInfo.packageName, new ArrayList<>(checkedScenarios)).execute();
  }

  private void updateTestLoopButton() {
    if (checkedScenarios.size() == 0) {
      launchAllButton.setText(R.string.run_default_loop);
    } else {
      launchAllButton.setText(
          String.format(
              getResources().getQuantityString(R.plurals.run_loops, checkedScenarios.size()),
              checkedScenarios.size()));
    }
  }

  /**
   * Starts the test loop by launching an intent.
   */
  public boolean startLoops(String packageName, List<Integer> scenarios) {
    long timeoutTime = timeout + System.currentTimeMillis();
    if (scenarios.isEmpty()) {
      scenarios.add(-1);
    }
    for (int scenario : scenarios) {
      try {
        if (System.currentTimeMillis() >= timeoutTime) {
          return false;
        }
        runScenario(packageName, scenario);
        while (runningTestLoop && System.currentTimeMillis() < timeoutTime) {
          Thread.sleep(1000);
        }
        if (runningTestLoop) {
          finishActivity(TEST_LOOP_REQUEST_CODE);
        }
        Thread.sleep(500); // mild hack to prevent crashes from reopening an Activity too fast
      } catch (IOException | InterruptedException e) {
        // TODO: handle this
      }
    }
    return true;
  }

  private void runScenario(String packageName, int scenario) throws IOException {
    String filename = String.format("results%d.json", scenario > 0 ? scenario : 0);
    File f = new File(Environment.getExternalStorageDirectory(), filename);
    //noinspection ResultOfMethodCallIgnored
    f.createNewFile();
    String myPackageName = getPackageName();
    Uri fileUri = FileProvider.getUriForFile(this, myPackageName, f);

    Intent intent =
        new Intent("com.google.intent.action.TEST_LOOP")
            .setPackage(packageName)
            .setDataAndType(fileUri, "application/javascript")
            .setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    if (scenario >= 0) {
      intent.putExtra("scenario", scenario);
    }

    runningTestLoop = true;
    startActivityForResult(intent, TEST_LOOP_REQUEST_CODE);
  }

  private boolean checkOrRequestPermission() {
    if (ContextCompat.checkSelfPermission(this, permission.WRITE_EXTERNAL_STORAGE)
        != PackageManager.PERMISSION_GRANTED) {
      // Should we show an explanation?
      if (ActivityCompat.shouldShowRequestPermissionRationale(this,
          permission.WRITE_EXTERNAL_STORAGE)) {
        // TODO: show an explanation
      } else {
        ActivityCompat.requestPermissions(this,
            new String[]{permission.WRITE_EXTERNAL_STORAGE},
            REQUEST_STORAGE_PERMISSION);
      }
      return false;
    }
    return true;
  }

  private class LoadManifestDataFromApplicationInfoTask extends
      AsyncTask<Void, Void, List<TestLoopGroup>> {

    @Override
    protected List<TestLoopGroup> doInBackground(Void... params) {
      List<TestLoopGroup> testLoopGroups = new ArrayList<>();
      try {
        ApplicationInfo appInfo = getPackageManager()
            .getApplicationInfo(resolveInfo.activityInfo.packageName, PackageManager.GET_META_DATA);
        Bundle metaData = appInfo.metaData;
        for (String key : metaData.keySet()) {
          if (key.startsWith("com.google.test.loops.")) {
            List<Integer> loops = new ArrayList<>();
            String loopsList = metaData.getString(key);
            String[] loopsArray = loopsList.split(",");
            for (String s : loopsArray) {
              if (s.contains("-")) {
                String[] bounds = s.split("-");
                if (bounds.length != 2) {
                  throw new RuntimeException("Bad list format");
                }
                int lowerBound = Integer.parseInt(bounds[0]);
                int upperBound = Integer.parseInt(bounds[1]);
                if (upperBound < lowerBound) {
                  throw new RuntimeException("Bad list format");
                }
                for (int i = lowerBound; i <= upperBound; i++) {
                  loops.add(i);
                }
              } else {
                loops.add(Integer.parseInt(s));
              }
            }
            String label;
            switch (key) {
              case "com.google.test.loops.player_experience":
                label = getResources().getString(R.string.player_experience);
                break;
              case "com.google.test.loops.gpu_compatibility":
                label = getResources().getString(R.string.gpu_compatibility);
                break;
              case "com.google.test.loops.compatibility":
                label = getResources().getString(R.string.compatibility);
                break;
              case "com.google.test.loops.performance":
                label = getResources().getString(R.string.performance);
                break;
              default:
                label = key;
                break;
            }
            testLoopGroups.add(new TestLoopGroup(label, loops));
          }
        }
        int count = metaData.getInt("com.google.test.loops", 0);
        List<Integer> loops = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
          loops.add(i);
        }
        testLoopGroups.add(new TestLoopGroup("All", loops));
      } catch (Exception e) {
        e.printStackTrace();
        // TODO: Handle it
      }
      return testLoopGroups;
    }

    @Override
    protected void onPostExecute(List<TestLoopGroup> testLoopGroups) {
      // View may have been destroyed by now
      if (launchAllButton != null && testLoopGroups.size() > 0) {
        adapter.addAll(testLoopGroups);
        adapter.notifyDataSetChanged();
        expandableListView.expandGroup(testLoopGroups.size() - 1); // expand the "All" group
        updateTestLoopButton();
      }
    }
  }

  private class RunScenariosTask extends AsyncTask<Void, Void, Boolean> {

    private final String packageName;
    private final List<Integer> scenarios;

    RunScenariosTask(String packageName, List<Integer> scenarios) {
      this.packageName = packageName;
      this.scenarios = scenarios;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
      return startLoops(packageName, scenarios);
    }

    @Override
    protected void onPostExecute(Boolean finishedAll) {
      if (finishedAll) {
        Toast
            .makeText(
                TestLoopsActivity.this, R.string.finished_executing, Toast.LENGTH_LONG)
            .show();
      } else {
        Toast.makeText(
            TestLoopsActivity.this, R.string.didnt_finish, Toast.LENGTH_LONG).show();
      }
    }
  }
}
