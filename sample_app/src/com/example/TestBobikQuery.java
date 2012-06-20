package com.example;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;

import bobik.*;
import org.json.JSONObject;

public class TestBobikQuery {
	
	public static void main(String[] args) throws Exception {
	    BobikClient bobik = new BobikClient("XXXXX");

        JSONObject request = new JSONObject();
        for (String url : getSearchUrls("Advil"))
            request.accumulate("urls", url);
        for (String query_set : new String[]{"cvs", "walgreens", "drugstore.com"})
            request.accumulate("query_sets", query_set);

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
        job.waitForCompletion();
    }


	 private static String[] getSearchUrls(String keyword) {
         String keyword_encoded = null;
         try {
             keyword_encoded = URLEncoder.encode(keyword, "utf-8");
         } catch (UnsupportedEncodingException e) {
             e.printStackTrace();
         }
         return new String[] {
            "http://www.cvs.com/search/_?pt=product&searchTerm=" + keyword_encoded,
            "http://www.walgreens.com/search/results.jsp?Ntt=" + keyword_encoded,
            "http://www.drugstore.com/search/search_results.asp?N=0&Ntx=mode%2Bmatchallpartial&Ntk=All&Ntt=" + keyword_encoded
        };
     }

}
