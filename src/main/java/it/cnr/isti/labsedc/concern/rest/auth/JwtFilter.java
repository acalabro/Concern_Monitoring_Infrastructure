package it.cnr.isti.labsedc.concern.rest.auth;

import io.jsonwebtoken.Claims;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtFilter implements ContainerRequestFilter {

    // Paths that require no token
    private static final String[] PUBLIC_PATHS = {
        "api/auth/login",
        "api/health",
        "monitoring"
    };

    // POST/DELETE/PUT that are allowed for USER role (read-like operations)
    private static final String[] USER_ALLOWED_POST_PATHS = {
        "api/rules/validate"
    };

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();
        String method = ctx.getMethod();

        // OPTIONS preflight – always allow
        if ("OPTIONS".equals(method)) return;

        // Public paths – no token required
        for (String pub : PUBLIC_PATHS) {
            if (path.startsWith(pub)) return;
        }

        // Extract and validate token
        String authHeader = ctx.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            abort(ctx, 401, "Authentication required");
            return;
        }

        Claims claims;
        try {
            claims = JwtUtil.validateToken(authHeader.substring(7));
        } catch (Exception e) {
            abort(ctx, 401, "Invalid or expired token");
            return;
        }

        String role = claims.get("role", String.class);
        ctx.setProperty("username", claims.getSubject());
        ctx.setProperty("role", role);

        // Write methods require ADMIN, except explicitly allowed POST paths
        boolean isWrite = "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method);
        if (isWrite && !"ADMIN".equals(role)) {
            for (String allowed : USER_ALLOWED_POST_PATHS) {
                if (path.equals(allowed)) return;
            }
            abort(ctx, 403, "Administrator access required");
        }
    }

    private static void abort(ContainerRequestContext ctx, int status, String message) {
        ctx.abortWith(Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity("{\"error\": \"" + message + "\"}")
                .build());
    }
}
