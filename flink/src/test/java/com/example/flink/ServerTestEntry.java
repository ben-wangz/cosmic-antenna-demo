package com.example.flink;

import com.example.flink.source.handler.SampleDataHandler;
import com.example.flink.source.server.MockServer;

public class ServerTestEntry {

    public static void main(String[] args) {

        System.out.println("In Main Method");

        // Create netty server
        try {

            System.out.println("Starting up the server on port 18888 ...");
            MockServer mockServer = new MockServer();
            mockServer.startup(18888);
            mockServer.registerHandler("ssss", SampleDataHandler.builder().build());

            System.out.println("UDP server started on port 18888 ...");
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
