package org.example.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.dao.CommandDAO;
import org.example.util.TokenValidator;

import java.io.IOException;

@WebServlet("/commands/fetch")
public class CommandFetchServlet extends HttpServlet {

    private final CommandDAO commandDAO = new CommandDAO();

    @Override
    protected void doGet(
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

            CommandDAO.CommandResult command = commandDAO.fetchCommand(deviceId);

            res.setContentType("application/json");

            if (command == null) {

                res.setStatus(HttpServletResponse.SC_NO_CONTENT);

                return;
            }

            String response = """
                    {
                        "commandId": %s,
                        "command": "%s"
                    }
                    """.formatted(command.id, command.commandText);

            res.setStatus(HttpServletResponse.SC_OK);

            res.getWriter().write(response);

        } catch (Exception e) {

            e.printStackTrace();

            res.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            res.setContentType("application/json");

            res.getWriter().write(
                    "{\"error\":\"Failed to fetch command\"}"
            );
        }
    }
}