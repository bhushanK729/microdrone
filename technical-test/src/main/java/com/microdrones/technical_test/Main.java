package com.microdrones.technical_test;

import com.google.gson.Gson;
import com.microdrones.technical_test.data.models.Config;
import com.microdrones.technical_test.data.models.Payload;
import com.microdrones.technical_test.data.models.Drones;
import com.microdrones.technical_test.data.models.Mission;
import com.microdrones.technical_test.data.models.Point;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Main {

    public static final String DATA_DIRECTORY_PATH_PREFIX = ".\\technical-test\\src\\main\\java\\com\\microdrones\\technical_test\\data";

    public static void main(String[] args) {
        Main main = new Main();
        for (int i = 1; i < 7; i++){
            Mission mission = readMission(i);
            Config config = readConfig(i);
            Drones drones = readDrones(i);
            double energy = main.calcRequiredEnergy(config, drones, mission);
            System.out.println(mission.name + " : " + (energy <= Double.parseDouble(""+(config.energy.numberOfBatteries * config.energy.capacity))));
        }
    }
    //Calculate total energy required during mission
    private double calcRequiredEnergy(Config config, Drones drone, Mission mission) {
        Payload payload = config.payload;
        if (payload == null) {
            payload = new Payload();
        }
        double ascensionEnergy = calcAEnergy(mission.altitude, config.verticalSpeeds.ascension,
                drone.currentLoadInFlight.ascension + payload.additionalLoad, mission);

        double horEnergy = 0.0;
        ArrayList<Point> points = mission.points;
        for (int i = 0; i < points.size() - 1; i++) {
            horEnergy += calcHEnergy(points.get(i), points.get(i + 1), mission.horizontalSpeed,
                    drone.currentLoadInFlight.translation + payload.additionalLoad);
        }

        double descentEnergy = calcAEnergy(mission.altitude, config.verticalSpeeds.descent,
                drone.currentLoadInFlight.descent + payload.additionalLoad, mission);
        return ascensionEnergy + horEnergy + descentEnergy;
    }

    public static final double EARTH_RADIUS = 6371000;
    //Calculate distance between two latitudes and longitude
    public static double distance(double lat1, double lon1,
                                  double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c;

        return distance;
    }
    //Get mission wise data from JSON file and parse json
    public static Mission readMission(int index){
        Mission mission = null;
        try {
            File f = new File(DATA_DIRECTORY_PATH_PREFIX+"\\missions\\mission-"+index+".json");
            if (f.exists()){
                InputStream is = new FileInputStream(DATA_DIRECTORY_PATH_PREFIX+"\\missions\\mission-"+index+".json");
                String result = new BufferedReader(new InputStreamReader(is))
                        .lines().parallel().collect(Collectors.joining("\n"));
                Gson gson = new Gson();
                mission = gson.fromJson(result, Mission.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mission;
    }
    //Get dron configuration in mission from JSON file and parse json
    public static Config readConfig(int index){
        Config config = null;
        try {
            File f = new File(DATA_DIRECTORY_PATH_PREFIX+"\\configurations\\config-"+index+".json");
            if (f.exists()){
                InputStream is = new FileInputStream(DATA_DIRECTORY_PATH_PREFIX+"\\configurations\\config-"+index+".json");
                String result = new BufferedReader(new InputStreamReader(is))
                        .lines().parallel().collect(Collectors.joining("\n"));
                Gson gson = new Gson();
                config = gson.fromJson(result, Config.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return config;
    }

    //Get dron info from JSON file and parse json
    public static Drones readDrones(int index){
        Drones drone = null;
        try {
            File f = new File(DATA_DIRECTORY_PATH_PREFIX+"\\drones\\drone-"+index+".json");
            if (f.exists()){
                InputStream is = new FileInputStream(DATA_DIRECTORY_PATH_PREFIX+"\\drones\\drone-"+index+".json");
                String result = new BufferedReader(new InputStreamReader(is))
                        .lines().parallel().collect(Collectors.joining("\n"));
                Gson gson = new Gson();
                drone = gson.fromJson(result, Drones.class);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return drone;
    }
    //Calculate horizontal dron energy
    private double calcHEnergy(Point p1, Point p2, double speed, double load) {
        double dist = distance(p1.latitude, p1.longitude, p2.latitude, p2.longitude);
        double time = dist / speed;
        p2.endTime = (p1.endTime + time);
        double energy = time * load * Math.pow(speed, 2);
        p2.energy = (p1.energy + energy);
        return energy;
    }
    //Calculate ascension and descent energy
    private double calcAEnergy(double altitude, double vertSpeed, double vertLoad, Mission mission) {
        double time = altitude / vertSpeed;
        mission.points.get(0).endTime = time;

        double energy = time * vertLoad * Math.pow(vertSpeed, 2);
        mission.points.get(0).energy = energy;

        mission.endTime = (mission.points.get(mission.points.size() - 1).endTime + time);
        mission.energyReq = energy;
        return energy;
    }
}
