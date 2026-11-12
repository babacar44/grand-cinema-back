package fr.grandcinema.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("cle-de-test-suffisamment-longue-pour-hs256-0123456789", 60);

    @Test
    void unJetonGenereContientLEmail() {
        String token = jwtService.genererToken("marie@exemple.fr", "CLIENT");
        assertThat(jwtService.extraireEmail(token)).isEqualTo("marie@exemple.fr");
    }

    @Test
    void unJetonModifieEstRefuse() {
        String token = jwtService.genererToken("marie@exemple.fr", "CLIENT");
        String falsifie = token.substring(0, token.length() - 2) + "xx";
        assertThat(jwtService.extraireEmail(falsifie)).isNull();
    }

    @Test
    void unJetonSigneAvecUneAutreCleEstRefuse() {
        JwtService autre = new JwtService("une-autre-cle-secrete-tres-longue-pour-le-test-987654", 60);
        String token = autre.genererToken("pirate@exemple.fr", "ADMIN");
        assertThat(jwtService.extraireEmail(token)).isNull();
    }

    @Test
    void unJetonExpireEstRefuse() {
        JwtService expireImmediatement =
                new JwtService("cle-de-test-suffisamment-longue-pour-hs256-0123456789", -1);
        String token = expireImmediatement.genererToken("marie@exemple.fr", "CLIENT");
        assertThat(jwtService.extraireEmail(token)).isNull();
    }
}
