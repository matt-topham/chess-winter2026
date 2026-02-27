package service;

import dataaccess.*;
import model.AuthData;
import model.UserData;
import org.eclipse.jetty.server.Authentication;

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

        UserData user = new UserData(request.username(), request.password(), request.email());
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
                || !user.password().equals(request.password())) {
            throw new UnauthorizedException("401 Error: unauthorized");
        }

        String token = UUID.randomUUID().toString();
        data.insertAuth(new AuthData(token, request.username()));

        return new LoginResult(request.username(), token);

    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
