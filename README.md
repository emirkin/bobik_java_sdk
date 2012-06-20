== Bobik SDK for Java

This is a community-supported SDK for interacting with Bobik.


== Using Bobik SDK
1. Include `bobik-1.0.jar` located in the `lib` directory
2. Start scraping!

```java
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
```

3. Full API reference is available at https://github.com/emirkin/bobik_java_sdk/tree/master/docs

== For Contributing Developers

1. Write to support@usebobik.com to become a collaborator.
2. The SDK source is fully contained within the `bobik.jar` directory.
3. Latest compiled jar goes to `lib`
4. Javadoc goes to `docs`
5. A sample test application (admittedly, very primitive) is in `sample_app`

== Bugs?
Submit them here on GitHub