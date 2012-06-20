package bobik;

import org.json.JSONObject;

import java.util.concurrent.Future;

/**
 * Provides tracking and control capabilities for Bobik jobs.
 * Refrain from using this class unless you want direct control over monitoring the job's progress, waiting
 * or aborting it. Most calls results in http calls to Bobik.
 * At the same time, there is an asynchronous monitor working that will call your JobListener.
 * Though implemented similarly, that callback is made in accordance with
 * estimated job completion time and is generally better optimized for user experience..
 */
public interface Job extends Future<JSONObject> {
    /**
     * Returns a value between 0 and 1 indicating the progress made on this job.
     * @return progress
     */
    public float getProgress() throws BobikException;

    /**
     * @return job_id
     */
    public String id();

    /**
     * Blocks until this job completes
     * @throws BobikException
     */
    public void waitForCompletion() throws BobikException;

}
