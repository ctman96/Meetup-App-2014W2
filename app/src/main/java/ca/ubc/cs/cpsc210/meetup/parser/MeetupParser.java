package ca.ubc.cs.cpsc210.meetup.parser;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import ca.ubc.cs.cpsc210.meetup.R;
import ca.ubc.cs.cpsc210.meetup.model.Section;
import ca.ubc.cs.cpsc210.meetup.model.StudentManager;
import ca.ubc.cs.cpsc210.meetup.util.SchedulePlot;

/**
 * Created by Cody on 21/03/2015.
 */
public class MeetupParser {
    SortedSet<Section> sections;
    String name;
    public int id;
    StudentManager manager;
    SchedulePlot schedulePlot;
    String dayOfWeek;
    String color = "#800080";
    int buildingIcon = R.drawable.ic_action_place_alt;

    public MeetupParser(StudentManager studentManager, String day){
        manager = studentManager;
        dayOfWeek = day;
        sections = new TreeSet<Section>();
        name = "";
        id = 0;
        String color = "#800080";
        schedulePlot = null;
    }


    public SchedulePlot parse(String input) {
        if (input.equals(""))
            return new SchedulePlot(new TreeSet<Section>(), "", color, buildingIcon);

        try{
            JSONObject obj = (JSONObject) new JSONTokener(input).nextValue();
            String firstName = obj.getString("FirstName");
            String lastName = obj.getString("LastName");
            name = firstName + " " + lastName;
            Log.d("MeetupParser", name);
            id = Integer.parseInt(obj.getString("Id"));
            Log.d("MeetupParser", obj.getString("Id"));
            manager.addStudent(lastName, firstName, id);
            JSONArray sects= obj.getJSONArray("Sections");
            for (int i = 0; i < sects.length(); i++){
                JSONObject o = sects.getJSONObject(i);
                String courseCode = o.getString("CourseName");
                Log.d("MeetupParser", courseCode);
                String coursenumb = o.getString("CourseNumber");
                Log.d("MeetupParser", coursenumb);
                int courseNumber = Integer.parseInt(o.getString("CourseNumber"));
                String sectionName = o.getString("SectionName");
                Log.d("MeetupParser", sectionName);
                manager.addSectionToSchedule(id, courseCode, courseNumber, sectionName);
            }
            sections = manager.get(id).getSchedule().getSections(dayOfWeek);
            schedulePlot = new SchedulePlot(sections, name, color, buildingIcon);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return schedulePlot;
    }
}
