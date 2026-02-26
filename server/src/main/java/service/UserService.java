package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.BadRequestException;
import dataaccess.AlreadyTakenException;
import model.AuthData;
import model.UserData;

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

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
