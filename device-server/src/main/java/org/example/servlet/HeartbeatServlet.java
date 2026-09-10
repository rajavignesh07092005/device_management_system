package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.dao.DeviceDAO;
import org.example.model.HeartbeatRequest;
import org.example.util.TokenValidator;

import java.io.IOException;

@WebServlet("/heartbeat")
public class HeartbeatServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final DeviceDAO deviceDAO = new DeviceDAO();

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res)
            throws IOException {

        String authorization = req.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {

            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");

            res.getWriter().write("{\"error\":\"Missing token\"}");

            return;
        }

        String token = authorization.substring(7);

        try {

            if (!TokenValidator.isValid(token)) {

                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                res.setContentType("application/json");

                res.getWriter().write("{\"error\":\"Invalid device token\"}");

                return;
            }

            HeartbeatRequest heartbeat = objectMapper.readValue(req.getReader(), HeartbeatRequest.class);

            deviceDAO.updateHeartbeat(
                    token,
                    heartbeat.hostname,
                    heartbeat.ipAddress,
                    heartbeat.osVersion,
                    heartbeat.serialNumber
            );

            res.setStatus(HttpServletResponse.SC_OK);
            res.setContentType("application/json");

            res.getWriter().write("{\"message\":\"Heartbeat received\"}");

        } catch (Exception e) {

            e.printStackTrace();

            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            res.setContentType("application/json");

            res.getWriter().write("{\"error\":\"Heartbeat failed\"}");
        }
    }
}