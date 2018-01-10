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

import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import edu.cmu.chimps.messageontap_api.JSONUtils;
import edu.cmu.chimps.messageontap_api.MessageOnTapPlugin;
import edu.cmu.chimps.messageontap_api.MethodConstants;
import edu.cmu.chimps.messageontap_api.ParseTree;
import edu.cmu.chimps.messageontap_api.SemanticTemplate;
import edu.cmu.chimps.messageontap_api.ServiceAttributes;
import edu.cmu.chimps.messageontap_api.Tag;

import static edu.cmu.chimps.smart_calendar.SmartCalendarUtils.getEventList;
import static edu.cmu.chimps.smart_calendar.SmartCalendarUtils.getHtml;
import static edu.cmu.chimps.smart_calendar.SmartCalendarUtils.getTid;
import static edu.cmu.chimps.smart_calendar.SmartCalendarUtils.getTimeString;

import static edu.cmu.chimps.smart_calendar.SmartCalendarUtils.setListLocation;

public class SmartCalendarPlugin extends MessageOnTapPlugin {

    public static final String TAG = "SmartCalendar plugin";
    public static final int EVENT_NAME_ID = 3726;
    public static final int EVENT_TIME_ID = 1567;
    public static final int EVENT_LOCATION_ID = 9123;

    public static final String SEMANTIC_TEMPLATE_SCHEDULE_REQUEST = "schedule_request";
    public static final String SEMANTIC_TEMPLATE_TODO_LIST = "todo_list";
    public static final String SEMANTIC_TEMPLATE_MEETUP = "meetup";


    private HashMap<Long, Long> mTidPutTreeToGetTime = new HashMap<>();
    private HashMap<Long,Long> mTidPutTreeToGetLocation = new HashMap<>();
    private HashMap<Long,Long> mTidAddAction_ShowBubble = new HashMap<>();
    private HashMap<Long,Long> mTidAddAction = new HashMap<>();
    private HashMap<Long,Long> mTidShowHtml = new HashMap<>();
    private HashMap<Long,Long> mTidShowBubble = new HashMap<>();
    private HashMap<Long,ArrayList<Event>> mEventList = new HashMap<>();
    private HashMap<Long, ParseTree> mTree = new HashMap<>();
    private HashMap<Long, Long> mEventBeginTime = new HashMap<>();
    private HashMap<Long, Long> mEventEndTime = new HashMap<>();

    /**
     * Return the trigger criteria of this plug-in. This will be called when
     * MessageOnTap is started (when this plugin is already enabled) or when
     * this plugin is being enabled.
     *
     * @return PluginData containing the trigger
     */

    public void clearLists(Set<String> mMandatory, Set<String> mOptional){
        mMandatory.clear();
        mOptional.clear();
    }

    /**
     * Return the semantic templates of this plug-in. This will be called when
     * MessageOnTap is started (when this plugin is already enabled) or when
     * this plugin is being enabled.
     *
     * @return Set set of semantic templates
     */
    @Override
    protected Set<SemanticTemplate> semanticTemplates() {
        Set<SemanticTemplate> templates = new HashSet<>();

        /*
         * Semantic template I: incoming schedule request.
         */
        Set<Tag> tags = new HashSet<>();
        Set<String> reSet = new HashSet<>();
        reSet.add("free");
        reSet.add("available");
        tags.add(new Tag(ServiceAttributes.Internal.TAG_TIME,
                new HashSet<String>(), Tag.Type.MANDATORY));
        tags.add(new Tag("tag_availability", reSet, Tag.Type.MANDATORY));
        templates.add(new SemanticTemplate().name(SEMANTIC_TEMPLATE_SCHEDULE_REQUEST)
                .tags(tags)
                .direction(ParseTree.Direction.INCOMING));

        /*
         * Semantic template II: to-do list.
         */
        tags.clear();
        reSet.clear();
        reSet.add("I");
        tags.add(new Tag("tag_I",
                reSet, Tag.Type.MANDATORY));
        tags.add(new Tag(ServiceAttributes.Internal.TAG_TIME,
                new HashSet<String>(), Tag.Type.MANDATORY));
        templates.add(new SemanticTemplate().name(SEMANTIC_TEMPLATE_TODO_LIST)
                .tags(tags)
                .direction(ParseTree.Direction.OUTGOING));


        /*
         * Semantic template III: meet up.
         */
        tags.clear();
        reSet.clear();
        reSet.add("meet");
        reSet.add("see");
        reSet.add("hangout");
        tags.add(new Tag("tag_meet",
                reSet, Tag.Type.MANDATORY));
        tags.add(new Tag(ServiceAttributes.Internal.TAG_TIME,
                new HashSet<String>(), Tag.Type.MANDATORY));
        tags.add(new Tag(ServiceAttributes.Internal.TAG_LOCATION,
                new HashSet<String>(), Tag.Type.OPTIONAL));
        templates.add(new SemanticTemplate().name(SEMANTIC_TEMPLATE_MEETUP)
                .tags(tags));

        return templates;
    }

