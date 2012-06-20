package bobik;

import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Provides interface to Bobik API.
 * <br>
 * Format of request and response objects is explained in detail at <a href="http://usebobik.com/api/docs#func_ref">Bobik API Docs</a>
 *
 * <br><br>
 * <b>Example:</b>
 * <pre>
 * {@code
    BobikClient bobik = new BobikClient("YOUR_AUTH_KEY");

    JSONObject request = new JSONObject();
    for (String url : new String[]{"amazon.com", "google.com"})
        request.accumulate("urls", url);
    for (String query : new String[]{"//a/@href", "return $('.logo').length"})
        request.accumulate("queries", query_set);

    Job job = bobik.scrape(request, new JobListener() {
        public void onSuccess(JSONObject scraped_data) {
            System.out.println("Received data: " + scraped_data);
        }

        public void onProgress(float currentProgress) {
            System.out.println("Current progress is " + currentProgress*100 + "%");
        }

        public void onErrors(Collection<String> errors){
            for (String s : errors)
                System.err.println("Error for job " + job.id() + ": " + s);
        }
    });
 * }
 * </pre>
 * @author Eugene Mirkin
 * @author Ashwath Murthy
 */
public class BobikClient {

    /*
     * The Bobik Auth Token
     */
    private String authToken;

    /*
     * Query timeout in milliseconds
     */
    private int timeoutMs = 30000;

    /*
     * Max number of concurrent Bobik jobs
     */
    private int numThreads = 10;

    /*
     * Executes jobs in parallel
     */
    private final ExecutorService runners;

    /**
     * @param authToken
     */
    public BobikClient(String authToken) {
        setAuthToken(authToken);
        runners = Executors.newFixedThreadPool(this.numThreads);
    }


