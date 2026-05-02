package it.cnr.isti.labsedc.concern.rest.auth;

import it.cnr.isti.labsedc.concern.storage.UserDAO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;

@Path("api/auth")
public class AuthAPI {

    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(String body) {
        try {
            JSONObject req = new JSONObject(body);
            String username = req.optString("username", "").trim();
            String password = req.optString("password", "");

            if (username.isEmpty() || password.isEmpty()) {
                return Response.status(400)
                        .entity("{\"error\": \"Username and password are required\"}").build();
            }

            UserDAO.UserRecord user = UserDAO.findUser(username);
            if (user == null || !UserDAO.checkPassword(password, user.passwordHash)) {
                return Response.status(401)
                        .entity("{\"error\": \"Invalid credentials\"}").build();
            }

            String token = JwtUtil.generateToken(user.username, user.role);

            JSONObject result = new JSONObject();
            result.put("token", token);
            result.put("username", user.username);
            result.put("role", user.role);

            return Response.ok(result.toString()).build();

        } catch (Exception e) {
            return Response.status(500)
                    .entity("{\"error\": \"Login failed\"}").build();
        }
    }
}
