package com.awscalculator.backend.auth;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.from(registrationId, oAuth2User.getAttributes());
        AuthProvider provider = OAuth2UserInfoFactory.providerOf(registrationId);

        UserService.FindOrCreateResult result = userService.findOrCreate(
                provider,
                userInfo.getProviderId(),
                userInfo.getEmail(),
                userInfo.isEmailVerified(),
                userInfo.getNickname(),
                userInfo.getProfileImageUrl()
        );

        return new CustomOAuth2User(
                result.user(),
                oAuth2User.getAttributes(),
                List.of(new SimpleGrantedAuthority("ROLE_" + result.user().getRole().name()))
        );
    }
}