    /**
     * Kicks off an asynchronous scraping job and returns a proxy object.
     * This proxy is only for added convenience of monitoring/cancelling the job.
     * The listener you pass as a parameter does all this and more already.
     * @param request The scraping request, built in accordance with http://usebobik.com/api/docs#func_ref
     * @param listener An object encapsulating various useful callbacks
     * @return proxy to monitor/abort the scraping job
     * @throws BobikException thrown on all Bobik-specific errors
     * @throws IOException thrown on network problems
     * @throws JSONException not common, thrown on surprise JSON parsing errors
     * @throws ExecutionException not common, thrown if there's a client-level error during the scheduling of threads
     * @throws InterruptedException not common, thrown if there's a client-level error during the scheduling of threads
     */
    public Job scrape(JSONObject request, final JobListener listener) throws BobikException, IOException, JSONException, ExecutionException, InterruptedException {
        JSONObject job_submission = callAPI(request, "POST");
        final long startTime = System.currentTimeMillis();
        final String job_id = getJobIdOrFail(job_submission);
        final Job job = new Job() {
            private JSONObject scraped_data = null;
            private Future<Object> job_waiter = null;
            private int estimated_completion_time_ms = -1;
            private boolean cancelled = false;

            private JSONObject getStatusRequestObj(boolean download_results) throws BobikException, JSONException {
                JSONObject progress_check = new JSONObject();
                progress_check.accumulate(BobikConstants.JOB_TOKEN_LABEL, id());
                progress_check.accumulate(BobikConstants.SKIP_DATA_TOKEN_LABEL, !download_results);
                return progress_check;
            }

            @Override
            public String id() {
                return job_id;
            }

            @Override
            public float getProgress() throws BobikException {
                try {
                    JSONObject status_check = callAPI(getStatusRequestObj(false), "GET");
                    processErrors(status_check, listener);
                    float progress = (float)status_check.getDouble(BobikConstants.PROGRESS_JSON_LABEL);
                    estimated_completion_time_ms = status_check.getInt(BobikConstants.ESTIMATED_TIME_REMAINING_TOKEN_LABEL);
                    listener.onProgress(progress);
                    if (progress == 1.0)
                        fetchScrapedData();
                    return progress;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new BobikException(e);
                }
            }

            protected void fetchScrapedData() throws BobikException{
                try {
                    JSONObject job_data = callAPI(getStatusRequestObj(true), "GET");
                    processErrors(job_data, listener);
                    scraped_data = job_data.getJSONObject(BobikConstants.RESULTS_TOKEN_LABEL);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new BobikException(e);
                }
                listener.onSuccess(scraped_data);
            }

            @Override
            public boolean cancel(boolean b) {
                try {
                    String url = "https://usebobik.com/jobs/" + id() + "/abort";
                    doHttp(url, "GET", new JSONObject());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                cancelled = true;
                return true;
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }

            @Override
            public boolean isDone() {
                try {
                    return cancelled || scraped_data != null || getProgress() == 1.0;
                } catch (BobikException e) {
                    e.printStackTrace();
                    return false;
                }
            }

            @Override
            public JSONObject get() throws InterruptedException, ExecutionException {
                try {
                    waitForCompletion();
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
                return scraped_data;
            }

            @Override
            public JSONObject get(long l, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
                return get();
            }

            private boolean checkTimer() throws  BobikException {
                if (System.currentTimeMillis() - startTime > timeoutMs)
                    throw new BobikException("Timing out after " + timeoutMs + "ms");
                return true;
            }

            @Override
            public void waitForCompletion() throws BobikException {
                // If this is the first time, kick of a monitor
                if (job_waiter == null) {
                    job_waiter = runners.submit(new Callable<Object>() {
                        @Override
                        public Object call() throws Exception {
                            while (checkTimer() && getProgress() != 1.0) {
                                try {
                                    Thread.sleep(estimated_completion_time_ms);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    new BobikException(e);
                                }
                            }
                            return null;
                        }
                    });
                }

                // Now, block on the monitor
                try {
                    job_waiter.get();
                } catch (Exception e) {
                    e.printStackTrace();
                    new BobikException(e);
                }
            }

        };

        listener.init(job);
        processErrors(job_submission, listener);
        return job;
    }


    /**
     * Called to Extract errors from a Bobik response and pass them on to user's listener
     * @param bobikResponse
     * @param listener
     * @throws BobikException not common, thrown on surprise parse errors
     */
    protected void processErrors(JSONObject bobikResponse, JobListener listener) throws JSONException {
        JSONArray errors;
        try {
            errors = bobikResponse.getJSONArray(BobikConstants.ERROR_TOKEN_LABEL);
        } catch (JSONException e) {
            return;
        }
        if (errors != null && errors.length() > 0) {
            Collection<String> messages = new ArrayList<String>(errors.length());
            for (int i=0; i<errors.length(); i++)
                messages.add(errors.getString(i));
            listener.onErrors(messages);
        }
    }

    /**
     * Retrieves the job id from the submission object
     * @param job_submission
     * @return id
     * @throws BobikException if job fails to start
     */
    private String getJobIdOrFail(JSONObject job_submission) throws BobikException {
        try {
            return job_submission.getString(BobikConstants.JOB_TOKEN_LABEL);
        } catch (JSONException e) {
            throw new BobikException("Job failed to start.");
        }
    }

    /**
     * Submits a JSON request and returns the response received from Bobik
     * @param request
     * @param httpMethod
     * @return response
     * @throws IOException
     * @throws BobikException
     * @throws JSONException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    protected JSONObject callAPI(JSONObject request, final String httpMethod) throws IOException, BobikException, JSONException, ExecutionException, InterruptedException {
        request.put(BobikConstants.AUTH_TOKEN_LABEL, authToken);
        HttpResponse response = doHttp(BobikConstants.BOBIK_URL, httpMethod, request).get();
        int responseCode = response.getStatusLine().getStatusCode();
        String responseBody = BobikClient.convertStreamToString(response.getEntity().getContent());

        switch (responseCode) {
            case HttpStatus.SC_OK:
            case HttpStatus.SC_ACCEPTED:
            case HttpStatus.SC_CREATED:
                JSONObject result = null;
                try {
                    result = new JSONObject(responseBody);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return result;
            default:
                throw new BobikException("Error - HTTP Response Code: " + responseCode + ", HTTP Response Body: " + responseBody);
        }
    }


    /**
     * Performs basic HTTP communication. Also unifies serialization of query parameters
     * @param url base url
     * @param httpMethod GET/POST
     * @param data
     * @return response
     * @throws IOException
     * @throws JSONException
     */
    private Future<HttpResponse> doHttp(final String url, final String httpMethod, final JSONObject data) throws IOException, JSONException {
        return runners.submit(new Callable<HttpResponse>() {
            @Override
            public HttpResponse call() throws Exception {
                HttpParams httpParams = new BasicHttpParams();
                HttpConnectionParams.setConnectionTimeout(httpParams, timeoutMs);
                HttpConnectionParams.setSoTimeout(httpParams, timeoutMs);
                final HttpClient client = new DefaultHttpClient(httpParams);
                HttpRequestBase httpRequest;
                if (httpMethod.equalsIgnoreCase("POST")) {
                    httpRequest = new HttpPost(url);
                    try {
                        //HttpEntity body = new UrlEncodedFormEntity(data.toString().getBytes("UTF8"));
                        StringEntity body = new StringEntity(data.toString());
                        body.setContentType("application/json");
                        ((HttpPost)httpRequest).setEntity(body);
                    } catch (UnsupportedEncodingException e) {
                        return null;
                    }
                } else {
                    List<NameValuePair> query_params = new LinkedList<NameValuePair>();
                    for (Iterator<String> i=data.keys(); i.hasNext(); ) {
                        String key = i.next();
                        String value = data.get(key).toString();
                        query_params.add(new BasicNameValuePair(key, value));
                    }
                    String query = URLEncodedUtils.format(query_params, "utf-8");
                    httpRequest = new HttpGet(url + (url.endsWith("?")? "&" : "?") + query);
                }
                httpRequest.setHeader("Accept", "application/json");
                return client.execute(httpRequest);
            }
        });
    }


    private static String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        try {
            String line = null;
            while ((line = reader.readLine()) != null)
                sb.append(line + "\n");
        } finally {
            is.close();
        }
        return sb.toString();
    }


    public String getAuthToken() {
        return authToken;
    }
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
    public int getTimeoutMs() {
        return timeoutMs;
    }
    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
