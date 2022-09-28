package com.lordnoisy.swanseaauthenticator;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailSender {
    private final static String EMAIL_PART_ONE = """
<html lang="en">
    <body>
        <style>
        .sideColumn {
            width: 65px;
        }
        .sideRow {
            height: 65px;
        }
        #middleRow {
            min-height:500px;
        }
        #table {
            width:100vw;
            height:1px;
            min-height:500px;
            max-width: 730px;
            background-color:#E4E6BC;
            border-radius:20px;
        }
        #centreHeader {
            background-color: #5952FF;
            height:auto;
        }
        #centreMain {
            background-color: #2B2B2B;
            height:100%;
            display: flex;
            flex-direction: column;
            justify-content: center;
        }
        #centreTable {
            max-height:100%;
            min-height:500px;
            height:1px;
            overflow:auto;
            border:none;
            font-family:Calibri, sans-serif; font-size:larger;
            color: whitesmoke;
            background-image: url("https://via.placeholder.com/10x10/E4E6BC?text=%E2%80%8E");
            background-repeat: repeat;
        }
        #headerLogo {
            max-width: 90%;
            max-height: 100%;
        }
        #headerRow {
            height:100px;
        }
        #table {
            font-family:Calibri, sans-serif; font-size:larger;
            color: whitesmoke;
            background-image: url("https://via.placeholder.com/10x10/E4E6BC?text=%E2%80%8E");
            background-repeat: repeat;
        }
        #mainText {
            text-align: center;
        }
        .cornerImg {
            border-radius: 20px;
        }
                    </style>
                    <table id="table">
                        <tr class="sideRow">
                            <th class="sideColumn">
                                <img class="cornerImg" src="https://via.placeholder.com/65x65/E4E6BC?text=%E2%80%8E";>
                            </th>
                            <th>
                                <img id="topImg" src="https://via.placeholder.com/500x65/E4E6BC?text=%E2%80%8E";>
                            </th>
                            <th class="sideColumn">
                                <img class="cornerImg" src="https://via.placeholder.com/65x65/E4E6BC?text=%E2%80%8E";>
                            </th>
                        </tr>
                        <tr id="middleRow">
                            <td>
                                <img class="side" src="https://via.placeholder.com/65x65/E4E6BC?text=%E2%80%8E";>
                            </td>
                            <td id="centreCell">
                                <table id="centreTable" cellspacing="0" cellpadding="0">
                                    <tr id="headerRow">
                                        <th id="centreHeader">
                                            <img id="headerLogo" alt="The SwanAuth Logo" src="https://media.discordapp.net/attachments/1018285865312727091/1023085000926699561/bannerlogo.png?width=1288&height=264"/>
                                        </th>
                                    </tr>
                                    <tr>
                                        <td id="centreMain">
                                            <p id="mainText">
                                    """;
    private final static String EMAIL_PART_TWO = """
                                    </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                            <td>
                                <img class="side" src="https://via.placeholder.com/65x65/E4E6BC?text=%E2%80%8E";>
                            </td>
                        </tr>
                        <tr class="sideRow">
                            <td>
                                <img class="cornerImg" src="https://via.placeholder.com/65x65/E4E6BC?text=%E2%80%8E";>
                            </td>
                            <td>
                                <img id="bottomImg" src="https://via.placeholder.com/500x65/E4E6BC?text=%E2%80%8E";>
                            </td>
                            <td>
                                <img class="cornerImg" src="https://via.placeholder.com/65x65/E4E6BC?text=%E2%80%8E";>
                            </td>
                        </tr>
                    </table>
                </body>
            </html>""";

    private final String host;
    private final int port;
    private final String username;
    private final String password;
    private final String senderEmail;

    /**
     * Constructor for EmailSender
     *
     * @param host        the email server
     * @param port        the port
     * @param username    the email address username
     * @param password    the email address password
     * @param senderEmail the actual email address
     */
    public EmailSender(String host, int port, String username, String password, String senderEmail) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.senderEmail = senderEmail;
    }

    /**
     * Send the verification email
     *
     * @param studentNumber      a swansea based student number
     * @param verificationString a random generated string to use as verification
     */
    public boolean sendVerificationEmail(String studentNumber, String verificationString, String guildName) {
        String studentEmail = studentNumber + "@swansea.ac.uk";
        String subjectHeader = guildName + " Discord Server Verification";
        String body = EMAIL_PART_ONE + "Your verification code for the \"" + guildName + "\" Discord server is: " +
                verificationString + ".<br><br> To finish your verification, use the /verify command by" +
                " typing in chat the following: /verify " + verificationString + EMAIL_PART_TWO;

        return this.sendMail(studentEmail, subjectHeader, body);
    }

    /**
     * Send an email
     *
     * @param to      the recipient
     * @param subject the subject header
     * @param content the email content/body
     */
    public boolean sendMail(String to, String subject, String content) {
        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.socketFactory.port", port);
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "true");
        props.put("mail.smtp.ssl.trust", host);

        // Create the Session Object
        Session session = Session.getDefaultInstance(
                props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                }
        );
        //session.setDebug(true);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setReplyTo(InternetAddress.parse(senderEmail));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");
            Transport.send(message);
            return true;
        } catch (Exception exc) {
            exc.printStackTrace();
            return false;
        }
    }
}
