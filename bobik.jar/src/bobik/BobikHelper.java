package bobik;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
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
     * </pre>
     * If the incoming arrays have different length, then a smart selector logic applies.
     * Empty arrays are ignored. Non-empty arrays must all have the same length. If they don't,
     * then an exception is thrown.
     * @param x a hash of parallel arrays
     * @throws ArrayIndexOutOfBoundsException if arrays are not of equal length
     * @throws JSONException if the incoming object is not of the expected format
     * @return a transposed array
     */
    public static List<JSONObject> transpose(JSONObject x) throws ArrayIndexOutOfBoundsException, JSONException {
        List<JSONObject> results = null;
        // Do a first pass to check array lengths and determine the keys that we'll be working with
        List<String> keys = new LinkedList<String>();
        int common_array_length = 0;
        for (Iterator i=x.keys(); i.hasNext(); ) {
            String key = (String)i.next();
            JSONArray values = x.getJSONArray(key);
            if (values.length() == 0)
                continue;   // this key has no values => skip it
            if (common_array_length == 0)
                common_array_length = values.length();  // this is the first non-zero length => use it as standard
            else if (values.length() != common_array_length)
                throw new ArrayIndexOutOfBoundsException("Number of elements labelled '" + key
                        + "' (" + values.length() + ") does not match expected length of " + common_array_length);
            keys.add(key);
        }
        // Fill array with empty container objects
        if (results == null) {
            results = new ArrayList<JSONObject>(common_array_length);
            for (int z=0; z<common_array_length; z++)
                results.add(new JSONObject());
        }
        // Now, populate the array with real results, inserting null when no data is available upon transposition
        for (Iterator i=keys.iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            JSONArray values = x.getJSONArray(key);
            for (int z=0; z<values.length(); z++) {
                Object val = null;
                try {
                    val = values.get(z);
                } catch (JSONException e) {
                }
                results.get(z).put(key, val);
            }
        }
        return results;

    }
}
