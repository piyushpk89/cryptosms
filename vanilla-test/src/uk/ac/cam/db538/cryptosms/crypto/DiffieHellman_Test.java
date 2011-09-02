package uk.ac.cam.db538.cryptosms.crypto;

import java.math.BigInteger;

import junit.framework.TestCase;

public class DiffieHellman_Test extends TestCase {
	
	BigInteger modulusValid = DiffieHellman.DEFAULT_MODULUS;
	BigInteger keyMaxValid = DiffieHellman.DEFAULT_KEY_MAX;
	BigInteger generatorValid = DiffieHellman.DEFAULT_GENERATOR;
	
	public void setUp() {
		Encryption.setEncryption(new EncryptionNone());
	}

	public void testConstructorRejectsInvalidInput() {
		BigInteger modulusTooShort
			= new BigInteger("38459096831174158002160789619337146248747508748888690184583179012637052776288926291752288276058943383380188860452613827531647698542232472656230144910812489933419469622978160973438843198612110939605861663913568383679142750525591975305112037426752202861125690148529907024584539036064169186919636979149827425554424686648536134122249857318220273298992676152289939123998875863459205465814447926336115451499973253858981932649245745300842758636627144923787984826644990021138537162123569880364332559511979370230903817240092272277138577865788688912163531932476056745658760579468183824875456526436300143608431237396626288301091967251230608034274499915514626204404758706996418944679836984230022679222732959066557856597852615893472891084054152083987076421499582020455307333599014268638284465487510490271905066723284872349624491270111210742763274004476924755577746766481414359381384449175724817074485995607555010136951165202506089153385402");
		BigInteger modulusTooLong
			= new BigInteger("58753262325017786883898235877401329812866814863313685739009635651654439234220663218052644769798727383888914934887381248422872596942172003810789170160936508747968989610607705251495813667023578005160403777324385130044248539900810549530855284040110683938704983858063731931684406189357605900624130447855003287788958932601047611462846844898328456807090738965116465037753473596365651595298699164007191217984009067009974831847664099010746766175355035710559293493594555873125475801481311427590935318980378393148363083377391812827747181124844218603697917188988011362023793727438096045545342002268770865402708917703214219015481695869454912181760153276167196623883051796715795733590076853414418396262618447148121247519401835230665181200253468703348115905645814142535584871909139419022161161000502326810982683414317781368292787189423479116209326521500682354212255715653587063741940783346106707240537182262016506291684811748079633465870431");
		BigInteger modulusRightSizeNotPrime
			= new BigInteger("4444197340601933085942049760646781752449715362287439773772767530554907815103711527603247842462004848542232823644733191864089186072351956659833333964062644138035808744253269549662401565813772142815798105839348475153462054497099333646547417173801686791388720670186478604791277584538749216635039092179665097906032354952880795434895527998981480775432126241230743519990466386631915076870144564546434859532003058678650640224680779891347317089551364784660493599979512756018301237311753588471742407784049442213866438912736785003287820375872326481469118366219338500135573318901046117537473936670757068226287257973905827048406582175138537737943329618563953852444859803257191155414737508073012119903524191894715116342368449106210944810675044077351298583140623854186833659197836828525659489147642485866534374418238958860468533154182126841202848929044745543078756788399212829295187769331932407611893546238500026815378589365609363033080763");
		BigInteger modulusRightSizeAndPrime
			= new BigInteger("4444197340601933085942049760646781752449715362287439773772767530554907815103711527603247842462004848542232823644733191864089186072351956659833333964062644138035808744253269549662401565813772142815798105839348475153462054497099333646547417173801686791388720670186478604791277584538749216635039092179665097906032354952880795434895527998981480775432126241230743519990466386631915076870144564546434859532003058678650640224680779891347317089551364784660493599979512756018301237311753588471742407784049442213866438912736785003287820375872326481469118366219338500135573318901046117537473936670757068226287257973905827048406582175138537737943329618563953852444859803257191155414737508073012119903524191894715116342368449106210944810675044077351298583140623854186833659197836828525659489147642485866534374418238958860468533154182126841202848929044745543078756788399212829295187769331932407611893546238500026815378589365609363033080767");
		
		BigInteger keyMaxTooShort
			= new BigInteger("5789604461865809771178549250434395392663499233282028201972879200395656481994");
		BigInteger keyMaxRightSizeNotPrime
			= new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639749");
		BigInteger keyMaxRightSizeAndPrime
			= new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639747");

		try {
			new DiffieHellman(modulusTooShort, keyMaxValid, generatorValid);
			fail("modulusTooShort");
		} catch (IllegalArgumentException ex) { }
		
		try {
			new DiffieHellman(modulusTooLong, keyMaxValid, generatorValid);
			fail("modulusTooLong");
		} catch (IllegalArgumentException ex) { }
		
		try {
			new DiffieHellman(modulusValid, keyMaxTooShort, generatorValid);
			fail("keyMax too short");
		} catch (IllegalArgumentException ex) { }
		
		try {
			new DiffieHellman(modulusRightSizeNotPrime, keyMaxValid, generatorValid);
			fail("modulus not a prime");
		} catch (IllegalArgumentException ex) { }
		
		try {
			new DiffieHellman(modulusValid, keyMaxRightSizeNotPrime, generatorValid);
			fail("keyMax not a prime");
		} catch (IllegalArgumentException ex) { }
		
		try {
			new DiffieHellman(modulusRightSizeAndPrime, keyMaxRightSizeAndPrime, generatorValid);
			fail("q is not a divisor of p-1");
		} catch (IllegalArgumentException ex) { }
		
		try {
			new DiffieHellman(modulusValid, keyMaxValid, BigInteger.valueOf(1));
			fail("g is not 1");
		} catch (IllegalArgumentException ex) { }

		try {
			new DiffieHellman(modulusValid, keyMaxValid, BigInteger.valueOf(7));
			fail("g^q % p is not 1");
		} catch (IllegalArgumentException ex) { }

		// valid input
		new DiffieHellman(modulusValid, keyMaxValid, generatorValid);
	}
	
