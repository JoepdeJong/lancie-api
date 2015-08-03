package ch.wisv.areafiftylan.controller;

import ch.wisv.areafiftylan.dto.UserDTO;
import ch.wisv.areafiftylan.model.Seat;
import ch.wisv.areafiftylan.model.User;
import ch.wisv.areafiftylan.service.SeatService;
import ch.wisv.areafiftylan.service.UserService;
import org.bouncycastle.cert.ocsp.Req;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Path;
import java.util.Collection;

@RestController
@RequestMapping("/users")
public class UserRestController {


    private UserService userService;

    private SeatService seatService;

    @Autowired
    UserRestController(UserService userService, SeatService seatService) {
        this.userService = userService;
        this.seatService = seatService;
    }

    //////////// USER MAPPINGS //////////////////

    /**
     * This method accepts POST requests on /users. It will create a new user and an empty profile attached to it.
     *
     * @param input The user that has to be created. It consists of 3 fields. The username, the email and the
     *                  plain-text password. The password is saved hashed using the BCryptPasswordEncoder
     * @return The generated object, in JSON format.
     */
    @RequestMapping(method = RequestMethod.POST)
    ResponseEntity<?> add(@Validated @RequestBody UserDTO input) {
        User save = userService.create(input);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(ServletUriComponentsBuilder
                .fromCurrentRequest().path("/{id}")
                .buildAndExpand(save.getId()).toUri());

        return new ResponseEntity<>(save, httpHeaders, HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{userId}", method = RequestMethod.GET)
    User getUserById(@PathVariable Long userId) {
        return this.userService.getUserById(userId).get();
    }

    @RequestMapping(method = RequestMethod.GET)
    Collection<User> readUsers() {
        return userService.getAllUsers();
    }

    @RequestMapping(value = "/{userId}/seat", method = RequestMethod.GET)
    Seat getSeatByUser(@PathVariable Long userId){
        User user = userService.getUserById(userId).get();

        return seatService.getSeatByUser(user);
    }
}