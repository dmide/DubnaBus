package ru.ratadubna.dubnabus;

import java.util.ArrayList;

public class BusRoutes {
	private String desc;
	private int routeServiceId;
	private int routeRealId;
	private static ArrayList<BusRoutes> routes = new ArrayList<BusRoutes>();

	public static ArrayList<BusRoutes> GetRoutes() {
		return routes;
	}

	public static void add(int routeServiceId, String desc, int routeRealId) {
		BusRoutes route = new BusRoutes();
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

}
