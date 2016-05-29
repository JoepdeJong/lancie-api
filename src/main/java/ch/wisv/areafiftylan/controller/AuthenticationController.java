package ch.wisv.areafiftylan.controller;

import ch.wisv.areafiftylan.dto.UserDTO;
import ch.wisv.areafiftylan.exception.InvalidTokenException;
import ch.wisv.areafiftylan.exception.TokenNotFoundException;
import ch.wisv.areafiftylan.exception.UserNotFoundException;
import ch.wisv.areafiftylan.model.User;
import ch.wisv.areafiftylan.security.token.PasswordResetToken;
import ch.wisv.areafiftylan.security.token.VerificationToken;
import ch.wisv.areafiftylan.service.AuthenticationService;
import ch.wisv.areafiftylan.service.UserService;
import ch.wisv.areafiftylan.service.repository.token.PasswordResetTokenRepository;
import ch.wisv.areafiftylan.service.repository.token.VerificationTokenRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import static ch.wisv.areafiftylan.util.ResponseEntityBuilder.createResponseEntity;


/**
 * This class handles all authentication related requests. These requests deal with the login message, the user email
 * verification and the password resets. Due to the minimal logic involved with tokens, it interacts directly with their
 * respective repositories.
 */
@Controller
@Log4j2
public class AuthenticationController {

    private UserService userService;
    private AuthenticationService authenticationService;

    private VerificationTokenRepository verificationTokenRepository;
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    public AuthenticationController(UserService userService, AuthenticationService authenticationService,
                                    VerificationTokenRepository verificationTokenRepository,
                                    PasswordResetTokenRepository passwordResetTokenRepository) {
        this.userService = userService;
        this.authenticationService = authenticationService;

        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    /**
     * This basic GET method for the /login endpoint returns a simple login form.
     *
     * @return Login view
     */
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView getLoginPage() {
        return new ModelAndView("loginForm");
    }

    @RequestMapping(value = "/token", method = RequestMethod.GET)
    public ResponseEntity<?> checkSession() {
        return createResponseEntity(HttpStatus.OK, "Here's your token!");
    }

    @RequestMapping(value = "/token", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody UserDTO userDTO) {
        String authToken = authenticationService.createNewAuthToken(userDTO.getUsername(), userDTO.getPassword());

        return createResponseEntity(HttpStatus.OK, "Token successfully created", authToken);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(value = "/token/verify", method = RequestMethod.GET)
    public ResponseEntity<?> verifyToken() {
        return createResponseEntity(HttpStatus.OK, "Token is valid!");
    }

    /**
     * This method requests a passwordResetToken and sends it to the user. With this token, the user can reset his
     * password.
     *
     * @param request The HttpServletRequest of the call
     * @param body    The body, should only contain an email parameter TODO: This can be done more elegantly
     *
     * @return A status message telling whether the action was successful
     */
    @RequestMapping(value = "/requestResetPassword", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<?> requestResetPassword(HttpServletRequest request, @RequestBody Map<String, String> body) {
        String email = body.get("email");

        log.log(Level.getLevel("A5L"), "Requesting password reset on email {}.", email);

        User user = userService.getUserByEmail(email).orElseThrow(() -> new UserNotFoundException(email));

        userService.requestResetPassword(user, request);

        log.log(Level.getLevel("A5L"), "Successfully requested password reset on email {}.", email);

        return createResponseEntity(HttpStatus.OK, "Password reset link sent to " + email);
    }

    /**
     * After requesting a token, the user can reset his password with a POST call to this method. It should contain the
     * token and a password. The user is derived from the token.
     *
     * @param body The body, containing a token and password parameter. TODO: This should be validated
     *
     * @return A status message telling whethere the action was successful
     */
    @RequestMapping(value = "/resetPassword", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) throws InvalidTokenException {
        String token = body.get("token");
        String password = body.get("password");

        PasswordResetToken passwordResetToken =
                passwordResetTokenRepository.findByToken(token).orElseThrow(() -> new TokenNotFoundException(token));

        //Check validity of the token
        if (!passwordResetToken.isValid()) {
            throw new InvalidTokenException();
        }

        // Get the user associated to the token
        User user = passwordResetToken.getUser();
        userService.resetPassword(user.getId(), password);

        // Disable the token for future use.
        passwordResetToken.use();
        passwordResetTokenRepository.saveAndFlush(passwordResetToken);

        return createResponseEntity(HttpStatus.OK, "Password set!");
    }

    /**
     * This function confirms the registration of a user based on the provided token. If retrieves the token from the
     * repository. From this token, we get the user. For this user, the enabled flag is set to true. After this, the
     * token is marked as used so it can not be used again.
     * <p>
     * TODO: Token logic could be moved to a dedicated service
     *
     * @param token The token as generated for the user opon registration
     *
     * @return A status message containing information about the operation
     *
     * @throws TokenNotFoundException if the token can't be found
     */
    @RequestMapping(value = "/confirmRegistration", method = RequestMethod.GET)
    public ResponseEntity<?> confirmRegistration(@RequestParam("token") String token)
            throws TokenNotFoundException, InvalidTokenException {

        VerificationToken verificationToken =
                verificationTokenRepository.findByToken(token).orElseThrow(() -> new TokenNotFoundException(token));

        // Get the user associated with this token
        User user = verificationToken.getUser();
        if (!verificationToken.isValid()) {
            throw new InvalidTokenException();
        }

        userService.verify(user.getId());
        verificationToken.use();

        //Disable the token for future use.
        verificationTokenRepository.saveAndFlush(verificationToken);

        return createResponseEntity(HttpStatus.OK, "Succesfully verified");
    }
}
