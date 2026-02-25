package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;

public class ClearService {
    private final DataAccess data;

    public ClearService(DataAccess data) {
        this.data = data;
    }

    public void clear() throws DataAccessException {
        data.clear();
    }
}
