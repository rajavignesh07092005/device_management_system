package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.DeviceDAO;

import java.io.IOException;
import java.util.Map;

@WebServlet("/admin/devices")
public class AdminDeviceListServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();
    private final DeviceDAO deviceDAO = new DeviceDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        try {
            var devices = deviceDAO.getAllDevices();
            res.setStatus(HttpServletResponse.SC_OK);
            mapper.writeValue(res.getWriter(), devices);
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(res.getWriter(), Map.of("error", "Could not fetch device list"));
        }
    }
}
