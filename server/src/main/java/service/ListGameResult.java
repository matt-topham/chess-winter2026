package service;

import model.GameData;
import java.util.Collection;

public record ListGameResult(
        Collection<GameData> games
) {
}
