package ch.wisv.areafiftylan.service;

import ch.wisv.areafiftylan.exception.InvalidRFIDException;
import ch.wisv.areafiftylan.exception.RFIDNotFoundException;
import ch.wisv.areafiftylan.exception.RFIDTakenException;
import ch.wisv.areafiftylan.exception.TicketAlreadyLinkedException;
import ch.wisv.areafiftylan.model.Ticket;
import ch.wisv.areafiftylan.model.relations.RFIDLink;
import ch.wisv.areafiftylan.service.repository.RFIDLinkRepository;
import ch.wisv.areafiftylan.service.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Collection;

import static ch.wisv.areafiftylan.util.ResponseEntityBuilder.createResponseEntity;

/**
 * Created by beer on 5-5-16.
 */
@Service
public class RFIDServiceImpl implements RFIDService{
    @Autowired
    private RFIDLinkRepository rfidLinkRepository;

    @Autowired
    private TicketService ticketService;

    @Override
    public Collection<RFIDLink> getAllRFIDLinks() {
        return rfidLinkRepository.findAll();
    }

    @Override
    public Long getTicketIdByRFID(String rfid) {
        if(!RFIDLink.isValidRFID(rfid)){
            throw new InvalidRFIDException(rfid);
        }

        return getLinkByRFID(rfid).getTicket().getId();
    }

    @Override
    public String getRFIDByTicketId(Long ticketId) {
        return getLinkByTicketId(ticketId).getRFID();
    }

    @Override
    public boolean isRFIDUsed(String rfid) {
        return rfidLinkRepository.findByRfid(rfid).isPresent();
    }

    @Override
    public boolean isTicketLinked(Long ticketId) {return rfidLinkRepository.findByTicketId(ticketId).isPresent(); }

    @Override
    public void addRFIDLink(String rfid, Long ticketId) {
        if(isRFIDUsed(rfid)){
            throw new RFIDTakenException(rfid);
        }

        if(isTicketLinked(ticketId)){
            throw new TicketAlreadyLinkedException();
        }

        Ticket t = ticketService.getTicketById(ticketId);

        RFIDLink newLink = new RFIDLink(rfid, t);

        rfidLinkRepository.saveAndFlush(newLink);
    }

    public RFIDLink getLinkByRFID(String rfid) {
        return rfidLinkRepository.findByRfid(rfid)
                .orElseThrow(() -> new RFIDNotFoundException());
    }

    public RFIDLink getLinkByTicketId(Long ticketId) {
        return rfidLinkRepository.findByTicketId(ticketId)
                .orElseThrow(() -> new RFIDNotFoundException());
    }
}
