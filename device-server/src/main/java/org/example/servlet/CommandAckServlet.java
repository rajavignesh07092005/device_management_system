package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.dao.CommandDAO;
import org.example.model.CommandAckRequest;
import org.example.util.TokenValidator;

import java.io.IOException;

@WebServlet("/commands/ack")
public class CommandAckServlet extends HttpServlet {

    private final ObjectMapper objectMapper =
            new ObjectMapper();

    private final CommandDAO commandDAO =
            new CommandDAO();

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res)
            throws IOException {

        String authorization = req.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {

            res.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            res.setContentType("application/json");

            res.getWriter().write(
                    "{\"error\":\"Missing token\"}"
            );

            return;
        }

        String token = authorization.substring(7);

        try {

            if (!TokenValidator.isValid(token)) {

                res.setStatus(
                        HttpServletResponse.SC_UNAUTHORIZED
                );

                res.setContentType("application/json");

                res.getWriter().write(
                        "{\"error\":\"Invalid device token\"}"
                );

                return;
            }

            int deviceId = TokenValidator.getDeviceId(token);

            CommandAckRequest request = objectMapper.readValue(req.getReader(), CommandAckRequest.class);

            boolean acknowledged = commandDAO.acknowledgeCommand(request.commandId, deviceId);

            res.setContentType("application/json");

            if (!acknowledged) {

                res.setStatus(
                        HttpServletResponse.SC_NOT_FOUND
                );

                res.getWriter().write(
                        "{\"error\":\"Command not found or not in FETCHED state\"}"
                );

                return;
            }

            res.setStatus(HttpServletResponse.SC_OK);

            res.getWriter().write(
                    "{\"message\":\"Command acknowledged successfully\"}"
            );

        } catch (Exception e) {

            e.printStackTrace();

            res.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            res.setContentType("application/json");

            res.getWriter().write(
                    "{\"error\":\"Failed to acknowledge command\"}"
            );
        }
    }
}