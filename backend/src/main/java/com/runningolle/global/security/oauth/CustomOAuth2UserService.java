package com.runningolle.global.security.oauth;

import com.runningolle.domain.user.entity.User;
import com.runningolle.domain.user.enums.AccountStatus;
import com.runningolle.domain.user.repository.UserRepository;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User kakaoUser = super.loadUser(userRequest);
        Object kakaoIdAttribute = kakaoUser.getAttribute("id");
        if (kakaoIdAttribute == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("missing_kakao_id"),
                    "카카오 사용자 ID가 없습니다."
            );
        }
        String kakaoId = kakaoIdAttribute.toString();

        User user = userRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> userRepository.save(User.createKakaoUser(kakaoId)));

        if (user.getAccountStatus() == AccountStatus.WITHDRAWN) {
            throw new OAuth2AuthenticationException(new OAuth2Error("account_withdrawn"),
                    "탈퇴 처리된 계정입니다.");
        }

        Map<String, Object> attributes = new HashMap<>(kakaoUser.getAttributes());
        attributes.put("localUserId", user.getId().toString());
        attributes.put("onboardingCompleted", user.getOnboardingCompleted());

        return new DefaultOAuth2User(
                java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes,
                "id"
        );
    }
}
