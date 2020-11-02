package com.xuershangda.joystick.nav;

/**
 * app接口的一些常量
 */
public class Consts {
    public static final String IP = "10.1.156.135";
    public static final int TCP_PORT = 9090;
    public static final String HOST = "http://" + IP + ":5000/";
    public static final String API_GET_MAP = "robot/download/synth_image";
    public static final String API_GET_POS = "robot/getPosition";
    public static final String API_GET_STATUS = "robot/getStatus";
    public static final String API_GET_MAPINFO = "robot/getMapInfos";
    public static final String API_SET_POS = "robot/setPosition";
    public static final String API_SET_GOAL = "robot/setGoal";
}
