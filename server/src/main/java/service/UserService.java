package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.BadRequestException;
import dataaccess.AlreadyTakenException;
import model.AuthData;
import model.UserData;

import static org.junit.platform.commons.util.StringUtils.isBlank;

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
    }
}