    @Override
    protected void initNewSession(long sid, HashMap<String, Object> params) throws Exception {
        Log.e(TAG, "Session created here!");
        Log.e("Calanderplugin:", JSONUtils.hashMapToString(params));

        if (params.get(ServiceAttributes.Internal.TRIGGER_SOURCE).equals(SEMANTIC_TEMPLATE_SCHEDULE_REQUEST)){

            mTree.put(sid,(ParseTree) JSONUtils.jsonToSimpleObject((String)params
                    .get(ServiceAttributes.Internal.PARSE_TREE),ParseTree.class));
            Log.e(TAG, "Tree is " + params.get(ServiceAttributes.Internal.PARSE_TREE));

            ParseTree tree = new ParseTree();
            ParseTree.Node timeNode = new ParseTree.Node();
            timeNode.setWord(getTimeString(params));
            Log.e(TAG,getTimeString(params));
            Set<String> timeTagSet = new HashSet<>();
            timeTagSet.add(ServiceAttributes.Graph.Event.TIME);
            timeNode.setTagList(timeTagSet);
            timeNode.setId(EVENT_TIME_ID);
            timeNode.setParentId(EVENT_NAME_ID);

            ParseTree.Node eventNode = new ParseTree.Node();
            Set<String> nameTagSet = new HashSet<>();
            nameTagSet.add(ServiceAttributes.Graph.Event.NAME);
            eventNode.setTagList(nameTagSet);
            eventNode.setId(EVENT_NAME_ID);
            eventNode.setParentId(-1);//root id

            Set<Integer> ChildrenId = new HashSet<>();
            ChildrenId.add(EVENT_TIME_ID);
            eventNode.setChildrenIds(ChildrenId);

            SparseArray<ParseTree.Node> array = new SparseArray<>();
            array.put(EVENT_TIME_ID,timeNode);
            array.put(EVENT_NAME_ID,eventNode);
            tree.setNodeList(array);
            tree.direction = ParseTree.Direction.UNKNOWN;
            mTree.put(sid,tree);

            Log.e(TAG, "Start to Send Tree to PMS");
            params.remove(ServiceAttributes.Internal.PARSE_TREE);
            params.put(ServiceAttributes.Internal.PARSE_TREE, JSONUtils.simpleObjectToJson(mTree.get(sid), ParseTree.class));
            Log.e(TAG, "Put Tree" + JSONUtils.simpleObjectToJson(mTree.get(sid), ParseTree.class));
            mTidPutTreeToGetTime.put(sid, createTask(sid, MethodConstants.GRAPH_TYPE,
                    MethodConstants.GRAPH_METHOD_RETRIEVE, params));
            Log.e(TAG, "Send Tree to PMS");
        }
        else{
            if (params.get(ServiceAttributes.Internal.CURRENT_MESSAGE_EMBEDDED_TIME).toString().isEmpty()){
//
                Log.e(TAG, "initNewSession: get messsage embeded time");
                ArrayList<ArrayList<Long>> messageTime = (ArrayList<ArrayList<Long>>)params.get(ServiceAttributes.Internal.CURRENT_MESSAGE_EMBEDDED_TIME);
                mEventBeginTime.put(sid,messageTime.get(0).get(0));
                mEventEndTime.put(sid,messageTime.get(0).get(1));
                params.put(ServiceAttributes.UI.BUBBLE_FIRST_LINE, "Add Calendar");
                params.put(ServiceAttributes.UI.BUBBLE_SECOND_LINE, mEventBeginTime + "-" + mEventEndTime);
                params.put(ServiceAttributes.UI.ICON_TYPE_STRING, getResources().getString(R.string.fa_calendar));
            }
            else{
                params.put(ServiceAttributes.UI.BUBBLE_FIRST_LINE, "Add Calendar");
                params.put(ServiceAttributes.UI.BUBBLE_SECOND_LINE, "");
                params.put(ServiceAttributes.UI.ICON_TYPE_STRING, getResources().getString(R.string.fa_calendar));
            }
            mTidAddAction_ShowBubble.put(sid, createTask(sid, MethodConstants.UI_TYPE,
                    MethodConstants.UI_METHOD_SHOW_BUBBLE, params));
        }



    }


