package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.model.AdminCreateRequest;
import org.example.service.AdminService;

import java.io.IOException;
import java.util.Map;

@WebServlet("/admin/create")
public class AdminCreateServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AdminService adminService = new AdminService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        try {
            AdminCreateRequest request = mapper.readValue(req.getInputStream(), AdminCreateRequest.class);
            adminService.createAdmin(request);

            res.setStatus(HttpServletResponse.SC_CREATED);
            mapper.writeValue(res.getWriter(), Map.of("message", "Admin created successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(res.getWriter(), Map.of("error", "Admin creation failed"));
        }
    }
}
