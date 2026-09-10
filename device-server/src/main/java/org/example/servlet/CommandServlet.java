package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.dao.CommandDAO;
import org.example.model.CommandRequest;

import java.io.IOException;
import java.util.Map;

@WebServlet("/commands")
public class CommandServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();

    private final CommandDAO commandDAO = new CommandDAO();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        res.setContentType("application/json");
        try {
            CommandRequest request = mapper.readValue(req.getInputStream(), CommandRequest.class);

            if (request.command == null || request.command.isBlank()) {
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                mapper.writeValue(res.getWriter(), Map.of("error", "Command text cannot be empty"));
                return;
            }

            boolean added = commandDAO.addCommand(request.deviceId, request.command);
            if (!added) {
                res.setStatus(HttpServletResponse.SC_NOT_FOUND);
                mapper.writeValue(res.getWriter(), Map.of("error", "Device not found"));
                return;
            }

            res.setStatus(HttpServletResponse.SC_CREATED);
            mapper.writeValue(res.getWriter(), Map.of("message", "Command added successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(res.getWriter(), Map.of("error", "Could not add command"));
        }
    }
}