    @Override
    protected void newTaskResponded(long sid, long tid, HashMap<String, Object> params) throws Exception {
        Log.e(TAG, "Got task response!");

        if (tid == getTid(mTidPutTreeToGetTime, sid)){

            try{
                mEventList.put(sid, getEventList(params));
                Log.e(TAG, "Event List=" + getEventList(params).toString());
                Log.e(TAG, "Got Task ID");

                // set location node
                SparseArray<ParseTree.Node> nodeList = mTree.get(sid).getNodeList();
                nodeList.remove(EVENT_NAME_ID);
                ParseTree.Node locationNode = new ParseTree.Node();
                locationNode.setId(EVENT_LOCATION_ID);
                locationNode.setParentId(-1);
                Set<Integer> setChildIds = new HashSet<>();
                setChildIds.add(EVENT_TIME_ID);
                locationNode.setChildrenIds(setChildIds);
                //set tag
                Set<String> set = new HashSet<>();
                set.add(ServiceAttributes.Graph.Place.NAME);
                locationNode.setTagList(set);
                nodeList.put(EVENT_LOCATION_ID, locationNode);
                //put and send tree
                mTree.get(sid).setNodeList(nodeList);
                Log.e(TAG, "mTree is : " + JSONUtils.simpleObjectToJson(mTree.get(sid), ParseTree.class));
                params.remove(ServiceAttributes.Internal.PARSE_TREE);
                params.put(ServiceAttributes.Internal.PARSE_TREE,JSONUtils.simpleObjectToJson(mTree.get(sid), ParseTree.class));
                mTidPutTreeToGetLocation.put(sid, createTask(sid, MethodConstants.GRAPH_TYPE,
                        MethodConstants.GRAPH_METHOD_RETRIEVE, params));
                Log.e(TAG,"PUT TREE TO GET LOCATION");

            }catch (Exception e){
                e.printStackTrace();
                endSession(sid);
            }
        } else if (tid == getTid(mTidPutTreeToGetLocation, sid)) {
            //getCardMessage and put it into params

            try {
                setListLocation(mEventList.get(sid), params);
                params.put(ServiceAttributes.UI.BUBBLE_FIRST_LINE, "Smart Calendar");
                params.put(ServiceAttributes.UI.BUBBLE_SECOND_LINE,"Show Events");
                mTidShowBubble.put(sid, createTask(sid, MethodConstants.UI_TYPE,
                        MethodConstants.UI_METHOD_SHOW_BUBBLE, params));
            } catch (Exception e) {
                e.printStackTrace();
                endSession(sid);
            }
        } else if (tid == getTid(mTidShowBubble, sid)){

            try {
                if (params.get(ServiceAttributes.UI.STATUS).equals(ServiceAttributes.UI.Status.CLICKED)){
                    params.put("html_string", getHtml(eventListSortByTime(mEventList.get(sid))));
                    mTidShowHtml.put(sid, createTask(sid, MethodConstants.UI_TYPE,
                            MethodConstants.UI_METHOD_LOAD_WEBVIEW, params));
                } else {
                    endSession(sid);
                }
            }catch (Exception e){
                e.printStackTrace();
                endSession(sid);
            }
        } else if (tid == getTid(mTidShowHtml, sid)){
            Log.e("Calanderplugin_mission:", mTidShowHtml.toString());
            Log.e(TAG, "Successfully Run Action");
            Log.e(TAG, "Ending session (triggerListShow)");
            endSession(sid);
            Log.e(TAG, "Session ended");
        }
        // Add Action
        if (tid == getTid(mTidAddAction_ShowBubble, sid)){

            Log.e("CalanderpluginAction:", mTidAddAction_ShowBubble.toString());
            if (ServiceAttributes.UI.Status.valueOf((String)params.get(ServiceAttributes.UI.STATUS))
                    == ServiceAttributes.UI.Status.CLICKED){       //BUBBLE_STATUS)==1
                params.put(ServiceAttributes.Action.CAL_EXTRA_TIME_START, mEventBeginTime);
                params.put(ServiceAttributes.Action.CAL_EXTRA_TIME_END, mEventEndTime);
                mTidAddAction.put(sid, createTask(sid, MethodConstants.ACTION_TYPE,
                        MethodConstants.ACTION_METHOD_CALENDAR_NEW, params));
            } else {
                endSession(sid);
            }

        } else if (tid == getTid(mTidAddAction, sid)){
            Log.e("CalanderpluginAction:", mTidAddAction.toString());
            Log.e(TAG, "Ending session (triggerListAdd)");
            endSession(sid);
            Log.e(TAG, "Session ended");
        }
        params.put("html_string", "<h1>123213123</h1>");
                    mTidShowHtml.put(sid, createTask(sid, MethodConstants.UI_TYPE,
                            MethodConstants.UI_METHOD_LOAD_WEBVIEW, params));


    }

    private ArrayList<Event> eventListSortByTime(ArrayList<Event> events){
        Collections.sort(events, new sortByTime());
        return events;

    }

    class sortByTime implements Comparator{
        public int compare(Object o1, Object o2){
            Event e1 = (Event) o1;
            Event e2 = (Event) o2;
            return e1.getBeginTime().compareTo(e2.getBeginTime());
        }
    }
    @Override
    protected void endSession(long sid) {
        mTidPutTreeToGetTime.remove(sid); mTidPutTreeToGetLocation.remove(sid); mTidShowBubble.remove(sid);
        mTidShowHtml.remove(sid); mTidAddAction.remove(sid); mTidAddAction_ShowBubble.remove(sid);
        mEventList.remove(sid); mTree.remove(sid);
        mEventBeginTime.remove(sid); mEventEndTime.remove(sid);
        super.endSession(sid);
    }
}


