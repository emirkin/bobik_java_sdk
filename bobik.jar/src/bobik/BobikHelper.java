package bobik;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A collection of recurring and useful utility functions
 * @author Eugene Mirkin
 */
public class BobikHelper {

    /**
     * Takes a hash of parallel arrays
     * and turns it into a list of hashes, each having all keys from the original hash.
     * <br><br>
     * <b>Example:</b>
     * <pre>
     * {@code
     *  {'title':['A', 'B', 'C'], 'price':[4,2,5]}
     *      becomes
        [{'price':4, 'title':'A'} , {'price':2, 'title':'B'}, {'price':5, 'title':'C'}]
     * }
     * @param x a hash of parallel arrays
     * @throws ArrayIndexOutOfBoundsException if arrays are not of equal length
     * @throws JSONException if the incoming object is not of the expected format
     * @return a transposed array
     */
    public static List<JSONObject> transpose(JSONObject x) throws ArrayIndexOutOfBoundsException, JSONException {
        List<JSONObject> results = null;
        for (Iterator i=x.keys(); i.hasNext(); ) {
            String key = (String)i.next();
            JSONArray values = x.getJSONArray(key);
            // Fill array with empty container objects if this is the first pass
            if (results == null) {
                results = new ArrayList<JSONObject>(values.length());
                for (int z=0; z<values.length(); z++)
                    results.add(new JSONObject());
            }
            for (int z=0; z<values.length(); z++)
                results.get(z).put(key, values.get(z));
        }
        return results;

    }
}
