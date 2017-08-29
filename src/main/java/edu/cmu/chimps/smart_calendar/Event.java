/*
  Copyright 2017 CHIMPS Lab, Carnegie Mellon University
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package edu.cmu.chimps.smart_calendar;


public class Event {
    private String mEventName;
    private Long mBeginTime;
    private Long mEndTime;
    private String mLocation;

    public Long getBeginTime() {
        return mBeginTime;
    }

    public void setBeginTime(Long beginTime) {
        mBeginTime = beginTime;
    }

    public Long getEndTime() {
        return mEndTime;
    }

    public void setEndTime(Long endTime) {
        mEndTime = endTime;
    }

    public String getEventName() {
        return mEventName;
    }

    public void setEventName(String eventName) {
        mEventName = eventName;
    }

    public String getLocation() {
        return mLocation;
    }

    public void setLocation(String location) {
         mLocation = location;
    }
}
