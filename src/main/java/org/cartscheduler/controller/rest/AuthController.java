package org.cartscheduler.controller.rest;

import org.cartscheduler.dto.rest.request.RestAuthRequest;
import org.cartscheduler.dto.rest.response.RestAuthResponse;
import org.cartscheduler.entity.ParticipantAccessToken;
import org.cartscheduler.repository.ParticipantAccessTokenRepository;
import org.cartscheduler.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ParticipantAccessTokenRepository participantAccessTokenRepository;

    @PostMapping("/")
    public RestAuthResponse authenticateAndGetToken(@RequestBody RestAuthRequest authRequest) {

        ParticipantAccessToken participantAccessToken = participantAccessTokenRepository.findAccessForToken(authRequest.getInvitationToken());

        if (participantAccessToken != null) {
            RestAuthResponse response = new RestAuthResponse();
            response.setJwtToken(jwtService.generateToken(participantAccessToken.getParticipant().getEmail()));
            response.setExpiresAt(jwtService.getExpirationTime());

            return response;
        } else {
            throw new UsernameNotFoundException("Invalid user request!");
        }
    }
}
