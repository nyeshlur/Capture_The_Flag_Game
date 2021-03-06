package edu.uwb.css533.service.api;

import edu.uwb.css533.service.db.GameDao;
import org.jdbi.v3.core.Jdbi;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Path("/capture_the_flag")
public class GameService {

    private Jdbi jdbi;
    private GameDao dao;
    private static HttpClient HTTP_CLIENT;

    public GameService(Jdbi jdbi, GameDao dao) {
        this.jdbi = jdbi;
        this.dao = dao;

        HTTP_CLIENT = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @GET
    @Path("/start_game")
    public Response startGame(@QueryParam("session_id") int sid, @QueryParam("user_id") int uid) {
        Integer numPlayers = dao.checkNumPlayers(sid);
        //session does not exist
        if(numPlayers == null) {
            return Response.ok(-1).build();
        }
        try {
            Boolean status = dao.getGameStatus(sid);
            Integer p1_id = dao.getPlayer1ID(sid);

            long gameStart;
            if(uid == p1_id && status == false){
                Response flagResponse = getFlag(sid);
                String flagName = dao.getFlag(sid);
                String feature1 = dao.getFeature1(sid);
                Integer feature1_code = dao.getFeature1Code(sid);
                String feature2 = dao.getFeature2(sid);
                Integer feature2_code = dao.getFeature2Code(sid);
                String feature3 = dao.getFeature3(sid);
                Integer feature3_code = dao.getFeature3Code(sid);
                gameStart = System.currentTimeMillis();
                dao.updateGameStatus(true, sid);
                dao.updateGameStartTime(gameStart, sid);
                return Response.ok(flagName + "," + feature1_code + feature1 + "," +
                        feature2_code + feature2 + "," + feature3_code + feature3 +
                        "," + gameStart).build();
            } else {
                if(status == true) {
                    String flagName = dao.getFlag(sid);
                    String feature1 = dao.getFeature1(sid);
                    Integer feature1_code = dao.getFeature1Code(sid);
                    String feature2 = dao.getFeature2(sid);
                    Integer feature2_code = dao.getFeature2Code(sid);
                    String feature3 = dao.getFeature3(sid);
                    Integer feature3_code = dao.getFeature3Code(sid);
                    gameStart = dao.getGameStartTime(sid);
                    return Response.ok(flagName + "," + feature1_code + feature1 + "," +
                            feature2_code + feature2 + "," + feature3_code + feature3 +
                            "," + gameStart).build();
                }
                return Response.ok(-1).build();
            }

        } catch (Exception e) {
            return Response.ok(-1).build();
        }
    }

    public HttpRequest requestFlag(int id) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI("http://127.0.0.1:8060/flag/get_flag?session_id=" + id))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .build();
    }

    public Response getFlag(int id) throws Exception {
        HttpRequest request = requestFlag(id);
        String response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString()).body();
        return Response.ok(response).build();

    }

    @GET
    @Path("/check_feature")
    public Response checkFeature(@QueryParam("session_id") int sid, @QueryParam("user_id") int uid, @QueryParam("feature") int feature) {
        Integer feature1_code = dao.getFeature1Code(sid);
        Integer feature2_code = dao.getFeature2Code(sid);
        Integer feature3_code = dao.getFeature3Code(sid);
        Integer p1_id = dao.getPlayer1ID(sid);
        Integer p2_id = dao.getPlayer2ID(sid);

        if(feature == feature1_code || feature == feature2_code || feature == feature3_code) {
            try {
                if(uid == p1_id) {
                    Integer progress = dao.getPlayer1Progress(sid);
                    progress++;
                    dao.updatePlayer1Progress(progress, sid);
                    if(progress == 3) {
                        long gameCompleted = System.currentTimeMillis();
                        long startGameTime = dao.getGameStartTime(sid);
                        long currentWinningTime = dao.getWinnerTime(sid);
                        long timeDifference = gameCompleted - startGameTime;
                        if(currentWinningTime == -1 || currentWinningTime > timeDifference) {
                            dao.updateWinnerTime(timeDifference, sid);
                            dao.updateWinner(uid, sid);
                        }
                    }
                } else if (uid == p2_id) {
                    Integer progress = dao.getPlayer2Progress(sid);
                    progress++;
                    dao.updatePlayer2Progress(progress, sid);
                    if(progress == 3) {
                        long gameCompleted = System.currentTimeMillis();
                        long startGameTime = dao.getGameStartTime(sid);
                        long currentWinningTime = dao.getWinnerTime(sid);
                        long timeDifference = gameCompleted - startGameTime;
                        if(currentWinningTime == -1 || currentWinningTime > timeDifference) {
                            dao.updateWinnerTime(timeDifference, sid);
                            dao.updateWinner(uid, sid);
                        }
                    }
                }

                if(feature == feature1_code) {
                    return Response.ok(1).build();
                } else if (feature == feature2_code) {
                    return Response.ok(2).build();
                } else {
                    return Response.ok(3).build();
                }

            } catch (Exception e) {
                return Response.ok(-1).build();
            }
        } else {
            return Response.ok(-2).build();
        }
    }


    @GET
    @Path("/end_game")
    public Response endGame(@QueryParam("session_id") int sid) {
        Integer numPlayers = dao.checkNumPlayers(sid);
        //session does not exist
        if(numPlayers == null) {
            return Response.ok(-1).build();
        }
        Integer winningPlayerID = dao.getWinner(sid);
        long winningTimeInSeconds = dao.getWinnerTime(sid);
        Integer player1Progress = dao.getPlayer1Progress(sid);
        Integer player2Progress = dao.getPlayer2Progress(sid);
        Integer player2ID = dao.getPlayer2ID(sid);
        try {
            if ((player1Progress >= 3 && player2ID == -1) || (player1Progress >= 3 && player2Progress >= 3)) {
                String flagName = dao.getFlag(sid);
                long bestTime = dao.getBestTime(flagName);
                if(winningTimeInSeconds < bestTime || bestTime == -1) {
                    dao.updateBestTime(winningTimeInSeconds, flagName);
                }
                bestTime = dao.getBestTime(flagName);
                return Response.ok(winningTimeInSeconds + "," + bestTime).build();
            } else {
                return Response.ok(-1).build();
            }
        } catch (Exception e) {
            return Response.ok(-2).build();
        }
    }
}
