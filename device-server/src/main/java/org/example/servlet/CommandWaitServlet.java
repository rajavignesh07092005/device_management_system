package org.example.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.util.TokenValidator;
import org.example.dao.CommandDAO;

import java.io.IOException;

@WebServlet("/commands/wait")
public class CommandWaitServlet extends HttpServlet {

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

            long timeout = System.currentTimeMillis() + 30000;

            while (System.currentTimeMillis() < timeout) {

                if (commandDAO.hasPendingCommand(deviceId)) {

                    res.setStatus(
                            HttpServletResponse.SC_OK
                    );

                    res.setContentType("application/json");

                    res.getWriter().write(
                            "{\"commandAvailable\":true}"
                    );

                    return;
                }

                Thread.sleep(1000);
            }

            res.setStatus(
                    HttpServletResponse.SC_NO_CONTENT
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

        } catch (Exception e) {

            e.printStackTrace();

            res.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            res.setContentType("application/json");

            res.getWriter().write(
                    "{\"error\":\"Command wait failed\"}"
            );
        }
    }
}