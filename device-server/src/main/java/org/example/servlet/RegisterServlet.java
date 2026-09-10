package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.model.RegisterRequest;
import org.example.service.DeviceService;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DeviceService deviceService = new DeviceService();

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse res)
            throws ServletException, IOException {

        try {

            RegisterRequest request = objectMapper.readValue(req.getReader(), RegisterRequest.class);

            String deviceToken = deviceService.register(request);

            res.setStatus(HttpServletResponse.SC_CREATED);
            res.setContentType("application/json");

            String response = """
                    {
                        "message": "Device registered successfully",
                        "deviceToken": "%s"
                    }
                    """.formatted(deviceToken);

            res.getWriter().write(response);

        } catch (IllegalArgumentException e) {

            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");

            res.getWriter().write(
                    "{\"error\":\"Invalid PSK\"}"
            );

        } catch (Exception e) {

            e.printStackTrace();

            res.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            res.setContentType("application/json");

            res.getWriter().write(
                    "{\"error\":\"Registration failed\"}"
            );
        }
    }
}