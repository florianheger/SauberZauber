package de.sauberzauber.backend.push;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Message encryption for Web Push (RFC 8291, {@code aes128gcm}) and P-256 key handling, using only the JDK.
 */
final class WebPushCrypto {

	private static final SecureRandom RANDOM = new SecureRandom();

	private static final int RECORD_SIZE = 4096;

	private static final ECParameterSpec P256;

	static {
		try {
			AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
			parameters.init(new ECGenParameterSpec("secp256r1"));
			P256 = parameters.getParameterSpec(ECParameterSpec.class);
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private WebPushCrypto() {
	}

	/** Encrypts {@code plaintext} for the subscription's public key and auth secret (both base64url). */
	static byte[] encrypt(byte[] plaintext, String uaPublic, String authSecret) throws GeneralSecurityException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(P256, RANDOM);
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		return encrypt(plaintext, decode(uaPublic), decode(authSecret), generator.generateKeyPair(), salt);
	}

	/** RFC 8291 section 3.4, with a given application server key pair and salt (for tests). */
	static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret, KeyPair asKeyPair, byte[] salt)
			throws GeneralSecurityException {
		byte[] asPublic = encodePoint((ECPublicKey) asKeyPair.getPublic());
		KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
		agreement.init(asKeyPair.getPrivate());
		agreement.doPhase(publicKey(uaPublic), true);
		byte[] ecdhSecret = agreement.generateSecret();

		byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, asPublic);
		byte[] ikm = hkdf(authSecret, ecdhSecret, keyInfo, 32);
		byte[] cek = hkdf(salt, ikm, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
		byte[] nonce = hkdf(salt, ikm, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
		// single record: the plaintext followed by the last-record delimiter 0x02
		byte[] ciphertext = cipher.doFinal(concat(plaintext, new byte[] { 2 }));
		if (ciphertext.length > RECORD_SIZE) {
			throw new IllegalArgumentException("Push message too large");
		}

		return ByteBuffer.allocate(16 + 4 + 1 + asPublic.length + ciphertext.length)
			.put(salt)
			.putInt(RECORD_SIZE)
			.put((byte) asPublic.length)
			.put(asPublic)
			.put(ciphertext)
			.array();
	}

	/** HKDF-SHA256 (RFC 5869) for output lengths up to one hash block. */
	private static byte[] hkdf(byte[] salt, byte[] ikm, byte[] info, int length) throws GeneralSecurityException {
		byte[] prk = hmac(salt, ikm);
		return Arrays.copyOf(hmac(prk, concat(info, new byte[] { 1 })), length);
	}

	private static byte[] hmac(byte[] key, byte[] data) throws GeneralSecurityException {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(key, "HmacSHA256"));
		return mac.doFinal(data);
	}

	/** Parses an uncompressed P-256 point (65 bytes, {@code 0x04 || x || y}). */
	static ECPublicKey publicKey(byte[] point) throws GeneralSecurityException {
		if (point.length != 65 || point[0] != 4) {
			throw new IllegalArgumentException("Not an uncompressed P-256 public key");
		}
		ECPoint w = new ECPoint(new BigInteger(1, Arrays.copyOfRange(point, 1, 33)),
				new BigInteger(1, Arrays.copyOfRange(point, 33, 65)));
		return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(w, P256));
	}

	/** Parses a raw 32-byte P-256 private key. */
	static ECPrivateKey privateKey(byte[] scalar) throws GeneralSecurityException {
		if (scalar.length != 32) {
			throw new IllegalArgumentException("Not a raw P-256 private key");
		}
		return (ECPrivateKey) KeyFactory.getInstance("EC")
			.generatePrivate(new ECPrivateKeySpec(new BigInteger(1, scalar), P256));
	}

	static byte[] encodePoint(ECPublicKey key) {
		return concat(new byte[] { 4 }, unsigned32(key.getW().getAffineX()), unsigned32(key.getW().getAffineY()));
	}

	private static byte[] unsigned32(BigInteger value) {
		byte[] bytes = value.toByteArray();
		byte[] result = new byte[32];
		int length = Math.min(bytes.length, 32);
		System.arraycopy(bytes, bytes.length - length, result, 32 - length, length);
		return result;
	}

	static byte[] decode(String base64url) {
		return Base64.getUrlDecoder().decode(base64url.replace('+', '-').replace('/', '_').replace("=", ""));
	}

	static String encode(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static byte[] concat(byte[]... parts) {
		ByteBuffer buffer = ByteBuffer.allocate(Arrays.stream(parts).mapToInt((part) -> part.length).sum());
		for (byte[] part : parts) {
			buffer.put(part);
		}
		return buffer.array();
	}

}
