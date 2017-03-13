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

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

class StableArrayAdapter extends ArrayAdapter<ResolveInfo> {

  public StableArrayAdapter(Context context, List<ResolveInfo> objects) {
    super(context, android.R.layout.simple_list_item_1, objects);
  }

  @Override
  public long getItemId(int position) {
    ResolveInfo item = getItem(position);
    return item.activityInfo.packageName.hashCode();
  }

  @NonNull
  @Override
  public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    if (convertView == null) {
      convertView = LayoutInflater.from(getContext())
          .inflate(android.R.layout.simple_list_item_1, parent, false);
    }
    ResolveInfo item = getItem(position);
    TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
    tv.setText(item.loadLabel(this.getContext().getPackageManager()));
    return convertView;
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }
}