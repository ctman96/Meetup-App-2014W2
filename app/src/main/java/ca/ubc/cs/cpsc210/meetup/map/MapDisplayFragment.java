package ca.ubc.cs.cpsc210.meetup.map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.osmdroid.DefaultResourceProxyImpl;
import org.osmdroid.ResourceProxy;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.OverlayManager;
import org.osmdroid.views.overlay.PathOverlay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ca.ubc.cs.cpsc210.meetup.R;
import ca.ubc.cs.cpsc210.meetup.model.Building;
import ca.ubc.cs.cpsc210.meetup.model.Course;
import ca.ubc.cs.cpsc210.meetup.model.CourseFactory;
import ca.ubc.cs.cpsc210.meetup.model.Place;
import ca.ubc.cs.cpsc210.meetup.model.PlaceFactory;
import ca.ubc.cs.cpsc210.meetup.model.Schedule;
import ca.ubc.cs.cpsc210.meetup.model.Section;
import ca.ubc.cs.cpsc210.meetup.model.Student;
import ca.ubc.cs.cpsc210.meetup.model.StudentManager;
import ca.ubc.cs.cpsc210.meetup.parser.GeoPointParser;
import ca.ubc.cs.cpsc210.meetup.parser.MeetupParser;
import ca.ubc.cs.cpsc210.meetup.parser.PlacesParser;
import ca.ubc.cs.cpsc210.meetup.util.LatLon;
import ca.ubc.cs.cpsc210.meetup.util.SchedulePlot;

/**
 * Fragment holding the map in the UI.
 */
public class MapDisplayFragment extends Fragment {

    /**
     * Log tag for LogCat messages
     */
    private final static String LOG_TAG = "MapDisplayFragment";

    /**
     * Preference manager to access user preferences
     */
    private SharedPreferences sharedPreferences;

    /**
     * String to know whether we are dealing with MWF or TR schedule.
     * You will need to update this string based on the settings dialog at appropriate
     * points in time. See the project page for details on how to access
     * the value of a setting.
     */
    private String activeDay = "MWF";

    /**
     * A central location in campus that might be handy.
     */
    private final static GeoPoint UBC_MARTHA_PIPER_FOUNTAIN = new GeoPoint(49.264865,
            -123.252782);
    private final static LatLon UBC_MARTHA_PIPER_FOUNTAIN_LATLON = new LatLon(49.264865,
            -123.252782);
    /**
     * Meetup Service URL
     * CPSC 210 Students: Complete the string.
     */
    private final String getStudentURL = "";

    /**
     * FourSquare URLs. You must complete the client_id and client_secret with values
     * you sign up for.
     */
    private static String FOUR_SQUARE_URL = "https://api.foursquare.com/v2/venues/explore";
    private static String FOUR_SQUARE_CLIENT_ID = "2DH0W3ORF3WCOVO1U2KTEC5FQJFZLKN5BRP1PYJE4BV3RQEE";
    private static String FOUR_SQUARE_CLIENT_SECRET = "FZHC35BRWK21NZG2FR4GQCZDULXZPCNPVFCZBUNFQQS5UQPF";


    /**
     * Overlays for displaying my schedules, buildings, etc.
     */
    private List<PathOverlay> scheduleOverlay;
    private ItemizedIconOverlay<OverlayItem> buildingOverlay;
    private OverlayItem selectedBuildingOnMap;

    /**
     * View that shows the map
     */
    private MapView mapView;

    /**
     * Access to domain model objects. Only store "me" in the studentManager for
     * the base project (i.e., unless you are doing bonus work).
     */
    private StudentManager studentManager;
    private Student randomStudent = null;
    private Student randomStudent2 = null;
    private Student randomStudent3 = null;
    private Student me = null;
    private static int ME_ID = 35833145;

    /**
     * Map controller for zooming in/out, centering
     */
    private IMapController mapController;

    // ******************** Android methods for starting, resuming, ...

