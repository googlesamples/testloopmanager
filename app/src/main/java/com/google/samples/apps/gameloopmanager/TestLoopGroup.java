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

import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.List;

/** A container for all of the test loop group data. */
public class TestLoopGroup implements Parcelable {

  private String label;
  private List<Integer> loops;

  public TestLoopGroup(String label, List<Integer> loops) {
    this.label = label;
    this.loops = loops;
  }

  public String getLabel() {
    return label;
  }

  public List<Integer> getLoops() {
    return loops;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TestLoopGroup that = (TestLoopGroup) o;

    if (label != null ? !label.equals(that.label) : that.label != null) {
      return false;
    }
    return loops != null ? loops.equals(that.loops) : that.loops == null;
  }

  @Override
  public int hashCode() {
    int result = label != null ? label.hashCode() : 0;
    result = 31 * result + (loops != null ? loops.hashCode() : 0);
    return result;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.label);
    dest.writeList(this.loops);
  }

  protected TestLoopGroup(Parcel in) {
    this.label = in.readString();
    this.loops = new ArrayList<Integer>();
    in.readList(this.loops, Integer.class.getClassLoader());
  }

  public static final Creator<TestLoopGroup> CREATOR = new Creator<TestLoopGroup>() {
    @Override
    public TestLoopGroup createFromParcel(Parcel source) {
      return new TestLoopGroup(source);
    }

    @Override
    public TestLoopGroup[] newArray(int size) {
      return new TestLoopGroup[size];
    }
  };
}
