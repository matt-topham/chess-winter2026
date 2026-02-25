package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;

public class UserService {

    private final DataAccess data;

    public UserService(DataAccess data) {
        this.data = data;
    }
}
