package service;

import dataaccess.*;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

public class UserService {

    private final DataAccess data;

    public UserService(DataAccess data) {
        this.data = data;
    }

    public RegisterResult register(RegisterRequest request)
            throws BadRequestException, AlreadyTakenException, DataAccessException {

        //404
        if (request == null
                || isBlank(request.username())
                || isBlank(request.password())
                || isBlank(request.email())) {
            throw new BadRequestException("404 Error: Bad request");
        }

        //403
        UserData alreadyExists = data.getUser(request.username());
        if (alreadyExists != null) {
            throw new AlreadyTakenException("403 Error: Already taken");
        }

        String hashedPassword = BCrypt.hashpw(request.password(), BCrypt.gensalt());
        UserData user = new UserData(request.username(), hashedPassword, request.email());
        data.insertUser(user);

        String token = UUID.randomUUID().toString();
        data.insertAuth(new AuthData(token, request.username()));

        return new RegisterResult(request.username(), token);
    }

    public LoginResult login(LoginRequest request)
        throws UnauthorizedException, BadRequestException, DataAccessException {

        //400
        if (request == null
                || isBlank(request.username())
                || isBlank(request.password())) {
            throw new BadRequestException("400 Error: Bad request");
        }

        //401
        UserData user = data.getUser(request.username());
        if (user == null
                || user.password() == null
                || !BCrypt.checkpw(request.password(), user.password())) {
            throw new UnauthorizedException("401 Error: Unauthorized");
        }

        String token = UUID.randomUUID().toString();
        data.insertAuth(new AuthData(token, request.username()));

        return new LoginResult(request.username(), token);

    }

    public void logout(LogoutRequest request)
            throws UnauthorizedException, DataAccessException {
        if (request == null || isBlank(request.authToken())) {
            throw new UnauthorizedException("401 Error: Unauthorized");
        }

        var auth = data.getAuth(request.authToken());
        if (auth == null) {
            throw new UnauthorizedException("401 Error: Unauthorized");
        }

        data.deleteAuth(request.authToken());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
