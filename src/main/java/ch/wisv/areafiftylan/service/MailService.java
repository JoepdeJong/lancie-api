package ch.wisv.areafiftylan.service;

import ch.wisv.areafiftylan.dto.MailDTO;
import ch.wisv.areafiftylan.model.Team;
import ch.wisv.areafiftylan.model.User;

import javax.mail.MessagingException;

public interface MailService {

    void sendMail(String recipientEmail, String recipientName, String senderEmail, String subject, String message)
            throws MessagingException;

    void sendTemplateMailToTeam(Team team, MailDTO mailDTO) throws MessagingException;

    void sendTemplateMailToAll(MailDTO mailDTO);

    void sendTemplateMailToUser(User user, MailDTO mailDTO) throws MessagingException;
}
