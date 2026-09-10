package org.example.model;

public class Device {

    public int id;
    public String hostname;
    public String ipAddress;
    public String osVersion;
    public String serialNumber;
    public String deviceToken;

    public Device() {
    }

    public Device(int id,
                  String hostname,
                  String ipAddress,
                  String osVersion,
                  String serialNumber,
                  String deviceToken) {

        this.id = id;
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.osVersion = osVersion;
        this.serialNumber = serialNumber;
        this.deviceToken = deviceToken;
    }
}