    // You should not need to touch this method
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        scheduleOverlay = new ArrayList<PathOverlay>();

        // You need to setup the courses for the app to know about. Ideally
        // we would access a web service like the UBC student information system
        // but that is not currently possible
        initializeCourses();

        // Initialize the data for the "me" schedule. Note that this will be
        // hard-coded for now
        initializeMySchedule();

        // You are going to need an overlay to draw buildings and locations on the map
        buildingOverlay = createBuildingOverlay();
    }

    // You should not need to touch this method
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK)
            return;
    }

    // You should not need to touch this method
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (mapView == null) {
            mapView = new MapView(getActivity(), null);

            mapView.setTileSource(TileSourceFactory.MAPNIK);
            mapView.setClickable(true);
            mapView.setBuiltInZoomControls(true);
            mapView.setMultiTouchControls(true);

            mapController = mapView.getController();
            mapController.setZoom(mapView.getMaxZoomLevel() - 2);
            mapController.setCenter(UBC_MARTHA_PIPER_FOUNTAIN);
        }

        return mapView;
    }

    // You should not need to touch this method
    @Override
    public void onDestroyView() {
        Log.d(LOG_TAG, "onDestroyView");
        ((ViewGroup) mapView.getParent()).removeView(mapView);
        super.onDestroyView();
    }

    // You should not need to touch this method
    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "onDestroy");
        super.onDestroy();
    }

    // You should not need to touch this method
    @Override
    public void onResume() {
        Log.d(LOG_TAG, "onResume");
        super.onResume();
    }

    // You should not need to touch this method
    @Override
    public void onPause() {
        Log.d(LOG_TAG, "onPause");
        super.onPause();
    }

    /**
     * Save map's zoom level and centre. You should not need to
     * touch this method
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(LOG_TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);

        if (mapView != null) {
            outState.putInt("zoomLevel", mapView.getZoomLevel());
            IGeoPoint cntr = mapView.getMapCenter();
            outState.putInt("latE6", cntr.getLatitudeE6());
            outState.putInt("lonE6", cntr.getLongitudeE6());
            Log.i("MapSave", "Zoom: " + mapView.getZoomLevel());
        }
    }

    // ****************** App Functionality

    /**
     * Show my schedule on the map. Every time "me"'s schedule shows, the map
     * should be cleared of all existing schedules, buildings, meetup locations, etc.
     */
    public void showMySchedule() {
        clearSchedules();
        String dayOfWeek = sharedPreferences.getString("dayOfWeek", "MWF");
        Schedule mySchedule = me.getSchedule();
        SortedSet<Section> sectionsOnDay = mySchedule.getSections(dayOfWeek);
        String name = me.getFirstName() + " " + me.getLastName();
        SchedulePlot mySchedulePlot = new SchedulePlot(sectionsOnDay, name, "#0099ff", R.drawable.ic_action_place);
        //Toast.makeText(getActivity(), "TESTING TOAST", Toast.LENGTH_SHORT).show();
        //createSimpleDialog("testing dialog").show();

        new GetRoutingForSchedule().execute(mySchedulePlot);
        plotBuildings(mySchedulePlot);
    }

    /**
     * Retrieve a random student's schedule from the Meetup web service and
     * plot a route for the schedule on the map. The plot should be for
     * the given day of the week as determined when "me"'s schedule
     * was plotted.
     */
    public void showRandomStudentsSchedule() {
        if(randomStudent == null) {
            new GetRandomSchedule().execute();
        }else
            createSimpleDialog("Schedule already loaded, please clear and try again").show();
    }

    /**
     * Clear all schedules on the map
     */
    public void clearSchedules() {
        randomStudent = null;
        OverlayManager om = mapView.getOverlayManager();
        om.clear();
        scheduleOverlay.clear();
        buildingOverlay.removeAllItems();
        om.addAll(scheduleOverlay);
        om.add(buildingOverlay);
        mapView.invalidate();
    }

    /**
     * Find all possible locations at which "me" and random student could meet
     * up for the set day of the week and the set time to meet and the set
     * distance either "me" or random is willing to travel to meet.
     * A meetup is only possible if both "me" and random are free at the
     * time specified in the settings and each of us must have at least an hour
     * (>= 60 minutes) free. You should display dialog boxes if there are
     * conditions under which no meetup can happen (e.g., me or random is
     * in class at the specified time)
     */
    public void findMeetupPlace() {
        if (randomStudent != null) {
            Schedule mySchedule = me.getSchedule();
            Schedule randSchedule = randomStudent.getSchedule();
            String dayOfWeek = sharedPreferences.getString("dayOfWeek", "MWF");
            String timeOfDay = sharedPreferences.getString("timeOfDay", "12");
            String radius = sharedPreferences.getString("placeDistance", "closest_stop_me");
            Log.d(LOG_TAG, dayOfWeek + " " + timeOfDay + " " + radius);

            if (mySchedule.breakAtTime(dayOfWeek, timeOfDay) && randSchedule.breakAtTime(dayOfWeek, timeOfDay)) {
                Building myBuilding = mySchedule.whereAmI(dayOfWeek, timeOfDay);
                Building randBuilding = randSchedule.whereAmI(dayOfWeek, timeOfDay);
                LatLon myBreak = null;
                LatLon randBreak = null;

                if (myBuilding == null)
                    myBreak = UBC_MARTHA_PIPER_FOUNTAIN_LATLON;
                else
                    myBreak = myBuilding.getLatLon();
                if (randBuilding == null)
                    randBreak = UBC_MARTHA_PIPER_FOUNTAIN_LATLON;
                else
                    randBreak = randBuilding.getLatLon();
                Log.d(LOG_TAG, myBreak.toString() + "|" + randBreak.toString());

                //LatLon midpoint = LatLon.midpoint(myBreak, randBreak);
                //Log.d(LOG_TAG, midpoint.toString());

                Set<Place> myplaces = PlaceFactory.getInstance().findPlacesWithinDistance(myBreak, Integer.parseInt(radius));
                Log.d(LOG_TAG, "places within range of me: " + String.valueOf(myplaces.size()));
                Set<Place> randplaces = PlaceFactory.getInstance().findPlacesWithinDistance(randBreak, Integer.parseInt(radius));
                Log.d(LOG_TAG, "places within range of random: " + String.valueOf(randplaces.size()));
                Set<Place> places = intersection(myplaces, randplaces);
                Log.d(LOG_TAG, String.valueOf(places.size()));
                List<Place> list = new ArrayList(places);
                if (list.isEmpty())
                    createSimpleDialog("No places within radius, or places not initialized.").show();
                else
                    alertSimpleListView(list, radius);
            } else {
                Log.d(LOG_TAG, "no breaks at " + timeOfDay);
                String msg = "You and " + randomStudent.getFirstName() + " " + randomStudent.getLastName() + " are unable to meetup at the specified time";
                createSimpleDialog(msg).show();
            }
        }else
            createSimpleDialog("No student has been loaded. Please get press 'get a random schedule' and try again").show();
        
    }
    private Set<Place> intersection(Set<Place> set1, Set<Place> set2){
        Set<Place> places = new HashSet<Place>();
        for (Place p1 : set1){
            for (Place p2 : set2){
                if (p1.equals(p2)){
                    places.add(p1);
                }
            }
        }
        return places;
    }


    public void alertSimpleListView(List<Place> list, String radius) {
        List<String> names = new ArrayList<String>();
        for (Place p : list){
            names.add(p.getName());
        }
        final CharSequence[] items = names.toArray(new CharSequence[names.size()]);
        final List<Place> places = list;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Locations within " + radius + "m");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Place p = places.get(item);

                plotABuilding(p, p.getName(), p.getDisplayText(), R.drawable.ic_action_place_meetup);
                dialog.dismiss();

            }
        }).show();
    }



    /**
     * Initialize the PlaceFactory with information from FourSquare
     */
    public void initializePlaces() {
        // CPSC 210 Students: You should not need to touch this method, but
        // you will have to implement GetPlaces below.
        new GetPlaces().execute();
    }


    /**
     * Plot all buildings referred to in the given information about plotting
     * a schedule.
     * @param schedulePlot All information about the schedule and route to plot.
     */
    private void plotBuildings(SchedulePlot schedulePlot) {

        // CPSC 210 Students: You will need to ensure the buildingOverlay is in
        // the overlayManager. The following code achieves this. You should not likely
        // need to touch it
        OverlayManager om = mapView.getOverlayManager();
        om.add(buildingOverlay);

        // CPSC 210 Students: Complete this method by plotting each building in the
        // schedulePlot with an appropriate message displayed
        SortedSet<Section> sections = schedulePlot.getSections();
        for (Section s : sections){
            Building b = s.getBuilding();
            String msg = s.getCourse().getCode() + s.getCourse().getNumber() + " in " + b.getName() + " from " + s.getCourseTime().getStartTime() + " to " + s.getCourseTime().getEndTime();
            plotABuilding(b, schedulePlot.getName(), msg, schedulePlot.getIcon());
        }
   



    }

    /**
     * Plot a building onto the map
     * @param building The building to put on the map
     * @param title The title to put in the dialog box when the building is tapped on the map
     * @param msg The message to display when the building is tapped
     * @param drawableToUse The icon to use. Can be R.drawable.ic_action_place (or any icon in the res/drawable directory)
     */
    private void plotABuilding(Building building, String title, String msg, int drawableToUse) {
        // CPSC 210 Students: You should not need to touch this method
        OverlayItem buildingItem = new OverlayItem(title, msg,
                new GeoPoint(building.getLatLon().getLatitude(), building.getLatLon().getLongitude()));

        //Create new marker
        Drawable icon = this.getResources().getDrawable(drawableToUse);

        //Set the bounding for the drawable
        icon.setBounds(
                0 - icon.getIntrinsicWidth() / 2, 0 - icon.getIntrinsicHeight(),
                icon.getIntrinsicWidth() / 2, 0);

        //Set the new marker to the overlay
        buildingItem.setMarker(icon);
        buildingOverlay.addItem(buildingItem);
    }
    private void plotABuilding(Place building, String title, String msg, int drawableToUse) {
        // CPSC 210 Students: You should not need to touch this method
        OverlayItem buildingItem = new OverlayItem(title, msg,
                new GeoPoint(building.getLatLon().getLatitude(), building.getLatLon().getLongitude()));

        //Create new marker
        Drawable icon = this.getResources().getDrawable(drawableToUse);

        //Set the bounding for the drawable
        icon.setBounds(
                0 - icon.getIntrinsicWidth() / 2, 0 - icon.getIntrinsicHeight(),
                icon.getIntrinsicWidth() / 2, 0);

        //Set the new marker to the overlay
        buildingItem.setMarker(icon);
        buildingOverlay.addItem(buildingItem);
    }



    /**
     * Initialize your schedule by coding it directly in. This is the schedule
     * that will appear on the map when you select "Show My Schedule".
     */
    private void initializeMySchedule() {
        // CPSC 210 Students; Implement this method
        studentManager = new StudentManager();
        studentManager.addStudent("Newman", "Cody", ME_ID);
        me = studentManager.get(ME_ID);

        studentManager.addSectionToSchedule(ME_ID, "PHYS", 101, "202");
        studentManager.addSectionToSchedule(ME_ID, "SCIE", 113, "212");
        studentManager.addSectionToSchedule(ME_ID, "CPSC", 210, "201");
        studentManager.addSectionToSchedule(ME_ID, "MATH", 221, "202");


    }

    /**
     * Helper to create simple alert dialog to display message
     *
     * @param msg message to display in alert dialog
     * @return the alert dialog
     */
    private AlertDialog createSimpleDialog(String msg) {
        // CPSC 210 Students; You should not need to modify this method
        AlertDialog.Builder dialogBldr = new AlertDialog.Builder(getActivity());
        dialogBldr.setMessage(msg);
        dialogBldr.setNeutralButton(R.string.ok, null);

        return dialogBldr.create();
    }

    /**
     * Create the overlay used for buildings. CPSC 210 students, you should not need to
     * touch this method.
     * @return An overlay
     */
    private ItemizedIconOverlay<OverlayItem> createBuildingOverlay() {
        ResourceProxy rp = new DefaultResourceProxyImpl(getActivity());

        ItemizedIconOverlay.OnItemGestureListener<OverlayItem> gestureListener =
                new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {

            /**
             * Display building description in dialog box when user taps stop.
             *
             * @param index
             *            index of item tapped
             * @param oi
             *            the OverlayItem that was tapped
             * @return true to indicate that tap event has been handled
             */
            @Override
            public boolean onItemSingleTapUp(int index, OverlayItem oi) {

                new AlertDialog.Builder(getActivity())
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                if (selectedBuildingOnMap != null) {
                                    mapView.invalidate();
                                }
                            }
                        }).setTitle(oi.getTitle()).setMessage(oi.getSnippet())
                        .show();

                selectedBuildingOnMap = oi;
                mapView.invalidate();
                return true;
            }

            @Override
            public boolean onItemLongPress(int index, OverlayItem oi) {
                // do nothing
                return false;
            }
        };

        return new ItemizedIconOverlay<OverlayItem>(
                new ArrayList<OverlayItem>(), getResources().getDrawable(
                R.drawable.ic_action_place), gestureListener, rp);
    }


    /**
     * Create overlay with a specific color
     * @param colour A string with a hex colour value
     */
    private PathOverlay createPathOverlay(String colour) {
        // CPSC 210 Students, you should not need to touch this method
        PathOverlay po = new PathOverlay(Color.parseColor(colour),
                getActivity());
        Paint pathPaint = new Paint();
        pathPaint.setColor(Color.parseColor(colour));
        pathPaint.setStrokeWidth(4.0f);
        pathPaint.setStyle(Paint.Style.STROKE);
        po.setPaint(pathPaint);
        return po;
    }

   // *********************** Asynchronous tasks

    /**
     * This asynchronous task is responsible for contacting the Meetup web service
     * for the schedule of a random student. The task must plot the retrieved
     * student's route for the schedule on the map in a different colour than the "me" schedule
     * or must display a dialog box that a schedule was not retrieved.
     */
    private class GetRandomSchedule extends AsyncTask<Void, Void, SchedulePlot> {

        // Some overview explanation of asynchronous tasks is on the project web page.

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected SchedulePlot doInBackground(Void... params) {

            String routeCall = "";
            String dayOfWeek = sharedPreferences.getString("dayOfWeek", "MWF");
            try {
                routeCall = makeRoutingCall("http://kramer.nss.cs.ubc.ca:8081/getStudent/");
                Log.d(LOG_TAG, routeCall);
            } catch (MalformedURLException e) {
                Log.d(LOG_TAG, "MalformedURLException on Meetup call");
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Unable to connect to webservice", Toast.LENGTH_SHORT).show();
            }
            MeetupParser meetupParser = new MeetupParser(studentManager, dayOfWeek);
            Log.d(LOG_TAG, "MeetupParser created, parsing call...");
            SchedulePlot randSchedulePlot = meetupParser.parse(routeCall);
            Log.d(LOG_TAG, "Parsing completed");
            randomStudent = studentManager.get(meetupParser.id);

            if (routeCall == "" || randSchedulePlot.getName() == ""){
                return null;
            }else{
                return randSchedulePlot;
            }
        }

        private String makeRoutingCall(String httpRequest) throws MalformedURLException, IOException {
            URL url = new URL(httpRequest);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            InputStream in = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String returnString = br.readLine();
            client.disconnect();
            return returnString;
        }

        @Override
        protected void onPostExecute(SchedulePlot schedulePlot) {

            if (schedulePlot == null){
                Toast.makeText(getActivity(), "No Student retrieved", Toast.LENGTH_SHORT).show();
                //createSimpleDialog("No Student retrieved").show;
            }else {
                new GetRoutingForSchedule().execute(schedulePlot);
                plotBuildings(schedulePlot);
            }

        }
    }

    /**
     * This asynchronous task is responsible for contacting the MapQuest web service
     * to retrieve a route between the buildings on the schedule and for plotting any
     * determined route on the map.
     */
    private class GetRoutingForSchedule extends AsyncTask<SchedulePlot, Void, SchedulePlot> {

        @Override
        protected void onPreExecute() {
            Log.d(LOG_TAG, "Getting Routing For Schedule...");
        }

        @Override
        protected SchedulePlot doInBackground(SchedulePlot... params) {

            // The params[0] element contains the schedulePlot object
            SchedulePlot scheduleToPlot = params[0];
            List<GeoPoint> route = null;
            SortedSet<Section> sections = scheduleToPlot.getSections();
            if (sections != null && sections.size() > 1) {
                Iterator<Section> itr = sections.iterator();
                Section sec = itr.next();
                LatLon latLon = sec.getBuilding().getLatLon();
                String s = "&from=" + latLon.getLatitude() + "," + latLon.getLongitude();
                while (itr.hasNext()) {
                    sec = itr.next();
                    latLon = sec.getBuilding().getLatLon();
                    s = s + "&to=" + latLon.getLatitude() + "," + latLon.getLongitude();
                }
                String html = "http://open.mapquestapi.com/directions/v2/route?key=Fmjtd%7Cluu82luzng%2C2x%3Do5-948s5a&routeType=pedestrian";
                Log.d(LOG_TAG, html + s);
                String routeCall = "";
                try {
                    routeCall = makeRoutingCall(html + s);
                    Log.d(LOG_TAG, routeCall);
                } catch (MalformedURLException e) {
                    Log.d(LOG_TAG, "MalformedURLException on Mapquest call");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                GeoPointParser parser = new GeoPointParser();
                Log.d(LOG_TAG, "Calling GeoPoint Parser...");
                route = parser.parse(routeCall);
                Log.d(LOG_TAG, "GeoPoints parsed");

                scheduleToPlot.setRoute(route);
            } else
            Log.d(LOG_TAG, "Sections for day was empty");

            return scheduleToPlot;
        }


        /**
         * An example helper method to call a web service
         */
        private String makeRoutingCall(String httpRequest) throws MalformedURLException, IOException {
            URL url = new URL(httpRequest);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            InputStream in = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String returnString = br.readLine();
            client.disconnect();
            return returnString;
        }

        @Override
        protected void onPostExecute(SchedulePlot schedulePlot) {

            // CPSC 210 Students: This method should plot the route onto the map
            // with the given line colour specified in schedulePlot. If there is
            // no route to plot, a dialog box should be displayed.

            // To actually make something show on the map, you can use overlays.
            Log.d(LOG_TAG, "Plotting route...");
            if (schedulePlot.getRoute() != null)
                Log.d(LOG_TAG, "Route size: " + schedulePlot.getRoute().size());
            else
                Log.d(LOG_TAG, "Route is null");
            if (schedulePlot.getRoute() != null && schedulePlot.getRoute().size() > 1) {
                PathOverlay po = createPathOverlay(schedulePlot.getColourOfLine());
                List<GeoPoint> points = schedulePlot.getRoute();
                for (GeoPoint gp : points) {
                     po.addPoint(gp);
                }
                scheduleOverlay.add(po);
            }else {
                if (schedulePlot.getSections().size() < 1)
                    Toast.makeText(getActivity(), schedulePlot.getName() + " has no courses today", Toast.LENGTH_SHORT).show();
            }
            OverlayManager om = mapView.getOverlayManager();
            om.addAll(scheduleOverlay);
            mapView.invalidate(); // cause map to redraw
    
        }

    }

    /**
     * This asynchronous task is responsible for contacting the FourSquare web service
     * to retrieve all places around UBC that have to do with food. It should load
     * any determined places into PlaceFactory and then display a dialog box of how it did
     */
    private class GetPlaces extends AsyncTask<Void, Void, String> {

        protected String doInBackground(Void... params) {
            double latitude = UBC_MARTHA_PIPER_FOUNTAIN.getLatitude();
            double longitude = UBC_MARTHA_PIPER_FOUNTAIN.getLongitude();
            String latlon = String.valueOf(latitude) + "," + String.valueOf(longitude);
            String routeCall = "";
            String html = FOUR_SQUARE_URL + "?client_id=" + FOUR_SQUARE_CLIENT_ID + "&client_secret=" + FOUR_SQUARE_CLIENT_SECRET + "&v=20150321" + "&ll=" + latlon + "&section=food" + "&radius=2500";
            Log.d (LOG_TAG, html);
            try{
                routeCall = makeRoutingCall(html);
                Log.d(LOG_TAG, routeCall);
            }catch(MalformedURLException e){
                Log.d(LOG_TAG, "MalformedURLException on FourSquare call");
            }catch (IOException e){
                e.printStackTrace();
            }

       
            return routeCall;
        }

        private String makeRoutingCall(String httpRequest) throws MalformedURLException, IOException {
            URL url = new URL(httpRequest);
            HttpURLConnection client = (HttpURLConnection) url.openConnection();
            InputStream in = client.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String returnString = br.readLine();
            client.disconnect();
            return returnString;
        }

        protected void onPostExecute(String jSONOfPlaces) {

            PlacesParser placesParser = new PlacesParser();
            placesParser.parse(jSONOfPlaces);
            Toast.makeText(getActivity(), "Loaded " + String.valueOf(placesParser.placesAdded) + " places nearby", Toast.LENGTH_SHORT).show();
            //createSimpleDialog("Found " + String.valueOf(placesParser.placesAdded) + " new places").show();
      
        }
    }

    /**
     * Initialize the CourseFactory with some courses.
     */
    private void initializeCourses() {
        // CPSC 210 Students: You can change this data if you desire.
        CourseFactory courseFactory = CourseFactory.getInstance();

        Building dmpBuilding = new Building("DMP", new LatLon(49.261474, -123.248060));

        Course cpsc210 = courseFactory.getCourse("CPSC", 210);
        Section aSection = new Section("202", "MWF", "12:00", "12:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);
        aSection = new Section("201", "MWF", "16:00", "16:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);
        aSection = new Section("BCS", "MWF", "12:00", "12:50", dmpBuilding);
        cpsc210.addSection(aSection);
        aSection.setCourse(cpsc210);

        Course engl222 = courseFactory.getCourse("ENGL", 222);
        aSection = new Section("007", "MWF", "14:00", "14:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        engl222.addSection(aSection);
        aSection.setCourse(engl222);

        Course scie220 = courseFactory.getCourse("SCIE", 220);
        aSection = new Section("200", "MWF", "15:00", "15:50", new Building("Swing", new LatLon(49.262786, -123.255044)));
        scie220.addSection(aSection);
        aSection.setCourse(scie220);

        Course math200 = courseFactory.getCourse("MATH", 200);
        aSection = new Section("201", "MWF", "09:00", "09:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        math200.addSection(aSection);
        aSection.setCourse(math200);

        Course math101 = courseFactory.getCourse("MATH", 101);                                                                  // added after
        aSection = new Section("211", "MWF", "15:00", "15:50", new Building("Woodward", new LatLon(49.264704,-123.247536)));
        math101.addSection(aSection);
        aSection.setCourse(math101);

        Course fren102 = courseFactory.getCourse("FREN", 102);
        aSection = new Section("202", "MWF", "11:00", "11:50", new Building("Barber", new LatLon(49.267442,-123.252471)));
        fren102.addSection(aSection);
        aSection.setCourse(fren102);

        Course japn103 = courseFactory.getCourse("JAPN", 103);
        aSection = new Section("002", "MWF", "10:00", "11:50", new Building("Buchanan", new LatLon(49.269258, -123.254784)));
        japn103.addSection(aSection);
        aSection.setCourse(japn103);

        Course scie113 = courseFactory.getCourse("SCIE", 113);
        aSection = new Section("213", "MWF", "13:00", "13:50", new Building("Swing", new LatLon(49.262786, -123.255044)));
        scie113.addSection(aSection);
        aSection.setCourse(scie113);
        aSection = new Section("212", "MWF", "12:00", "12:50", new Building("Klinck", new LatLon(49.266112, -123.254776))); //added after
        scie113.addSection(aSection);
        aSection.setCourse(scie113);

        Course micb308 = courseFactory.getCourse("MICB", 308);
        aSection = new Section("201", "MWF", "12:00", "12:50", new Building("Woodward", new LatLon(49.264704,-123.247536)));
        micb308.addSection(aSection);
        aSection.setCourse(micb308);

        Course math221 = courseFactory.getCourse("MATH", 221);
        aSection = new Section("202", "TR", "11:00", "12:20", new Building("Klinck", new LatLon(49.266112, -123.254776)));
        math221.addSection(aSection);
        aSection.setCourse(math221);

        Course phys203 = courseFactory.getCourse("PHYS", 203);
        aSection = new Section("201", "TR", "09:30", "10:50", new Building("Hennings", new LatLon(49.266400,-123.252047)));
        phys203.addSection(aSection);
        aSection.setCourse(phys203);

        Course phys101 = courseFactory.getCourse("PHYS", 101);                                                               //added after
        aSection = new Section("202", "MWF", "11:00", "11:50", new Building("HEBB", new LatLon(49.266315, -123.251346)));
        phys101.addSection(aSection);
        aSection.setCourse(phys101);

        Course crwr209 = courseFactory.getCourse("CRWR", 209);
        aSection = new Section("002", "TR", "12:30", "13:50", new Building("Geography", new LatLon(49.266039,-123.256129)));
        crwr209.addSection(aSection);
        aSection.setCourse(crwr209);

        Course fnh330 = courseFactory.getCourse("FNH", 330);
        aSection = new Section("002", "TR", "15:00", "16:20", new Building("MacMillian", new LatLon(49.261167,-123.251157)));
        fnh330.addSection(aSection);
        aSection.setCourse(fnh330);

        Course cpsc499 = courseFactory.getCourse("CPSC", 430);
        aSection = new Section("201", "TR", "16:20", "17:50", new Building("Liu", new LatLon(49.267632,-123.259334)));
        cpsc499.addSection(aSection);
        aSection.setCourse(cpsc499);

        Course chem250 = courseFactory.getCourse("CHEM", 250);
        aSection = new Section("203", "TR", "10:00", "11:20", new Building("Klinck", new LatLon(49.266112, -123.254776)));
        chem250.addSection(aSection);
        aSection.setCourse(chem250);

        Course eosc222 = courseFactory.getCourse("EOSC", 222);
        aSection = new Section("200", "TR", "11:00", "12:20", new Building("ESB", new LatLon(49.262866, -123.25323)));
        eosc222.addSection(aSection);
        aSection.setCourse(eosc222);

        Course biol201 = courseFactory.getCourse("BIOL", 201);
        aSection = new Section("201", "TR", "14:00", "15:20", new Building("BioSci", new LatLon(49.263920, -123.251552)));
        biol201.addSection(aSection);
        aSection.setCourse(biol201);
    }

}
