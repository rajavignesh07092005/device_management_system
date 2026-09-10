package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.CommandDAO;

import java.io.IOException;
import java.util.Map;

@WebServlet("/admin/history")
public class AdminHistoryServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CommandDAO commandDAO = new CommandDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        String deviceIdParam = req.getParameter("deviceId");

        if (deviceIdParam == null) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(res.getWriter(), Map.of("error", "deviceId query parameter is required"));
            return;
        }

        try {
            int deviceId = Integer.parseInt(deviceIdParam);
            var history = commandDAO.getHistoryForDevice(deviceId);
            res.setStatus(HttpServletResponse.SC_OK);
            mapper.writeValue(res.getWriter(), history);
        } catch (NumberFormatException e) {
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(res.getWriter(), Map.of("error", "deviceId must be a number"));
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(res.getWriter(), Map.of("error", "Could not fetch history"));
        }
    }
}
