package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.BadRequestException;
import model.AuthData;
import model.UserData;

public class UserService {

    private final DataAccess data;

    public UserService(DataAccess data) {
        this.data = data;
    }

    public RegisterResult register(RegisterRequest request)
            throws BadRequestException, AlreadyTakenException, DataAccessException {


    }
}
