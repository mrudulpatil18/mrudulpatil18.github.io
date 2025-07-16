package com.ssg;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            StaticSiteGenerator ssg = new StaticSiteGenerator("dist"); // Only output directory needed

            ssg.generateSite();

        } catch (IOException e) {
            System.err.println("Error generating site: " + e.getMessage());
            e.printStackTrace();
        }
    }
}