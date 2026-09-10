package org.example.service;

import org.example.dao.DeviceDAO;
import org.example.model.RegisterRequest;

import java.util.UUID;

public class DeviceService {

    private final DeviceDAO deviceDAO = new DeviceDAO();

    
    private static final String SERVER_PSK = System.getenv("DEVICE_PSK");

    public String register(RegisterRequest request) throws Exception {
        if (!SERVER_PSK.equals(request.psk)) {
            throw new IllegalArgumentException("Invalid PSK");
        }

        Integer existingId = deviceDAO.findDeviceIdByIdentity(request.hostname, request.serialNumber);
        String deviceToken = UUID.randomUUID().toString();

        if (existingId != null) {
            // Same physical device re-registering — update it, don't duplicate it.
            deviceDAO.reissueToken(existingId, request.ipAddress, request.osVersion, deviceToken);
        } else {
            // Genuinely new device.
            deviceDAO.insertNewDevice(request.hostname, request.ipAddress, request.osVersion,
                    request.serialNumber, deviceToken);
        }

        return deviceToken;
    }

}
