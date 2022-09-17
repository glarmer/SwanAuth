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
    public boolean sendVerificationEmail(String studentNumber, String verificationString) {
        String studentEmail = studentNumber + "@swansea.ac.uk";
        String subjectHeader = "Swancord Verification";
        String body = "Hi, your Swancord verification code is: " + verificationString + ". To finish your verification reply to the bot by" +
                " saying \"$verify " + verificationString + "\"";

        this.sendMail(studentEmail,subjectHeader,body);

        return true;
    }

    /**
     * Send an email
     * @param to the recipient
     * @param subject the subject header
     * @param content the email content/body
     */
    public void sendMail( String to, String subject, String content ) {
        // Set Properties
        Properties props = new Properties();
        props.put( "mail.smtp.auth", "true" );
        props.put( "mail.smtp.host", host );
        props.put( "mail.smtp.port", port );
        props.put( "mail.smtp.starttls.enable", "true" );
        props.put( "mail.debug", debug );
        props.put( "mail.smtp.socketFactory.port", port );
        props.put( "mail.smtp.socketFactory.fallback", "false" );
        props.put( "mail.smtp.ssl.trust", host );

        //props.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");

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
            //Finally, send the email
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setReplyTo(InternetAddress.parse(senderEmail));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setContent(content, "text/html; charset=utf-8");
            Transport.send(message);
        }
        catch(MessagingException exc) {
            exc.printStackTrace();
        }
    }
}
