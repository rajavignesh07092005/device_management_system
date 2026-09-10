package org.example.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.example.util.TokenValidator;

import java.io.IOException;

@WebServlet("/device")
public class DeviceServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse res)
            throws IOException {

        String authorization = req.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {

            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json");

            res.getWriter().write("{\"error\":\"Missing or invalid authorization\"}");

            return;
        }

        String token = authorization.substring(7);

        try {

            boolean valid = TokenValidator.isValid(token);

            if (!valid) {

                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                res.setContentType("application/json");

                res.getWriter().write("{\"error\":\"Invalid device token\"}");

                return;
            }

            res.setStatus(HttpServletResponse.SC_OK);
            res.setContentType("application/json");

            res.getWriter().write("{\"message\":\"Device authenticated successfully\"}");

        } catch (Exception e) {

            e.printStackTrace();

            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            res.setContentType("application/json");

            res.getWriter().write("{\"error\":\"Server error\"}");
        }
    }
}