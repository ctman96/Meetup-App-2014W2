package ca.ubc.cs.cpsc210.meetup.parser;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Cody on 20/03/2015.
 */
public class GeoPointParser {

    public List<GeoPoint> parse(String input) {
        List<GeoPoint> geopoints = new ArrayList<GeoPoint>();
        try{
            JSONObject obj = (JSONObject) new JSONTokener(input).nextValue();
            JSONObject rt = obj.getJSONObject("route");
            JSONArray legs = rt.getJSONArray("legs");
            for (int i = 0; i < legs.length(); i++){
                JSONArray mnv = legs.getJSONObject(i).getJSONArray("maneuvers");
                for (int n = 0; n < mnv.length(); n++){
                    JSONObject startPoint = mnv.getJSONObject(n).getJSONObject("startPoint");
                    double lat =  startPoint.getDouble("lat");
                    Log.d("GeoParser", String.valueOf(lat));
                    double lng =  startPoint.getDouble("lng");
                    Log.d("GeoParser", String.valueOf(lng));
                    geopoints.add(new GeoPoint(lat, lng));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return geopoints;
    }

}
