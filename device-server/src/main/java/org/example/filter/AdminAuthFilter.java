package org.example.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.dao.AdminDAO;

import java.io.IOException;
import java.util.Map;


public class AdminAuthFilter implements Filter {

    private final ObjectMapper mapper = new ObjectMapper();
    private final AdminDAO adminDAO = new AdminDAO();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        // Let /admin/login through WITHOUT a token check —
        // you can't have a token yet if you're trying to log in.
        if (req.getRequestURI().endsWith("/admin/login")) {
            chain.doFilter(request, response);
            return;
        }

        res.setContentType("application/json");
        String authorization = req.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(res.getWriter(), Map.of("error", "Missing admin token"));
            return;
        }

        String token = authorization.substring(7);

        try {
            if (!adminDAO.isValidToken(token)) {
                res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                mapper.writeValue(res.getWriter(), Map.of("error", "Invalid admin token"));
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            mapper.writeValue(res.getWriter(), Map.of("error", "Admin auth check failed"));
            return;
        }

        // Token is valid — let the actual servlet run.
        chain.doFilter(request, response);
    }
}
