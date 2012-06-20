package bobik;

import org.json.JSONObject;

import java.util.Collection;

/**
 * Contains async callbacks for Bobik jobs
 */
public abstract class JobListener {

    /**
     * Job being monitored
     */
    protected Job job;

    /**
     * Called when the job is started
     * @param job
     */
    public final void init(Job job) {
        this.job = job;
    }
    /**
     * Called when the job is finished and data is scraped
     * @param scraped_data
     */
    public abstract void onSuccess(JSONObject scraped_data);

    /**
     * Called whenever some progress is made.
     * Unless overridden, simply prints progress to System.out
     * @param currentProgress a value between 0 and 1 (inclusive)
     */
    public abstract void onProgress(float currentProgress);

    /**
     * Called whenever there is an error during job execution.
     * If a successful job contains some errors, this function will be called prior
     * to <i>onSuccess</i>.
     * @param errors
     */
    public abstract void onErrors(Collection<String> errors);
}