	public void testSetPrivateKey() {
		DiffieHellman dh = new DiffieHellman();
		
		try {
			dh.setPrivateKey(BigInteger.ONE);
			fail("Private key can't be 1");
		} catch (IllegalArgumentException ex) { }

		try {
			dh.setPrivateKey(BigInteger.ZERO);
			fail("Private key can't be 0");
		} catch (IllegalArgumentException ex) { }

		try {
			dh.setPrivateKey(DiffieHellman.DEFAULT_KEY_MAX.subtract(BigInteger.ONE));
			fail("Private key can't be greater or equal to q-1");
		} catch (IllegalArgumentException ex) { }

		try {
			dh.setPrivateKey(BigInteger.valueOf(-2));
			fail("Private key can't be negative");
		} catch (IllegalArgumentException ex) { }

		dh.setPrivateKey(DiffieHellman.DEFAULT_KEY_MAX.subtract(BigInteger.valueOf(2)));
		dh.setPrivateKey(BigInteger.valueOf(2));
	}
	
	public void testKeyExchange() {
		for (int i = 0; i < 50; ++i) {
			DiffieHellman alice = new DiffieHellman();
			DiffieHellman bob = new DiffieHellman();
			
			alice.generateKeys();
			bob.generateKeys();
			
			BigInteger publicAlice = alice.getPublicKey();
			BigInteger publicBob = bob.getPublicKey();
			
			BigInteger sharedAlice = alice.getSharedKey(publicBob);
			BigInteger sharedBob = bob.getSharedKey(publicAlice);
			
			assertEquals(sharedAlice.toString(), sharedBob.toString());
		}
	}
}