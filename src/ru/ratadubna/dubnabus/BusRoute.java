package ru.ratadubna.dubnabus;

import java.util.ArrayList;

public class BusRoute {
    private String desc;
    private int routeServiceId, routeRealId;
    private boolean isActive;
    private static final ArrayList<BusRoute> routes = new ArrayList<BusRoute>();

    public static ArrayList<BusRoute> getRoutesArray() {
        return routes;
    }

    public static int getRoutesArraySize() {
        return routes.size();
    }

    public static BusRoute getRoute(int pos) {
        return routes.get(pos);
    }

    public static void addRouteToArray(int routeServiceId, String desc, int routeRealId) {
        BusRoute route = new BusRoute();
        route.routeServiceId = routeServiceId;
        route.desc = desc;
        route.routeRealId = routeRealId;
        routes.add(route);
    }

    public String getDesc() {
        return desc;
    }

    public int getRouteServiceId() {
        return routeServiceId;
    }

    public int getRouteRealId() {
        return routeRealId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}
