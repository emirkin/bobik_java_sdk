## Bobik SDK for Java

This is a community-supported SDK for web scraping in Java.

### Installing

Include `bobik-1.0.jar` located in the `lib` directory.
If you are scraping from an Android application, this is enough.
If you are using a vanilla Java environment, you might need to include `HttpComponents` and an `org.json` packages (see http://usebobik.com/sdk).


### Using
Here's a quick example to get you started.

```java
    BobikClient bobik = new BobikClient("YOUR_AUTH_KEY");

    JSONObject request = new JSONObject();
    for (String url : new String[]{"amazon.com", "google.com"})
        request.accumulate("urls", url);
    for (String query : new String[]{"//a/@href", "return $('.logo').length"})
        request.accumulate("queries", query);

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
```

Full API reference is available at http://usebobik.com/sdk/java

### Contributing

1. Write to support@usebobik.com to become a collaborator.
2. The SDK source is fully contained within the `bobik.jar` directory.
3. Latest compiled jar goes to `lib`
4. Javadoc goes to `docs`
5. A sample test application (admittedly, very primitive) is in `sample_app`

### Bugs?
Submit them here on GitHub: https://github.com/emirkin/bobik_java_sdk/issues