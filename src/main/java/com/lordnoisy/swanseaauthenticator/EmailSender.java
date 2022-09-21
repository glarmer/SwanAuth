package com.lordnoisy.swanseaauthenticator;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailSender {
    private String host;
    private int port;
    private boolean debug;
    private String username;
    private String password;
    private String senderEmail;

    /**
     * Constructor for EmailSender
     * @param host the email server
     * @param port the port
     * @param username the email address username
     * @param password the email address password
     * @param senderEmail the actual email address
     */
    public EmailSender(String host, int port, String username, String password, String senderEmail) {
        this.host = host;
        this.port = port;
        this.debug = true;
        this.username = username;
        this.password = password;
        this.senderEmail = senderEmail;
    }

    /**
     * Send the verification email
     * @param studentNumber a swansea based student number
     * @param verificationString a random generated string to use as verification
     */
    public boolean sendVerificationEmail(String studentNumber, String verificationString, String guildName) {
        String studentEmail = studentNumber + "@swansea.ac.uk";
        String subjectHeader = guildName + " Discord Server Verification";
        String body = "Your verification code for the \"" + guildName + "\" Discord server is: " +
                verificationString + ".<br><br> To finish your verification, use the /verify command by" +
                " typing in chat the following: /verify " + verificationString;

        return this.sendMail(studentEmail, subjectHeader, body);
    }

    /**
     * Send an email
     * @param to the recipient
     * @param subject the subject header
     * @param content the email content/body
     */
    public boolean sendMail( String to, String subject, String content ) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.socketFactory.port", port);
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.ssl.trust", host);

        // Create the Session Object
        Session session = Session.getDefaultInstance(
                props,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password );
                    }
                }
        );
        session.setDebug(true);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setReplyTo(InternetAddress.parse(senderEmail));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");
            Transport.send(message);
            return true;
        }
        catch(MessagingException exc) {
            exc.printStackTrace();
            return false;
        }
    }
}
