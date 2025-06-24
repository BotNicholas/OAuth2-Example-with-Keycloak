package org.botnicholas.projects.oauth2demo.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.coyote.BadRequestException;
import org.botnicholas.projects.oauth2demo.controllers.dto.AuthDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
//import org.springframework.web.client.RestTemplate;

import java.security.Principal;
import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    @Value("${client-id}")
    private String clientId;

    @Value("${resource-url}")
    private String resourceServerUrl;

    @Autowired
    ObjectMapper mapper;

//    @PostMapping("/auth")
//    public String auth(@RequestBody AuthDTO authDTO) {
//        var headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
//
//        var body = String.format("client_id=%s&username=%s&password=%s&grant_type=password", clientId, authDTO.login(), authDTO.password());
//
//        var requestEntity = new HttpEntity<>(body, headers);
//
//        var restTemplate = new RestTemplate();
//        var response = restTemplate.exchange(resourceServerUrl, HttpMethod.POST, requestEntity, Map.class);
//
//        if (HttpStatus.OK.equals(response.getStatusCode())) {
//            return response.getBody().toString();
//        }
//
//        return null;
//    }




    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "http://192.168.1.5:7080/realms/Oauth2Demo/protocol/openid-connect/auth?client_id=SpringBootOauth2Demo&redirect_uri=http://192.168.1.5:8080/api/return-token&response_type=code&scope=openid").build();
    }

    @GetMapping("/return-token")
    public ResponseEntity<String> returnCode(@RequestParam String code) throws BadRequestException, JsonProcessingException {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var body = String.format("client_id=%s&code=%s&redirect_uri=%s&grant_type=authorization_code", clientId, code, "http://192.168.1.5:8080/api/return-token");

        var requestEntity = new HttpEntity<>(body, headers);

        var restTemplate = new RestTemplate();
        var response = restTemplate.exchange(resourceServerUrl, HttpMethod.POST, requestEntity, Map.class);

        if (HttpStatus.OK.equals(response.getStatusCode())) {
            var responseString = """
            <script>
                window.opener.postMessage(%s, "*");
            </script>
            """.formatted(mapper.writeValueAsString(response.getBody()));
            return ResponseEntity.ok(responseString);
        }

        throw new BadRequestException("Invalid code");
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody String idToken, Authentication authentication) throws JsonProcessingException {
        var subFromIdToken = mapper.readValue(new String(Base64.getDecoder().decode(idToken.split("\\.")[1])), Map.class).get("sub");
        var subFromAuthentication = ((Jwt)authentication.getPrincipal()).getClaim("sub");
        if (!subFromAuthentication.equals(subFromIdToken)) {
            throw new InvalidBearerTokenException("You're not the owner of this token");
        }

        RestTemplate restTemplate = new RestTemplate();
        var result = restTemplate.getForObject("http://192.168.1.5:7080/realms/Oauth2Demo/protocol/openid-connect/logout?post_logout_redirect_uri=http://192.168.1.5:4200&id_token_hint=" + idToken, String.class);

        return ResponseEntity.noContent().build();
//        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "http://192.168.1.5:7080/realms/Oauth2Demo/protocol/openid-connect/auth?client_id=SpringBootOauth2Demo&redirect_uri=http://192.168.1.5:8080/api/return-token&response_type=code&scope=openid").build();
    }
}
