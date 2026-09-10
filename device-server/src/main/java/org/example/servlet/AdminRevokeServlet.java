package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.DeviceDAO;
import org.example.model.RevokeRequest;

import java.io.IOException;
import java.util.Map;

@WebServlet("/admin/revoke")
public class AdminRevokeServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();
    private final DeviceDAO deviceDAO = new DeviceDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        try {
            RevokeRequest request = mapper.readValue(req.getInputStream(), RevokeRequest.class);
            boolean revoked = deviceDAO.revokeToken(request.deviceId);

            if (!revoked) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(res.getWriter(), Map.of("error", "Device not found"));
                return;
            }

            res.setStatus(HttpServletResponse.SC_OK);
            mapper.writeValue(res.getWriter(), Map.of("message", "Device token revoked"));
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(res.getWriter(), Map.of("error", "Revoke failed"));
        }
    }
}
