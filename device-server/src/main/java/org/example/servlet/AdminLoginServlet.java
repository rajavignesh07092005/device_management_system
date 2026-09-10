package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.model.AdminLoginRequest;
import org.example.service.AdminService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/admin/login")
public class AdminLoginServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AdminService adminService = new AdminService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        try {
            AdminLoginRequest request = mapper.readValue(req.getInputStream(), AdminLoginRequest.class);
            String token = adminService.login(request);

            res.setStatus(HttpServletResponse.SC_OK);
            mapper.writeValue(res.getWriter(), Map.of(
                    "message", "Admin login successful",
                    "adminToken", token
            ));
        } catch (IllegalArgumentException e) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(res.getWriter(), Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(res.getWriter(), Map.of("error", "Login failed"));
        }
    }
}
