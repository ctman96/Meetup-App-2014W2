package ca.ubc.cs.cpsc210.meetup.parser;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import ca.ubc.cs.cpsc210.meetup.model.EatingPlace;
import ca.ubc.cs.cpsc210.meetup.model.PlaceFactory;
import ca.ubc.cs.cpsc210.meetup.util.LatLon;

/**
 * Created by Cody on 21/03/2015.
 */
public class PlacesParser {
    private PlaceFactory placeFactory;
    private String name;
    private double lat;
    private double lng;
    private LatLon latlon;
    public int placesAdded;

    /**
     * Parse JSON from Foursquare output stored into a file
     * REQUIRES: input is a file with valid data
     * EFFECTS: parsed data is put into PlaceFactory
     */
    private final static String LOG_TAG = "PlacesParser";

    public void parse(String input) {
        placeFactory = PlaceFactory.getInstance();
        clearFields();
        placesAdded = 0;
        try{
            JSONObject obj = (JSONObject) new JSONTokener(input).nextValue();
            JSONObject rsp = obj.getJSONObject("response");
            JSONArray grp = rsp.getJSONArray("groups");
            JSONArray items = grp.getJSONObject(0).getJSONArray("items");
            for (int i = 0; i < items.length(); i++){
                clearFields();
                JSONObject venue = items.getJSONObject(i).getJSONObject("venue");

                name =venue.getString("name");
                Log.d(LOG_TAG, "name: " + name);
                lat = venue.getJSONObject("location").getDouble("lat");
                Log.d(LOG_TAG, "lat: " + lat);
                lng = venue.getJSONObject("location").getDouble("lng");
                Log.d(LOG_TAG, "lon: " + lng);
                latlon = new LatLon(lat, lng);
                EatingPlace place = new EatingPlace(name, latlon);

                JSONArray categories = venue.getJSONArray("categories");
                for (int n = 0; n < categories.length(); n++){
                    String category = categories.getJSONObject(n).getString("name");
                    place.addTag(category);
                }

                //***Description information***
                String rating = "Rating: ";
                String status = "Status: ";
                String price = "Pricing: ";
                String reviews = "Tips: ";

                //*rating*
                try {
                    rating = rating + venue.getString("rating");
                }catch (Exception e) {
                    rating = rating + "Not given";
                }
                Log.d("PlacesParser", rating);

                //*status*
                try {
                    if (venue.getJSONObject("hours").getBoolean("isOpen"))
                        status = status + "Open";
                    else
                        status = status + "Closed";
                }catch(Exception e){
                    Log.d("PlacesParser", "No status given");
                    status = status + "Not given";
                }
                Log.d("PlacesParser", status);

                //*price*
                try {
                    price = price + venue.getJSONObject("price").getString("message");
                }catch (Exception e){
                    Log.d("PlacesParser", "price not given");
                    price = price + "Not given";
                }
                Log.d("PlacesParser", price);

                String tags = place.getTags();
                Log.d("PlacesParser", tags);

                //*tips*
                try {
                    JSONArray tips = items.getJSONObject(i).getJSONArray("tips");
                    for (int z = 0; z < tips.length(); z++) {
                        String s = "\n" + " " + tips.getJSONObject(z).getString("text");
                        JSONObject user = tips.getJSONObject(z).getJSONObject("user");
                        s = s + "   -" + user.getString("firstName") + " " + user.getString("lastName");
                        reviews = reviews + s;
                    }
                }catch (Exception e){
                    Log.d("PlacesParser", "no reviews given");
                    reviews = reviews + "None available";
                }
                Log.d("PlacesParser", reviews);

                String msg = rating + "\n" +  status + "\n" + tags + "\n" + reviews;
                place.setDisplayText(msg);

                placeFactory.add(place);
                placesAdded = placesAdded + 1;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    private void clearFields(){
        name = null;
        lat = 0.00;
        lng = 0.00;
        latlon = null;
    }
}
