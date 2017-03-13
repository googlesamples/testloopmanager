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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** An ExpandableListAdapter for holding {@link TestLoopGroup}s. */
public class TestLoopGroupAdapter extends BaseExpandableListAdapter {

  private final List<TestLoopGroup> testLoopGroups = new ArrayList<>();
  private final Set<Integer> checkedScenarios;
  private final LayoutInflater inflater;

  public TestLoopGroupAdapter(Context context, Set<Integer> checkedScenarios) {
    this.inflater = LayoutInflater.from(context);
    this.checkedScenarios = checkedScenarios;
  }

  public void addAll(List<TestLoopGroup> loopGroups) {
    testLoopGroups.addAll(loopGroups);
  }

  @Override
  public int getGroupCount() {
    return testLoopGroups.size();
  }

  @Override
  public int getChildrenCount(int groupPosition) {
    return getGroup(groupPosition).getLoops().size() + 1;
  }

  @Override
  public TestLoopGroup getGroup(int groupPosition) {
    return testLoopGroups.get(groupPosition);
  }

  @Override
  public Integer getChild(int groupPosition, int childPosition) {
    return getGroup(groupPosition).getLoops().get(childPosition);
  }

  @Override
  public long getGroupId(int groupPosition) {
    return groupPosition;
  }

  @Override
  public long getChildId(int groupPosition, int childPosition) {
    return ((long) groupPosition) << 32 | childPosition;
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }

  @Override
  public View getGroupView(int groupPosition, boolean isExpanded, View convertView,
      ViewGroup parent) {
    TextView groupTitle;
    if (convertView == null) {
      convertView = inflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
    }
    groupTitle = (TextView) convertView;
    groupTitle.setText(getGroup(groupPosition).getLabel());
    return convertView;
  }

  @Override
  public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
      View convertView, ViewGroup parent) {
    if (convertView == null) {
      convertView = inflater.inflate(R.layout.test_loop_item, parent, false);
    }
    CheckBox checkBox = (CheckBox) convertView;
    if (childPosition == 0) {
      boolean allSelected = checkedScenarios.containsAll(getGroup(groupPosition).getLoops());
      checkBox.setChecked(allSelected);
      if (allSelected) {
        checkBox.setText(R.string.unselect_all);
      } else {
        checkBox.setText(R.string.select_all);
      }
    } else {
      Integer item = getChild(groupPosition, childPosition - 1);
      checkBox.setText(String.valueOf(item));
      checkBox.setChecked(checkedScenarios.contains(item));
    }
    return convertView;
  }

  @Override
  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return true;
  }
}
