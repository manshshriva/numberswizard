package com.num.wiz.aws.lambda.handler;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.*;
import com.amazon.speech.ui.*;
import com.num.wiz.aws.lambda.models.NumberWizardModel;
import com.num.wiz.aws.lambda.service.*;
import com.num.wiz.aws.lambda.service.enums.GameLevel;
import com.num.wiz.aws.lambda.service.enums.GameSate;
import com.num.wiz.aws.lambda.service.enums.GameType;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NumberWizardBySpeechlet implements Speechlet {

    private static final Logger logger = LoggerFactory.getLogger(NumberWizardBySpeechlet.class);
    private static final String CARD_TITLE = "Numbers Wizard";

    /** TEXT FOR THE INTENTS **/
    private static final String GAME_PLAY_TEXT = "Hello %s , let me know which game you want to play? Addition, Subtraction, Multiplication or Division? Please choose one of these.";
    private static final String GAME_LEVEL_TEXT = "Great, Please tell me which difficulty level you want? Please choose one from easy, medium and hard.";
    private static final String GAME_START_TEXT = "Sounds Good, In this level you will be challenged to answer %s of numbers. You will be scoring %s points for each correct answers. Let's start, what is the result when %s %s %s";
    private static final String GAME_RESTART_TEXT = "Restarting game of %s , with current points %s .What is the result when %s %s %s";
    private static final String CONTINUE_GAME_TEXT = "Next question, what is the result when %s %s %s";
    private static final String GAME_RESULT_CORRECT_TEXT = "You are right, the result is %s . Alright!! ";
    private static final String GAME_RESUME_TEXT = "Welcome Back %s , to Number Wizard! Please say New game to start a new game or say resume to continue playing the previous game.";
    private static final String GAME_MESSAGE_TEXT = "Game %s . %s with difficulty level %s .";
    private static final String SAVED_GAME_START_TEXT = " Please say which game you want to resume? You can start the saved games by saying, game name and then the level . Like, Addition with level Easy .";
    private static final String GAME_SCORE_TEXT = "Your current score for %s , level %s is %s . You have earned %s badge .";
    private static final String GAME_EXIT_MESSAGE = "Good Bye, your total score is %s . And you have earned a %s badge.";
    private static final String REPEAT_RESPONSE_TEXT = "Sorry !! I could not capture your response. Please try saying again.";
    private static final String GAME_RESULT_WRONG_TEXT = "Sorry it's the wrong answer. The correct answer is %s . Alright!!";
    public static final String NICKNAME_REPROMPT = "Please tell me your nickname by saying, My nickname is, and then your nickname.";
    private static final Map<String, String> GAME_JARGAN_MAP = new HashMap(4);

    /** INTENT NAMES **/
    private static final String NICK_NAME_INTENT = "NickNameCapture";
    private static final String GAME_NAME_INTENT = "GameNameCapture";
    private static final String GAME_LEVEL_INTENT = "GameLevelCapture";
    private static final String GAME_RESULT_INTENT = "GameStarted";
    private static final String GAME_RESUME_INTENT = "Resume";
    private static final String SAVED_GAME_START_INTENT = "SavedGameStart";
    private static final String GAME_SCORE_INTENT = "GetScoreIntent";

    /** SLOT NAMES **/
    private static final String NAME_INTENT_SLOT = "USNickName";
    private static final String GAME_NAME_INTENT_SLOT = "GameNames";
    private static final String GAME_LEVEL_INTENT_SLOT = "GameLevel";
    private static final String GAME_LEVEL_RESULT_INTENT_SLOT = "GameResult";
    private static final String GAME_STATUS_INTENT_SLOT = "GameResume";


    /** SESSION ATTRIBUTE NAMES **/
    private static final String USER_NAME_SESSION_ATTRIBUTE = "userName";
    private static final String GAME_LEVEL_SESSION_ATTRIBUTE = "gameLevel";
    private static final String GAME_NAME_SESSION_ATTRIBUTE = "gameName";
    private static final String GAME_TYPE_RESULT_SESSION_ATTRIBUTE = "result";
    private static final String GAME_POINTS_SESSION_ATTRIBUTE = "points";
    private static final String USER_DATA_SESSION_ATTRIBUTE = "userData";
    private static final String CURRENT_GAME_NAME_SESSION_ATTRIBUTE = "currentGameName";
    private static final String GAME_STATE_SESSION_ATTRIBUTE = "GameState";

    private Map getGameJarganMap() {
        if (GAME_JARGAN_MAP.size() != 4) {
            GAME_JARGAN_MAP.put(GameType.ADDITION.name(), "added to");
            GAME_JARGAN_MAP.put(GameType.SUBTRACTION.name(), "minus");
            GAME_JARGAN_MAP.put(GameType.MULTIPLICATION.name(), "multiplied by");
            GAME_JARGAN_MAP.put(GameType.DIVISION.name(), "divided");
        }
        return GAME_JARGAN_MAP;
    }

    private SpeechletResponse getWelcomeResponse() {
        String speechText = "Welcome to Numbers Wizard!! The skill that challenges yours Numbering skill on four areas, Addition, Subtraction, Multiplication and Division ." +
                "You will be scoring points for each correct answer and 0 points for a wrong answer. Based on your points you will be ranked and also achieve the badges starting from Newbie," +
                "Novice,Graduate,Expert,Master and Guru.  But first, I would like to get to know you better. Tell me your nickname by saying, My nickname is, and then your nickname.";
        return getAskResponse(CARD_TITLE, speechText, NICKNAME_REPROMPT);
    }

    private SpeechletResponse getHelpResponse( String speechText, String reprompt) {
        reprompt = StringUtils.isBlank(reprompt)? null: reprompt;
        return getAskResponse(CARD_TITLE, speechText, reprompt);
    }

    @Override public void onSessionStarted(SessionStartedRequest sessionStartedRequest, Session session)
            throws SpeechletException {
        logger.info("onSessionStarted requestId={}, sessionId={}",sessionStartedRequest.getRequestId(),session.getSessionId());

        String usrId = session.getUser().getUserId();//getAttribute(USER_ID_SESSION_ATTRIBUTE);
        List<NumberWizardModel> userDataList = AwsServiceHelper.getSavedGame(usrId);
        if (userDataList.size() >0) {
            session.setAttribute(USER_DATA_SESSION_ATTRIBUTE, userDataList);
            String nickName = userDataList.get(0).getNickname();
            session.setAttribute(USER_NAME_SESSION_ATTRIBUTE, nickName);
        }
    }

    @Override public SpeechletResponse onLaunch(LaunchRequest launchRequest, Session session)
            throws SpeechletException {
        String usrId = session.getUser().getUserId();//getAttribute(USER_ID_SESSION_ATTRIBUTE);
        List<NumberWizardModel> userDataList = AwsServiceHelper.getSavedGame(usrId);
        if(userDataList.size() > 0) {

            session.setAttribute(USER_DATA_SESSION_ATTRIBUTE, userDataList);
            String nickName = userDataList.get(0).getNickname();
            session.setAttribute(USER_NAME_SESSION_ATTRIBUTE,nickName);

            return getAskResponse(CARD_TITLE, String.format(GAME_RESUME_TEXT,nickName), null);

        } else {
            return getWelcomeResponse();
        }
    }

    @Override public SpeechletResponse onIntent(IntentRequest intentRequest, Session session)
            throws SpeechletException {

        String userName = (String) session.getAttribute(USER_NAME_SESSION_ATTRIBUTE);
        try {
            Intent intent = intentRequest.getIntent();
            String intentName = (intent != null) ? intent.getName() : "OTHERS";
            logger.info("onIntent requestId={}, sessionAttributes={} with intent={}", intentRequest.getRequestId(), session.getAttributes(), intentName);

            String gameState = (String)session.getAttribute(GAME_STATE_SESSION_ATTRIBUTE);

            if (null != gameState && gameState.equalsIgnoreCase(GameSate.RESULT.name()) && !GAME_RESULT_INTENT.equalsIgnoreCase(intentName)) {
                intentName = GAME_RESULT_INTENT;
            }

            if (NICK_NAME_INTENT.equals(intentName)) { // START of intent first time
                userName = (StringUtils.isNotBlank(userName)? userName : intent.getSlot(NAME_INTENT_SLOT).getValue());
                session.setAttribute(USER_NAME_SESSION_ATTRIBUTE, userName);

                return getAskResponse(CARD_TITLE, String.format(GAME_PLAY_TEXT, userName), null);

            } else if (GAME_NAME_INTENT.equals(intentName)) { // Choose game type (+,-,*,/) intent
                String gameName = intent.getSlot(GAME_NAME_INTENT_SLOT).getValue();
                session.setAttribute(GAME_NAME_SESSION_ATTRIBUTE, gameName.toUpperCase());
                logger.info("onIntent gameName={}", gameName);

                return getAskResponse(CARD_TITLE, GAME_LEVEL_TEXT, null);

            } else if (GAME_LEVEL_INTENT.equals(intentName)) { // Choose game level (high,medium,easy) intent
                String gameLevel = intent.getSlot(GAME_LEVEL_INTENT_SLOT).getValue();

                return getAskResponse(CARD_TITLE, gameResponseIntent(session, gameLevel), null);

            } else if (GAME_RESULT_INTENT.equals(intentName)) { // result section of the game
                String response, reprompt = null;
                int gamePoint =0;
                session.setAttribute(GAME_STATE_SESSION_ATTRIBUTE, GameSate.RESULT.name());
                try {
                    String userGameResultValue = intent.getSlot(GAME_LEVEL_RESULT_INTENT_SLOT).getValue();

                    String gameName = (String) session.getAttribute(GAME_NAME_SESSION_ATTRIBUTE);
                    String gameLevel = (String) session.getAttribute(GAME_LEVEL_SESSION_ATTRIBUTE);
                    List<Object> userDataList = (List<Object>)session.getAttribute(USER_DATA_SESSION_ATTRIBUTE);
                    if(null != userDataList && userDataList.size() > 0 && null == session.getAttribute(GAME_POINTS_SESSION_ATTRIBUTE)) {
                        gamePoint = getTheCurrentGameScore(userDataList, gameName.toUpperCase() + PointsMappingService.SEPARATOR + gameLevel);
                        session.setAttribute(GAME_POINTS_SESSION_ATTRIBUTE, gamePoint);
                    }

                    int actualGameResult = (Integer) session.getAttribute(GAME_TYPE_RESULT_SESSION_ATTRIBUTE);
                    logger.info("onIntent {} gameName={}, gameLevel={}, actualGameResult={}", GAME_RESULT_INTENT, gameName, gameLevel, actualGameResult);

                    if (null == gameName) {
                        gameName = GameType.ADDITION.name();
                        session.setAttribute(GAME_NAME_SESSION_ATTRIBUTE, gameName);
                    }

                    if (null == gameLevel) {
                        gameLevel = GameLevel.easy.name();
                        session.setAttribute(GAME_LEVEL_SESSION_ATTRIBUTE, gameLevel);
                    }

                    Triple triple = MathHelperService.getTheGameForLevel(gameName, gameLevel);
                    session.setAttribute(GAME_TYPE_RESULT_SESSION_ATTRIBUTE, triple.getRight());

                    if (String.valueOf(actualGameResult).equals(userGameResultValue)) { // if answer is correct
                        if (null != session.getAttribute(GAME_POINTS_SESSION_ATTRIBUTE)) {
                            gamePoint = (Integer)session.getAttribute(GAME_POINTS_SESSION_ATTRIBUTE);
                        }
                        gamePoint = getWinningScore(gameName.toUpperCase() + PointsMappingService.SEPARATOR + gameLevel, gamePoint);
                        //INFO add and update the score
                        session.setAttribute(GAME_POINTS_SESSION_ATTRIBUTE, gamePoint);
                        session.setAttribute(CURRENT_GAME_NAME_SESSION_ATTRIBUTE, gameName.toUpperCase() + PointsMappingService.SEPARATOR + gameLevel);

                        saveRecordToDb(session);
                        response = String.format(GAME_RESULT_CORRECT_TEXT + CONTINUE_GAME_TEXT, actualGameResult, triple.getLeft(), getGameJarganMap().get(gameName.toUpperCase()), triple.getMiddle());
                        reprompt = String.format(CONTINUE_GAME_TEXT,triple.getLeft(), getGameJarganMap().get(gameName.toUpperCase()), triple.getMiddle());

                    } else {
                        response = String.format(GAME_RESULT_WRONG_TEXT + CONTINUE_GAME_TEXT, actualGameResult, triple.getLeft(), getGameJarganMap().get(gameName.toUpperCase()), triple.getMiddle());
                        reprompt = String.format(CONTINUE_GAME_TEXT, triple.getLeft(), getGameJarganMap().get(gameName.toUpperCase()), triple.getMiddle());
                    }
                } catch (NumberFormatException | ClassCastException e) {
                    response = REPEAT_RESPONSE_TEXT;
                }

                return getAskResponse(CARD_TITLE, response, reprompt);

            } else if (GAME_RESUME_INTENT.equals(intentName)) {
                //When player wants to go to a new game or resume the old game

                String userGameStatus = intent.getSlot(GAME_STATUS_INTENT_SLOT).getValue();
                String userNickName = (String) session.getAttribute(USER_NAME_SESSION_ATTRIBUTE);
                if ("resume".equalsIgnoreCase(userGameStatus) || "saved".equalsIgnoreCase(userGameStatus)) {

                    logger.info("Inside GAME_RESUME_INTENT Resume Game");
                    List<Object> gameList = (List<Object>) session.getAttribute(USER_DATA_SESSION_ATTRIBUTE);
                    int count = 0;
                    String gameNames = "";

                    if(gameList.size() ==1) {//if only one game is saved
                        Map numWiz = (HashMap) gameList.get(0);
                        String [] savedGamesArray = ((String) numWiz.get("saved_games")).split("\\.");
                        String gameType = savedGamesArray[0];
                        String gameLevel = savedGamesArray[1];
                        return getAskResponse(CARD_TITLE, startTheExistingGame(gameType, gameLevel, session, GAME_START_TEXT), null);
                    }

                    for (Object model : gameList) {
                        Map numWiz = (HashMap) model;
                        count++;
                        String [] savedGamesArray = ((String) numWiz.get("saved_games")).split("\\.");
                        String gameType = savedGamesArray[0];
                        String gameLevel = savedGamesArray[1];
                        gameNames = gameNames + "," + String.format(GAME_MESSAGE_TEXT, String.valueOf(count), gameType, gameLevel);

                    }
                    return getAskResponse(CARD_TITLE, gameNames + SAVED_GAME_START_TEXT, null);

                } else {//show the intent to start the new game
                    logger.info("Inside GAME_RESUME_INTENT New Game");
                    return getAskResponse(CARD_TITLE, String.format(GAME_PLAY_TEXT, userNickName), null);
                }

            } else if (SAVED_GAME_START_INTENT.equals(intentName)) {
                //INFO take the game name and level, and begin the saved game
                String savedGameName = intent.getSlot(GAME_NAME_INTENT_SLOT).getValue();
                String savedGameLevel = intent.getSlot(GAME_LEVEL_INTENT_SLOT).getValue();

                return getAskResponse(CARD_TITLE, startTheExistingGame(savedGameName, savedGameLevel, session, GAME_START_TEXT), null);

            } else if (GAME_SCORE_INTENT.equals(intentName)) {
                Integer gamePoint = (Integer) session.getAttribute(GAME_POINTS_SESSION_ATTRIBUTE);
                String gameName = (String) session.getAttribute(GAME_NAME_SESSION_ATTRIBUTE);
                String gameLevel = (String) session.getAttribute(GAME_LEVEL_SESSION_ATTRIBUTE);

                String badge = PointsMappingService.getBadge(gamePoint);
                logger.info("GAME_SCORE_INTENT Points={} , gameName={} , gameLevel={} and badge={} ", gamePoint, gameName, gameLevel, badge);

                Triple triple = MathHelperService.getTheGameForLevel(gameName, gameLevel);
                session.setAttribute(GAME_TYPE_RESULT_SESSION_ATTRIBUTE, triple.getRight());
                String continueGame = String.format(CONTINUE_GAME_TEXT, triple.getLeft(), getGameJarganMap().get(gameName.toUpperCase()), triple.getMiddle());
                String score = String.format(GAME_SCORE_TEXT, gameName, gameLevel, gamePoint, badge);

                return getAskResponse(CARD_TITLE, score + continueGame, null);

            } else if ("AMAZON.HelpIntent".equals(intentName) || "AMAZON.NoIntent".equals(intentName)) {
                return getWelcomeResponse();

            } else if ("AMAZON.StopIntent".equals(intentName) || "AMAZON.CancelIntent".equals(intentName)) {
                logger.info("Inside intentName={}", intentName);
                Integer totalScore = (Integer) session.getAttribute(GAME_POINTS_SESSION_ATTRIBUTE);
                totalScore = totalScore == null? 0:totalScore;
                String badge = PointsMappingService.getBadge(totalScore);

                String goodByeMessage = String.format(GAME_EXIT_MESSAGE, totalScore, badge);
                logger.info("Good Bye Message {}", goodByeMessage);
                saveRecordToDb(session);

                SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
                outputSpeech.setSsml("<speak>" + goodByeMessage + "</speak>");
                return SpeechletResponse.newTellResponse(outputSpeech);

            } else {
                logger.info("Inside intentName={}", "OTHERS");
                String currentGameName = (String) session.getAttribute(CURRENT_GAME_NAME_SESSION_ATTRIBUTE);
                String gameLevel = (String) session.getAttribute(GAME_LEVEL_SESSION_ATTRIBUTE);
                if(StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(currentGameName) && null == gameState) {
                    return getAskResponse(CARD_TITLE, REPEAT_RESPONSE_TEXT, null);
                } else if (null != gameState && gameState.equalsIgnoreCase(GameSate.RESULT.name())) {
                    return getAskResponse(CARD_TITLE, REPEAT_RESPONSE_TEXT, null);
                } else if (null != gameState && gameState.equalsIgnoreCase(GameSate.INPROGRESS.name())) {
                    return getAskResponse(CARD_TITLE, gameResponseIntent(session, gameLevel), null);
                } else {
                    return getWelcomeResponse();
                }
            }
        } catch (Exception e) {
            logger.error("There was an exception while processing the Intent {}", e.getStackTrace());
            String gameName = (String) session.getAttribute(GAME_NAME_SESSION_ATTRIBUTE);
            String gameLevel = (String) session.getAttribute(GAME_LEVEL_SESSION_ATTRIBUTE);

            if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(gameName) && StringUtils.isNotBlank(gameLevel)) {
                return getAskResponse(CARD_TITLE, "Sorry! There was a problem. " + startTheExistingGame(gameName, gameLevel, session, GAME_RESTART_TEXT), null);
            } else {
                return getHelpResponse("Sorry! There was a problem. Restarting the game again!!. Please let me know your nickname.", NICKNAME_REPROMPT);
            }
        }
    }

    private String gameResponseIntent(Session session, String gameLevel) {
        if (StringUtils.isBlank(gameLevel)) {
            gameLevel = (String)session.getAttribute(GAME_LEVEL_SESSION_ATTRIBUTE);
        }
        session.setAttribute(GAME_LEVEL_SESSION_ATTRIBUTE, gameLevel);
        logger.info("onIntent gameLevel={}", gameLevel);

        String gameName = (String) session.getAttribute(GAME_NAME_SESSION_ATTRIBUTE);

        if (null == gameName) {
            gameName = GameType.ADDITION.name();
            session.setAttribute(GAME_NAME_SESSION_ATTRIBUTE, gameName.toUpperCase());
        }

        Triple triple = MathHelperService.getTheGameForLevel(gameName, gameLevel);
        session.setAttribute(GAME_TYPE_RESULT_SESSION_ATTRIBUTE, triple.getRight());
        session.setAttribute(CURRENT_GAME_NAME_SESSION_ATTRIBUTE, gameName + PointsMappingService.SEPARATOR + gameLevel);
        Integer points = PointsMappingService.getPointGameMapping().get(gameName + PointsMappingService.SEPARATOR + gameLevel);

        session.setAttribute(GAME_STATE_SESSION_ATTRIBUTE, GameSate.INPROGRESS.name());
        return String.format(GAME_START_TEXT, gameName, points, triple.getLeft(), getGameJarganMap().get(gameName.toUpperCase()), triple.getMiddle());
    }

    private String startTheExistingGame(String gameName, String gameLevel, Session session, String message) {
        session.setAttribute(GAME_NAME_SESSION_ATTRIBUTE, gameName);
        session.setAttribute(GAME_LEVEL_SESSION_ATTRIBUTE, gameLevel);

        Integer points = PointsMappingService.getPointGameMapping().get(gameName.toUpperCase() + PointsMappingService.SEPARATOR + gameLevel);

        Triple triple = MathHelperService.getTheGameForLevel(gameName, gameLevel);
        session.setAttribute(GAME_TYPE_RESULT_SESSION_ATTRIBUTE, triple.getRight());

        return String.format(message, gameName, points, triple.getLeft(), getGameJarganMap().get(gameName.toUpperCase()), triple.getMiddle());
    }

    private int getTheCurrentGameScore(List<Object> gameList, String savedGameName) {
        int currentGameScore = 0;
        for (Object model : gameList) {
            Map numWiz = (HashMap) model;
            if(((String) numWiz.get("saved_games")).equalsIgnoreCase(savedGameName)) {
                currentGameScore = (int) numWiz.get("profile_score");
            }
        }
        return currentGameScore;
    }

    private int getWinningScore(String gameNameAndLevel, Integer currentScore) {
        if (null == currentScore) {
            currentScore = 0;
        }
        int points;
        Map<String, Integer> pointsForGameMapping = PointsMappingService.getPointGameMapping();
        points = pointsForGameMapping.get(gameNameAndLevel);
        logger.info("Inside getWinningScore method, for game name and level {}", gameNameAndLevel);
        return points+currentScore;
    }

    @Override public void onSessionEnded(SessionEndedRequest sessionEndedRequest, Session session)
            throws SpeechletException {

        logger.info("onSessionEnded requestId={}, sessionId={}",sessionEndedRequest.getRequestId(),session.getSessionId());
        saveRecordToDb(session);


    }

    private void saveRecordToDb(Session session) {
        logger.info("Inside Save Record to db");
        //INFO get the score and the update the db, also calculate the badge
        String currentGame = (String) session.getAttribute(CURRENT_GAME_NAME_SESSION_ATTRIBUTE);
        String userId = session.getUser().getUserId();
        Integer totalScore = (Integer) session.getAttribute(GAME_POINTS_SESSION_ATTRIBUTE);
        String nickName = (String) session.getAttribute(USER_NAME_SESSION_ATTRIBUTE);

        if (StringUtils.isNotBlank(currentGame) && StringUtils.isNotBlank(userId) && StringUtils.isNotBlank(nickName)) {
            NumberWizardModel numberWizardModel = new NumberWizardModel();
            numberWizardModel.setUser_id(userId);
            numberWizardModel.setSaved_games(currentGame);
            numberWizardModel.setNickname(nickName);
            numberWizardModel.setProfile_score((null == totalScore) ? 0 : totalScore);
            numberWizardModel.setProfile_badge(PointsMappingService.getBadge(totalScore));
            AwsServiceHelper.updateDataIntoDb(numberWizardModel);
            logger.info("Saved Record to db", numberWizardModel, session.getSessionId());
        }
    }

    private SpeechletResponse getAskResponse(String cardTitle, String speechText, String repromptString) {
        StandardCard standardCard = new StandardCard();

        if (StringUtils.isNotBlank(AwsServiceHelper.getImageUrl())) {
            String imageUrl = AwsServiceHelper.getImageUrl();
            if(!imageUrl.contains("https")) {
                imageUrl = "https" + imageUrl.substring(4);
            }
            logger.info("ImageUrl={}",imageUrl);
            Image image = new Image();
            image.setLargeImageUrl(imageUrl);
            image.setSmallImageUrl(imageUrl);
            standardCard.setImage(image);
            standardCard.setTitle(cardTitle);
        }


        SsmlOutputSpeech outputSpeech = new SsmlOutputSpeech();
        outputSpeech.setSsml("<speak>" + speechText + "</speak>");

        SsmlOutputSpeech repromptOutputSpeech = new SsmlOutputSpeech();

        if (StringUtils.isNotBlank(repromptString)) {
            repromptOutputSpeech.setSsml("<speak>" + repromptString + "</speak>");
        } else {
            repromptOutputSpeech.setSsml("<speak>" + speechText + "</speak>");
        }

        Reprompt reprompt = getReprompt(repromptOutputSpeech);

        return SpeechletResponse.newAskResponse(outputSpeech, reprompt,standardCard);
    }

    private Reprompt getReprompt(OutputSpeech outputSpeech) {
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(outputSpeech);

        return reprompt;
    }

    private PlainTextOutputSpeech getPlainTextOutputSpeech(String speechText) {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return speech;
    }

    private SimpleCard getSimpleCard(String title, String content) {
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(content);

        return card;
    }
}
