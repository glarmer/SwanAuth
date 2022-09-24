package com.lordnoisy.swanseaauthenticator;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailSender {
    private final static String EMAIL_PART_ONE = """
            <html lang="en">
                <body>
                    <div style="font-family:Calibri, sans-serif; font-size:larger; ">
                        <div style="border: solid #E4E6BC 65px; display:flex; justify-content:center; width:90%; height:auto; max-height:500px; max-width: 600px; background-image: url(https://media.discordapp.net/attachments/1018285865312727091/1023247964178763937/emailbackground.png); background-repeat:no-repeat; background-size:cover; background-color: #E4E6BC; border-radius:15px;">
                            <div style="width:100%; height:100%; min-height:500px; max-width: 600px; border: solid #2B2B2B; background-color: #2B2B2B; display:flex; flex-direction:column;">
                                <div style="background-color:#5952FF; padding-left: 30px; padding-right: 30px;">
                                    <img alt="The SwanAuth Logo" height="auto" width="100%" src="https://media.discordapp.net/attachments/1018285865312727091/1023085000926699561/bannerlogo.png?width=1288&height=264"/>
                                </div>
                                <div style="display:flex; justify-content:center; flex-grow:1; flex-direction:column;">
                                    <p style="margin:50px; color:whitesmoke; align-self:center;">""";
    private final static String EMAIL_PART_TWO = """
                                    </p>
                                </div>
                            </div>
                        </div>
                    </div>
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
                        return new PasswordAuthentication(username, password);
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
        } catch (MessagingException exc) {
            exc.printStackTrace();
            return false;
        }
    }
}
