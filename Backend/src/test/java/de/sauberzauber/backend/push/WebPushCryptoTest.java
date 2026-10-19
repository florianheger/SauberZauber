package de.sauberzauber.backend.push;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import org.junit.jupiter.api.Test;

import static de.sauberzauber.backend.push.WebPushCrypto.decode;
import static org.assertj.core.api.Assertions.assertThat;

class WebPushCryptoTest {

	/** The example from RFC 8291, Appendix A. */
	@Test
	void encryptsLikeTheRfcExample() throws Exception {
		KeyPair asKeyPair = new KeyPair(
				WebPushCrypto.publicKey(decode(
						"BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8")),
				WebPushCrypto.privateKey(decode("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw")));

		byte[] body = WebPushCrypto.encrypt("When I grow up, I want to be a watermelon".getBytes(StandardCharsets.US_ASCII),
				decode("BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4"),
				decode("BTBZMqHH6r4Tts7J_aSIgg"), asKeyPair, decode("DGv6ra1nlYgDCS1FRnbzlw"));

		assertThat(WebPushCrypto.encode(body)).isEqualTo("DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy"
				+ "27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPX"
				+ "LWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN");
	}

}
