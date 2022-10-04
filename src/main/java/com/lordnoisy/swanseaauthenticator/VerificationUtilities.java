package com.lordnoisy.swanseaauthenticator;

import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import reactor.core.publisher.Mono;

import static com.lordnoisy.swanseaauthenticator.Main.*;

public class VerificationUtilities {

    public static String beginVerification(GuildData guildData, String studentNumber, SQLRunner sqlRunner, Member member, EmailSender emailSender, String guildName) {
        String resultToReturn;
        String discordID = member.getId().asString();
        Snowflake guildSnowflake = guildData.getGuildID();
        boolean isServerConfigured = (guildData.getVerifiedRoleID() != null);
        if (isServerConfigured) {
            resultToReturn = BEGIN_COMMAND_SUCCESS_RESULT;
            if (studentNumber.matches("\\d+") && studentNumber.length() < 256) {
                //Check that the user hasn't previously verified under a different student number
                Account account = sqlRunner.getAccountFromDiscordID(discordID);
                String userID;
                String accountID;
                if (account == null) {
                    userID = sqlRunner.getOrCreateUserIDFromStudentID(studentNumber);
                    accountID = sqlRunner.getOrCreateAccountIDFromDiscordIDAndUserID(userID, discordID);
                } else {
                    userID = account.getUserID();
                    accountID = account.getAccountID();
                }
                if (!studentNumber.equals(sqlRunner.getStudentIDFromUserID(userID))) {
                    if (sqlRunner.isVerifiedAnywhere(accountID)) {
                        return VERIFIED_UNDER_ANOTHER_STUDENT_ID_ERROR;
                    } else {
                        // At this point we have a problem
                        // It has found an account ID
                        // But that account ID is linked to a user with a different student number
                        // This could happen if someone erroneously enters the wrong number first time
                        // So now we need to deal with that

                        // We know they aren't verified ANYWHERE yet, so what we can do is
                        // delete their previous tokens
                        // update their account to use the correct userID - studentID

                        String newUserID = sqlRunner.getOrCreateUserIDFromStudentID(studentNumber);
                        if (newUserID == null) {
                            return DATABASE_ERROR;
                        }
                        if (sqlRunner.updateAccount(newUserID, accountID)) {
                            if (sqlRunner.deleteAllVerificationTokens(accountID)) {
                                userID = newUserID;
                            } else {
                                return DATABASE_ERROR;
                            }
                        } else {
                            return DATABASE_ERROR;
                        }
                    }

                }
                if (userID != null) {
                    if (!sqlRunner.isBanned(userID, guildSnowflake.asString())) {
                        if (accountID != null) {
                            if (!sqlRunner.isVerified(accountID, guildSnowflake.asString())) {
                                //Check that there aren't 3 or more verification tokens made within the past 12 hours - discourages spam
                                int rows = sqlRunner.selectRecentVerificationTokens(accountID, guildSnowflake.asString());
                                if (rows == -1) {
                                    resultToReturn = DATABASE_ERROR;
                                } else if (rows < 3) {
                                    String verificationCode = StringUtilities.getAlphaNumericString(StringUtilities.TOKEN_LENGTH);
                                    if (emailSender.sendVerificationEmail(studentNumber, verificationCode, guildName)) {
                                        sqlRunner.insertVerificationToken(accountID, guildSnowflake.asString(), verificationCode);
                                    } else {
                                        resultToReturn = EMAIL_ERROR;
                                    }
                                } else {
                                    resultToReturn = TOO_MANY_ATTEMPTS_ERROR;
                                }
                            } else {
                                resultToReturn = ACCOUNT_ALREADY_VERIFIED_ERROR;
                            }
                        } else {
                            resultToReturn = DATABASE_ERROR;
                        }
                    } else {
                        return USER_IS_BANNED_RESULT;
                    }
                } else {
                    resultToReturn = DATABASE_ERROR;
                }
            } else {
                resultToReturn = INCORRECT_STUDENT_NUMBER_ERROR;
            }
        } else {
            resultToReturn = SERVER_NOT_CONFIGURED_ERROR;
        }
        return resultToReturn;
    }

    public static String finaliseVerification(String tokenInput, boolean isServerConfigured, SQLRunner sqlRunner, String memberID, Snowflake guildSnowflake) {
        String result;
        if (!StringUtilities.isValidPotentialToken(tokenInput)) {
            return INCORRECT_TOKEN_ERROR;
        }
        if (isServerConfigured) {
            Account account = sqlRunner.getAccountFromDiscordID(memberID);
            if (account == null) {
                return HAVE_NOT_BEGUN_ERROR;
            } else {
                String accountID = account.getAccountID();
                if (!sqlRunner.isVerified(accountID, guildSnowflake.asString())) {
                    int rows = sqlRunner.selectVerificationToken(accountID, guildSnowflake.asString(), tokenInput);
                    if (rows > 0) {
                        String guildID = guildSnowflake.asString();
                        sqlRunner.insertVerification(accountID, guildID);
                        sqlRunner.deleteVerificationTokens(accountID, guildID);
                    } else if (rows == -1) {
                        return DATABASE_ERROR;
                    } else {
                        return INCORRECT_TOKEN_ERROR;
                    }
                    return VERIFY_COMMAND_SUCCESS;
                } else {
                    result = ACCOUNT_ALREADY_VERIFIED_ERROR;
                }
            }
        } else {
            result = SERVER_NOT_CONFIGURED_ERROR;
        }
        return result;
    }

}
