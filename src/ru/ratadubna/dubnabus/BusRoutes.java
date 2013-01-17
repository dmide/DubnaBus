package ru.ratadubna.dubnabus;

import java.util.ArrayList;

public class BusRoutes {
	private String desc;
	private int num;
	private static ArrayList<BusRoutes> routes = new ArrayList<BusRoutes>();
	
	public static ArrayList<BusRoutes> GetRoutes(){
		return routes;
	}
	
	public static void Add(int num, String desc){
		BusRoutes route = new BusRoutes();
		route.num = num;
		route.desc = desc;
		routes.add(route);
	}
	
	public String GetDesc(){
		return desc;
	}
	
	public int GetId(){
		return num;
	}
	
}
