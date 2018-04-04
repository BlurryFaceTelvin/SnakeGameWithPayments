package com.example.paymentlib;

import com.africastalking.AfricasTalking;
import com.africastalking.Server;
import com.google.gson.Gson;

import java.util.HashMap;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

public class PaymentServer {
    private static int RPC_PORT = 35897;
    private static int HTTP_PORT = 30001;
    public static void main(String[] args) {
        System.out.println("Starting...........");
        AfricasTalking.initialize("musix", "1412d2e38ef0c7541cec1bdb854fcfd8a0ad2fab924065bfe3965878f6357f1f");
        Server server = new Server();
        try {
            server.startInsecure(RPC_PORT);

        } catch (Exception e) {
            e.printStackTrace();
        }

        //set our port
        port(HTTP_PORT);
        HashMap<String,String> transactions = new HashMap<>();
        get("/transaction/status",(request, response) -> {

            return transactions.get("status");
        });
        post("/notify",(request, response) -> {
            Gson gson = new Gson();
            AfricasTalkingNotification notification = gson.fromJson(request.body(),AfricasTalkingNotification.class);
            transactions.put("status",notification.status);
            System.out.println(request.body());
            return "OK";
        });

    }
    //model for data
    static class AfricasTalkingNotification{
        String status;
    }


}
