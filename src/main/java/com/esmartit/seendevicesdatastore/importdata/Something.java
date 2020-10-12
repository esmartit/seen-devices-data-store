package com.esmartit.seendevicesdatastore.importdata;

import java.util.Arrays;

import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;

public class Something {

    public void something() {

        Arrays.asList(project(
                fields(excludeId(),
                        computed("spotId", "$spotId"),
                        computed("sensorId", "$sensorId"),
                        computed("clientMac", "$clientMac"),
                        computed("dateSeen", eq("$dateToString",
                                and(eq("format", "%Y/%m/%d"), eq("date", "$seenTime")))),
                        computed("timePart", eq("$dateToString",
                                and(eq("format", "%H:%M:%S"), eq("date", "$seenTime")))),
                        computed("power", "$rssi"), computed("status", "$status"),
                        computed("ssid", ""), computed("countryId", "$countryId"),
                        computed("stateId", "$stateId"), computed("cityId", "$cityId"),
                        computed("zipCode", "$zipCode"), include("date"))),
                match(and(and(gte("dateSeen", "2020/10/08"), lte("dateSeen", "2020/10/08")),
                        and(gte("timePart", "00:00:00"), lte("timePart", "23:59:59")),
                        and(ne("status", "NO_POSITION"), in("status", Arrays.asList("IN", "LIMIT", "OUT"))), eq("countryId", "ES"), eq("stateId", "12"), eq("cityId", "28"), in("zipCode", Arrays.asList("28221")), in("clientMac", Arrays.asList()), not(in("clientMac", Arrays.asList())))));
    }
}
